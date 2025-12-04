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
import android.view.SurfaceView

@Composable
fun VideoSurface(
    modifier: Modifier = Modifier,
    isOverlay: Boolean = false,
    onRendererReady: (SurfaceView) -> Unit
) {
    val context = LocalContext.current
    var renderer: SurfaceView? by remember { mutableStateOf(null) }

    Box(
        modifier = modifier.background(Color.Black.copy(alpha = 0.5f))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                SurfaceView(context).apply {
                    setZOrderMediaOverlay(isOverlay)
                    onRendererReady(this)
                    renderer = this
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            // SurfaceView için özel temizlik gerekmez
            renderer = null
        }
    }
}
