package com.videocall.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.videocall.app.R
import com.videocall.app.data.PreferencesManager
import com.videocall.app.model.CallStatistics
import com.videocall.app.model.UserPresence
import com.videocall.app.model.UserStatus
import com.videocall.app.utils.AppShareManager
import com.videocall.app.utils.LocaleHelper
import com.videocall.app.utils.CameraCapabilities
import com.videocall.app.utils.ImagePicker
import com.videocall.app.model.VideoQuality
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Delete
import com.videocall.app.ui.theme.Teal
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    callHistory: List<com.videocall.app.model.CallHistory> = emptyList(),
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
    onLogout: () -> Unit = {}
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()
    
    // İzin launcher'ları
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // İzin durumu değişti, UI otomatik güncellenecek
    }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // İzin durumu değişti, UI otomatik güncellenecek
    }
    
    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        // İzin durumu değişti, UI otomatik güncellenecek
    }
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
            } catch (_: Exception) {
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
            // totalCalls > 0 kontrolü gereksiz çünkü else bloğundayız (callHistory.isEmpty() == false)
            val averageCallDuration = totalDuration / totalCalls
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

        // İzinler
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
                PermissionRow(
                    permissionName = "Rehber",
                    description = "Kişileri eklemek ve görüntülemek için",
                    icon = Icons.Default.Contacts,
                    isGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_CONTACTS
                    ) == PackageManager.PERMISSION_GRANTED,
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.READ_CONTACTS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                        }
                    }
                )
                PermissionRow(
                    permissionName = "Kamera",
                    description = "Video görüşmeleri için",
                    icon = Icons.Default.CameraAlt,
                    isGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED,
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                )
                PermissionRow(
                    permissionName = "Mikrofon",
                    description = "Sesli görüşmeler için",
                    icon = Icons.Default.Mic,
                    isGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED,
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }
        }

        // Profil Fotoğrafı
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Profil Fotoğrafı",
                    style = MaterialTheme.typography.titleMedium
                )
                
                var profilePhotoUri by remember {
                    mutableStateOf(preferencesManager.getProfilePhotoUri())
                }
                var showPhotoPickerDialog by remember { mutableStateOf(false) }
                
                val imagePickerLauncher = ImagePicker.rememberImagePickerLauncher { uri ->
                    if (uri != null) {
                        val fileName = "profile_${System.currentTimeMillis()}.jpg"
                        val copiedUri = ImagePicker.copyUriToAppStorage(context, uri, fileName)
                        if (copiedUri != null) {
                            profilePhotoUri = copiedUri.toString()
                            preferencesManager.saveProfilePhotoUri(copiedUri.toString())
                        }
                    }
                    showPhotoPickerDialog = false
                }
                
                // Profil fotoğrafı veya avatar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(3.dp, Teal, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val photoUri = profilePhotoUri
                    if (photoUri != null && photoUri.isNotBlank()) {
                        Image(
                            painter = rememberAsyncImagePainter(
                                ImageRequest.Builder(context)
                                    .data(photoUri)
                                    .crossfade(true)
                                    .build()
                            ),
                            contentDescription = "Profil Fotoğrafı",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Varsayılan avatar (baş harfler)
                        val phoneNumber = preferencesManager.getPhoneNumber()
                        val userName = phoneNumber?.let { 
                            // Telefon numarasından isim bulmaya çalış
                            // Şimdilik sadece baş harf göster
                            phoneNumber.take(1).uppercase()
                        } ?: "?"
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Teal.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.headlineLarge,
                                color = Teal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                // Fotoğraf seçme butonları
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showPhotoPickerDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.size(4.dp))
                        Text("Fotoğraf Seç")
                    }
                    
                    if (profilePhotoUri != null) {
                        OutlinedButton(
                            onClick = {
                                profilePhotoUri = null
                                preferencesManager.removeProfilePhoto()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text("Kaldır")
                        }
                    }
                }
                
                // Fotoğraf seçim dialog'u
                if (showPhotoPickerDialog) {
                    AlertDialog(
                        onDismissRequest = { showPhotoPickerDialog = false },
                        title = { Text("Profil Fotoğrafı Seç") },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Fotoğrafınızı nereden seçmek istersiniz?")
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    imagePickerLauncher.launch("image/*")
                                }
                            ) {
                                Text("Galeriden Seç")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showPhotoPickerDialog = false }) {
                                Text("İptal")
                            }
                        }
                    )
                }
            }
        }

        // Telefon Numarası Bilgisi
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Telefon Numarası",
                    style = MaterialTheme.typography.titleMedium
                )
                val savedPhoneNumber = preferencesManager.getPhoneNumber()
                if (savedPhoneNumber != null && savedPhoneNumber.isNotBlank()) {
                    Text(
                        text = savedPhoneNumber,
                        style = MaterialTheme.typography.bodyLarge,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Bu numara backend'e kayıt için kullanılıyor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Telefon numarası kayıtlı değil",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Uygulamayı kullanmak için telefon numaranızı kaydetmelisiniz (Welcome ekranından)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

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
                var verificationCode by remember { mutableStateOf("") }
                var verificationResult by remember { mutableStateOf<Boolean?>(null) }

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
                                val (_, codes) = onEnable2FA()
                                backupCodes = codes
                                val phoneNumber = preferencesManager.getPhoneNumber() ?: ""
                                qrCodeUrl = onGenerate2FAQR(phoneNumber)
                                // QR kod bitmap'ini oluştur
                                coroutineScope.launch {
                                    qrCodeBitmap = onGenerate2FAQRBitmap(phoneNumber)
                                }
                                show2FASetup = true
                                twoFAEnabled = true
                                verificationResult = null
                                verificationCode = ""
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
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "QR Kodu Tarayın ve Doğrulayın",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // QR kod görselleştirme
                                    if (qrCodeBitmap != null) {
                                        Image(
                                            bitmap = qrCodeBitmap!!.asImageBitmap(),
                                            contentDescription = "2FA QR Kodu",
                                            modifier = Modifier
                                                .fillMaxWidth(0.8f)
                                                .aspectRatio(1f),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Google Authenticator veya benzeri bir uygulama ile QR kodu tarayın.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        // Doğrulama Kodu Alanı
                                        OutlinedTextField(
                                            value = verificationCode,
                                            onValueChange = { code ->
                                                if (code.length <= 6 && code.all { it.isDigit() }) {
                                                    verificationCode = code
                                                }
                                            },
                                            label = { Text("6 Haneli Doğrulama Kodu") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.fillMaxWidth(0.8f),
                                            singleLine = true
                                        )

                                        Button(
                                            onClick = {
                                                verificationResult = onVerify2FA(verificationCode)
                                            },
                                            enabled = verificationCode.length == 6,
                                            modifier = Modifier.fillMaxWidth(0.8f)
                                        ) {
                                            Text("Doğrula")
                                        }

                                        verificationResult?.let { result ->
                                            val (text, color) = if (result) {
                                                "Doğrulama başarılı!" to Teal
                                            } else {
                                                "Doğrulama başarısız. Lütfen tekrar deneyin." to MaterialTheme.colorScheme.error
                                            }
                                            Text(
                                                text = text,
                                                color = color,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                    } else {
                                        // QR kod henüz oluşturulmadıysa
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
                        UserStatus.entries.forEach { status ->
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
                        mutableIntStateOf(preferencesManager.getAutoCleanupDays())
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
                                            contentColor = Teal
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

        // Görüntü Kalitesi Ayarları
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Görüntü Kalitesi",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Video görüşmelerinde kullanılacak çözünürlük ve frame rate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                val supports4K = remember { CameraCapabilities.supports4K(context) }
                val supports1080p60fps = remember { CameraCapabilities.supports1080p60fps(context) }
                
                var currentQuality by remember {
                    mutableStateOf(
                        try {
                            VideoQuality.valueOf(preferencesManager.getVideoQuality())
                        } catch (_: Exception) {
                            VideoQuality.HIGH
                        }
                    )
                }
                var showQualityDropdown by remember { mutableStateOf(false) }
                
                val availableQualities = remember(supports4K, supports1080p60fps) {
                    buildList {
                        if (supports1080p60fps) {
                            add(VideoQuality.FULLHD_60FPS)
                        }
                        add(VideoQuality.HIGH)
                        if (supports4K) {
                            add(VideoQuality.UHD_4K)
                        }
                        add(VideoQuality.MEDIUM)
                        add(VideoQuality.LOW)
                    }
                }
                
                // Mevcut kalite listede yoksa varsayılan olarak HIGH seç
                if (currentQuality !in availableQualities) {
                    currentQuality = VideoQuality.HIGH
                }
                
                OutlinedButton(
                    onClick = { showQualityDropdown = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (currentQuality) {
                            VideoQuality.FULLHD_60FPS -> "1080p - 60fps (FullHD)"
                            VideoQuality.HIGH -> "720p - 30fps (HD)"
                            VideoQuality.UHD_4K -> "2160p - 30fps (4K UHD)"
                            VideoQuality.MEDIUM -> "480p - 30fps"
                            VideoQuality.LOW -> "240p - 15fps"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                DropdownMenu(
                    expanded = showQualityDropdown,
                    onDismissRequest = { showQualityDropdown = false }
                ) {
                    availableQualities.forEach { quality ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    when (quality) {
                                        VideoQuality.FULLHD_60FPS -> "1080p - 60fps (FullHD)"
                                        VideoQuality.HIGH -> "720p - 30fps (HD)"
                                        VideoQuality.UHD_4K -> "2160p - 30fps (4K UHD)"
                                        VideoQuality.MEDIUM -> "480p - 30fps"
                                        VideoQuality.LOW -> "240p - 15fps"
                                    }
                                )
                            },
                            onClick = {
                                currentQuality = quality
                                preferencesManager.saveVideoQuality(quality.name)
                                showQualityDropdown = false
                            }
                        )
                    }
                }
                
                // Desteklenmeyen kaliteler hakkında bilgi
                if (!supports1080p60fps && !supports4K) {
                    Text(
                        text = "Cihazınız 1080p@60fps veya 4K çözünürlüğü desteklemiyor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                } else if (!supports4K) {
                    Text(
                        text = "Cihazınız 4K çözünürlüğü desteklemiyor",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
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
                        containerColor = Teal
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

                // Çıkış Yap butonu
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(
                        text = "Çıkış Yap",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
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

@Composable
private fun PermissionRow(
    permissionName: String,
    description: String,
    isGranted: Boolean,
    icon: ImageVector = Icons.Default.Contacts,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) Teal else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = permissionName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Cancel,
            contentDescription = if (isGranted) "İzin verildi" else "İzin verilmedi",
            tint = if (isGranted) Teal else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )
    }
}
