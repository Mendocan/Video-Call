@file:Suppress("UNUSED_PARAMETER", "UNUSED_ANONYMOUS_PARAMETER")

package com.videocall.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.StopScreenShare
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.videocall.app.data.PreferencesManager
import com.videocall.app.model.CallHistory
import com.videocall.app.model.CallType
import com.videocall.app.model.CallUiState
import com.videocall.app.model.Contact
import com.videocall.app.model.Participant
import com.videocall.app.ui.components.VideoSurface
import com.videocall.app.ui.theme.Danger
import com.videocall.app.ui.theme.Teal
import org.webrtc.SurfaceViewRenderer

@Composable
fun CallScreen(
    uiState: CallUiState,
    addedContacts: List<Contact>,
    callHistory: List<CallHistory>,
    networkState: com.videocall.app.data.NetworkState,
    onStartCall: () -> Unit,
    onHangUp: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleMic: () -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleAudioOnly: () -> Unit = {},
    onSetBackgroundMode: (com.videocall.app.model.BackgroundMode) -> Unit = {},
    onCycleBackgroundMode: () -> Unit = {},
    onSetFilter: (com.videocall.app.model.FilterType) -> Unit = {},
    onCycleFilter: () -> Unit = {},
    onStartCallWithContact: (Contact) -> Unit,
    onLocalRendererReady: (SurfaceViewRenderer) -> Unit,
    onRemoteRendererReady: (SurfaceViewRenderer) -> Unit,
    onNavigateToQRCode: () -> Unit,
    onToggleFavorite: (Contact) -> Unit = {},
    onDeleteCallHistory: (Long) -> Unit = {},
    onClearAllCallHistory: () -> Unit = {},
    onSendChatMessage: (String) -> Unit = {},
    onToggleChatVisibility: () -> Unit = {},
    onShareFile: (android.net.Uri, String, String) -> Unit = { _, _, _ -> },
    onToggleScreenSharing: () -> Unit = {},
    onRequestScreenSharing: () -> Unit = {},
    onSetAudioRoute: (com.videocall.app.audio.AudioRoute) -> Unit = {},
    isBluetoothConnected: Boolean = false,
    bluetoothDeviceName: String? = null,
    audioRoute: com.videocall.app.audio.AudioRoute = com.videocall.app.audio.AudioRoute.SPEAKER,
    isVoiceCommandsEnabled: Boolean = false,
    isListening: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val preferencesManager = remember { PreferencesManager(context) }
    val permissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.MODIFY_AUDIO_SETTINGS
    )

    var permissionsGranted by remember { mutableStateOf(false) }
    var showCallConfirmation by remember { mutableStateOf<Contact?>(null) }
    var isInCallMode by remember { mutableStateOf(false) }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        permissionsGranted = !result.values.contains(false)
    }

    LaunchedEffect(uiState.isConnected, uiState.statusMessage) {
        // Görüşme başladığında görüşme moduna geç
        if (uiState.isConnected && !uiState.statusMessage.contains("sonlandırıldı")) {
            isInCallMode = true
        }
        // Görüşme sonlandırıldığında liste moduna dön
        if (uiState.statusMessage.contains("sonlandırıldı") || !uiState.isConnected) {
            isInCallMode = false
        }
    }

    // Liste modu - Ekli kişiler listesi
    if (!isInCallMode) {
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
                Text(
                    text = "Aramalar",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateToQRCode) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Kod",
                            tint = Teal
                        )
                    }
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
            }

            if (uiState.statusMessage.isNotBlank()) {
                StatusBanner(uiState.statusMessage)
            }

            // Ağ durumu uyarısı
            if (!networkState.isConnected) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.SignalCellularAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "İnternet Bağlantısı Yok",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Görüşme yapabilmek için Wi-Fi veya mobil veri bağlantısı gereklidir",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            if (addedContacts.isEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Henüz ekli kişi yok",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Kişiler sayfasından kişi ekleyebilirsiniz",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Favori kişiler
                val favoriteContacts = addedContacts.filter { it.isFavorite }
                if (favoriteContacts.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Favori Kişiler",
                                style = MaterialTheme.typography.titleMedium
                            )
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(favoriteContacts) { contact ->
                                    ContactItemRow(
                                        contact = contact,
                                        networkState = networkState,
                                        onCallClick = { showCallConfirmation = contact },
                                        onFavoriteClick = { onToggleFavorite(contact) }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Tüm ekli kişiler
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
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
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(addedContacts) { contact ->
                                ContactItemRow(
                                    contact = contact,
                                    networkState = networkState,
                                    onCallClick = { showCallConfirmation = contact },
                                    onFavoriteClick = { onToggleFavorite(contact) }
                                )
                            }
                        }
                    }
                }
            }

            // Arama geçmişi
            if (callHistory.isNotEmpty()) {
                var showClearAllDialog by remember { mutableStateOf(false) }
                var searchQuery by remember { mutableStateOf("") }
                var filterType by remember { mutableStateOf<CallType?>(null) }
                
                val filteredHistory = remember(callHistory, searchQuery, filterType) {
                    callHistory.filter { history ->
                        val matchesSearch = searchQuery.isBlank() || 
                            history.contactName?.lowercase()?.contains(searchQuery.lowercase()) == true ||
                            history.phoneNumber.lowercase().contains(searchQuery.lowercase())
                        val matchesFilter = filterType == null || history.callType == filterType
                        matchesSearch && matchesFilter
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Arama Geçmişi",
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(
                                onClick = { showClearAllDialog = true }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = "Tümünü Sil",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        
                        // Arama ve filtreleme
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("İsim veya telefon ile ara...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Ara",
                                    tint = Teal
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Temizle",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true
                        )
                        
                        // Filtre butonları
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { filterType = null },
                                modifier = Modifier.weight(1f),
                                colors = if (filterType == null) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = Teal.copy(alpha = 0.2f)
                                    )
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Text("Tümü", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(
                                onClick = { filterType = CallType.INCOMING },
                                modifier = Modifier.weight(1f),
                                colors = if (filterType == CallType.INCOMING) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = Teal.copy(alpha = 0.2f)
                                    )
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Text("Gelen", style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(
                                onClick = { filterType = CallType.OUTGOING },
                                modifier = Modifier.weight(1f),
                                colors = if (filterType == CallType.OUTGOING) {
                                    ButtonDefaults.outlinedButtonColors(
                                        containerColor = Teal.copy(alpha = 0.2f)
                                    )
                                } else {
                                    ButtonDefaults.outlinedButtonColors()
                                }
                            ) {
                                Text("Giden", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.height(200.dp)
                        ) {
                            if (filteredHistory.isEmpty()) {
                                item {
                                    Text(
                                        text = if (searchQuery.isNotEmpty() || filterType != null) {
                                            "Arama sonucu bulunamadı"
                                        } else {
                                            "Arama geçmişi yok"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                items(filteredHistory) { history ->
                                    CallHistoryItem(
                                        history = history,
                                        onDelete = { onDeleteCallHistory(history.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Tüm geçmişi temizleme onay dialog'u
                if (showClearAllDialog) {
                    AlertDialog(
                        onDismissRequest = { showClearAllDialog = false },
                        title = { Text("Tüm Geçmişi Sil") },
                        text = { Text("Tüm arama geçmişini silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    onClearAllCallHistory()
                                    showClearAllDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Sil")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showClearAllDialog = false }) {
                                Text("İptal")
                            }
                        }
                    )
                }
            }
        }

        // Onay dialog'u
        showCallConfirmation?.let { contact ->
            AlertDialog(
                onDismissRequest = { showCallConfirmation = null },
                title = { Text("Arama Başlat") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${contact.name} ile görüşme başlatmak istediğinizden emin misiniz?")
                        if (contact.phoneNumber != null) {
                            Text(
                                text = "Telefon: ${contact.phoneNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onStartCallWithContact(contact)
                            showCallConfirmation = null
                            isInCallMode = true
                        }
                    ) {
                        Text("Evet, Başlat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCallConfirmation = null }) {
                        Text("İptal")
                    }
                }
            )
        }
        return
    }

    // Görüşme modu - Kamera ekranı
    LaunchedEffect(Unit) {
        val grantedNow = permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        permissionsGranted = grantedNow
        if (!grantedNow) {
            permissionLauncher.launch(permissions)
        }
    }

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                // Grup görüşme modu - Grid layout
                if (uiState.isGroupCall && uiState.participantsList.isNotEmpty()) {
                    GroupCallGrid(
                        participants = uiState.participantsList,
                        isAudioOnly = uiState.isAudioOnly,
                        onLocalRendererReady = onLocalRendererReady,
                        onRemoteRendererReady = { participant, renderer ->
                            // Her katılımcı için ayrı renderer gerekir
                            // Şimdilik sadece ilk remote renderer'ı kullanıyoruz
                            if (uiState.participantsList.firstOrNull { !it.isLocal } == participant) {
                                onRemoteRendererReady(renderer)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Tek kişilik görüşme - Ana video ekranı (tam ekran) - sadece video modunda
                    if (!uiState.isAudioOnly) {
                        VideoSurface(
                            modifier = Modifier.fillMaxSize(),
                            isOverlay = false,
                            onRendererReady = onRemoteRendererReady
                        )
                        if (!uiState.remoteVideoVisible) {
                            Placeholder(
                                text = if (uiState.isConnected) "Karşı taraf video gönderisi bekleniyor" else "Bağlantı bekleniyor"
                            )
                        }
                    } else {
                        // Audio-only modunda avatar veya kişi bilgisi göster
                        Placeholder(
                            text = if (uiState.isConnected) "Sesli görüşme devam ediyor" else "Bağlantı bekleniyor"
                        )
                    }
                }
        
        // Görüşme süresi ve şifreleme göstergesi (üst ortada)
        if (uiState.isConnected) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
                    .zIndex(3f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Görüşme süresi
                if (uiState.callDuration > 0) {
                    val hours = uiState.callDuration / 3600
                    val minutes = (uiState.callDuration % 3600) / 60
                    val seconds = uiState.callDuration % 60
                    val durationText = if (hours > 0) {
                        String.format("%02d:%02d:%02d", hours, minutes, seconds)
                    } else {
                        String.format("%02d:%02d", minutes, seconds)
                    }
                    
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Şifreleme seviyesi ve gizli mod göstergesi
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(
                                Color.Black.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.encryptionLevel == com.videocall.app.model.EncryptionLevel.END_TO_END) {
                                Icons.Default.Lock
                            } else {
                                Icons.Default.Security
                            },
                            contentDescription = null,
                            tint = Teal,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when (uiState.encryptionLevel) {
                                com.videocall.app.model.EncryptionLevel.END_TO_END -> "End-to-End Şifreli"
                                com.videocall.app.model.EncryptionLevel.STANDARD -> "Standart Şifreli"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Bluetooth durumu göstergesi
                    if (isBluetoothConnected) {
                        Row(
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = null,
                                tint = Teal,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = bluetoothDeviceName ?: "Bluetooth",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    // Sesli komut dinleme göstergesi
                    if (isVoiceCommandsEnabled && isListening) {
                        Row(
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color(0xFFFF9800), // Orange
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Dinleniyor...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    
                    // Gizli mod göstergesi
                    if (preferencesManager.isPrivacyModeEnabled()) {
                        Row(
                            modifier = Modifier
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color(0xFFFF9800), // Orange
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Gizli Mod",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

                // Sağ üstte küçük local video (sürüklenebilir) - sadece video modunda
                if (!uiState.isAudioOnly) {
                    var localVideoOffsetX by remember { mutableFloatStateOf(0f) }
                    var localVideoOffsetY by remember { mutableFloatStateOf(0f) }
                    
                    val screenWidth = with(density) { context.resources.displayMetrics.widthPixels.toFloat() }
                    val screenHeight = with(density) { context.resources.displayMetrics.heightPixels.toFloat() }
                    val videoSizePx = with(density) { 120.dp.toPx() }
                    val paddingPx = with(density) { 12.dp.toPx() }
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset { 
                                IntOffset(
                                    localVideoOffsetX.toInt(),
                                    localVideoOffsetY.toInt()
                                )
                            }
                            .padding(12.dp)
                            .size(120.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    localVideoOffsetX = (localVideoOffsetX + dragAmount.x)
                                        .coerceIn(
                                            -screenWidth + videoSizePx + paddingPx * 2f,
                                            screenWidth - videoSizePx - paddingPx * 2f
                                        )
                                    localVideoOffsetY = (localVideoOffsetY + dragAmount.y)
                                        .coerceIn(
                                            -screenHeight + videoSizePx + paddingPx * 2f,
                                            screenHeight - videoSizePx - paddingPx * 2f
                                        )
                                }
                            }
                            .zIndex(1f)
                    ) {
                        VideoSurface(
                            modifier = Modifier.fillMaxSize(),
                            isOverlay = true,
                            onRendererReady = onLocalRendererReady
                        )
                        if (!uiState.localVideoVisible) {
                            Placeholder("Kamera hazırlanıyor")
                        }
                    }
                }

        // Altta ikon kontrolleri
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 24.dp, start = 16.dp, end = 16.dp)
                .zIndex(2f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val micIcon = if (uiState.isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff
            val micDesc = if (uiState.isMicEnabled) "Mikrofonu Kapat" else "Mikrofonu Aç"
            
            val cameraIcon = if (uiState.isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff
            val cameraDesc = if (uiState.isCameraEnabled) "Kamerayı Kapat" else "Kamerayı Aç"
            
            val surfaceColor = MaterialTheme.colorScheme.surface
            val onSurfaceColor = MaterialTheme.colorScheme.onSurface
            val micTint = if (uiState.isMicEnabled) Teal else Danger
            val cameraTint = if (uiState.isCameraEnabled) Teal else Danger

            // Sesli arama toggle (audio-only mode)
            FloatingActionButton(
                onClick = { onToggleAudioOnly() },
                modifier = Modifier.size(48.dp),
                containerColor = surfaceColor.copy(alpha = 0.9f),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = if (uiState.isAudioOnly) "Video Aç" else "Sadece Ses",
                    tint = if (uiState.isAudioOnly) Teal else onSurfaceColor.copy(alpha = 0.7f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Filtre toggle (sadece video modunda)
            if (!uiState.isAudioOnly) {
                FloatingActionButton(
                    onClick = { onCycleFilter() },
                    modifier = Modifier.size(48.dp),
                    containerColor = surfaceColor.copy(alpha = 0.9f),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = when (uiState.filter) {
                            com.videocall.app.model.FilterType.NONE -> Icons.Default.Person
                            com.videocall.app.model.FilterType.SEPIA -> Icons.Default.Search // Geçici ikon
                            com.videocall.app.model.FilterType.BLACK_WHITE -> Icons.Default.Person // Geçici ikon
                            com.videocall.app.model.FilterType.VINTAGE -> Icons.Default.Search // Geçici ikon
                        },
                        contentDescription = when (uiState.filter) {
                            com.videocall.app.model.FilterType.NONE -> "Filtre"
                            com.videocall.app.model.FilterType.SEPIA -> "Sepia Aktif"
                            com.videocall.app.model.FilterType.BLACK_WHITE -> "Siyah-Beyaz Aktif"
                            com.videocall.app.model.FilterType.VINTAGE -> "Vintage Aktif"
                        },
                        tint = if (uiState.filter != com.videocall.app.model.FilterType.NONE) Teal else onSurfaceColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Arka plan modu toggle (sadece video modunda)
            if (!uiState.isAudioOnly) {
                FloatingActionButton(
                    onClick = { onCycleBackgroundMode() },
                    modifier = Modifier.size(48.dp),
                    containerColor = surfaceColor.copy(alpha = 0.9f),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = when (uiState.backgroundMode) {
                            com.videocall.app.model.BackgroundMode.NONE -> Icons.Default.Person
                            com.videocall.app.model.BackgroundMode.BLUR -> Icons.Default.Search // Geçici ikon - blur için daha uygun ikon eklenebilir
                            com.videocall.app.model.BackgroundMode.COLOR -> Icons.Default.Person // Geçici ikon - renk için daha uygun ikon eklenebilir
                        },
                        contentDescription = when (uiState.backgroundMode) {
                            com.videocall.app.model.BackgroundMode.NONE -> "Arka Plan Efekti"
                            com.videocall.app.model.BackgroundMode.BLUR -> "Blur Aktif"
                            com.videocall.app.model.BackgroundMode.COLOR -> "Renk Aktif"
                        },
                        tint = if (uiState.backgroundMode != com.videocall.app.model.BackgroundMode.NONE) Teal else onSurfaceColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Kamerayı ters çevir (sadece video modunda)
            if (!uiState.isAudioOnly) {
                FloatingActionButton(
                    onClick = { if (permissionsGranted) onSwitchCamera() },
                    modifier = Modifier.size(48.dp),
                    containerColor = surfaceColor.copy(alpha = 0.9f),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Kamerayı Ters Çevir",
                        tint = if (permissionsGranted) Teal else onSurfaceColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Mikrofon toggle
            FloatingActionButton(
                onClick = { if (permissionsGranted) onToggleMic() },
                modifier = Modifier.size(48.dp),
                containerColor = surfaceColor.copy(alpha = 0.9f),
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = micIcon,
                    contentDescription = micDesc,
                    tint = if (permissionsGranted) micTint else onSurfaceColor.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            // Ses yönlendirme toggle (Bluetooth/Hoparlör/Kulaklık)
            if (uiState.isConnected) {
                FloatingActionButton(
                    onClick = {
                        val nextRoute = when (audioRoute) {
                            com.videocall.app.audio.AudioRoute.SPEAKER -> {
                                if (isBluetoothConnected) {
                                    com.videocall.app.audio.AudioRoute.BLUETOOTH
                                } else {
                                    com.videocall.app.audio.AudioRoute.EARPIECE
                                }
                            }
                            com.videocall.app.audio.AudioRoute.EARPIECE -> com.videocall.app.audio.AudioRoute.SPEAKER
                            com.videocall.app.audio.AudioRoute.BLUETOOTH -> com.videocall.app.audio.AudioRoute.SPEAKER
                            com.videocall.app.audio.AudioRoute.HEADSET -> com.videocall.app.audio.AudioRoute.SPEAKER
                        }
                        onSetAudioRoute(nextRoute)
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (audioRoute == com.videocall.app.audio.AudioRoute.BLUETOOTH) {
                        Teal.copy(alpha = 0.9f)
                    } else {
                        surfaceColor.copy(alpha = 0.9f)
                    },
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = when (audioRoute) {
                            com.videocall.app.audio.AudioRoute.BLUETOOTH -> Icons.Default.Phone
                            com.videocall.app.audio.AudioRoute.SPEAKER -> Icons.Default.Person // Geçici ikon
                            com.videocall.app.audio.AudioRoute.EARPIECE -> Icons.Default.Phone
                            com.videocall.app.audio.AudioRoute.HEADSET -> Icons.Default.Phone
                        },
                        contentDescription = when (audioRoute) {
                            com.videocall.app.audio.AudioRoute.BLUETOOTH -> "Bluetooth Aktif"
                            com.videocall.app.audio.AudioRoute.SPEAKER -> "Hoparlör"
                            com.videocall.app.audio.AudioRoute.EARPIECE -> "Kulaklık"
                            com.videocall.app.audio.AudioRoute.HEADSET -> "Kablolu Kulaklık"
                        },
                        tint = if (audioRoute == com.videocall.app.audio.AudioRoute.BLUETOOTH) {
                            Color.White
                        } else {
                            onSurfaceColor.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Chat toggle (görüşme sırasında)
            if (uiState.isConnected) {
                FloatingActionButton(
                    onClick = onToggleChatVisibility,
                    modifier = Modifier.size(48.dp),
                    containerColor = surfaceColor.copy(alpha = 0.9f),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isChatVisible) Icons.Default.Close else Icons.AutoMirrored.Filled.Chat,
                        contentDescription = if (uiState.isChatVisible) "Chat'i Kapat" else "Chat'i Aç",
                        tint = if (uiState.isChatVisible) Teal else onSurfaceColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Ekran paylaşımı toggle (görüşme sırasında, sadece video modunda)
            if (uiState.isConnected && !uiState.isAudioOnly) {
                FloatingActionButton(
                    onClick = {
                        if (uiState.isScreenSharing) {
                            onToggleScreenSharing()
                        } else {
                            onRequestScreenSharing()
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = if (uiState.isScreenSharing) {
                        Teal.copy(alpha = 0.9f)
                    } else {
                        surfaceColor.copy(alpha = 0.9f)
                    },
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isScreenSharing) Icons.AutoMirrored.Filled.StopScreenShare else Icons.AutoMirrored.Filled.ScreenShare,
                        contentDescription = if (uiState.isScreenSharing) "Ekran Paylaşımını Durdur" else "Ekran Paylaş",
                        tint = if (uiState.isScreenSharing) Color.White else onSurfaceColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Sonlandır (büyük, kırmızı)
            FloatingActionButton(
                onClick = onHangUp,
                modifier = Modifier.size(56.dp),
                containerColor = Danger,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "Görüşmeyi Sonlandır",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Kamera toggle (sadece video modunda)
            if (!uiState.isAudioOnly) {
                FloatingActionButton(
                    onClick = { if (permissionsGranted) onToggleCamera() },
                    modifier = Modifier.size(48.dp),
                    containerColor = surfaceColor.copy(alpha = 0.9f),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = cameraIcon,
                        contentDescription = cameraDesc,
                        tint = if (permissionsGranted) cameraTint else onSurfaceColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Onay dialog'u
        showCallConfirmation?.let { contact ->
            AlertDialog(
                onDismissRequest = { showCallConfirmation = null },
                title = { Text("Arama Başlat") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${contact.name} ile görüşme başlatmak istediğinizden emin misiniz?")
                        if (contact.phoneNumber != null) {
                            Text(
                                text = "Telefon: ${contact.phoneNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onStartCallWithContact(contact)
                            showCallConfirmation = null
                        }
                    ) {
                        Text("Evet, Başlat")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCallConfirmation = null }) {
                        Text("İptal")
                    }
                }
            )
        }
    }
}

@Composable
private fun StatusBanner(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(12.dp)
        )
    }
}

@Composable
private fun CallHistoryItem(
    history: CallHistory,
    onDelete: () -> Unit = {}
) {
    val backgroundColor = when (history.callType) {
        CallType.INCOMING -> Teal.copy(alpha = 0.2f) // Gelen - Turkuaz
        CallType.OUTGOING -> Color(0xFF4CAF50).copy(alpha = 0.2f) // Giden - Yeşil
    }
    val iconColor = when (history.callType) {
        CallType.INCOMING -> Teal
        CallType.OUTGOING -> Color(0xFF4CAF50)
    }
    val callTypeText = when (history.callType) {
        CallType.INCOMING -> "Gelen"
        CallType.OUTGOING -> "Giden"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (history.callType == CallType.INCOMING) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = history.contactName ?: history.phoneNumber,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = "$callTypeText • ${history.getFormattedDate()}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        if (onDelete != {}) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ContactItemRow(
    contact: Contact,
    networkState: com.videocall.app.data.NetworkState,
    onCallClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCallClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = Teal,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (contact.isFavorite) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Favori",
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
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
        IconButton(
            onClick = { onFavoriteClick() },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (contact.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = if (contact.isFavorite) "Favorilerden Kaldır" else "Favorilere Ekle",
                tint = if (contact.isFavorite) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
        Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "Ara",
            tint = if (networkState.isConnected) Teal else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .size(24.dp)
                .clickable { onCallClick() }
        )
    }
}

@Composable
private fun ChatPanel(
    messages: List<com.videocall.app.model.ChatMessage>,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    val listState = remember { androidx.compose.foundation.lazy.LazyListState() }
    
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Chat başlığı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Mesaj listesi
            androidx.compose.foundation.lazy.LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages.size) { index ->
                    val message = messages[index]
                    ChatMessageItem(
                        message = message,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Mesaj gönderme alanı
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Mesaj yazın...") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (messageText.isNotBlank()) {
                                    onSendMessage(messageText)
                                    messageText = ""
                                }
                            },
                            enabled = messageText.isNotBlank()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Gönder",
                                tint = if (messageText.isNotBlank()) Teal else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: com.videocall.app.model.ChatMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = if (message.isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .padding(horizontal = if (message.isFromMe) 32.dp else 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isFromMe) {
                    Teal.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!message.isFromMe && message.senderName != null) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (message.isFromMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isFromMe) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = message.getFormattedTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message.isFromMe) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun Placeholder(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(12.dp)
        )
    }
}

/**
 * Grup görüşme için grid layout
 */
@Composable
fun GroupCallGrid(
    participants: List<Participant>,
    isAudioOnly: Boolean,
    onLocalRendererReady: (SurfaceViewRenderer) -> Unit,
    onRemoteRendererReady: (Participant, SurfaceViewRenderer) -> Unit,
    modifier: Modifier = Modifier
) {
    // Grid boyutunu katılımcı sayısına göre belirle
    val gridColumns = when {
        participants.size <= 2 -> 1
        participants.size <= 4 -> 2
        participants.size <= 9 -> 3
        else -> 4
    }
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        modifier = modifier,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(participants) { participant ->
            ParticipantVideoItem(
                participant = participant,
                isAudioOnly = isAudioOnly,
                onLocalRendererReady = if (participant.isLocal) onLocalRendererReady else null,
                onRemoteRendererReady = if (!participant.isLocal) { renderer ->
                    onRemoteRendererReady(participant, renderer)
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
        }
    }
}

/**
 * Tek bir katılımcı için video item
 */
@Composable
fun ParticipantVideoItem(
    participant: Participant,
    isAudioOnly: Boolean,
    onLocalRendererReady: ((SurfaceViewRenderer) -> Unit)?,
    onRemoteRendererReady: ((SurfaceViewRenderer) -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Black)
    ) {
        if (!isAudioOnly && (participant.isVideoEnabled || participant.isLocal)) {
            // Video göster
            if (participant.isLocal && onLocalRendererReady != null) {
                VideoSurface(
                    modifier = Modifier.fillMaxSize(),
                    isOverlay = false,
                    onRendererReady = onLocalRendererReady
                )
            } else if (!participant.isLocal && onRemoteRendererReady != null) {
                VideoSurface(
                    modifier = Modifier.fillMaxSize(),
                    isOverlay = false,
                    onRendererReady = onRemoteRendererReady
                )
            }
            
            if (!participant.isVideoEnabled && !participant.isLocal) {
                // Video kapalı placeholder
                Placeholder(
                    text = participant.name ?: "Katılımcı",
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            // Audio-only veya video kapalı - Avatar göster
            Placeholder(
                text = participant.name ?: "Katılımcı",
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Katılımcı bilgisi overlay (alt kısım)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = participant.name ?: "Katılımcı",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Mikrofon durumu
                    Icon(
                        imageVector = if (participant.isMuted || !participant.isAudioEnabled) {
                            Icons.Default.MicOff
                        } else {
                            Icons.Default.Mic
                        },
                        contentDescription = null,
                        tint = if (participant.isMuted || !participant.isAudioEnabled) {
                            Danger
                        } else {
                            Color.White
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    
                    // Video durumu
                    if (!isAudioOnly) {
                        Icon(
                            imageVector = if (participant.isVideoEnabled) {
                                Icons.Default.Videocam
                            } else {
                                Icons.Default.VideocamOff
                            },
                            contentDescription = null,
                            tint = if (participant.isVideoEnabled) {
                                Color.White
                            } else {
                                Danger
                            },
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

