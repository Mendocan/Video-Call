package com.videocall.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.videocall.app.R
import com.videocall.app.model.Contact
import com.videocall.app.model.UserStatus
import com.videocall.app.ui.theme.Teal
import com.videocall.app.viewmodel.VideoCallViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    addedContacts: List<Contact>,
    viewModel: VideoCallViewModel,
    onNavigateToSettings: () -> Unit,
    onStartCallWithContact: (Contact) -> Unit,
    onRemoveContact: (Contact) -> Unit,
    onContactClick: (Contact) -> Unit = {},
    onNavigateToCall: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val complianceItems = listOf(
        "Tüm medya ve sinyalleşme trafiği TLS 1.3 üzerinden uçtan uca şifrelenir.",
        "Rehber verileri yalnızca davet sürecinde okunur, sunucuya kaydedilmez.",
        "KVKK ve GDPR kapsamında veri sahibinin silme ve bilgi alma hakları desteklenir.",
        "Görüntülü görüşmeler cihaz üzerinde işlenir; bulutta kalıcı kayıt tutulmaz."
    )

    val context = LocalContext.current
    val contactsPermissionGranted = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Sohbetler") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Görüşmeler") }
            )
        }

        // Tab Content
        when (selectedTab) {
            0 -> {
                // Sohbetler Tab
                ChatScreen(
                    viewModel = viewModel,
                    addedContacts = addedContacts,
                    modifier = Modifier.fillMaxSize()
                )
            }
            1 -> {
                // Görüşmeler Tab - CallHistory içeriği
                val callHistory by viewModel.callHistory.collectAsState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Görüşmeler",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    
                    if (callHistory.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Henüz görüşme geçmişi yok",
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(callHistory) { call ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = call.contactName ?: call.phoneNumber,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            // phoneNumber zaten non-nullable, sadece boş olup olmadığını kontrol et
                                            if (call.phoneNumber.isNotBlank()) {
                                                Text(
                                                    text = call.phoneNumber,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        Text(
                                            text = call.duration?.let { "${it / 60}:${String.format("%02d", it % 60)}" } ?: "--:--",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// StatusCard ve ActionCard kaldırıldı - Tab bar üzerinden erişilebilir

@Composable
fun AddedContactItem(
    modifier: Modifier = Modifier,
    contact: Contact,
    onCallClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onClick: () -> Unit = {}
) {
    // Uygulamayı kullanan kişilerin çerçevesi turuncu olacak
    val isAppUser = contact.status == UserStatus.ONLINE
    val borderColor = if (isAppUser) {
        androidx.compose.ui.graphics.Color(0xFFFF9800) // Turuncu
    } else {
        Teal
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isAppUser) {
                    Modifier.border(2.dp, borderColor, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            ),
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

