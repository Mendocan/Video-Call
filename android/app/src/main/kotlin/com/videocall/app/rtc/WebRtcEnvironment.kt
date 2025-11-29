package com.videocall.app.rtc

import android.content.Context
import org.webrtc.PeerConnectionFactory
import java.util.concurrent.atomic.AtomicBoolean

object WebRtcEnvironment {
    private val initialized = AtomicBoolean(false)

    fun initialize(context: Context) {
        if (initialized.get()) return
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(false)
            .createInitializationOptions()

        PeerConnectionFactory.initialize(options)
        initialized.set(true)
    }
}

