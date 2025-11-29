package com.videocall.app.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.videocall.app.ui.theme.RejectRed
import com.videocall.app.ui.theme.Teal
import kotlin.math.abs

@Composable
fun IncomingCallScreen(
    callerName: String?,
    callerPhoneNumber: String,
    isGroupCall: Boolean = false,
    groupName: String? = null,
    memberCount: Int? = null,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = dragOffset,
        animationSpec = tween(durationMillis = 200),
        label = "drag_animation"
    )

    var isDragging by remember { mutableStateOf(false) }
    val acceptThreshold = 150f
    val rejectThreshold = -150f

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        // Sol taraf - Kabul Et (Turkuaz)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .fillMaxSize()
                .background(Teal.copy(alpha = if (dragOffset > 0) 0.3f else 0f))
                .zIndex(1f),
            contentAlignment = Alignment.Center
        ) {
            if (dragOffset > 50f) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Kabul Et",
                        tint = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        text = "Kabul Et",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Sağ taraf - Reddet (Bordo-kırmızı)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .fillMaxSize()
                .align(Alignment.CenterEnd)
                .background(RejectRed.copy(alpha = if (dragOffset < 0) 0.3f else 0f))
                .zIndex(1f),
            contentAlignment = Alignment.Center
        ) {
            if (dragOffset < -50f) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Reddet",
                        tint = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                    Text(
                        text = "Reddet",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Orta kart (sürüklenebilir)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.toInt(), 0) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            when {
                                dragOffset > acceptThreshold -> {
                                    onAccept()
                                    dragOffset = 0f
                                }
                                dragOffset < rejectThreshold -> {
                                    onReject()
                                    dragOffset = 0f
                                }
                                else -> {
                                    dragOffset = 0f
                                }
                            }
                            isDragging = false
                        }
                    ) { change, dragAmount ->
                        isDragging = true
                        dragOffset = (dragOffset + dragAmount.x).coerceIn(-300f, 300f)
                    }
                }
                .zIndex(2f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Text(
                    text = "Gelen Arama",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .background(Teal.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = Teal,
                        modifier = Modifier.padding(32.dp)
                    )
                }

                Text(
                    text = if (isGroupCall) {
                        groupName ?: "Grup Araması"
                    } else {
                        callerName ?: "Bilinmeyen"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )

                if (isGroupCall) {
                    Text(
                        text = "${memberCount ?: 0} katılımcı",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Arayan: ${callerName ?: callerPhoneNumber}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                } else {
                    Text(
                        text = callerPhoneNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                }

                Text(
                    text = "Sürükleyerek kabul et veya reddet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

