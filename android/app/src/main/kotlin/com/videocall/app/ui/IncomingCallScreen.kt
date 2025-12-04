package com.videocall.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videocall.app.ui.theme.RejectRed
import com.videocall.app.ui.theme.Teal

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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.9f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
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
                    callerName ?: callerPhoneNumber ?: "Bilinmeyen"
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
                    modifier = Modifier.padding(bottom = 48.dp)
                )
            } else {
                Text(
                    text = callerPhoneNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 48.dp)
                )
            }

            // Kabul ve Reddet Butonları
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Reddet Butonu (Kırmızı)
                Box(
                    modifier = Modifier.clickable(onClick = onReject)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(RejectRed, CircleShape)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CallEnd,
                                contentDescription = "Reddet",
                                tint = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Text(
                            text = "Reddet",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // Kabul Et Butonu (Yeşil)
                Box(
                    modifier = Modifier.clickable(onClick = onAccept)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(androidx.compose.ui.graphics.Color(0xFF4CAF50), CircleShape)
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Kabul Et",
                                tint = Color.White,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Text(
                            text = "Kabul Et",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

