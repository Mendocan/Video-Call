package com.videocall.app.signaling

import kotlinx.coroutines.CompletableDeferred
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
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.concurrent.TimeUnit

class SignalingClient(
    private val url: String,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {

    private var webSocket: WebSocket? = null
    private var pendingOpen: CompletableDeferred<Unit>? = null
    private var activeRoom: String? = null
    private var isRegistered: Boolean = false

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

    // Kullanıcı kaydı
    suspend fun register(phoneNumber: String, name: String?) {
        if (!isRegistered) {
            connect()
            // Bağlantı kurulduktan sonra webSocket'in hazır olduğundan emin ol
            if (webSocket == null) {
                android.util.Log.e("SignalingClient", "WebSocket bağlantısı kurulamadı, register mesajı gönderilemedi")
                return
            }
        }
        android.util.Log.d("SignalingClient", "Register mesajı gönderiliyor: phoneNumber=$phoneNumber, name=$name")
        send(SignalingMessage.Register(phoneNumber, name))
    }

    fun sendOffer(description: SessionDescription) {
        send(SignalingMessage.Offer(description.description))
    }

    fun sendAnswer(description: SessionDescription) {
        send(SignalingMessage.Answer(description.description))
    }

    fun sendIceCandidate(candidate: IceCandidate) {
        send(
            SignalingMessage.IceCandidateMessage(
                SignalingMessage.IceCandidatePayload(
                    candidate = candidate.sdp,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex
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

    // Bireysel arama başlatma
    fun startCall(targetPhoneNumber: String, callerPhoneNumber: String, callerName: String?) {
        if (webSocket == null) {
            android.util.Log.e("SignalingClient", "WebSocket null, arama başlatılamadı")
            return
        }
        if (!isRegistered) {
            android.util.Log.e("SignalingClient", "Kullanıcı kayıtlı değil, arama başlatılamadı")
            return
        }
        android.util.Log.d("SignalingClient", "Arama başlatılıyor: target=$targetPhoneNumber, caller=$callerPhoneNumber")
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

    fun leave() {
        activeRoom?.let {
            send(SignalingMessage.Leave(it))
        }
        close()
    }

    private fun send(message: SignalingMessage) {
        if (webSocket == null) {
            android.util.Log.e("SignalingClient", "WebSocket null, mesaj gönderilemedi: ${message.type}")
            return
        }
        try {
            val json = SignalingMessage.toJson(message)
            val sent = webSocket!!.send(json)
            if (!sent) {
                android.util.Log.e("SignalingClient", "Mesaj gönderilemedi (queue dolu): ${message.type}")
            } else {
                android.util.Log.d("SignalingClient", "Mesaj gönderildi: ${message.type}")
            }
        } catch (e: Exception) {
            android.util.Log.e("SignalingClient", "Mesaj gönderme hatası: ${message.type}", e)
        }
    }

    private fun createListener(roomCode: String? = null): WebSocketListener {
        return object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (roomCode != null) {
                    activeRoom = roomCode
                    _status.value = SignalingStatus.Connected(roomCode)
                    send(SignalingMessage.Join(roomCode))
                } else {
                    _status.value = SignalingStatus.Connected("")
                }
                pendingOpen?.complete(Unit)
                pendingOpen = null
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                SignalingMessage.fromJson(text)?.let { message ->
                    // Registered mesajı geldiğinde kayıt durumunu güncelle
                    if (message is SignalingMessage.Registered) {
                        isRegistered = true
                    }
                    _messages.tryEmit(message)
                }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                activeRoom = null
                isRegistered = false
                _status.value = SignalingStatus.Disconnected
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _status.value = SignalingStatus.Error(t)
                pendingOpen?.completeExceptionally(t)
                pendingOpen = null
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

