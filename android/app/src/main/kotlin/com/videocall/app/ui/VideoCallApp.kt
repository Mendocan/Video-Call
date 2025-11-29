@file:Suppress("UNUSED_PARAMETER", "UNUSED_ANONYMOUS_PARAMETER")

package com.videocall.app.ui

import android.content.pm.PackageManager
import android.os.Build

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.videocall.app.data.PreferencesManager
import com.videocall.app.data.SubscriptionManager
import com.videocall.app.ui.theme.VideoCallTheme
import com.videocall.app.viewmodel.VideoCallViewModel
import androidx.compose.material.icons.filled.QrCode
import android.content.Intent
import android.net.Uri

private enum class VideoCallDestination(val route: String, val label: String) {
    Welcome("welcome", "Hoşgeldiniz"),
    Home("home", "Ana Sayfa"),
    Call("call", "Aramalar"),
    Contacts("contacts", "Kişiler"),
    Legal("legal", "Yasal"),
    Settings("settings", "Ayarlar"),
    QRCode("qrcode", "QR Kod"),
    Calendar("calendar", "Takvim"),
    Chat("chat", "Sohbetler"),
    ShareApp("share", "Paylaş"),
    SubscriptionExpired("subscription_expired", "Abonelik Sona Erdi"),
    Groups("groups", "Gruplar"),
    CreateGroup("create_group", "Grup Oluştur"),
    BlockedUsers("blocked_users", "Engellenenler")
}

@Composable
fun VideoCallApp(viewModel: VideoCallViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val contactsPermission by viewModel.contactsPermissionGranted.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val addedContacts by viewModel.addedContacts.collectAsState()
    val incomingCall by viewModel.incomingCall.collectAsState()
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

    val destinations = listOf(
        VideoCallDestination.Home,
        VideoCallDestination.Chat,
        VideoCallDestination.Contacts,
        VideoCallDestination.Calendar
    )

    VideoCallTheme {
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
                                    VideoCallDestination.Call -> Icons.Default.VideoCall
                                    VideoCallDestination.Contacts -> Icons.Default.Group
                                    VideoCallDestination.Legal -> Icons.Default.Description
                                    VideoCallDestination.Settings -> Icons.Default.Settings
                                    VideoCallDestination.QRCode -> Icons.Default.QrCode
                                    VideoCallDestination.Calendar -> Icons.Default.CalendarToday
                                    VideoCallDestination.Chat -> Icons.Default.Description
                                    VideoCallDestination.ShareApp -> Icons.Default.Share
                                    VideoCallDestination.SubscriptionExpired -> Icons.Default.Settings
                                    VideoCallDestination.Groups -> Icons.Default.Group
                                    VideoCallDestination.CreateGroup -> Icons.Default.Group
                                    VideoCallDestination.BlockedUsers -> Icons.Default.Block
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
                    WelcomeScreen(
                        savedPhoneNumber = preferencesManager.getPhoneNumber(),
                        onLoginSuccess = { phoneNumber, rememberMe ->
                            if (rememberMe) {
                                preferencesManager.savePhoneNumber(phoneNumber)
                            } else {
                                // "Beni Hatırla" seçili değilse telefon numarasını sil
                                preferencesManager.savePhoneNumber("")
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
                            
                            navController.navigate(VideoCallDestination.Home.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onNavigateToLegal = {
                            navController.navigate(VideoCallDestination.Legal.route)
                        }
                    )
                }
                composable(VideoCallDestination.Home.route) {
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
                        uiState = uiState,
                        addedContacts = addedContacts,
                        networkState = networkState,
                        onNavigateToCall = {
                            // Görüşme başlatmadan önce abonelik kontrolü - Test için devre dışı
                            // TODO: Production'da abonelik kontrolünü aktif et
                            // if (subscriptionManager.hasActiveSubscription()) {
                                navController.navigate(VideoCallDestination.Call.route)
                            // } else {
                            //     navController.navigate(VideoCallDestination.SubscriptionExpired.route)
                            // }
                        },
                        onNavigateToContacts = { navController.navigate(VideoCallDestination.Contacts.route) },
                        onNavigateToLegal = { navController.navigate(VideoCallDestination.Legal.route) },
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
                        onToggleFavorite = { contact ->
                            viewModel.toggleFavoriteContact(contact)
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
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            val activity = callContext as? com.videocall.app.MainActivity
                            if (activity != null && callContext.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                                activity.enterPictureInPictureModeHelper()
                            } else {
                                navController.popBackStack()
                            }
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
                        isVoiceCommandsEnabled = isVoiceCommandsEnabled
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
                            val sendIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, json)
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, "Bağlantı Bilgilerini Paylaş")
                            context.startActivity(shareIntent)
                        },
                        onManualConnect = { json ->
                            viewModel.processQRCode(json)
                            navController.popBackStack()
                        }
                    )
                }
                composable(VideoCallDestination.Calendar.route) {
                    val scheduledCalls by viewModel.scheduledCalls.collectAsState()
                    
                    CalendarScreen(
                        scheduledCalls = scheduledCalls,
                        contacts = addedContacts,
                        onScheduleCall = { contactName, phoneNumber, scheduledTime, roomCode, notes ->
                            viewModel.scheduleCall(
                                contactName = contactName,
                                contactPhoneNumber = phoneNumber,
                                scheduledTime = scheduledTime,
                                roomCode = roomCode,
                                notes = notes
                            )
                        },
                        onCancelCall = { callId ->
                            viewModel.cancelScheduledCall(callId)
                        },
                        onCompleteCall = { callId ->
                            viewModel.completeScheduledCall(callId)
                        },
                        onDeleteCall = { callId ->
                            viewModel.deleteScheduledCall(callId)
                        }
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
                composable(VideoCallDestination.SubscriptionExpired.route) {
                    SubscriptionExpiredScreen(
                        onRenewSubscription = {
                            // Web sitesindeki pricing sayfasına yönlendir
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://videocall.app/pricing"))
                            context.startActivity(intent)
                        }
                    )
                }
            }
        }
    }
}

