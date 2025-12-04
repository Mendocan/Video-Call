@file:Suppress("UNUSED_PARAMETER", "UNUSED_ANONYMOUS_PARAMETER")

package com.videocall.app.ui

import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.videocall.app.data.PreferencesManager
import com.videocall.app.data.SubscriptionManager
import com.videocall.app.ui.theme.VideoCallTheme
import com.videocall.app.viewmodel.VideoCallViewModel
import kotlinx.coroutines.launch

private enum class VideoCallDestination(val route: String, val label: String) {
    Welcome("welcome", "Hoş Geldiniz"),
    Home("home", "Ana Sayfa"),
    Call("call", "Görüşmeler"),
    Contacts("contacts", "Kişi Ekleme"),
    Legal("legal", "Yasal"),
    Settings("settings", "Ayarlar"),
    QRCode("qrcode", "QR Kod"),
    Stories("stories", "Hikayeler"),
    Chat("chat", "Sohbetler"),
    ShareApp("share", "Paylaş"),
    SubscriptionExpired("subscription_expired", "Abonelik Sona Erdi"),
    Groups("groups", "Gruplar"),
    CreateGroup("create_group", "Grup Oluştur"),
    BlockedUsers("blocked_users", "Engellenenler"),
    LiveStream("live_stream", "Canlı Yayın")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoCallApp(viewModel: VideoCallViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val contactsPermission by viewModel.contactsPermissionGranted.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val addedContacts by viewModel.addedContacts.collectAsState()
    val incomingCall by viewModel.incomingCall.collectAsState()
    val outgoingCall by viewModel.outgoingCall.collectAsState()
    val callHistory by viewModel.callHistory.collectAsState()
    val networkState by viewModel.networkState.collectAsState()
    val qrCodeBitmap by viewModel.qrCodeBitmap.collectAsState()
    val connectionInfoJson by viewModel.connectionInfoJson.collectAsState()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val subscriptionManager = remember { SubscriptionManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val appSecurityManager = remember { com.videocall.app.security.AppSecurityManager(context) }
    val playIntegrityManager = remember { com.videocall.app.security.PlayIntegrityManager(context) }
    
    // Widget intent'lerini işle
    LaunchedEffect(Unit) {
        val activity = context as? android.app.Activity
        val intent = activity?.intent
        when (intent?.action) {
            "com.videocall.app.OPEN_CALL" -> {
                navController.navigate(VideoCallDestination.Call.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
            "com.videocall.app.OPEN_CONTACTS" -> {
                navController.navigate(VideoCallDestination.Contacts.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // İlk açılış ve abonelik kontrolü
    LaunchedEffect(Unit) {
        // Güvenlik kontrolü yap
        val securityCheck = appSecurityManager.performSecurityCheck()
        if (!securityCheck.isSecure) {
            // Güvenlik sorunları tespit edildi - backend'e rapor gönder
            android.util.Log.w("VideoCallApp", "Güvenlik sorunları: ${securityCheck.issues.joinToString(", ")}")
            // TODO: Production'da backend'e güvenlik raporu gönder
            // Şimdilik sadece log'a yazıyoruz
        }
        
        // Play Integrity kontrolü (production'da aktif)
        coroutineScope.launch {
            val integrityResult = playIntegrityManager.verifyAppIntegrity()
            if (!integrityResult.isValid) {
                android.util.Log.w("VideoCallApp", "Play Integrity uyarısı: ${integrityResult.message}")
                // TODO: Production'da güvenilir olmayan cihazlarda uygulamayı kısıtla
            }
        }
        
        val isFirstLaunch = preferencesManager.isFirstLaunch()
        
        // İlk açılış değilse abonelik ve cihaz doğrulama kontrolü yap
        // TODO: Production'da abonelik kontrolünü aktif et
        // Test için geçici olarak devre dışı bırakıldı
        /*
        if (!isFirstLaunch) {
            val phoneNumber = preferencesManager.getPhoneNumber()
            
            // Telefon numarası varsa cihaz doğrulaması yap
            if (phoneNumber != null) {
                val deviceVerification = subscriptionManager.verifyDevice(phoneNumber)
                if (!deviceVerification.isAuthorized) {
                    // Cihaz yetkisiz veya limit aşıldı
                    if (navController.currentBackStackEntry == null) {
                        navController.navigate(VideoCallDestination.SubscriptionExpired.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                    return@LaunchedEffect
                }
            }
            
            val hasActiveSubscription = subscriptionManager.hasActiveSubscription()
            if (!hasActiveSubscription) {
                // Abonelik yoksa veya süresi dolmuşsa expired ekranına yönlendir
                if (navController.currentBackStackEntry == null) {
                    navController.navigate(VideoCallDestination.SubscriptionExpired.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                return@LaunchedEffect
            }
        }
        */
        
        val startDestination = if (isFirstLaunch) {
            VideoCallDestination.Welcome.route
        } else {
            VideoCallDestination.Home.route
        }
        if (navController.currentBackStackEntry == null) {
            navController.navigate(startDestination) {
                popUpTo(0) { inclusive = true }
            }
        }

        val granted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updateContactsPermission(granted)
    }

    // Tab bar: Sohbetler, Görüşmeler, Kişi Ekleme, Takvim
    val destinations = listOf(
        VideoCallDestination.Chat,      // Sohbetler
        VideoCallDestination.Call,      // Görüşmeler
        VideoCallDestination.Contacts,   // Kişi Ekleme
        VideoCallDestination.Stories    // Hikayeler
    )

    VideoCallTheme {
        // Giden arama ekranı (full screen overlay) - Öncelikli
        outgoingCall?.let { call ->
            OutgoingCallScreen(
                contactName = call.contactName,
                phoneNumber = call.phoneNumber,
                onCancel = {
                    viewModel.cancelOutgoingCall()
                }
            )
            return@VideoCallTheme
        }
        
        // Gelen arama ekranı (full screen overlay)
        incomingCall?.let { call ->
            IncomingCallScreen(
                callerName = call.callerName,
                callerPhoneNumber = call.callerPhoneNumber,
                isGroupCall = call.isGroupCall,
                groupName = call.groupName,
                memberCount = null, // ViewModel'den gelecek, şimdilik null
                onAccept = {
                    viewModel.acceptIncomingCall()
                    navController.navigate(VideoCallDestination.Call.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onReject = {
                    viewModel.rejectIncomingCall()
                }
            )
            return@VideoCallTheme
        }

        Scaffold(
            topBar = {
                // Welcome ekranında topBar gösterme
                val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                if (currentRoute != VideoCallDestination.Welcome.route) {
                    // Üstte sabit bağlantı durumu ve ayarlar ikonu
                    TopAppBar(
                        title = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Bağlantı durumu ve kayıt göstergesi
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Kayıt göstergesi (yeşil/kırmızı nokta)
                                    val isAnyRecording = uiState.isRecording || uiState.isOtherPartyRecording
                                    val recordingColor = if (isAnyRecording) {
                                        MaterialTheme.colorScheme.error // Kırmızı - kayıt yapılıyor
                                    } else {
                                        androidx.compose.ui.graphics.Color(0xFF4CAF50) // Yeşil - kayıt yapılmıyor
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(recordingColor, androidx.compose.foundation.shape.CircleShape)
                                    )
                                    
                                    Icon(
                                        imageVector = when (networkState.networkType) {
                                            com.videocall.app.data.NetworkType.WIFI -> Icons.Default.Wifi
                                            com.videocall.app.data.NetworkType.MOBILE_DATA -> Icons.Default.SignalCellularAlt
                                            com.videocall.app.data.NetworkType.NONE -> Icons.Default.SignalCellularAlt
                                        },
                                        contentDescription = null,
                                        tint = if (networkState.isConnected) com.videocall.app.ui.theme.Teal else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = when {
                                            uiState.statusMessage.isNotBlank() -> uiState.statusMessage
                                            networkState.isConnected && networkState.networkType == com.videocall.app.data.NetworkType.WIFI -> "Wi-Fi Bağlı"
                                            networkState.isConnected && networkState.networkType == com.videocall.app.data.NetworkType.MOBILE_DATA -> "Mobil Veri Bağlı"
                                            else -> "Bağlantı Yok"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (networkState.isConnected) com.videocall.app.ui.theme.Teal else MaterialTheme.colorScheme.error
                                    )
                                }
                                // Ayarlar ikonu
                                IconButton(onClick = { navController.navigate(VideoCallDestination.Settings.route) }) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Ayarlar",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = currentRoute == destination.route,
                            onClick = {
                                if (currentRoute != destination.route) {
                                    navController.navigate(destination.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                val icon = when (destination) {
                                    VideoCallDestination.Welcome -> Icons.Default.Home
                                    VideoCallDestination.Home -> Icons.Default.Home
                                    VideoCallDestination.Call -> Icons.Default.Call // Görüşmeler
                                    VideoCallDestination.Contacts -> Icons.Default.Person // Kişi Ekleme
                                    VideoCallDestination.Legal -> Icons.Default.Description
                                    VideoCallDestination.Settings -> Icons.Default.Settings
                                    VideoCallDestination.QRCode -> Icons.Default.QrCode
                                    VideoCallDestination.Stories -> Icons.Default.Videocam // Hikayeler
                                    VideoCallDestination.Chat -> Icons.AutoMirrored.Filled.Chat // Sohbetler
                                    VideoCallDestination.ShareApp -> Icons.Default.Share
                                    VideoCallDestination.SubscriptionExpired -> Icons.Default.Settings
                                    VideoCallDestination.Groups -> Icons.Default.Group
                                    VideoCallDestination.CreateGroup -> Icons.Default.Group
                                    VideoCallDestination.BlockedUsers -> Icons.Default.Block
                                    VideoCallDestination.LiveStream -> Icons.Default.Videocam
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = destination.label,
                                    tint = if (currentRoute == destination.route) {
                                        com.videocall.app.ui.theme.Teal
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            },
                            label = {
                                Text(
                                    text = destination.label,
                                    color = if (currentRoute == destination.route) {
                                        com.videocall.app.ui.theme.Teal
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = VideoCallDestination.Welcome.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(VideoCallDestination.Welcome.route) {
                    val viewModel: VideoCallViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                        factory = VideoCallViewModel.factory(LocalContext.current.applicationContext as android.app.Application)
                    )
                    val uiState by viewModel.uiState.collectAsState()
                    var registeredPhoneNumber by remember { mutableStateOf<String?>(null) }
                    
                    // Registered mesajı geldiğinde login'i tamamla
                    LaunchedEffect(uiState.statusMessage) {
                        val currentPhoneNumber = registeredPhoneNumber
                        if (uiState.statusMessage == "Sunucuya bağlandı" && currentPhoneNumber != null) {
                            val phoneNumber: String = currentPhoneNumber
                            if (preferencesManager.getPhoneNumber() != null) {
                                preferencesManager.setTermsAccepted(true)
                                preferencesManager.setFirstLaunchCompleted()
                                
                                coroutineScope.launch {
                                    val deviceVerification = subscriptionManager.registerDevice(phoneNumber)
                                    if (!deviceVerification.isAuthorized) {
                                        android.util.Log.e("VideoCallApp", "Cihaz kaydı başarısız: ${deviceVerification.message}")
                                    }
                                }
                                
                                navController.navigate(VideoCallDestination.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                    }
                    
                    WelcomeScreen(
                        savedPhoneNumber = preferencesManager.getPhoneNumber(),
                        onLoginSuccess = { phoneNumber, rememberMe ->
                            // Telefon numarasını her zaman kaydet (register için gerekli)
                            // "Beni Hatırla" sadece WelcomeScreen'i tekrar göstermemek için
                            preferencesManager.savePhoneNumber(phoneNumber)
                            if (!rememberMe) {
                                // "Beni Hatırla" seçili değilse sadece flag'i kaydet
                                // Telefon numarası silinmez (register için gerekli)
                                android.util.Log.d("VideoCallApp", "Beni Hatırla seçili değil, ancak telefon numarası kaydedildi (register için)")
                            }
                            preferencesManager.setTermsAccepted(true)
                            preferencesManager.setFirstLaunchCompleted()
                            
                            // Cihaz kaydı yap (APK paylaşımını önlemek için)
                            coroutineScope.launch {
                                val deviceVerification = subscriptionManager.registerDevice(phoneNumber)
                                if (!deviceVerification.isAuthorized) {
                                    // Cihaz kaydı başarısız - kullanıcıyı bilgilendir
                                    // TODO: Kullanıcıya hata mesajı göster (Snackbar veya AlertDialog)
                                    android.util.Log.e("VideoCallApp", "Cihaz kaydı başarısız: ${deviceVerification.message}")
                                }
                            }
                            
                            // Login sonrası izin kontrolü yap
                            val contactsPermissionGranted = ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.READ_CONTACTS
                            ) == PackageManager.PERMISSION_GRANTED
                            
                            // İzin yoksa Ayarlar'a yönlendir
                            if (!contactsPermissionGranted) {
                                navController.navigate(VideoCallDestination.Settings.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            } else {
                                navController.navigate(VideoCallDestination.Home.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        },
                        onNavigateToLegal = {
                            navController.navigate(VideoCallDestination.Legal.route)
                        }
                    )
                }
                composable(VideoCallDestination.Home.route) {
                    // HomeScreen'e geçildiğinde kullanıcıyı backend'e kaydet
                    LaunchedEffect(Unit) {
                        viewModel.registerToBackend()
                    }
                    
                    // Abonelik kontrolü - Test için geçici olarak devre dışı
                    // TODO: Production'da abonelik kontrolünü aktif et
                    /*
                    LaunchedEffect(Unit) {
                        val hasActiveSubscription = subscriptionManager.hasActiveSubscription()
                        if (!hasActiveSubscription) {
                            navController.navigate(VideoCallDestination.SubscriptionExpired.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                    */
                    
                    HomeScreen(
                        addedContacts = addedContacts,
                        onNavigateToSettings = { navController.navigate(VideoCallDestination.Settings.route) },
                        onStartCallWithContact = { contact ->
                            // Görüşme başlatmadan önce abonelik kontrolü - Test için devre dışı
                            // TODO: Production'da abonelik kontrolünü aktif et
                            // if (subscriptionManager.hasActiveSubscription()) {
                                viewModel.startCallWithContact(contact)
                                navController.navigate(VideoCallDestination.Call.route)
                            // } else {
                            //     navController.navigate(VideoCallDestination.SubscriptionExpired.route)
                            // }
                        },
                        onRemoveContact = { contact ->
                            viewModel.removeContact(contact)
                        },
                        onContactClick = { contact ->
                            // Kişiye tıklandığında chat'e git
                            navController.navigate(VideoCallDestination.Chat.route)
                        }
                    )
                }
                composable(VideoCallDestination.Call.route) {
                    // Abonelik kontrolü - Test için geçici olarak devre dışı
                    // TODO: Production'da abonelik kontrolünü aktif et
                    /*
                    LaunchedEffect(Unit) {
                        val hasActiveSubscription = subscriptionManager.hasActiveSubscription()
                        if (!hasActiveSubscription) {
                            navController.navigate(VideoCallDestination.SubscriptionExpired.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                    */
                    
                    // PiP modu için geri tuşu işleme
                    val callContext = LocalContext.current
                    BackHandler(enabled = uiState.isConnected) {
                        // Görüşme aktifse ve PiP destekleniyorsa, geri tuşuna basıldığında PiP moduna geç
                        // minSdk 26 olduğu için SDK_INT kontrolü gereksiz
                        val activity = callContext as? com.videocall.app.MainActivity
                        if (activity != null && callContext.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                            activity.enterPictureInPictureModeHelper()
                        } else {
                            navController.popBackStack()
                        }
                    }
                    
                    val isBluetoothConnected by viewModel.isBluetoothConnected.collectAsState()
                    val bluetoothDeviceName by viewModel.bluetoothDeviceName.collectAsState()
                    val audioRoute by viewModel.audioRoute.collectAsState()
                    val isVoiceCommandsEnabled by viewModel.isVoiceCommandsEnabled.collectAsState()
                    val isListening by viewModel.isListening.collectAsState()
                    
                    CallScreen(
                        uiState = uiState,
                        addedContacts = addedContacts,
                        callHistory = callHistory,
                        networkState = networkState,
                        onStartCall = {
                            // Görüşme başlatmadan önce abonelik kontrolü - Test için devre dışı
                            // TODO: Production'da abonelik kontrolünü aktif et
                            // if (subscriptionManager.hasActiveSubscription()) {
                                viewModel.startCall()
                            // } else {
                            //     navController.navigate(VideoCallDestination.SubscriptionExpired.route)
                            // }
                        },
                        onHangUp = viewModel::hangUp,
                        onToggleCamera = viewModel::toggleCamera,
                        onToggleMic = viewModel::toggleMicrophone,
                        onSwitchCamera = viewModel::switchCamera,
                        onToggleAudioOnly = viewModel::toggleAudioOnlyMode,
                        onSetBackgroundMode = viewModel::setBackgroundMode,
                        onCycleBackgroundMode = viewModel::cycleBackgroundMode,
                        onSetFilter = viewModel::setFilter,
                        onCycleFilter = viewModel::cycleFilter,
                        onSetAudioRoute = viewModel::setAudioRoute,
                        isBluetoothConnected = isBluetoothConnected,
                        bluetoothDeviceName = bluetoothDeviceName,
                        audioRoute = audioRoute,
                        isVoiceCommandsEnabled = isVoiceCommandsEnabled,
                        isListening = isListening,
                        onStartCallWithContact = { contact ->
                            // Görüşme başlatmadan önce abonelik kontrolü - Test için devre dışı
                            // TODO: Production'da abonelik kontrolünü aktif et
                            // if (subscriptionManager.hasActiveSubscription()) {
                                viewModel.startCallWithContact(contact)
                            // } else {
                            //     navController.navigate(VideoCallDestination.SubscriptionExpired.route)
                            // }
                        },
                        onLocalRendererReady = viewModel::attachLocalRenderer,
                        onRemoteRendererReady = viewModel::attachRemoteRenderer,
                        onNavigateToQRCode = { navController.navigate(VideoCallDestination.QRCode.route) },
                        onToggleFavorite = { contact ->
                            viewModel.toggleFavoriteContact(contact)
                        },
                        onDeleteCallHistory = { callHistoryId ->
                            viewModel.deleteCallHistory(callHistoryId)
                        },
                        onClearAllCallHistory = {
                            viewModel.clearAllCallHistory()
                        },
                        onSendChatMessage = viewModel::sendChatMessage,
                        onToggleChatVisibility = viewModel::toggleChatVisibility,
                        onShareFile = { uri, fileName, mimeType ->
                            viewModel.shareFile(uri, fileName, mimeType)
                        },
                        onToggleScreenSharing = viewModel::stopScreenSharing,
                        onRequestScreenSharing = {
                            // MediaProjection permission request
                            // CallScreen'de launcher ile yapılacak
                        }
                    )
                }
                composable(VideoCallDestination.Contacts.route) {
                    ContactsScreen(
                        onGenerateQRCode = {
                            viewModel.generateContactsQRCode()
                            navController.navigate(VideoCallDestination.QRCode.route)
                        },
                        onNavigateToCreateGroup = {
                            navController.navigate(VideoCallDestination.CreateGroup.route)
                        },
                        permissionGranted = contactsPermission,
                        contacts = contacts,
                        addedContacts = addedContacts,
                        onPermissionResult = { granted ->
                            viewModel.updateContactsPermission(granted)
                        },
                        onContactAdd = { contact ->
                            viewModel.addContact(contact)
                        },
                        onContactUpdate = { contact ->
                            viewModel.updateContact(contact)
                        }
                    )
                }
                composable(VideoCallDestination.Chat.route) {
                    ChatScreen(
                        viewModel = viewModel,
                        addedContacts = addedContacts
                    )
                }
                composable(VideoCallDestination.Legal.route) {
                    LegalScreen(
                        onBack = {
                            // Welcome ekranından geldiyse welcome'a, değilse geri git
                            if (navController.previousBackStackEntry?.destination?.route == VideoCallDestination.Welcome.route) {
                                navController.navigate(VideoCallDestination.Welcome.route) {
                                    popUpTo(VideoCallDestination.Welcome.route) { inclusive = true }
                                }
                            } else {
                                navController.popBackStack()
                            }
                        }
                    )
                }
                composable(VideoCallDestination.Settings.route) {
                    val isOfflineMode by viewModel.isOfflineMode.collectAsState()
                    val localIpAddress by viewModel.localIpAddress.collectAsState()
                    val discoveredPeers by viewModel.discoveredPeers.collectAsState()
                    val isVoiceCommandsEnabled by viewModel.isVoiceCommandsEnabled.collectAsState()
                    
                    SettingsScreen(
                        callHistory = callHistory,
                        onEnable2FA = {
                            viewModel.enable2FA()
                        },
                        onDisable2FA = {
                            viewModel.disable2FA()
                        },
                        onVerify2FA = { code ->
                            viewModel.verify2FACode(code)
                        },
                        onGenerate2FAQR = { accountName ->
                            viewModel.generate2FAQRCodeUrl(accountName)
                        },
                        onGenerate2FAQRBitmap = { accountName ->
                            kotlinx.coroutines.runBlocking {
                                viewModel.generate2FAQRCodeBitmapAsync(accountName)
                            }
                        },
                        onRegenerateBackupCodes = {
                            viewModel.regenerateBackupCodes()
                        },
                        onEnableOfflineMode = {
                            viewModel.enableOfflineMode()
                        },
                        onDisableOfflineMode = {
                            viewModel.disableOfflineMode()
                        },
                        onDiscoverPeers = {
                            viewModel.discoverLocalPeers()
                        },
                        isOfflineMode = isOfflineMode,
                        localIpAddress = localIpAddress,
                        discoveredPeers = discoveredPeers,
                        onEnableVoiceCommands = {
                            viewModel.enableVoiceCommands()
                        },
                        onDisableVoiceCommands = {
                            viewModel.disableVoiceCommands()
                        },
                        isVoiceCommandsEnabled = isVoiceCommandsEnabled,
                        onLogout = {
                            viewModel.logout()
                            // Welcome ekranına dön
                            navController.navigate(VideoCallDestination.Welcome.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable(VideoCallDestination.QRCode.route) {
                    QRCodeScreen(
                        qrBitmap = qrCodeBitmap,
                        connectionInfoJson = connectionInfoJson,
                        onClose = { navController.popBackStack() },
                        onQRCodeScanned = { qrData ->
                            viewModel.processQRCode(qrData)
                            navController.popBackStack()
                        },
                        onGenerateQR = viewModel::createSecureQRCode,
                        onShareConnection = { json ->
                            // Android Intent ile paylaş
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, json)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Bağlantı Bilgilerini Paylaş")
                            context.startActivity(shareIntent)
                        },
                        onManualConnect = { json ->
                            viewModel.processQRCode(json)
                            navController.popBackStack()
                        }
                    )
                }
                composable(VideoCallDestination.Stories.route) {
                    val stories by viewModel.stories.collectAsState()
                    StoriesScreen(
                        stories = stories,
                        viewModel = viewModel,
                        addedContacts = addedContacts
                    )
                }
                composable(VideoCallDestination.Groups.route) {
                    GroupsScreen(
                        onNavigateToCreateGroup = {
                            navController.navigate(VideoCallDestination.CreateGroup.route)
                        },
                        onNavigateToBlockedUsers = {
                            navController.navigate(VideoCallDestination.BlockedUsers.route)
                        }
                    )
                }
                composable(VideoCallDestination.CreateGroup.route) {
                    CreateGroupScreen(
                        addedContacts = addedContacts,
                        onCreateGroup = { groupName, members ->
                            viewModel.createGroup(groupName, members)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(VideoCallDestination.BlockedUsers.route) {
                    BlockedUsersScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(VideoCallDestination.LiveStream.route) {
                    LaunchedEffect(Unit) {
                        viewModel.loadGroups()
                    }
                    val groups by viewModel.groups.collectAsState()
                    LiveStreamSelectionScreen(
                        viewModel = viewModel,
                        addedContacts = addedContacts,
                        groups = groups,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(VideoCallDestination.SubscriptionExpired.route) {
                    SubscriptionExpiredScreen(
                        onRenewSubscription = {
                            // Web sitesindeki pricing sayfasına yönlendir
                            val intent = Intent(Intent.ACTION_VIEW, "https://videocall.app/pricing".toUri())
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}
