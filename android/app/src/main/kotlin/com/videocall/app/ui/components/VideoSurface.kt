package com.videocall.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.SurfaceViewRenderer

@Composable
fun VideoSurface(
    modifier: Modifier = Modifier,
    isOverlay: Boolean = false,
    onRendererReady: (SurfaceViewRenderer) -> Unit
) {
    val context = LocalContext.current
    var renderer: SurfaceViewRenderer? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.5f))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                SurfaceViewRenderer(context).apply {
                    setEnableHardwareScaler(true)
                    setZOrderMediaOverlay(isOverlay)
                    setZOrderOnTop(isOverlay)
                    onRendererReady(this)
                    renderer = this
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            renderer?.release()
            renderer = null
        }
    }
}

