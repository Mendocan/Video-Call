package com.videocall.app.signaling

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Cihaz üzerinde çalışan hafif WebSocket signaling sunucusu.
 */
class LocalSignalingServer(
    private val port: Int,
    private val tokenProvider: () -> String?,
    private val onPeerStatusChanged: (Boolean) -> Unit
) {

    private val server = object : WebSocketServer(InetSocketAddress(port)) {
        override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
            // İlk mesajda doğrulama bekleniyor
        }

        override fun onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean) {
            if (authorizedConnection?.connection == conn) {
                authorizedConnection = null
                onPeerStatusChanged(false)
            }
        }

        override fun onMessage(conn: WebSocket, message: String) {
            val signalingMessage = SignalingMessage.fromJson(message) ?: return
            if (signalingMessage is SignalingMessage.Auth) {
                verifyAuth(conn, signalingMessage)
                return
            }

            if (authorizedConnection?.connection == conn) {
                _incoming.tryEmit(signalingMessage)
            } else {
                conn.close(CODE_UNAUTHORIZED, "Unauthorized")
            }
        }

        override fun onError(conn: WebSocket?, ex: Exception) {
            if (conn != null && authorizedConnection?.connection == conn) {
                authorizedConnection = null
                onPeerStatusChanged(false)
            }
        }

        override fun onStart() {}
    }

    private data class AuthorizedConnection(
        val connection: WebSocket,
        val phoneHash: String
    )

    private val started = AtomicBoolean(false)
    private val _incoming = MutableSharedFlow<SignalingMessage>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val incoming: SharedFlow<SignalingMessage> = _incoming
    private var authorizedConnection: AuthorizedConnection? = null

    fun start() {
        if (started.compareAndSet(false, true)) {
            server.start()
        }
    }

    fun stop() {
        authorizedConnection = null
        started.set(false)
        runCatching { server.stop() }
    }

    fun send(message: SignalingMessage): Boolean {
        val json = SignalingMessage.toJson(message)
        val target = authorizedConnection?.connection ?: return false
        target.send(json)
        return true
    }

    fun broadcast(message: SignalingMessage): Boolean {
        // LocalSignalingServer tek bir authorized connection'a sahip, broadcast = send
        return send(message)
    }

    fun updateToken(newToken: String?) {
        if (newToken == null) {
            authorizedConnection?.connection?.close(CODE_UNAUTHORIZED, "Token reset")
            authorizedConnection = null
            onPeerStatusChanged(false)
        }
    }

    fun getPort(): Int = port

    private fun verifyAuth(conn: WebSocket, auth: SignalingMessage.Auth) {
        val expectedToken = tokenProvider()
        if (expectedToken != null && expectedToken == auth.token) {
            authorizedConnection = AuthorizedConnection(conn, auth.phoneHash)
            onPeerStatusChanged(true)
        } else {
            conn.close(CODE_UNAUTHORIZED, "Invalid token")
        }
    }

    companion object {
        private const val CODE_UNAUTHORIZED = 4001
    }
}

