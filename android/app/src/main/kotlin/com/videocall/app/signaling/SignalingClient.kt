package com.videocall.app.signaling

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import com.videocall.app.directcall.ice.DirectCallIceCandidate
import java.util.concurrent.TimeUnit

class SignalingClient(
    private val url: String,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS) // Render.com uyku modu için uzun timeout
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .pingInterval(20, TimeUnit.SECONDS) // Android Doze mode için 20 saniye (10 saniye çok sık)
        .retryOnConnectionFailure(true)
        .build()
) {

    private var webSocket: WebSocket? = null
    private var pendingOpen: CompletableDeferred<Unit>? = null
    private var activeRoom: String? = null
    private var isRegistered: Boolean = false
    private var pendingRegistration: CompletableDeferred<Unit>? = null // Registration synchronization
    
    // Reconnect mekanizması
    private var shouldReconnect: Boolean = false
    private var reconnectAttempts: Int = 0
    private val maxReconnectAttempts: Int = 5
    private val reconnectDelayMs: Long = 2000 // 2 saniye
    private var registeredPhoneNumber: String? = null
    private var registeredName: String? = null
    private val reconnectScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _messages = MutableSharedFlow<SignalingMessage>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<SignalingMessage> = _messages.asSharedFlow()

    private val _status = MutableStateFlow<SignalingStatus>(SignalingStatus.Disconnected)
    val status: StateFlow<SignalingStatus> = _status.asStateFlow()

    // Yeni bağlantı (room code olmadan)
    suspend fun connect() {
        if (_status.value is SignalingStatus.Connected && isRegistered) return
        close()
        _status.value = SignalingStatus.Connecting("")
        val request = Request.Builder()
            .url(url)
            .build()
        val deferred = CompletableDeferred<Unit>()
        pendingOpen = deferred
        webSocket = okHttpClient.newWebSocket(request, createListener())
        deferred.await()
    }

    // Geriye dönük uyumluluk için (room code ile bağlantı - eski sistem)
    suspend fun connect(roomCode: String) {
        if (_status.value is SignalingStatus.Connected && roomCode == activeRoom) return
        close()
        _status.value = SignalingStatus.Connecting(roomCode)
        val request = Request.Builder()
            .url("$url?room=$roomCode")
            .build()
        val deferred = CompletableDeferred<Unit>()
        pendingOpen = deferred
        webSocket = okHttpClient.newWebSocket(request, createListener(roomCode))
        deferred.await()
    }

    // Kullanıcı kaydı (OTP doğrulama kaldırıldı)
    suspend fun register(phoneNumber: String, name: String?) {
        // Kayıt bilgilerini sakla (reconnect için)
        registeredPhoneNumber = phoneNumber
        registeredName = name
        
        // WebSocket bağlantısını kontrol et
        val currentStatus = _status.value
        if (currentStatus !is SignalingStatus.Connected) {
            android.util.Log.d("SignalingClient", "Register: WebSocket bağlantısı yok, bağlanılıyor...")
            connect()
            // Bağlantının kurulmasını bekle
            var waitCount = 0
            while (_status.value !is SignalingStatus.Connected && waitCount < 50) {
                delay(100)
                waitCount++
                kotlinx.coroutines.yield()
            }
            if (_status.value !is SignalingStatus.Connected) {
                android.util.Log.e("SignalingClient", "Register: WebSocket bağlantısı kurulamadı")
                throw IllegalStateException("WebSocket connection failed")
            }
        }
        
        // WebSocket'in hazır olduğundan emin ol
        if (webSocket == null) {
            android.util.Log.e("SignalingClient", "Register: WebSocket null, bağlantı kurulamadı")
            throw IllegalStateException("WebSocket is null")
        }
        
        android.util.Log.d("SignalingClient", "Register: WebSocket bağlantısı hazır, status=$currentStatus, isRegistered=$isRegistered, webSocket=${if (webSocket != null) "NOT_NULL" else "NULL"}")
        
        // Eğer zaten kayıtlıysa tekrar kayıt yapma
        if (isRegistered) {
            android.util.Log.d("SignalingClient", "Register: Zaten kayıtlı, tekrar kayıt yapılmıyor")
            return
        }
        
        // Registration için CompletableDeferred oluştur
        pendingRegistration = CompletableDeferred()
        
        android.util.Log.d("SignalingClient", "Register mesajı gönderiliyor: phoneNumber=$phoneNumber, name=$name")
        send(SignalingMessage.Register(phoneNumber, name, null))
        android.util.Log.d("SignalingClient", "Register mesajı gönderildi, yanıt bekleniyor...")
        
        // Registration tamamlanana kadar bekle (timeout ile)
        try {
            kotlinx.coroutines.withTimeout(10000) { // 10 saniye timeout (5 saniye yeterli değil)
                pendingRegistration?.await()
            }
            android.util.Log.d("SignalingClient", "Register: Registration başarılı, isRegistered=$isRegistered")
        } catch (e: Exception) {
            android.util.Log.e("SignalingClient", "Register: Registration timeout veya hata", e)
            pendingRegistration = null
            throw e
        }
    }
    
    // Otomatik reconnect başlat
    private fun startReconnect() {
        if (!shouldReconnect || reconnectAttempts >= maxReconnectAttempts) {
            shouldReconnect = false
            reconnectAttempts = 0
            return
        }
        
        reconnectAttempts++
        android.util.Log.d("SignalingClient", "Yeniden bağlanma denemesi $reconnectAttempts/$maxReconnectAttempts")
        
        reconnectScope.launch {
            delay(reconnectDelayMs * reconnectAttempts) // Exponential backoff
            if (shouldReconnect) {
                try {
                    connect()
                    // Bağlantı başarılıysa tekrar register ol
                    registeredPhoneNumber?.let { phone ->
                        register(phone, registeredName)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SignalingClient", "Reconnect başarısız", e)
                    if (reconnectAttempts < maxReconnectAttempts) {
                        startReconnect()
                    }
                }
            }
        }
    }
    
    // Reconnect'i durdur
    fun stopReconnect() {
        shouldReconnect = false
        reconnectAttempts = 0
    }
    
    // Reconnect'i başlat
    fun enableReconnect() {
        shouldReconnect = true
        reconnectAttempts = 0
    }

    fun sendOffer(sdp: String) {
        send(SignalingMessage.Offer(sdp))
    }

    fun sendAnswer(sdp: String) {
        send(SignalingMessage.Answer(sdp))
    }

    fun sendIceCandidate(candidate: DirectCallIceCandidate) {
        // DirectCallIceCandidate'ı SDP formatına çevir
        val candidateString = candidate.toSdpString()
        send(
            SignalingMessage.IceCandidateMessage(
                SignalingMessage.IceCandidatePayload(
                    candidate = candidateString,
                    sdpMid = null, // DirectCall'da sdpMid gerekmez
                    sdpMLineIndex = candidate.componentId
                )
            )
        )
    }

    fun sendChat(message: SignalingMessage.Chat) {
        send(message)
    }

    fun sendFileShare(message: SignalingMessage.FileShare) {
        send(message)
    }
    
    fun sendChatEdit(message: SignalingMessage.ChatEdit) {
        send(message)
    }
    
    fun sendMessageStatusUpdate(message: SignalingMessage.MessageStatusUpdate) {
        send(message)
    }
    
    fun sendChatDelete(message: SignalingMessage.ChatDelete) {
        send(message)
    }
    
    // Canlı yayın fonksiyonları
    fun startLive(
        title: String?,
        targetPhoneNumbers: List<String>?,
        groupIds: List<String>?,
        broadcasterPhoneNumber: String,
        broadcasterName: String?
    ) {
        send(SignalingMessage.StartLive(
            title = title,
            targetPhoneNumbers = targetPhoneNumbers,
            groupIds = groupIds,
            broadcasterPhoneNumber = broadcasterPhoneNumber,
            broadcasterName = broadcasterName
        ))
    }
    
    fun joinLive(liveId: String, viewerPhoneNumber: String, viewerName: String?) {
        send(SignalingMessage.JoinLive(
            liveId = liveId,
            viewerPhoneNumber = viewerPhoneNumber,
            viewerName = viewerName
        ))
    }
    
    fun endLive(liveId: String, broadcasterPhoneNumber: String) {
        send(SignalingMessage.EndLive(
            liveId = liveId,
            broadcasterPhoneNumber = broadcasterPhoneNumber
        ))
    }
    
    // Kayıt durumu bildirimi
    fun sendRecordingStatus(isRecording: Boolean, senderPhoneNumber: String, roomCode: String?) {
        send(SignalingMessage.RecordingStatus(
            isRecording = isRecording,
            senderPhoneNumber = senderPhoneNumber,
            roomCode = roomCode
        ))
    }

    // Bireysel arama başlatma
    suspend fun startCall(targetPhoneNumber: String, callerPhoneNumber: String, callerName: String?) {
        // WebSocket bağlantısı yoksa bağlan
        if (webSocket == null || _status.value !is SignalingStatus.Connected) {
            android.util.Log.d("SignalingClient", "WebSocket bağlantısı yok, bağlanılıyor...")
            connect()
        }
        
        // Register olmamışsa register ol
        if (!isRegistered) {
            android.util.Log.w("SignalingClient", "Kullanıcı kayıtlı değil! Önce registerToBackend() çağrılmalı. Kayıt yapılıyor...")
            // Telefon numarasını callerPhoneNumber'dan al
            try {
                register(callerPhoneNumber, callerName) // Bu artık await ile bekliyor
                android.util.Log.d("SignalingClient", "Kayıt tamamlandı, arama başlatılıyor")
            } catch (e: Exception) {
                android.util.Log.e("SignalingClient", "Kayıt başarısız, arama başlatılamadı", e)
                return
            }
        }
        
        if (webSocket == null) {
            android.util.Log.e("SignalingClient", "WebSocket null, arama başlatılamadı")
            return
        }
        
        val wsStatus = "connected"
        android.util.Log.d("SignalingClient", "Arama başlatılıyor: target=$targetPhoneNumber, caller=$callerPhoneNumber, isRegistered=$isRegistered, WebSocket=$wsStatus")
        send(SignalingMessage.CallRequest(
            targetPhoneNumber = targetPhoneNumber,
            groupId = null,
            callerPhoneNumber = callerPhoneNumber,
            callerName = callerName
        ))
    }

    // Grup araması başlatma
    fun startGroupCall(groupId: String, callerPhoneNumber: String, callerName: String?) {
        send(SignalingMessage.CallRequest(
            targetPhoneNumber = null,
            groupId = groupId,
            callerPhoneNumber = callerPhoneNumber,
            callerName = callerName
        ))
    }

    // Arama kabul
    fun acceptCall(groupId: String) {
        send(SignalingMessage.CallAccept(groupId))
    }

    // Arama reddetme
    fun rejectCall(groupId: String) {
        send(SignalingMessage.CallReject(groupId))
    }

    // Grup oluşturma
    fun createGroup(groupName: String, memberPhoneNumbers: List<String>) {
        send(SignalingMessage.CreateGroup(groupName, memberPhoneNumbers))
    }

    // Gruba katılma
    fun joinGroup(groupId: String) {
        send(SignalingMessage.JoinGroup(groupId))
    }

    // Gruptan ayrılma
    fun leaveGroup(groupId: String) {
        send(SignalingMessage.LeaveGroup(groupId))
    }

    // Kullanıcının gruplarını listele
    fun getGroups() {
        send(SignalingMessage.GetGroups(""))
    }

    // Grup bilgilerini getir
    fun getGroupInfo(groupId: String) {
        send(SignalingMessage.GetGroupInfo(groupId))
    }

    // Kullanıcı durumu sorgulama
    fun getUserStatus(phoneNumber: String) {
        send(SignalingMessage.UserStatusRequest(phoneNumber))
    }

    // Kullanıcı lookup (keşif) - "Bu numara kayıtlı mı?" kontrolü
    fun lookupUser(phoneNumber: String) {
        android.util.Log.d("SignalingClient", "Kullanıcı lookup: phoneNumber=$phoneNumber")
        send(SignalingMessage.UserLookup(phoneNumber))
    }

    // Firebase kaldırıldı - WebSocket kullanıyoruz (bağımsız yapı)

    // Kullanıcı engelleme
    fun blockUser(targetPhoneNumber: String) {
        send(SignalingMessage.BlockUser(targetPhoneNumber))
    }

    // Kullanıcı engelini kaldırma
    fun unblockUser(targetPhoneNumber: String) {
        send(SignalingMessage.UnblockUser(targetPhoneNumber))
    }

    // Engellenenler listesini getir
    fun getBlockedUsers() {
        send(SignalingMessage.GetBlockedUsers(""))
    }

    // Canlı yayın başlat
    fun startLiveStream(
        title: String,
        broadcasterPhoneNumber: String,
        broadcasterName: String?,
        contactPhoneNumbers: List<String> = emptyList(),
        groupIds: List<String> = emptyList()
    ) {
        send(SignalingMessage.StartLive(
            title = title,
            targetPhoneNumbers = contactPhoneNumbers.ifEmpty { null },
            groupIds = groupIds.ifEmpty { null },
            broadcasterPhoneNumber = broadcasterPhoneNumber,
            broadcasterName = broadcasterName
        ))
    }

    // Logout mesajı gönder
    fun logout() {
        if (isRegistered && registeredPhoneNumber != null) {
            android.util.Log.d("SignalingClient", "Logout mesajı gönderiliyor: phoneNumber=${registeredPhoneNumber}")
            send(SignalingMessage.Logout(registeredPhoneNumber!!))
            // Backend logout mesajını işleyecek ve cleanup yapacak
            // Kısa bir süre bekle ki mesaj gönderilsin, sonra bağlantıyı kapat
            reconnectScope.launch {
                delay(500) // 500ms bekle
                close()
            }
        } else {
            // Kayıtlı değilse direkt kapat
            close()
        }
    }

    fun leave() {
        activeRoom?.let {
            send(SignalingMessage.Leave(it))
        }
        close()
    }

    fun send(message: SignalingMessage) {
        if (webSocket == null) {
            android.util.Log.e("SignalingClient", "❌ WebSocket null, mesaj gönderilemedi: ${message.type}")
            return
        }
        
        // Bağlantı durumunu kontrol et
        val currentStatus = _status.value
        if (currentStatus !is SignalingStatus.Connected) {
            android.util.Log.e("SignalingClient", "❌ WebSocket bağlantısı yok (durum: $currentStatus), mesaj gönderilemedi: ${message.type}")
            return
        }
        
        try {
            val json = SignalingMessage.toJson(message)
            val sent = webSocket!!.send(json)
            if (!sent) {
                android.util.Log.e("SignalingClient", "❌ Mesaj gönderilemedi (queue dolu veya bağlantı kapalı): ${message.type}")
            } else {
                android.util.Log.d("SignalingClient", "✅ Mesaj gönderildi: ${message.type}, status=$currentStatus")
            }
        } catch (e: Exception) {
            android.util.Log.e("SignalingClient", "❌ Mesaj gönderme hatası: ${message.type}", e)
        }
    }

    private fun createListener(roomCode: String? = null): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Bağlantı başarılı, reconnect counter'ı sıfırla
                reconnectAttempts = 0
                shouldReconnect = false // Başarılı bağlantı sonrası reconnect'i durdur
                
                android.util.Log.d("SignalingClient", "✅ WebSocket bağlantısı başarılı: roomCode=$roomCode, responseCode=${response.code}")
                this@SignalingClient.webSocket = webSocket
                
                if (roomCode != null) {
                    activeRoom = roomCode
                    _status.value = SignalingStatus.Connected(roomCode)
                    send(SignalingMessage.Join(roomCode))
                    android.util.Log.d("SignalingClient", "Join mesajı gönderildi: roomCode=$roomCode")
                } else {
                    _status.value = SignalingStatus.Connected("")
                    android.util.Log.d("SignalingClient", "WebSocket bağlantısı kuruldu (room code yok)")
                }
                pendingOpen?.complete(Unit)
                pendingOpen = null
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                android.util.Log.d("SignalingClient", "Mesaj alındı: ${text.take(100)}...")
                SignalingMessage.fromJson(text)?.let { message ->
                    android.util.Log.d("SignalingClient", "Mesaj parse edildi: type=${message.type}")
                    // Registered mesajı geldiğinde kayıt durumunu güncelle
                    if (message is SignalingMessage.Registered) {
                        android.util.Log.i("SignalingClient", "✅ Registered mesajı alındı: phoneNumber=${message.phoneNumber}, name=${message.name}")
                        isRegistered = true
                        pendingRegistration?.complete(Unit) // Registration tamamlandı
                        pendingRegistration = null
                    } else if (message is SignalingMessage.RegisterError) {
                        android.util.Log.e("SignalingClient", "❌ RegisterError mesajı alındı: ${message.message}")
                        // Register hatası geldiğinde pending registration'ı iptal et
                        pendingRegistration?.completeExceptionally(Exception(message.message))
                        pendingRegistration = null
                    }
                    _messages.tryEmit(message)
                } ?: run {
                    android.util.Log.w("SignalingClient", "⚠️ Mesaj parse edilemedi: $text")
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                activeRoom = null
                isRegistered = false
                this@SignalingClient.webSocket = null
                _status.value = SignalingStatus.Disconnected
                
                // Normal kapatma değilse (1000 = normal closure) reconnect dene
                if (code != 1000 && shouldReconnect) {
                    android.util.Log.d("SignalingClient", "WebSocket kapandı (code=$code), reconnect başlatılıyor")
                    startReconnect()
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                this@SignalingClient.webSocket = null
                _status.value = SignalingStatus.Error(t)
                pendingOpen?.completeExceptionally(t)
                pendingOpen = null
                
                // Hata durumunda reconnect dene
                if (shouldReconnect) {
                    android.util.Log.d("SignalingClient", "WebSocket hatası, reconnect başlatılıyor: ${t.message}")
                    startReconnect()
                }
            }
        }
    }

    fun close() {
        webSocket?.close(CLOSE_CODE_NORMAL, "User request")
        webSocket = null
        activeRoom = null
        isRegistered = false
        _status.value = SignalingStatus.Disconnected
    }

    companion object {
        private const val CLOSE_CODE_NORMAL = 1000
    }
}

sealed interface SignalingStatus {
    data object Disconnected : SignalingStatus
    data class Connecting(val roomCode: String) : SignalingStatus
    data class Connected(val roomCode: String) : SignalingStatus
    data class Error(val throwable: Throwable) : SignalingStatus
}

