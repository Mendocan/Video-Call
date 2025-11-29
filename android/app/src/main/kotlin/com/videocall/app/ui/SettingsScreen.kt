package com.videocall.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.RadioButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.videocall.app.data.PreferencesManager
import com.videocall.app.model.CallStatistics
import com.videocall.app.model.UserStatus
import com.videocall.app.model.UserPresence
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.size
import com.videocall.app.utils.AppShareManager
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.videocall.app.utils.LocaleHelper
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import com.videocall.app.R
import android.graphics.Bitmap

@Composable
fun SettingsScreen(
    callHistory: List<com.videocall.app.model.CallHistory> = emptyList(),
    onCalculateStatistics: () -> CallStatistics = { CallStatistics() },
    onEnable2FA: () -> Pair<String, List<String>> = { Pair("", emptyList()) },
    onDisable2FA: () -> Unit = {},
    onVerify2FA: (String) -> Boolean = { false },
    onGenerate2FAQR: (String) -> String? = { null },
    onGenerate2FAQRBitmap: suspend (String) -> android.graphics.Bitmap? = { null },
    onRegenerateBackupCodes: () -> List<String> = { emptyList() },
    onEnableOfflineMode: () -> Unit = {},
    onDisableOfflineMode: () -> Unit = {},
    onDiscoverPeers: () -> Unit = {},
    isOfflineMode: Boolean = false,
    localIpAddress: String? = null,
    discoveredPeers: List<com.videocall.app.network.PeerDevice> = emptyList(),
    onEnableVoiceCommands: () -> Unit = {},
    onDisableVoiceCommands: () -> Unit = {},
    isVoiceCommandsEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    var currentLanguage by remember { 
        mutableStateOf(preferencesManager.getLanguage() ?: "tr")
    }
    var blockUnknownNumbers by remember { 
        mutableStateOf(preferencesManager.isBlockUnknownNumbers()) 
    }
    
    // User Status
    var currentStatus by remember {
        mutableStateOf(
            try {
                UserStatus.valueOf(preferencesManager.getUserStatus())
            } catch (e: Exception) {
                UserStatus.ONLINE
            }
        )
    }
    var customMessage by remember {
        mutableStateOf(preferencesManager.getCustomStatusMessage() ?: "")
    }
    var showStatusDropdown by remember { mutableStateOf(false) }
    
    val statistics = remember(callHistory) {
        if (callHistory.isEmpty()) {
            CallStatistics()
        } else {
            val totalCalls = callHistory.size
            val totalDuration = callHistory.sumOf { it.duration }
            val totalIncomingCalls = callHistory.count { it.callType == com.videocall.app.model.CallType.INCOMING }
            val totalOutgoingCalls = callHistory.count { it.callType == com.videocall.app.model.CallType.OUTGOING }
            val averageCallDuration = if (totalCalls > 0) totalDuration / totalCalls else 0L
            val longestCallDuration = callHistory.maxOfOrNull { it.duration } ?: 0L
            
            val contactCallCounts = callHistory
                .groupBy { it.contactName ?: it.phoneNumber }
                .mapValues { it.value.size }
            val favoriteContact = contactCallCounts.maxByOrNull { it.value }?.key
            
            CallStatistics(
                totalCalls = totalCalls,
                totalDuration = totalDuration,
                totalIncomingCalls = totalIncomingCalls,
                totalOutgoingCalls = totalOutgoingCalls,
                averageCallDuration = averageCallDuration,
                longestCallDuration = longestCallDuration,
                favoriteContact = favoriteContact
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Ayarlar",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        
        // Görüşme İstatistikleri
        if (statistics.totalCalls > 0) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Görüşme İstatistikleri",
                        style = MaterialTheme.typography.titleMedium
                    )
                    StatisticsRow("Toplam Görüşme", "${statistics.totalCalls}")
                    StatisticsRow("Toplam Süre", statistics.getFormattedTotalDuration())
                    StatisticsRow("Gelen Aramalar", "${statistics.totalIncomingCalls}")
                    StatisticsRow("Giden Aramalar", "${statistics.totalOutgoingCalls}")
                    StatisticsRow("Ortalama Süre", statistics.getFormattedAverageDuration())
                    StatisticsRow("En Uzun Görüşme", "${statistics.longestCallDuration / 60}dk ${statistics.longestCallDuration % 60}sn")
                    if (statistics.favoriteContact != null) {
                        StatisticsRow("En Çok Aranan", statistics.favoriteContact)
                    }
                }
            }
        }

        // Dil Seçimi
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = context.getString(R.string.settings_language),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = context.getString(R.string.settings_language_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Kompakt dil seçimi - DropdownMenu kullan
                var showLanguageDropdown by remember { mutableStateOf(false) }
                val currentLanguageName = LocaleHelper.getLanguageName(currentLanguage)
                
                OutlinedButton(
                    onClick = { showLanguageDropdown = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currentLanguageName,
                        modifier = Modifier.weight(1f)
                    )
                }
                DropdownMenu(
                    expanded = showLanguageDropdown,
                    onDismissRequest = { showLanguageDropdown = false }
                ) {
                    LocaleHelper.getSupportedLanguages().forEach { (code, name) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                currentLanguage = code
                                preferencesManager.setLanguage(code)
                                showLanguageDropdown = false
                                // Uygulamayı yeniden başlatmak için activity'yi restart et
                                (context as? android.app.Activity)?.recreate()
                            }
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = context.getString(R.string.app_info),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Video Call v2.0.0",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Görüntülü görüşme uygulaması",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // İki Faktörlü Doğrulama (2FA) Ayarları
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "İki Faktörlü Doğrulama (2FA)",
                    style = MaterialTheme.typography.titleMedium
                )
                var twoFAEnabled by remember {
                    mutableStateOf(preferencesManager.is2FAEnabled())
                }
                var show2FASetup by remember { mutableStateOf(false) }
                var showBackupCodes by remember { mutableStateOf(false) }
                var backupCodes by remember { mutableStateOf<List<String>>(emptyList()) }
                var qrCodeUrl by remember { mutableStateOf<String?>(null) }
                var qrCodeBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "2FA'yı Etkinleştir",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Hesabınızı ekstra güvenlik katmanı ile koruyun",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = twoFAEnabled,
                        onCheckedChange = {
                            if (it) {
                                // 2FA'yı etkinleştir
                                val (secret, codes) = onEnable2FA()
                                backupCodes = codes
                                val phoneNumber = preferencesManager.getPhoneNumber() ?: ""
                                qrCodeUrl = onGenerate2FAQR(phoneNumber)
                                // QR kod bitmap'ini oluştur
                                coroutineScope.launch {
                                    qrCodeBitmap = onGenerate2FAQRBitmap(phoneNumber)
                                }
                                show2FASetup = true
                                twoFAEnabled = true
                            } else {
                                // 2FA'yı devre dışı bırak
                                onDisable2FA()
                                twoFAEnabled = false
                                show2FASetup = false
                                showBackupCodes = false
                            }
                        }
                    )
                }
                
                if (twoFAEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (show2FASetup && qrCodeUrl != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "QR Kodu Tarayın",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // QR kod görselleştirme
                                    if (qrCodeBitmap != null) {
                                        Image(
                                            bitmap = qrCodeBitmap!!.asImageBitmap(),
                                            contentDescription = "2FA QR Kodu",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(1f),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Google Authenticator veya benzeri bir uygulama ile QR kodu tarayın",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else if (qrCodeUrl != null) {
                                        // QR kod henüz oluşturulmadıysa URL göster
                                        Text(
                                            text = "QR kod oluşturuluyor...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (backupCodes.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Yedek Kodlar",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                TextButton(
                                    onClick = {
                                        showBackupCodes = !showBackupCodes
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (showBackupCodes) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            
                            if (showBackupCodes) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "Bu kodları güvenli bir yerde saklayın. 2FA koduna erişemezseniz bu kodları kullanabilirsiniz.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        backupCodes.forEach { code ->
                                            Text(
                                                text = code,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                backupCodes = onRegenerateBackupCodes()
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Refresh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text("Yeni Kodlar Oluştur")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Durum Mesajı Ayarları
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Durum Mesajı",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Diğer kullanıcılara görünen durumunuzu ayarlayın",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Durum Seçimi
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showStatusDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = UserPresence(currentStatus, customMessage.takeIf { it.isNotBlank() }).getStatusText(),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    DropdownMenu(
                        expanded = showStatusDropdown,
                        onDismissRequest = { showStatusDropdown = false }
                    ) {
                        UserStatus.values().forEach { status ->
                            DropdownMenuItem(
                                text = { Text(UserPresence(status).getStatusText()) },
                                onClick = {
                                    currentStatus = status
                                    preferencesManager.saveUserStatus(status.name)
                                    showStatusDropdown = false
                                }
                            )
                        }
                    }
                    
                    // Özel Mesaj
                    OutlinedTextField(
                        value = customMessage,
                        onValueChange = { 
                            customMessage = it
                            preferencesManager.saveCustomStatusMessage(it.takeIf { msg -> msg.isNotBlank() })
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Özel Durum Mesajı (Opsiyonel)") },
                        placeholder = { Text("Örn: Toplantıdayım") },
                        singleLine = true
                    )
                }
            }
        }

        // Otomatik Temizleme Ayarları
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Görüşme Geçmişi Ayarları",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Otomatik Temizleme",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Eski görüşme geçmişini otomatik olarak sil",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = preferencesManager.isAutoCleanupEnabled(),
                        onCheckedChange = {
                            preferencesManager.setAutoCleanupEnabled(it)
                        }
                    )
                }
                if (preferencesManager.isAutoCleanupEnabled()) {
                    var cleanupDays by remember {
                        mutableStateOf(preferencesManager.getAutoCleanupDays())
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Temizleme Süresi: $cleanupDays gün",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(7, 14, 30, 60, 90).forEach { days ->
                                OutlinedButton(
                                    onClick = {
                                        cleanupDays = days
                                        preferencesManager.setAutoCleanupDays(days)
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = if (cleanupDays == days) {
                                        ButtonDefaults.outlinedButtonColors(
                                            contentColor = com.videocall.app.ui.theme.Teal
                                        )
                                    } else {
                                        ButtonDefaults.outlinedButtonColors()
                                    }
                                ) {
                                    Text("$days gün")
                                }
                            }
                        }
                    }
                }
            }
        }


        // Offline Mod Ayarları
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Offline Mod (Yerel Ağ)",
                    style = MaterialTheme.typography.titleMedium
                )
                var offlineModeEnabled by remember {
                    mutableStateOf(isOfflineMode)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Offline Modu Etkinleştir",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "İnternet olmadan yerel ağda görüşme yapın",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = offlineModeEnabled,
                        onCheckedChange = {
                            offlineModeEnabled = it
                            if (it) {
                                onEnableOfflineMode()
                            } else {
                                onDisableOfflineMode()
                            }
                        }
                    )
                }
                
                if (offlineModeEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (localIpAddress != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Yerel IP Adresi",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = localIpAddress,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        
                        OutlinedButton(
                            onClick = { onDiscoverPeers() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Eş Cihazları Keşfet")
                        }
                        
                        if (discoveredPeers.isNotEmpty()) {
                            Text(
                                text = "Keşfedilen Cihazlar: ${discoveredPeers.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            discoveredPeers.take(5).forEach { peer ->
                                Text(
                                    text = "• ${peer.deviceName}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sesli Komutlar Ayarları
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sesli Komutlar (Hands-Free)",
                    style = MaterialTheme.typography.titleMedium
                )
                var voiceCommandsEnabled by remember {
                    mutableStateOf(isVoiceCommandsEnabled)
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sesli Komutları Etkinleştir",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Görüşme sırasında sesli komutlarla kontrol edin",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = voiceCommandsEnabled,
                        onCheckedChange = {
                            voiceCommandsEnabled = it
                            if (it) {
                                onEnableVoiceCommands()
                            } else {
                                onDisableVoiceCommands()
                            }
                        }
                    )
                }
                
                if (voiceCommandsEnabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Kullanılabilir Komutlar:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• \"Mikrofonu kapat/aç\"",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• \"Kamerayı kapat/aç\"",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• \"Görüşmeyi sonlandır\"",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• \"Hoparlör\" / \"Bluetooth\"",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• \"Chat aç\"",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "• \"Ekran paylaş\"",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // Gizli Mod Ayarları
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Gizlilik Ayarları",
                    style = MaterialTheme.typography.titleMedium
                )
                var privacyModeEnabled by remember {
                    mutableStateOf(preferencesManager.isPrivacyModeEnabled())
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gizli Mod",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Görüşme geçmişi kaydedilmez",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = privacyModeEnabled,
                        onCheckedChange = {
                            privacyModeEnabled = it
                            preferencesManager.setPrivacyModeEnabled(it)
                        }
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Güvenlik Ayarları",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bilinmeyen Numaraları Engelle",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Sadece ekli kişilerden gelen aramalar kabul edilir",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = blockUnknownNumbers,
                        onCheckedChange = {
                            blockUnknownNumbers = it
                            preferencesManager.setBlockUnknownNumbers(it)
                        }
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "İzinler",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "• Kamera: Görüntülü görüşme için gerekli",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Mikrofon: Sesli görüşme için gerekli",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Rehber: Kişi davet etmek için gerekli",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Destek",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Sorularınız için:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "destek@videocall.app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Uygulamayı Paylaş
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Uygulamayı Paylaş",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Video Call'u arkadaşlarınızla paylaşın",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                val shareManager = remember { AppShareManager(context) }
                
                // Genel Paylaşım
                Button(
                    onClick = {
                        val shareIntent = shareManager.shareApp()
                        context.startActivity(shareIntent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = com.videocall.app.ui.theme.Teal
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text("Uygulamayı Paylaş", modifier = Modifier.padding(start = 8.dp))
                }
                
                // Hızlı Paylaşım Seçenekleri
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp
                    shareManager.shareViaWhatsApp()?.let { intent ->
                        OutlinedButton(
                            onClick = { context.startActivity(intent) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("WhatsApp", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    // Telegram
                    shareManager.shareViaTelegram()?.let { intent ->
                        OutlinedButton(
                            onClick = { context.startActivity(intent) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Telegram", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // E-posta
                    OutlinedButton(
                        onClick = {
                            val intent = shareManager.shareViaEmail()
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("E-posta", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    // SMS
                    OutlinedButton(
                        onClick = {
                            val intent = shareManager.shareViaSMS()
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("SMS", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatisticsRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

