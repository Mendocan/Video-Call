package com.videocall.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.videocall.app.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.videocall.app.model.CallUiState
import com.videocall.app.model.Contact
import com.videocall.app.ui.theme.Teal

@Composable
fun HomeScreen(
    uiState: CallUiState,
    addedContacts: List<Contact>,
    networkState: com.videocall.app.data.NetworkState,
    onNavigateToCall: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToLegal: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onStartCallWithContact: (Contact) -> Unit,
    onRemoveContact: (Contact) -> Unit,
    onToggleFavorite: (Contact) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val complianceItems = listOf(
        "Tüm medya ve sinyalleşme trafiği TLS 1.3 üzerinden uçtan uca şifrelenir.",
        "Rehber verileri yalnızca davet sürecinde okunur, sunucuya kaydedilmez.",
        "KVKK ve GDPR kapsamında veri sahibinin silme ve bilgi alma hakları desteklenir.",
        "Görüntülü görüşmeler cihaz üzerinde işlenir; bulutta kalıcı kayıt tutulmaz."
    )
    val connectionStatus = when {
        uiState.isConnected -> "Aktif görüşme"
        else -> "Hazır"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Video Call Logo",
                modifier = Modifier.size(40.dp)
            )
            IconButton(onClick = onNavigateToSettings) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Ayarlar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Durum: ${uiState.statusMessage}",
                style = MaterialTheme.typography.bodyMedium,
                color = Teal
            )
            // Ağ durumu göstergesi
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (networkState.networkType) {
                        com.videocall.app.data.NetworkType.WIFI -> Icons.Default.Wifi
                        com.videocall.app.data.NetworkType.MOBILE_DATA -> Icons.Default.SignalCellularAlt
                        com.videocall.app.data.NetworkType.NONE -> Icons.Default.SignalCellularAlt
                    },
                    contentDescription = null,
                    tint = if (networkState.isConnected) Teal else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = when {
                        networkState.isConnected && networkState.networkType == com.videocall.app.data.NetworkType.WIFI -> "Wi-Fi"
                        networkState.isConnected && networkState.networkType == com.videocall.app.data.NetworkType.MOBILE_DATA -> "Mobil"
                        else -> "Bağlantı Yok"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (networkState.isConnected) Teal else MaterialTheme.colorScheme.error
                )
            }
        }

        StatusCard(
            participants = uiState.participants,
            connectionStatus = connectionStatus,
            isMicEnabled = uiState.isMicEnabled,
            isCameraEnabled = uiState.isCameraEnabled,
            modifier = Modifier.fillMaxWidth()
        )

        ActionCard(
            onNavigateToCall = onNavigateToCall,
            onNavigateToContacts = onNavigateToContacts,
            onNavigateToLegal = onNavigateToLegal
        )

        if (addedContacts.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ekli Kişiler",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(addedContacts) { contact ->
                            AddedContactItem(
                                contact = contact,
                                onCallClick = { onStartCallWithContact(contact) },
                                onRemoveClick = { onRemoveContact(contact) }
                            )
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Güvenlik ve Uyumluluk",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(complianceItems) { item ->
                        Text("• $item", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    participants: Int,
    connectionStatus: String,
    isMicEnabled: Boolean,
    isCameraEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = "Oturum Özeti", style = MaterialTheme.typography.titleMedium)
            SummaryRow("Bağlantı durumu", connectionStatus)
            SummaryRow("Katılımcı sayısı", participants.toString())
            SummaryRow("Kamera", if (isCameraEnabled) "Açık" else "Kapalı")
            SummaryRow("Mikrofon", if (isMicEnabled) "Açık" else "Kapalı")
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ActionCard(
    onNavigateToCall: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToLegal: () -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Hızlı Aksiyonlar",
                style = MaterialTheme.typography.titleMedium
            )
            Button(
                onClick = onNavigateToCall,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Görüntülü Görüşmeye Git")
            }
            OutlinedButton(
                onClick = onNavigateToContacts,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Kişi Ekle / Rehber")
            }
            OutlinedButton(
                onClick = onNavigateToLegal,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("Yasal Metinler")
            }
        }
    }
}

@Composable
private fun AddedContactItem(
    contact: Contact,
    onCallClick: () -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.padding(4.dp),
                tint = Teal
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (contact.phoneNumber != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = contact.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onCallClick) {
                Icon(
                    imageVector = Icons.Default.VideoCall,
                    contentDescription = "Görüşme Başlat",
                    tint = Teal
                )
            }
            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Kaldır",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

