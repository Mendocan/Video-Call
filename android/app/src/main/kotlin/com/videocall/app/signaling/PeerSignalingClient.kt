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
import java.util.concurrent.TimeUnit

/**
 * Uzak cihazın yerel signaling sunucusuna bağlanan istemci.
 */
class PeerSignalingClient(
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {

    sealed interface State {
        data object Idle : State
        data object Connecting : State
        data object Connected : State
        data class Error(val throwable: Throwable) : State
    }

    private var webSocket: WebSocket? = null
    private val _messages = MutableSharedFlow<SignalingMessage>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<SignalingMessage> = _messages.asSharedFlow()

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    suspend fun connect(host: String, port: Int, token: String, phoneHash: String) {
        close()
        _state.value = State.Connecting
        val deferred = CompletableDeferred<Unit>()
        val request = Request.Builder()
            .url("ws://$host:$port/peer")
            .build()
        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                val auth = SignalingMessage.Auth(token = token, phoneHash = phoneHash)
                webSocket.send(SignalingMessage.toJson(auth))
                _state.value = State.Connected
                deferred.complete(Unit)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                SignalingMessage.fromJson(text)?.let { _messages.tryEmit(it) }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                onMessage(webSocket, bytes.utf8())
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                _state.value = State.Error(t)
                deferred.completeExceptionally(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                _state.value = State.Idle
            }
        })
        deferred.await()
    }

    fun send(message: SignalingMessage): Boolean {
        val json = SignalingMessage.toJson(message)
        return webSocket?.send(json) == true
    }

    fun close() {
        webSocket?.close(1000, "Closed by client")
        webSocket = null
        _state.value = State.Idle
    }

    fun isConnected(): Boolean = _state.value is State.Connected
}

