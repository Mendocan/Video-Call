@file:Suppress("UNUSED_PARAMETER", "UNUSED_ANONYMOUS_PARAMETER")

package com.videocall.app.data

import android.content.Context
import com.videocall.app.network.SecureHttpClient
import com.videocall.app.utils.DeviceIdManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

data class SubscriptionStatus(
    val planId: String,
    val isPremium: Boolean,
    val expiresAt: Long?,
    val status: String
)

data class DeviceVerificationResult(
    val isAuthorized: Boolean,
    val message: String,
    val deviceLimitReached: Boolean = false
)

class SubscriptionManager(private val context: Context) {
    private val preferencesManager = PreferencesManager(context)
    private val deviceIdManager = DeviceIdManager(context)
    private val apiBaseUrl = "http://localhost:3000" // TODO: Production URL'e değiştirilecek
    private val httpClient = SecureHttpClient.getClient() // Güvenli HTTP istemcisi

    /**
     * Backend'den abonelik durumunu kontrol eder
     */
    suspend fun checkSubscriptionStatus(): SubscriptionStatus = withContext(Dispatchers.IO) {
        val phoneNumber = preferencesManager.getPhoneNumber() ?: return@withContext SubscriptionStatus(
            planId = "",
            isPremium = false,
            expiresAt = null,
            status = "no_user"
        )

        try {
            val request = Request.Builder()
                .url("$apiBaseUrl/api/subscription/$phoneNumber")
                .get()
                .build()
            
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: return@withContext SubscriptionStatus(
                    planId = "",
                    isPremium = false,
                    expiresAt = null,
                    status = "no_response"
                )
                val json = JSONObject(responseBody)
                
                if (json.getBoolean("success")) {
                    val subscriptionJson = json.getJSONObject("subscription")
                    val planId = subscriptionJson.getString("planId")
                    val isPremium = subscriptionJson.getBoolean("isPremium")
                    val expiresAtStr = subscriptionJson.optString("expiresAt", "").takeIf { it != "null" && it.isNotEmpty() }
                    
                    val expiresAt = expiresAtStr?.let {
                        java.time.Instant.parse(it).toEpochMilli()
                    }
                    
                    // Local storage'a kaydet
                    preferencesManager.saveSubscriptionPlan(planId)
                    if (expiresAt != null) {
                        preferencesManager.saveSubscriptionExpiresAt(expiresAt)
                    }
                    
                    return@withContext SubscriptionStatus(
                        planId = planId,
                        isPremium = isPremium,
                        expiresAt = expiresAt,
                        status = subscriptionJson.getString("status")
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SubscriptionManager", "Abonelik durumu kontrolü hatası", e)
        }

        // Hata durumunda local storage'dan oku
        val localPlanId = preferencesManager.getSubscriptionPlan() ?: ""
        val localExpiresAt = preferencesManager.getSubscriptionExpiresAt()
        
        SubscriptionStatus(
            planId = localPlanId,
            isPremium = preferencesManager.isPremium(),
            expiresAt = if (localExpiresAt > 0) localExpiresAt else null,
            status = "offline"
        )
    }

    /**
     * Kullanıcının premium aboneliği var mı kontrol eder
     */
    fun isPremium(): Boolean {
        return preferencesManager.isPremium()
    }

    /**
     * Görüşme yapılabilir mi kontrol eder (günlük limit kontrolü)
     */
    fun canMakeCall(durationMinutes: Int): Boolean {
        return preferencesManager.canMakeCall(durationMinutes)
    }

    /**
     * Abonelik durumunu kontrol eder ve görüşme yapılabilir mi döndürür
     */
    fun hasActiveSubscription(): Boolean {
        return isPremium()
    }

    /**
     * Cihazı backend'de kaydet ve doğrula (APK paylaşımını önlemek için)
     */
    suspend fun registerDevice(phoneNumber: String): DeviceVerificationResult = withContext(Dispatchers.IO) {
        val deviceId = deviceIdManager.getDeviceId()
        val hashedDeviceId = deviceIdManager.getHashedDeviceId()
        
        try {
            val requestBody = JSONObject().apply {
                put("phoneNumber", phoneNumber)
                put("deviceId", hashedDeviceId)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$apiBaseUrl/api/devices/register")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = if (response.isSuccessful) {
                response.body?.string() ?: "{}"
            } else {
                response.body?.string() ?: "{}"
            }

            val json = JSONObject(responseBody)
            
            if (json.getBoolean("success")) {
                // Cihaz ID'yi local storage'a kaydet
                preferencesManager.saveDeviceId(deviceId)
                
                return@withContext DeviceVerificationResult(
                    isAuthorized = true,
                    message = json.optString("message", "Cihaz başarıyla kaydedildi")
                )
            } else {
                val errorMessage = json.optString("error", "Cihaz kaydı başarısız")
                val deviceLimitReached = errorMessage.contains("limit") || errorMessage.contains("maksimum")
                
                return@withContext DeviceVerificationResult(
                    isAuthorized = false,
                    message = errorMessage,
                    deviceLimitReached = deviceLimitReached
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("SubscriptionManager", "Cihaz kaydı hatası", e)
            return@withContext DeviceVerificationResult(
                isAuthorized = false,
                message = "Bağlantı hatası: ${e.message}"
            )
        }
    }

    /**
     * Cihazın yetkili olup olmadığını kontrol et
     */
    suspend fun verifyDevice(phoneNumber: String): DeviceVerificationResult = withContext(Dispatchers.IO) {
        val deviceId = deviceIdManager.getDeviceId()
        val hashedDeviceId = deviceIdManager.getHashedDeviceId()
        
        // Local storage'da cihaz ID yoksa, kayıt yap
        val savedDeviceId = preferencesManager.getDeviceId()
        if (savedDeviceId == null || savedDeviceId != deviceId) {
            return@withContext registerDevice(phoneNumber)
        }
        
        try {
            val requestBody = JSONObject().apply {
                put("phoneNumber", phoneNumber)
                put("deviceId", hashedDeviceId)
            }.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$apiBaseUrl/api/devices/verify")
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = if (response.isSuccessful) {
                response.body?.string() ?: "{}"
            } else {
                response.body?.string() ?: "{}"
            }

            val json = JSONObject(responseBody)
            
            if (json.getBoolean("success")) {
                return@withContext DeviceVerificationResult(
                    isAuthorized = true,
                    message = "Cihaz yetkili"
                )
            } else {
                val errorMessage = json.optString("error", "Cihaz yetkisiz")
                val deviceLimitReached = errorMessage.contains("limit") || errorMessage.contains("maksimum")
                
                return@withContext DeviceVerificationResult(
                    isAuthorized = false,
                    message = errorMessage,
                    deviceLimitReached = deviceLimitReached
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("SubscriptionManager", "Cihaz doğrulama hatası", e)
            // Offline durumda, local storage'daki cihaz ID ile kontrol et
            val savedDeviceId = preferencesManager.getDeviceId()
            if (savedDeviceId == deviceId) {
                return@withContext DeviceVerificationResult(
                    isAuthorized = true,
                    message = "Offline mod: Cihaz yetkili (önceki kayıt)"
                )
            }
            
            return@withContext DeviceVerificationResult(
                isAuthorized = false,
                message = "Bağlantı hatası: ${e.message}"
            )
        }
    }
}

