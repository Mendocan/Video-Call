package com.videocall.app.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "videocall_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_FIRST_LAUNCH = "first_launch"
        private const val KEY_PHONE_NUMBER = "phone_number"
        private const val KEY_TERMS_ACCEPTED = "terms_accepted"
        private const val KEY_ADDED_CONTACTS = "added_contacts"
        private const val KEY_BLOCK_UNKNOWN_NUMBERS = "block_unknown_numbers"
        private const val KEY_CALL_HISTORY = "call_history"
        private const val KEY_SUBSCRIPTION_PLAN_ID = "subscription_plan_id"
        private const val KEY_SUBSCRIPTION_EXPIRES_AT = "subscription_expires_at"
        private const val KEY_DAILY_USAGE_MINUTES = "daily_usage_minutes"
        private const val KEY_DAILY_USAGE_DATE = "daily_usage_date"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_USER_STATUS = "user_status"
        private const val KEY_CUSTOM_STATUS_MESSAGE = "custom_status_message"
        private const val KEY_AUTO_CLEANUP_ENABLED = "auto_cleanup_enabled"
        private const val KEY_AUTO_CLEANUP_DAYS = "auto_cleanup_days"
        private const val KEY_LAST_CLEANUP_DATE = "last_cleanup_date"
        private const val KEY_PRIVACY_MODE_ENABLED = "privacy_mode_enabled"
        private const val KEY_SCHEDULED_CALLS = "scheduled_calls"
        private const val KEY_2FA_ENABLED = "2fa_enabled"
        private const val KEY_OFFLINE_MODE_ENABLED = "offline_mode_enabled"
        private const val KEY_VOICE_COMMANDS_ENABLED = "voice_commands_enabled"
        private const val KEY_2FA_SECRET = "2fa_secret"
        private const val KEY_2FA_BACKUP_CODES = "2fa_backup_codes"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_BLOCKED_USERS = "blocked_users"
        private const val KEY_CHAT_BACKGROUND_COLOR = "chat_background_color"
        private const val KEY_VIDEO_QUALITY = "video_quality"
        private const val KEY_PROFILE_PHOTO_URI = "profile_photo_uri"
    }

    fun isFirstLaunch(): Boolean {
        return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchCompleted() {
        prefs.edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }

    fun savePhoneNumber(phoneNumber: String) {
        prefs.edit().putString(KEY_PHONE_NUMBER, phoneNumber).apply()
    }

    fun getPhoneNumber(): String? {
        return prefs.getString(KEY_PHONE_NUMBER, null)
    }

    fun setTermsAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_TERMS_ACCEPTED, accepted).apply()
    }

    fun isTermsAccepted(): Boolean {
        return prefs.getBoolean(KEY_TERMS_ACCEPTED, false)
    }

    fun saveAddedContacts(contactsJson: String) {
        prefs.edit().putString(KEY_ADDED_CONTACTS, contactsJson).apply()
    }

    fun getAddedContacts(): String? {
        return prefs.getString(KEY_ADDED_CONTACTS, null)
    }

    fun setBlockUnknownNumbers(block: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCK_UNKNOWN_NUMBERS, block).apply()
    }

    fun isBlockUnknownNumbers(): Boolean {
        return prefs.getBoolean(KEY_BLOCK_UNKNOWN_NUMBERS, false)
    }

    fun saveCallHistory(historyJson: String) {
        prefs.edit().putString(KEY_CALL_HISTORY, historyJson).apply()
    }

    fun getCallHistory(): String? {
        return prefs.getString(KEY_CALL_HISTORY, null)
    }

    // Subscription Management
    fun saveSubscriptionPlan(planId: String) {
        prefs.edit().putString(KEY_SUBSCRIPTION_PLAN_ID, planId).apply()
    }

    fun getSubscriptionPlan(): String? {
        return prefs.getString(KEY_SUBSCRIPTION_PLAN_ID, null)
    }

    fun saveSubscriptionExpiresAt(expiresAt: Long) {
        prefs.edit().putLong(KEY_SUBSCRIPTION_EXPIRES_AT, expiresAt).apply()
    }

    fun getSubscriptionExpiresAt(): Long {
        return prefs.getLong(KEY_SUBSCRIPTION_EXPIRES_AT, 0)
    }

    fun isPremium(): Boolean {
        val planId = getSubscriptionPlan()
        val expiresAt = getSubscriptionExpiresAt()
        
        if (planId == null) {
            return false
        }
        
        // Abonelik süresi dolmuş mu kontrol et
        if (expiresAt > 0 && expiresAt < System.currentTimeMillis()) {
            // Süre dolmuş, abonelik yok
            saveSubscriptionPlan("")
            return false
        }
        
        // Geçerli plan ID'leri: 3months, 6months, 12months
        return planId == "3months" || planId == "6months" || planId == "12months"
    }

    // Daily Usage Tracking (for free plan limit)
    fun getDailyUsageMinutes(): Int {
        val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24) // Days since epoch
        val lastUsageDate = prefs.getLong(KEY_DAILY_USAGE_DATE, 0)
        
        // Eğer bugün değilse, sıfırla
        if (lastUsageDate != today) {
            prefs.edit().putInt(KEY_DAILY_USAGE_MINUTES, 0).putLong(KEY_DAILY_USAGE_DATE, today).apply()
            return 0
        }
        
        return prefs.getInt(KEY_DAILY_USAGE_MINUTES, 0)
    }

    fun addDailyUsageMinutes(minutes: Int) {
        val currentMinutes = getDailyUsageMinutes()
        val today = System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        prefs.edit()
            .putInt(KEY_DAILY_USAGE_MINUTES, currentMinutes + minutes)
            .putLong(KEY_DAILY_USAGE_DATE, today)
            .apply()
    }

    fun canMakeCall(durationMinutes: Int): Boolean {
        // Abonelik varsa ve süresi dolmamışsa görüşme yapabilir
        return isPremium()
    }

    // Device ID Management (APK paylaşımını önlemek için)
    fun saveDeviceId(deviceId: String) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }

    fun getDeviceId(): String? {
        return prefs.getString(KEY_DEVICE_ID, null)
    }
    
    // User Status Management
    fun saveUserStatus(status: String) {
        prefs.edit().putString(KEY_USER_STATUS, status).apply()
    }
    
    fun getUserStatus(): String {
        return prefs.getString(KEY_USER_STATUS, "ONLINE") ?: "ONLINE"
    }
    
    fun saveCustomStatusMessage(message: String?) {
        if (message.isNullOrBlank()) {
            prefs.edit().remove(KEY_CUSTOM_STATUS_MESSAGE).apply()
        } else {
            prefs.edit().putString(KEY_CUSTOM_STATUS_MESSAGE, message).apply()
        }
    }
    
    fun getCustomStatusMessage(): String? {
        return prefs.getString(KEY_CUSTOM_STATUS_MESSAGE, null)
    }
    
    // Auto Cleanup Settings
    fun setAutoCleanupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_CLEANUP_ENABLED, enabled).apply()
    }
    
    fun isAutoCleanupEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_CLEANUP_ENABLED, false)
    }
    
    fun setAutoCleanupDays(days: Int) {
        prefs.edit().putInt(KEY_AUTO_CLEANUP_DAYS, days).apply()
    }
    
    fun getAutoCleanupDays(): Int {
        return prefs.getInt(KEY_AUTO_CLEANUP_DAYS, 30) // Varsayılan: 30 gün
    }
    
    fun setLastCleanupDate(date: Long) {
        prefs.edit().putLong(KEY_LAST_CLEANUP_DATE, date).apply()
    }
    
    fun getLastCleanupDate(): Long {
        return prefs.getLong(KEY_LAST_CLEANUP_DATE, 0)
    }
    
    // Privacy Mode Settings
    fun setPrivacyModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRIVACY_MODE_ENABLED, enabled).apply()
    }
    
    fun isPrivacyModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_PRIVACY_MODE_ENABLED, false)
    }
    
    // Scheduled Calls Management
    fun saveScheduledCalls(callsJson: String) {
        prefs.edit().putString(KEY_SCHEDULED_CALLS, callsJson).apply()
    }
    
    fun getScheduledCalls(): String? {
        return prefs.getString(KEY_SCHEDULED_CALLS, null)
    }
    
    // Two-Factor Authentication (2FA) Management
    fun set2FAEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_2FA_ENABLED, enabled).apply()
    }
    
    fun is2FAEnabled(): Boolean {
        return prefs.getBoolean(KEY_2FA_ENABLED, false)
    }
    
    fun save2FASecret(secret: String) {
        prefs.edit().putString(KEY_2FA_SECRET, secret).apply()
    }
    
    fun get2FASecret(): String? {
        return prefs.getString(KEY_2FA_SECRET, null)
    }
    
    fun save2FABackupCodes(codes: List<String>) {
        val codesJson = codes.joinToString(",")
        prefs.edit().putString(KEY_2FA_BACKUP_CODES, codesJson).apply()
    }
    
    fun get2FABackupCodes(): List<String> {
        val codesJson = prefs.getString(KEY_2FA_BACKUP_CODES, null) ?: return emptyList()
        return if (codesJson.isNotEmpty()) {
            codesJson.split(",")
        } else {
            emptyList()
        }
    }
    
    fun use2FABackupCode(code: String): Boolean {
        val codes = get2FABackupCodes().toMutableList()
        val index = codes.indexOf(code)
        if (index != -1) {
            codes.removeAt(index)
            save2FABackupCodes(codes)
            return true
        }
        return false
    }
    
    // Offline Mode Settings
    fun setOfflineModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_OFFLINE_MODE_ENABLED, enabled).apply()
    }
    
    fun isOfflineModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_OFFLINE_MODE_ENABLED, false)
    }
    
    // Voice Commands Settings
    fun setVoiceCommandsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_VOICE_COMMANDS_ENABLED, enabled).apply()
    }
    
    fun isVoiceCommandsEnabled(): Boolean {
        return prefs.getBoolean(KEY_VOICE_COMMANDS_ENABLED, false)
    }
    
    // Language Management
    fun setLanguage(languageCode: String) {
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
    
    fun getLanguage(): String? {
        return prefs.getString(KEY_LANGUAGE, null)
    }

    // Engellenenler listesi yönetimi
    fun isUserBlocked(phoneNumber: String): Boolean {
        val blockedUsersJson = prefs.getString(KEY_BLOCKED_USERS, null) ?: return false
        if (blockedUsersJson.isEmpty()) return false
        
        try {
            val blockedList = blockedUsersJson.split(",")
            return blockedList.contains(phoneNumber)
        } catch (e: Exception) {
            return false
        }
    }

    fun blockUser(phoneNumber: String) {
        val currentList = getBlockedUsers().toMutableSet()
        currentList.add(phoneNumber)
        saveBlockedUsers(currentList.toList())
    }

    fun unblockUser(phoneNumber: String) {
        val currentList = getBlockedUsers().toMutableSet()
        currentList.remove(phoneNumber)
        saveBlockedUsers(currentList.toList())
    }

    fun getBlockedUsers(): List<String> {
        val blockedUsersJson = prefs.getString(KEY_BLOCKED_USERS, null) ?: return emptyList()
        if (blockedUsersJson.isEmpty()) return emptyList()
        
        try {
            return blockedUsersJson.split(",").filter { it.isNotEmpty() }
        } catch (e: Exception) {
            return emptyList()
        }
    }

    private fun saveBlockedUsers(phoneNumbers: List<String>) {
        val json = phoneNumbers.joinToString(",")
        prefs.edit().putString(KEY_BLOCKED_USERS, json).apply()
    }

    fun getUserName(): String? {
        // Ekli kişilerden kendi telefon numarasına göre isim bul
        val phoneNumber = getPhoneNumber() ?: return null
        val contactsJson = getAddedContacts() ?: return null
        
        try {
            // Basit JSON parse (gerçek implementasyonda JSONObject kullanılabilir)
            // Şimdilik null döndür, ViewModel'de zaten kontrol ediliyor
            return null
        } catch (e: Exception) {
            return null
        }
    }
    
    // Chat Background Color Management
    fun saveChatBackgroundColor(colorHex: String) {
        prefs.edit().putString(KEY_CHAT_BACKGROUND_COLOR, colorHex).apply()
    }
    
    fun getChatBackgroundColor(): String? {
        return prefs.getString(KEY_CHAT_BACKGROUND_COLOR, null)
    }
    
    // Video Quality Management
    fun saveVideoQuality(quality: String) {
        prefs.edit().putString(KEY_VIDEO_QUALITY, quality).apply()
    }
    
    fun getVideoQuality(): String {
        return prefs.getString(KEY_VIDEO_QUALITY, "HIGH") ?: "HIGH"
    }
    
    // Profile Photo Management
    fun saveProfilePhotoUri(photoUri: String) {
        prefs.edit().putString(KEY_PROFILE_PHOTO_URI, photoUri).apply()
    }
    
    fun getProfilePhotoUri(): String? {
        return prefs.getString(KEY_PROFILE_PHOTO_URI, null)
    }
    
    fun removeProfilePhoto() {
        prefs.edit().remove(KEY_PROFILE_PHOTO_URI).apply()
    }
}

