@file:Suppress("UNUSED_PARAMETER", "UNUSED_ANONYMOUS_PARAMETER", "UNUSED_VARIABLE")

package com.videocall.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.videocall.app.BuildConfig
import com.videocall.app.data.ContactsRepository
import com.videocall.app.data.InMemoryPresenceRepository
import com.videocall.app.data.NetworkManager
import com.videocall.app.data.NetworkState
import com.videocall.app.data.PreferencesManager
import com.videocall.app.data.PresenceRepository
import com.videocall.app.network.NetworkUtils
import com.videocall.app.security.QRCodeManager
import com.videocall.app.security.SecurityManager
import com.videocall.app.model.CallHistory
import com.videocall.app.model.CallStatistics
import com.videocall.app.model.CallType
import com.videocall.app.model.CallUiState
import com.videocall.app.model.ChatMessage
import com.videocall.app.model.Contact
import com.videocall.app.model.Participant
import com.videocall.app.model.PresenceEntry
import com.videocall.app.model.SharedFile
import com.videocall.app.model.FileTransferState
import com.videocall.app.model.ScheduledCall
import com.videocall.app.model.TwoFactorAuth
import com.videocall.app.security.TotpManager
import com.videocall.app.network.NetworkDiscovery
import com.videocall.app.audio.BluetoothAudioManager
import com.videocall.app.audio.AudioRoute
import com.videocall.app.voice.VoiceCommandManager
import com.videocall.app.voice.VoiceCommand
import com.videocall.app.utils.CalendarManager
import android.net.Uri
import android.content.ContentResolver
import android.content.Intent
import java.io.File
import java.io.FileOutputStream
import android.util.Base64
import com.videocall.app.rtc.RtcClient
import com.videocall.app.rtc.RtcEvent
import com.videocall.app.utils.NotificationManager
import com.videocall.app.utils.CallRecorder
import com.videocall.app.utils.VideoProcessor
import com.videocall.app.widget.VideoCallWidgetProvider
import com.videocall.app.signaling.LocalSignalingServer
import com.videocall.app.signaling.PeerSignalingClient
import com.videocall.app.signaling.SignalingClient
import com.videocall.app.signaling.SignalingMessage
import com.videocall.app.signaling.SignalingStatus
import com.videocall.app.utils.SignalingServerDiscovery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import org.json.JSONArray
import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

class VideoCallViewModel(
    private val rtcClient: RtcClient,
    initialSignalingClient: SignalingClient, // Mutable yapacağız
    private val contentResolver: ContentResolver,
    private val preferencesManager: PreferencesManager,
    private val networkManager: NetworkManager,
    @Suppress("StaticFieldLeak")
    private val context: Context
) : ViewModel() {
    
    // SignalingClient'ı mutable yap (backend'den IP alındıktan sonra güncellenecek)
    private var signalingClient: SignalingClient = initialSignalingClient

    private val _uiState = MutableStateFlow(CallUiState())
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()

    private val _contactsPermissionGranted = MutableStateFlow(false)
    val contactsPermissionGranted: StateFlow<Boolean> = _contactsPermissionGranted.asStateFlow()

    private val _contacts = MutableStateFlow<List<Contact>>(emptyList())
    val contacts: StateFlow<List<Contact>> = _contacts.asStateFlow()

    private val _addedContacts = MutableStateFlow<List<Contact>>(emptyList())
    val addedContacts: StateFlow<List<Contact>> = _addedContacts.asStateFlow()

    private val _incomingCall = MutableStateFlow<IncomingCall?>(null)
    val incomingCall: StateFlow<IncomingCall?> = _incomingCall.asStateFlow()

    private val _callHistory = MutableStateFlow<List<CallHistory>>(emptyList())
    val callHistory: StateFlow<List<CallHistory>> = _callHistory.asStateFlow()
    
    // Kişiye özel chat mesajları: phoneNumber -> List<ChatMessage>
    private val _chatMessagesByContact = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val chatMessagesByContact: StateFlow<Map<String, List<ChatMessage>>> = _chatMessagesByContact.asStateFlow()
    
    private val _callStatistics = MutableStateFlow(CallStatistics())
    val callStatistics: StateFlow<CallStatistics> = _callStatistics.asStateFlow()
    
    private val _scheduledCalls = MutableStateFlow<List<ScheduledCall>>(emptyList())
    val scheduledCalls: StateFlow<List<ScheduledCall>> = _scheduledCalls.asStateFlow()

    private val _networkState = MutableStateFlow<NetworkState>(NetworkState(
        isConnected = false,
        networkType = com.videocall.app.data.NetworkType.NONE,
        isWifiEnabled = false,
        isMobileDataEnabled = false
    ))
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _qrCodeBitmap = MutableStateFlow<android.graphics.Bitmap?>(null)
    val qrCodeBitmap: StateFlow<android.graphics.Bitmap?> = _qrCodeBitmap.asStateFlow()
    
    private val _connectionInfoJson = MutableStateFlow<String?>(null)
    val connectionInfoJson: StateFlow<String?> = _connectionInfoJson.asStateFlow()

    private val contactsRepository = ContactsRepository(contentResolver)
    private val qrCodeManager = QRCodeManager()
    private val securityManager = SecurityManager()
    private val presenceRepository: PresenceRepository = InMemoryPresenceRepository
    private val peerSignalingClient = PeerSignalingClient()
    private val notificationManager = NotificationManager(context)
    private val callRecorder = CallRecorder(context)
    private var videoProcessor: VideoProcessor? = null
    private val backgroundModeRef = AtomicReference(com.videocall.app.model.BackgroundMode.NONE)
    private val filterTypeRef = AtomicReference(com.videocall.app.model.FilterType.NONE)
    
    // Manager instances
    private val bluetoothAudioManager = BluetoothAudioManager(context)
    private val voiceCommandManager = VoiceCommandManager(context)
    private val networkDiscovery = NetworkDiscovery(context)
    private val calendarManager = CalendarManager(context)
    
    // Expose StateFlows from managers
    val isBluetoothConnected: StateFlow<Boolean> = bluetoothAudioManager.isBluetoothConnected
    val bluetoothDeviceName: StateFlow<String?> = bluetoothAudioManager.bluetoothDeviceName
    val audioRoute: StateFlow<AudioRoute> = bluetoothAudioManager.audioRoute
    val isVoiceCommandsEnabled: StateFlow<Boolean> = voiceCommandManager.isEnabled
    val isListening: StateFlow<Boolean> = voiceCommandManager.isListening
    val lastCommand: StateFlow<String?> = voiceCommandManager.lastCommand
    val isOfflineMode: StateFlow<Boolean> = networkDiscovery.isOfflineMode
    val localIpAddress: StateFlow<String?> = networkDiscovery.localIpAddress
    val discoveredPeers: StateFlow<List<com.videocall.app.network.PeerDevice>> = networkDiscovery.discoveredPeers

    private var localSignalingServer: LocalSignalingServer? = null
    private var localServerPort: Int = NetworkUtils.generateRandomPort()
    private var localServerToken: String? = null
    private var lastPublishedPresenceHash: String? = null
    private var signalingMode: SignalingMode = SignalingMode.CLOUD
    private var directChannel: DirectChannel = DirectChannel.NONE

    private var activeRoom: String? = null
    private var callDurationJob: Job? = null // Görüşme süresi takibi için
    private val _participants = MutableStateFlow<List<Participant>>(emptyList())
    val participants: StateFlow<List<Participant>> = _participants.asStateFlow()

    // handleIncomingCall fonksiyonunu ekle
    private fun handleIncomingCall(message: SignalingMessage.IncomingCall) {
        val incomingCall = IncomingCall(
            callerPhoneNumber = message.callerPhoneNumber,
            callerName = message.callerName,
            roomCode = message.roomCode,
            groupId = message.groupId,
            isGroupCall = message.isGroupCall,
            groupName = message.groupName
        )
        _incomingCall.value = incomingCall
        
        // Bildirim göster
        val displayName = message.callerName ?: message.callerPhoneNumber
        notificationManager.showIncomingCallNotification(
            callerName = displayName,
            callerPhoneNumber = message.callerPhoneNumber,
            roomCode = message.roomCode
        )
    }

    data class IncomingCall(
        val callerPhoneNumber: String,
        val callerName: String?,
        val roomCode: String,
        val groupId: String,
        val isGroupCall: Boolean = false,
        val groupName: String? = null
    )

    private enum class SignalingMode { CLOUD, DIRECT }
    private enum class DirectChannel { NONE, SERVER, CLIENT }
    private enum class SignalingSource { CLOUD, DIRECT_SERVER, DIRECT_CLIENT }

    init {
        observeRtcEvents()
        observeSignaling()
        observeSignalingStatus()
        ensureLocalSignalingServer()
        observePeerMessages()
        loadAddedContacts()
        loadCallHistory()
        loadScheduledCalls()
        syncCalendarEvents() // Takvimden randevuları oku
        observeNetworkState()
        initializeLocalParticipant()
        viewModelScope.launch {
            callHistory.collect {
                _callStatistics.value = calculateCallStatistics(it) // Geçmiş değiştikçe istatistikleri güncelle
            }
        }
        viewModelScope.launch {
            participants.collect { participantsList ->
                _uiState.update { it.copy(participantsList = participantsList, participants = participantsList.size, isGroupCall = participantsList.size > 2) }
            }
        }
        // Uygulama açıldığında otomatik temizleme yap
        performAutoCleanup()
        
        // Voice commands listener
        voiceCommandManager.setCommandListener { command ->
            handleVoiceCommand(command)
        }
        
        // Load voice commands preference
        if (preferencesManager.isVoiceCommandsEnabled()) {
            voiceCommandManager.enable()
        }
        
        // Otomatik IP bulma ve signaling client güncelleme
        viewModelScope.launch {
            try {
                android.util.Log.d("VideoCallViewModel", "Signaling server IP'si aranıyor...")
                val discoveredUrl = SignalingServerDiscovery.getSignalingServerUrl(BuildConfig.SIGNALING_URL)
                
                // Eğer farklı bir URL bulunduysa, yeni client oluştur
                if (discoveredUrl != BuildConfig.SIGNALING_URL) {
                    android.util.Log.d("VideoCallViewModel", "Yeni signaling server URL bulundu: $discoveredUrl")
                    // Eski client'ı kapat
                    signalingClient.close()
                    // Yeni client oluştur
                    signalingClient = SignalingClient(discoveredUrl)
                    android.util.Log.d("VideoCallViewModel", "Signaling client güncellendi")
                } else {
                    android.util.Log.d("VideoCallViewModel", "Default signaling URL kullanılıyor: $discoveredUrl")
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoCallViewModel", "Signaling server discovery hatası", e)
                // Hata olsa bile devam et, default URL kullanılacak
            }
            
            // Uygulama açıldığında otomatik kayıt
            val phoneNumber = preferencesManager.getPhoneNumber()
            if (phoneNumber != null) {
                val name = _addedContacts.value.find { it.phoneNumber == phoneNumber }?.name
                try {
                    android.util.Log.d("VideoCallViewModel", "Kullanıcı kaydı başlatılıyor: phoneNumber=$phoneNumber, name=$name")
                    signalingClient.register(phoneNumber, name)
                    android.util.Log.d("VideoCallViewModel", "Kullanıcı kaydı mesajı gönderildi")
                } catch (e: Exception) {
                    android.util.Log.e("VideoCallViewModel", "Kayıt hatası", e)
                    _uiState.update { it.copy(statusMessage = "Sunucuya bağlanılamadı: ${e.message}") }
                }
            } else {
                android.util.Log.w("VideoCallViewModel", "Telefon numarası kayıtlı değil, otomatik kayıt yapılamadı")
            }
        }
    }
    
    private fun initializeLocalParticipant() {
        val phoneNumber = preferencesManager.getPhoneNumber()
        val localParticipant = Participant(
            id = "local_${System.currentTimeMillis()}",
            name = phoneNumber ?: "Ben",
            phoneNumber = phoneNumber,
            isLocal = true,
            isVideoEnabled = _uiState.value.isCameraEnabled,
            isAudioEnabled = _uiState.value.isMicEnabled
        )
        _participants.value = listOf(localParticipant)
    }

    private fun observeNetworkState() {
        viewModelScope.launch {
            networkManager.observeNetworkState().collect { state ->
                _networkState.value = state
                // Ağ durumu değiştiğinde UI'ı güncelle
                if (!state.isConnected) {
                    _uiState.update { it.copy(statusMessage = "İnternet bağlantısı yok") }
                }
            }
        }
    }

    private fun ensureLocalSignalingServer() {
        if (localSignalingServer != null) return
        viewModelScope.launch {
            try {
                val server = LocalSignalingServer(
                    port = localServerPort,
                    tokenProvider = { localServerToken },
                    onPeerStatusChanged = { connected ->
                        directChannel = if (connected) DirectChannel.SERVER else DirectChannel.NONE
                        if (connected && signalingMode == SignalingMode.DIRECT) {
                            // QR okutan taraf bağlandı, sunucu tarafında offer gönder
                            viewModelScope.launch {
                                try {
                                    val offer = rtcClient.createOffer()
                                    sendOffer(offer)
                                    _uiState.update { it.copy(statusMessage = "Teklif gönderildi, yanıt bekleniyor...") }
                                } catch (e: Exception) {
                                    _uiState.update { it.copy(statusMessage = "Teklif gönderilemedi: ${e.message}") }
                                }
                            }
                        } else if (!connected && signalingMode == SignalingMode.DIRECT) {
                            _uiState.update { it.copy(statusMessage = "Yerel signaling bağlantısı kapandı") }
                        }
                    }
                )
                localSignalingServer = server
                server.start()
                server.incoming.collect { message ->
                    handleSignalingMessage(message, SignalingSource.DIRECT_SERVER)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Yerel signaling sunucusu başlatılamadı: ${e.message}") }
                localSignalingServer = null
            }
        }
    }

    private fun observePeerMessages() {
        viewModelScope.launch {
            peerSignalingClient.messages.collect { message ->
                handleSignalingMessage(message, SignalingSource.DIRECT_CLIENT)
            }
        }
    }

    private fun handleIncomingOffer(source: SignalingSource, offer: SignalingMessage.Offer) {
        val callerPhone = extractPhoneFromOffer(offer)
        val isBlocked = preferencesManager.isBlockUnknownNumbers() &&
            !_addedContacts.value.any { it.phoneNumber == callerPhone }

        if (isBlocked) {
            if (source == SignalingSource.CLOUD) {
                signalingClient.leave()
            } else {
                peerSignalingClient.close()
            }
            _uiState.update { it.copy(statusMessage = "Bilinmeyen numara engellendi") }
            return
        }

        val callerName = _addedContacts.value.find { it.phoneNumber == callerPhone }?.name
        val incomingCall = IncomingCall(
            callerPhoneNumber = callerPhone ?: "Bilinmeyen",
            callerName = callerName,
            roomCode = activeRoom ?: "",
            groupId = "", // Eski sistem için boş
            isGroupCall = false,
            groupName = null
        )
        _incomingCall.value = incomingCall
        
        // Bildirim göster
        notificationManager.showIncomingCallNotification(
            callerName = callerName ?: callerPhone ?: "Bilinmeyen",
            callerPhoneNumber = callerPhone ?: "Bilinmeyen",
            roomCode = activeRoom ?: ""
        )

        // Cloud signaling ile bağlantı - answer otomatik gönderilir (joinCall çağrıldığında)
        // DIRECT modu kaldırıldı - sadece cloud signaling kullanılıyor
    }

    fun updateContactsPermission(granted: Boolean) {
        _contactsPermissionGranted.value = granted
        if (granted) {
            loadContacts()
        }
    }

    fun loadContacts() {
        viewModelScope.launch {
            try {
                val contactsList = contactsRepository.getAllContacts()
                _contacts.value = contactsList
            } catch (e: Exception) {
                _contacts.value = emptyList()
            }
        }
    }

    fun addContact(contact: Contact) {
        val currentList = _addedContacts.value.toMutableList()
        if (!currentList.any { it.id == contact.id }) {
            currentList.add(contact)
            _addedContacts.value = currentList
            saveAddedContacts()
        }
    }

    fun removeContact(contact: Contact) {
        val currentList = _addedContacts.value.toMutableList()
        currentList.removeAll { it.id == contact.id }
        _addedContacts.value = currentList
        saveAddedContacts()
    }
    
    fun toggleFavoriteContact(contact: Contact) {
        val currentList = _addedContacts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            val updatedContact = currentList[index].copy(isFavorite = !currentList[index].isFavorite)
            currentList[index] = updatedContact
            _addedContacts.value = currentList
            saveAddedContacts()
        }
    }
    
    fun updateContact(contact: Contact) {
        val currentList = _addedContacts.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            currentList[index] = contact
            _addedContacts.value = currentList.sortedWith(compareBy({ !it.isFavorite }, { it.name }))
            saveAddedContacts()
        } else {
            // Eğer eklenmiş kişilerde yoksa, ekle
            addContact(contact)
        }
    }

    fun startCallWithContact(contact: Contact) {
        val networkState = _networkState.value
        if (!networkState.isConnected) {
            _uiState.update { it.copy(statusMessage = "İnternet bağlantısı gerekli. ${networkManager.getNetworkTypeText(networkState.networkType)}") }
            return
        }

        // Engellenenler listesi kontrolü
        if (contact.phoneNumber != null && preferencesManager.isUserBlocked(contact.phoneNumber)) {
            _uiState.update { it.copy(statusMessage = "${contact.name} engellenenler listenizde.") }
            return
        }

        viewModelScope.launch {
            val usedDirect = contact.phoneNumber != null && attemptDirectCall(contact)
            if (usedDirect) {
                return@launch
            }

            signalingMode = SignalingMode.CLOUD
            directChannel = DirectChannel.NONE
            
            // Yeni sistem: Direkt arama başlat
            val myPhoneNumber = preferencesManager.getPhoneNumber()
            val myName = _addedContacts.value.find { it.phoneNumber == myPhoneNumber }?.name
            
            if (contact.phoneNumber == null) {
                _uiState.update { it.copy(statusMessage = "Kişinin telefon numarası bulunamadı.") }
                return@launch
            }
            
            if (myPhoneNumber == null) {
                _uiState.update { it.copy(statusMessage = "Kendi telefon numaranız kayıtlı değil.") }
                return@launch
            }
            
            _uiState.update { it.copy(statusMessage = "Arama başlatılıyor...") }
            
            android.util.Log.d("VideoCallViewModel", "Arama başlatılıyor: target=${contact.phoneNumber}, caller=$myPhoneNumber")
            signalingClient.startCall(
                targetPhoneNumber = contact.phoneNumber,
                callerPhoneNumber = myPhoneNumber,
                callerName = myName
            )
            android.util.Log.d("VideoCallViewModel", "Arama mesajı gönderildi")
            
            addCallToHistory(
                contactName = contact.name,
                phoneNumber = contact.phoneNumber,
                callType = CallType.OUTGOING,
                roomCode = "" // Room code backend'den gelecek
            )
        }
    }

    fun createGroup(groupName: String, memberPhoneNumbers: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Grup oluşturuluyor...") }
            signalingClient.createGroup(groupName, memberPhoneNumbers)
        }
    }

    fun startGroupCall(groupId: String, groupName: String) {
        val networkState = _networkState.value
        if (!networkState.isConnected) {
            _uiState.update { it.copy(statusMessage = "İnternet bağlantısı gerekli.") }
            return
        }

        viewModelScope.launch {
            val myPhoneNumber = preferencesManager.getPhoneNumber()
            val myName = _addedContacts.value.find { it.phoneNumber == myPhoneNumber }?.name

            if (myPhoneNumber == null) {
                _uiState.update { it.copy(statusMessage = "Kendi telefon numaranız kayıtlı değil.") }
                return@launch
            }

            _uiState.update { it.copy(statusMessage = "Grup araması başlatılıyor...") }

            signalingClient.startGroupCall(
                groupId = groupId,
                callerPhoneNumber = myPhoneNumber,
                callerName = myName
            )
        }
    }

    private fun loadAddedContacts() {
        viewModelScope.launch {
            try {
                val json = preferencesManager.getAddedContacts()
                if (json != null) {
                    val contactsList = parseContactsFromJson(json)
                    _addedContacts.value = contactsList
                }
            } catch (e: Exception) {
                _addedContacts.value = emptyList()
            }
        }
    }

    private fun saveAddedContacts() {
        viewModelScope.launch {
            try {
                val json = contactsToJson(_addedContacts.value)
                preferencesManager.saveAddedContacts(json)
            } catch (e: Exception) {
                // Ignore save errors
            }
        }
    }

    private fun contactsToJson(contacts: List<Contact>): String {
        val jsonArray = JSONArray()
        contacts.forEach { contact ->
            val jsonObject = JSONObject()
            jsonObject.put("id", contact.id)
            jsonObject.put("name", contact.name)
            jsonObject.put("phoneNumber", contact.phoneNumber ?: "")
            jsonObject.put("email", contact.email ?: "")
            jsonObject.put("photoUri", contact.photoUri ?: "")
            jsonObject.put("isFavorite", contact.isFavorite)
            jsonObject.put("notes", contact.notes ?: "")
            if (contact.groups.isNotEmpty()) {
                val groupsArray = JSONArray()
                contact.groups.forEach { groupsArray.put(it) }
                jsonObject.put("groups", groupsArray)
            }
            if (contact.status != null) {
                jsonObject.put("status", contact.status.name)
            }
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    private fun parseContactsFromJson(json: String): List<Contact> {
        val contactsList = mutableListOf<Contact>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val groupsList = mutableListOf<String>()
            if (jsonObject.has("groups")) {
                val groupsArray = jsonObject.getJSONArray("groups")
                for (j in 0 until groupsArray.length()) {
                    groupsList.add(groupsArray.getString(j))
                }
            }
            val statusString = jsonObject.optString("status", "").takeIf { it.isNotEmpty() }
            val status = statusString?.let {
                try {
                    com.videocall.app.model.UserStatus.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }
            contactsList.add(
                Contact(
                    id = jsonObject.getLong("id"),
                    name = jsonObject.getString("name"),
                    phoneNumber = jsonObject.optString("phoneNumber").takeIf { it.isNotEmpty() },
                    email = jsonObject.optString("email").takeIf { it.isNotEmpty() },
                    photoUri = jsonObject.optString("photoUri").takeIf { it.isNotEmpty() },
                    isFavorite = jsonObject.optBoolean("isFavorite", false),
                    notes = jsonObject.optString("notes").takeIf { it.isNotEmpty() },
                    groups = groupsList,
                    status = status
                )
            )
        }
        return contactsList
    }

    private fun loadCallHistory() {
        viewModelScope.launch {
            try {
                val json = preferencesManager.getCallHistory()
                if (json != null) {
                    val historyList = parseCallHistoryFromJson(json)
                    // Otomatik temizleme kontrolü
                    val cleanedList = if (preferencesManager.isAutoCleanupEnabled()) {
                        autoCleanupCallHistory(historyList)
                    } else {
                        historyList
                    }
                    _callHistory.value = cleanedList.sortedByDescending { it.timestamp }
                }
            } catch (e: Exception) {
                _callHistory.value = emptyList()
            }
        }
    }
    
    private fun autoCleanupCallHistory(historyList: List<CallHistory>): List<CallHistory> {
        val cleanupDays = preferencesManager.getAutoCleanupDays()
        val cutoffTime = System.currentTimeMillis() - (cleanupDays * 24 * 60 * 60 * 1000L)
        val cleanedList = historyList.filter { it.timestamp >= cutoffTime }
        
        // Eğer temizleme yapıldıysa, son temizleme tarihini güncelle
        if (cleanedList.size < historyList.size) {
            preferencesManager.setLastCleanupDate(System.currentTimeMillis())
        }
        
        return cleanedList
    }
    
    fun performAutoCleanup() {
        if (preferencesManager.isAutoCleanupEnabled()) {
            viewModelScope.launch {
                val currentList = _callHistory.value
                val cleanedList = autoCleanupCallHistory(currentList)
                if (cleanedList.size < currentList.size) {
                    _callHistory.value = cleanedList.sortedByDescending { it.timestamp }
                    saveCallHistory()
                }
            }
        }
    }

    private fun saveCallHistory() {
        viewModelScope.launch {
            try {
                val json = callHistoryToJson(_callHistory.value)
                preferencesManager.saveCallHistory(json)
            } catch (e: Exception) {
                // Ignore save errors
            }
        }
    }
    
    fun deleteCallHistory(callHistoryId: Long) {
        val currentList = _callHistory.value.toMutableList()
        currentList.removeAll { it.id == callHistoryId }
        _callHistory.value = currentList
        saveCallHistory()
    }
    
    fun clearAllCallHistory() {
        _callHistory.value = emptyList()
        saveCallHistory()
    }

    private fun addCallToHistory(
        contactName: String?,
        phoneNumber: String,
        callType: CallType,
        roomCode: String
    ) {
        // Gizli mod aktifse görüşme geçmişine kaydetme
        if (preferencesManager.isPrivacyModeEnabled()) {
            return
        }
        
        val history = CallHistory(
            id = System.currentTimeMillis(),
            contactName = contactName,
            phoneNumber = phoneNumber,
            callType = callType,
            timestamp = System.currentTimeMillis(),
            roomCode = roomCode
        )
        val currentList = _callHistory.value.toMutableList()
        currentList.add(history)
        _callHistory.value = currentList.sortedByDescending { it.timestamp }
        saveCallHistory()
    }

    private fun callHistoryToJson(history: List<CallHistory>): String {
        val jsonArray = JSONArray()
        history.forEach { call ->
            val jsonObject = JSONObject()
            jsonObject.put("id", call.id)
            jsonObject.put("contactName", call.contactName ?: "")
            jsonObject.put("phoneNumber", call.phoneNumber)
            jsonObject.put("callType", call.callType.name)
            jsonObject.put("timestamp", call.timestamp)
            jsonObject.put("duration", call.duration)
            jsonObject.put("roomCode", call.roomCode)
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }

    private fun parseCallHistoryFromJson(json: String): List<CallHistory> {
        val historyList = mutableListOf<CallHistory>()
        val jsonArray = JSONArray(json)
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            historyList.add(
                CallHistory(
                    id = jsonObject.getLong("id"),
                    contactName = jsonObject.optString("contactName").takeIf { it.isNotEmpty() },
                    phoneNumber = jsonObject.getString("phoneNumber"),
                    callType = CallType.valueOf(jsonObject.getString("callType")),
                    timestamp = jsonObject.getLong("timestamp"),
                    duration = jsonObject.optLong("duration", 0),
                    roomCode = jsonObject.optString("roomCode", "")
                )
            )
        }
        return historyList
    }

    fun updateRoomCode(code: String) {
        val normalized = code.uppercase(Locale.getDefault()).take(12)
        _uiState.update { it.copy(roomCode = normalized) }
    }

    fun attachLocalRenderer(renderer: org.webrtc.SurfaceViewRenderer) {
        rtcClient.attachLocalRenderer(renderer)
    }

    fun attachRemoteRenderer(renderer: org.webrtc.SurfaceViewRenderer) {
        rtcClient.attachRemoteRenderer(renderer)
    }

    fun startCall() {
        val networkState = _networkState.value
        if (!networkState.isConnected) {
            _uiState.update { it.copy(statusMessage = "İnternet bağlantısı gerekli. ${networkManager.getNetworkTypeText(networkState.networkType)}") }
            return
        }

        // Bluetooth ses yönlendirmesini optimize et
        optimizeAudioQuality()
        if (isBluetoothConnected.value) {
            startBluetoothSco()
        }
        
        // Sesli komutları başlat (eğer etkinse)
        if (voiceCommandManager.isEnabled.value) {
            voiceCommandManager.startListening()
        }

        // Varsayılan olarak sadece cloud signaling kullan
        // QR kod/local network bağlantısı güvenilir değil (NAT, firewall sorunları)
        signalingMode = SignalingMode.CLOUD
        directChannel = DirectChannel.NONE
        
        val room = _uiState.value.roomCode.ifBlank { generateRoomCode() }
        activeRoom = room
        viewModelScope.launch {
            runCatching {
                val isAudioOnly = _uiState.value.isAudioOnly
                _uiState.update { it.copy(statusMessage = "Arama başlatılıyor...", roomCode = room) }
                
                // Cloud signaling ile bağlan
                signalingClient.connect(room)
                val offer = rtcClient.createOffer(audioOnly = isAudioOnly)
                sendOffer(offer)
                _uiState.update { it.copy(statusMessage = "Teklif gönderildi, yanıt bekleniyor...") }
                
                // Widget'ı güncelle
                updateWidget()
            }.onFailure { error ->
                _uiState.update { it.copy(statusMessage = error.message ?: "Arama başlatılamadı") }
            }
        }
    }

    fun joinCall() {
        val networkState = _networkState.value
        if (!networkState.isConnected) {
            _uiState.update { it.copy(statusMessage = "İnternet bağlantısı gerekli. ${networkManager.getNetworkTypeText(networkState.networkType)}") }
            return
        }

        // Bluetooth ses yönlendirmesini optimize et
        optimizeAudioQuality()
        if (isBluetoothConnected.value) {
            startBluetoothSco()
        }
        
        // Sesli komutları başlat (eğer etkinse)
        if (voiceCommandManager.isEnabled.value) {
            voiceCommandManager.startListening()
        }

        // Cloud signaling ile bağlan
        val room = _uiState.value.roomCode
        if (room.isBlank()) {
            _uiState.update { it.copy(statusMessage = "Katılmak için oda kodu girin") }
            return
        }
        activeRoom = room
        viewModelScope.launch {
            runCatching {
                _uiState.update { it.copy(statusMessage = "Odaya bağlanılıyor...") }
                signalingClient.connect(room)
                _uiState.update { it.copy(statusMessage = "Bağlandı, SDP bekleniyor...") }
            }.onFailure { error ->
                _uiState.update { it.copy(statusMessage = error.message ?: "Bağlantı kurulamadı") }
            }
        }
    }

    fun hangUp() {
        stopCallDurationTimer()
        // Bluetooth SCO'yu durdur
        bluetoothAudioManager.stopBluetoothSco()
        
        // Sesli komutları durdur
        voiceCommandManager.stopListening()
        
        // Video kaydını durdur
        callRecorder.stopVideoRecording()?.let { videoFile ->
            android.util.Log.i("VideoCallViewModel", "Video kaydı tamamlandı: ${videoFile.absolutePath}")
        }
        
        // Ses kaydını durdur
        callRecorder.stopRecording()?.let { audioFile ->
            android.util.Log.i("VideoCallViewModel", "Ses kaydı tamamlandı: ${audioFile.absolutePath}")
            // TODO: Video ve audio kayıtlarını birleştir (MediaMuxer ile)
            // TODO: Kullanıcıya kayıt tamamlandı bildirimi göster
        }
        signalingClient.leave()
        peerSignalingClient.close()
        localServerToken = null
        localSignalingServer?.updateToken(null)
        val publishedHash = lastPublishedPresenceHash
        if (publishedHash != null) {
            viewModelScope.launch {
                presenceRepository.clear(publishedHash)
            }
            lastPublishedPresenceHash = null
        }
        directChannel = DirectChannel.NONE
        signalingMode = SignalingMode.CLOUD
        rtcClient.disconnect()
        activeRoom = null
        _uiState.update {
            CallUiState(
                roomCode = "",
                statusMessage = "Arama sonlandırıldı",
                isConnected = false,
                localVideoVisible = false,
                remoteVideoVisible = false,
                callStartTime = null,
                callDuration = 0,
                chatMessages = emptyList(),
                isChatVisible = false,
                sharedFiles = emptyList()
            )
        }
        // Dosya transfer state'lerini temizle
        fileChunks.clear()
        fileReceivingProgress.clear()
        
        // Widget'ı güncelle
        updateWidget()
    }
    
    private fun startCallDurationTimer() {
        callDurationJob?.cancel()
        callDurationJob = viewModelScope.launch {
            while (true) {
                delay(1000) // Her saniye güncelle
                val startTime = _uiState.value.callStartTime
                if (startTime != null && _uiState.value.isConnected) {
                    val duration = (System.currentTimeMillis() - startTime) / 1000 // saniye cinsinden
                    _uiState.update { it.copy(callDuration = duration) }
                } else {
                    break
                }
            }
        }
    }
    
    private fun stopCallDurationTimer() {
        callDurationJob?.cancel()
        callDurationJob = null
    }

    fun toggleCamera() {
        val enabled = !_uiState.value.isCameraEnabled
        rtcClient.setVideoEnabled(enabled)
        _uiState.update { it.copy(isCameraEnabled = enabled) }
    }

    fun toggleMicrophone() {
        val enabled = !_uiState.value.isMicEnabled
        rtcClient.setAudioEnabled(enabled)
        _uiState.update { it.copy(isMicEnabled = enabled) }
    }
    
    fun toggleAudioOnlyMode() {
        val newAudioOnly = !_uiState.value.isAudioOnly
        rtcClient.setAudioOnlyMode(newAudioOnly)
        _uiState.update { 
            it.copy(
                isAudioOnly = newAudioOnly,
                isCameraEnabled = !newAudioOnly && it.isCameraEnabled,
                localVideoVisible = !newAudioOnly && it.localVideoVisible
            )
        }
    }
    
    fun setAudioOnlyMode(enabled: Boolean) {
        rtcClient.setAudioOnlyMode(enabled)
        _uiState.update { 
            it.copy(
                isAudioOnly = enabled,
                isCameraEnabled = !enabled && it.isCameraEnabled,
                localVideoVisible = !enabled && it.localVideoVisible
            )
        }
    }
    
    fun setVideoQuality(quality: com.videocall.app.model.VideoQuality) {
        rtcClient.setVideoQuality(quality)
        _uiState.update { it.copy(videoQuality = quality) }
    }
    
    fun setBackgroundMode(mode: com.videocall.app.model.BackgroundMode) {
        _uiState.update { it.copy(backgroundMode = mode) }
        // TODO: Gerçek video işleme implementasyonu eklenecek
        // WebRTC VideoSink kullanarak frame'leri işleyip blur/renk efekti uygulanacak
    }
    
    fun cycleBackgroundMode() {
        val current = _uiState.value.backgroundMode
        val next = when (current) {
            com.videocall.app.model.BackgroundMode.NONE -> com.videocall.app.model.BackgroundMode.BLUR
            com.videocall.app.model.BackgroundMode.BLUR -> com.videocall.app.model.BackgroundMode.COLOR
            com.videocall.app.model.BackgroundMode.COLOR -> com.videocall.app.model.BackgroundMode.NONE
        }
        setBackgroundMode(next)
    }
    
    fun setFilter(filter: com.videocall.app.model.FilterType) {
        _uiState.update { it.copy(filter = filter) }
        filterTypeRef.set(filter)
        
        // Video processor'ı güncelle
        videoProcessor?.let { processor ->
            // Processor zaten çalışıyor, sadece filtreyi güncelle
        } ?: run {
            // Processor yoksa oluştur (ilk görüşme başladığında)
            initializeVideoProcessor()
        }
    }
    
    /**
     * Video processor'ı başlatır
     */
    private fun initializeVideoProcessor() {
        if (videoProcessor == null) {
            try {
                val eglBase = rtcClient.getEglBase()
                videoProcessor = VideoProcessor(
                    context = context,
                    eglBase = eglBase,
                    scope = viewModelScope,
                    backgroundMode = backgroundModeRef,
                    filterType = filterTypeRef
                )
                
                // Local video track'e processor ekle
                val localTrack = rtcClient.getLocalVideoTrack()
                val processedTrack = videoProcessor!!.getProcessedVideoTrack(
                    rtcClient.getPeerConnectionFactory(),
                    localTrack
                )
                
                // İşlenmiş track'i renderer'lara bağla
                // Not: Bu implementasyon basitleştirilmiş, gerçek kullanımda renderer'ları güncellemek gerekir
            } catch (e: Exception) {
                android.util.Log.e("VideoCallViewModel", "Video processor başlatılamadı", e)
            }
        }
    }
    
    fun cycleFilter() {
        val current = _uiState.value.filter
        val next = when (current) {
            com.videocall.app.model.FilterType.NONE -> com.videocall.app.model.FilterType.SEPIA
            com.videocall.app.model.FilterType.SEPIA -> com.videocall.app.model.FilterType.BLACK_WHITE
            com.videocall.app.model.FilterType.BLACK_WHITE -> com.videocall.app.model.FilterType.VINTAGE
            com.videocall.app.model.FilterType.VINTAGE -> com.videocall.app.model.FilterType.NONE
        }
        setFilter(next)
    }
    
    // Group Call Management
    fun addParticipant(contact: Contact) {
        val currentList = _participants.value.toMutableList()
        val participant = Participant(
            id = "participant_${contact.id}_${System.currentTimeMillis()}",
            name = contact.name,
            phoneNumber = contact.phoneNumber,
            isLocal = false,
            isVideoEnabled = true,
            isAudioEnabled = true
        )
        if (!currentList.any { it.id == participant.id }) {
            currentList.add(participant)
            _participants.value = currentList
            // TODO: WebRTC'de yeni peer connection oluştur ve bağlantı kur
        }
    }
    
    fun removeParticipant(participantId: String) {
        val currentList = _participants.value.toMutableList()
        currentList.removeAll { it.id == participantId }
        _participants.value = currentList
        // TODO: WebRTC'de peer connection'ı kapat
    }
    
    // Chat Functions - Kişiye özel chat
    fun sendChatMessage(targetPhoneNumber: String, message: String) {
        if (message.isBlank() || targetPhoneNumber.isBlank()) return
        
        val myPhoneNumber = preferencesManager.getPhoneNumber() ?: "Unknown"
        val myName = _addedContacts.value.find { it.phoneNumber == myPhoneNumber }?.name
        
        val chatMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderPhoneNumber = myPhoneNumber,
            senderName = myName,
            message = message,
            timestamp = System.currentTimeMillis(),
            isFromMe = true
        )
        
        // Mesajı kişiye özel chat geçmişine ekle
        val currentChats = _chatMessagesByContact.value.toMutableMap()
        val contactMessages = currentChats.getOrDefault(targetPhoneNumber, emptyList()).toMutableList()
        contactMessages.add(chatMessage)
        currentChats[targetPhoneNumber] = contactMessages
        _chatMessagesByContact.value = currentChats
        
        // Signaling üzerinden gönder (kişiye özel)
        val signalingChat = SignalingMessage.Chat(
            message = message,
            senderPhoneNumber = myPhoneNumber,
            senderName = myName,
            targetPhoneNumber = targetPhoneNumber
        )
        
        when (signalingMode) {
            SignalingMode.CLOUD -> {
                signalingClient.sendChat(signalingChat)
            }
            SignalingMode.DIRECT -> {
                when (directChannel) {
                    DirectChannel.CLIENT -> peerSignalingClient.send(signalingChat)
                    DirectChannel.SERVER -> localSignalingServer?.broadcast(signalingChat)
                    DirectChannel.NONE -> Unit
                }
            }
        }
    }
    
    // Görüşme sırasında room-based chat (geriye dönük uyumluluk)
    fun sendChatMessage(message: String) {
        if (message.isBlank()) return
        
        val myPhoneNumber = preferencesManager.getPhoneNumber() ?: "Unknown"
        val myName = _addedContacts.value.find { it.phoneNumber == myPhoneNumber }?.name
        
        val chatMessage = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderPhoneNumber = myPhoneNumber,
            senderName = myName,
            message = message,
            timestamp = System.currentTimeMillis(),
            isFromMe = true
        )
        
        // Mesajı local state'e ekle (görüşme sırasında)
        val currentMessages = _uiState.value.chatMessages.toMutableList()
        currentMessages.add(chatMessage)
        _uiState.update { it.copy(chatMessages = currentMessages) }
        
        // Signaling üzerinden gönder (room-based)
        val signalingChat = SignalingMessage.Chat(
            message = message,
            senderPhoneNumber = myPhoneNumber,
            senderName = myName,
            targetPhoneNumber = null // Room-based chat
        )
        
        when (signalingMode) {
            SignalingMode.CLOUD -> {
                signalingClient.sendChat(signalingChat)
            }
            SignalingMode.DIRECT -> {
                when (directChannel) {
                    DirectChannel.CLIENT -> peerSignalingClient.send(signalingChat)
                    DirectChannel.SERVER -> localSignalingServer?.broadcast(signalingChat)
                    DirectChannel.NONE -> Unit
                }
            }
        }
    }
    
    // Kişiye özel chat mesajlarını getir
    fun getChatMessagesForContact(phoneNumber: String): List<ChatMessage> {
        return _chatMessagesByContact.value.getOrDefault(phoneNumber, emptyList())
    }
    
    private fun handleIncomingChatMessage(chatMessage: SignalingMessage.Chat) {
        val myPhoneNumber = preferencesManager.getPhoneNumber() ?: ""
        val isFromMe = chatMessage.senderPhoneNumber == myPhoneNumber
        
        val message = ChatMessage(
            id = System.currentTimeMillis().toString(),
            senderPhoneNumber = chatMessage.senderPhoneNumber,
            senderName = chatMessage.senderName,
            message = chatMessage.message,
            timestamp = System.currentTimeMillis(),
            isFromMe = isFromMe
        )
        
        // Kişiye özel chat mi, yoksa room-based chat mi?
        if (chatMessage.targetPhoneNumber != null) {
            // Kişiye özel chat - hedef kişi benim
            if (chatMessage.targetPhoneNumber == myPhoneNumber) {
                val currentChats = _chatMessagesByContact.value.toMutableMap()
                val contactMessages = currentChats.getOrDefault(chatMessage.senderPhoneNumber, emptyList()).toMutableList()
                contactMessages.add(message)
                currentChats[chatMessage.senderPhoneNumber] = contactMessages
                _chatMessagesByContact.value = currentChats
            }
        } else {
            // Room-based chat (görüşme sırasında)
            val currentMessages = _uiState.value.chatMessages.toMutableList()
            currentMessages.add(message)
            _uiState.update { it.copy(chatMessages = currentMessages) }
        }
    }
    
    fun toggleChatVisibility() {
        _uiState.update { it.copy(isChatVisible = !_uiState.value.isChatVisible) }
    }
    
    fun clearChatMessages() {
        _uiState.update { it.copy(chatMessages = emptyList()) }
    }
    
    // File Sharing Functions
    // Constants moved to companion object below
    
    private val fileChunks = mutableMapOf<String, MutableList<ByteArray>>() // fileId -> chunks
    private val fileReceivingProgress = mutableMapOf<String, Int>() // fileId -> received chunks count
    
    fun shareFile(fileUri: Uri, fileName: String, mimeType: String) {
        viewModelScope.launch {
            try {
                val fileSize = getFileSize(fileUri)
                if (fileSize > MAX_FILE_SIZE) {
                    _uiState.update { 
                        it.copy(statusMessage = "Dosya boyutu çok büyük. Maksimum: ${MAX_FILE_SIZE / (1024 * 1024)} MB") 
                    }
                    return@launch
                }
                
                val fileId = System.currentTimeMillis().toString()
                val myPhoneNumber = preferencesManager.getPhoneNumber() ?: "Unknown"
                val myName = _addedContacts.value.find { it.phoneNumber == myPhoneNumber }?.name
                
                // Dosyayı oku ve chunk'lara böl
                val fileBytes = readFileBytes(fileUri)
                val chunks = mutableListOf<ByteArray>()
                var offset = 0
                while (offset < fileBytes.size) {
                    val chunkSize = minOf(CHUNK_SIZE, fileBytes.size - offset)
                    chunks.add(fileBytes.sliceArray(offset until offset + chunkSize))
                    offset += chunkSize
                }
                
                // SharedFile oluştur ve state'e ekle
                val sharedFile = SharedFile(
                    id = fileId,
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = mimeType,
                    senderPhoneNumber = myPhoneNumber,
                    senderName = myName,
                    isFromMe = true,
                    transferState = FileTransferState.SENDING,
                    progress = 0f
                )
                
                val currentFiles = _uiState.value.sharedFiles.toMutableList()
                currentFiles.add(sharedFile)
                _uiState.update { it.copy(sharedFiles = currentFiles) }
                
                // Metadata gönder
                val metadata = SignalingMessage.FileShare(
                    fileId = fileId,
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = mimeType,
                    senderPhoneNumber = myPhoneNumber,
                    senderName = myName,
                    chunkIndex = 0,
                    totalChunks = chunks.size,
                    chunkData = null,
                    isMetadata = true
                )
                
                sendFileMessage(metadata)
                
                // Chunk'ları gönder
                chunks.forEachIndexed { index: Int, chunk: ByteArray ->
                    val chunkBase64 = Base64.encodeToString(chunk, Base64.NO_WRAP)
                    val chunkMessage = SignalingMessage.FileShare(
                        fileId = fileId,
                        fileName = fileName,
                        fileSize = fileSize,
                        mimeType = mimeType,
                        senderPhoneNumber = myPhoneNumber,
                        senderName = myName,
                        chunkIndex = index,
                        totalChunks = chunks.size,
                        chunkData = chunkBase64,
                        isMetadata = false
                    )
                    
                    sendFileMessage(chunkMessage)
                    
                    // Progress güncelle
                    val progress = (index + 1).toFloat() / chunks.size
                    updateFileProgress(fileId, progress, FileTransferState.SENDING)
                }
                
                // Tamamlandı
                updateFileProgress(fileId, 1f, FileTransferState.COMPLETED)
                
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Dosya gönderilemedi: ${e.message}") }
            }
        }
    }
    
    private fun sendFileMessage(message: SignalingMessage.FileShare) {
        when (signalingMode) {
            SignalingMode.CLOUD -> {
                signalingClient.sendFileShare(message)
            }
            SignalingMode.DIRECT -> {
                when (directChannel) {
                    DirectChannel.CLIENT -> peerSignalingClient.send(message)
                    DirectChannel.SERVER -> localSignalingServer?.broadcast(message)
                    DirectChannel.NONE -> Unit
                }
            }
        }
    }
    
    private fun handleIncomingFileShare(fileShare: SignalingMessage.FileShare) {
        val myPhoneNumber = preferencesManager.getPhoneNumber() ?: ""
        val isFromMe = fileShare.senderPhoneNumber == myPhoneNumber
        
        if (fileShare.isMetadata) {
            // Metadata alındı - yeni dosya transferi başlıyor
            fileChunks[fileShare.fileId] = mutableListOf()
            fileReceivingProgress[fileShare.fileId] = 0
            
            val sharedFile = SharedFile(
                id = fileShare.fileId,
                fileName = fileShare.fileName,
                fileSize = fileShare.fileSize,
                mimeType = fileShare.mimeType,
                senderPhoneNumber = fileShare.senderPhoneNumber,
                senderName = fileShare.senderName,
                isFromMe = isFromMe,
                transferState = FileTransferState.RECEIVING,
                progress = 0f
            )
            
            val currentFiles = _uiState.value.sharedFiles.toMutableList()
            currentFiles.add(sharedFile)
            _uiState.update { it.copy(sharedFiles = currentFiles) }
        } else {
            // Chunk alındı
            val chunks = fileChunks[fileShare.fileId] ?: return
            val receivedCount = fileReceivingProgress[fileShare.fileId] ?: 0
            
            try {
                val chunkBytes = Base64.decode(fileShare.chunkData, Base64.NO_WRAP)
                chunks.add(chunkBytes)
                fileReceivingProgress[fileShare.fileId] = receivedCount + 1
                
                val progress = (receivedCount + 1).toFloat() / fileShare.totalChunks
                updateFileProgress(fileShare.fileId, progress, FileTransferState.RECEIVING)
                
                // Tüm chunk'lar alındı mı?
                if (chunks.size == fileShare.totalChunks) {
                    // Dosyayı birleştir ve kaydet
                    saveReceivedFile(fileShare.fileId, fileShare.fileName, fileShare.mimeType, chunks)
                    fileChunks.remove(fileShare.fileId)
                    fileReceivingProgress.remove(fileShare.fileId)
                }
            } catch (e: Exception) {
                updateFileProgress(fileShare.fileId, 0f, FileTransferState.FAILED)
                _uiState.update { it.copy(statusMessage = "Dosya alınamadı: ${e.message}") }
            }
        }
    }
    
    private fun updateFileProgress(fileId: String, progress: Float, state: FileTransferState) {
        val currentFiles = _uiState.value.sharedFiles.toMutableList()
        val index = currentFiles.indexOfFirst { it.id == fileId }
        if (index != -1) {
            currentFiles[index] = currentFiles[index].copy(
                progress = progress,
                transferState = state
            )
            _uiState.update { it.copy(sharedFiles = currentFiles) }
        }
    }
    
    private fun saveReceivedFile(fileId: String, fileName: String, mimeType: String, chunks: List<ByteArray>) {
        viewModelScope.launch {
            try {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS
                )
                val file = File(downloadsDir, fileName)
                
                FileOutputStream(file).use { output ->
                    chunks.forEach { chunk ->
                        output.write(chunk)
                    }
                }
                
                val fileUri = Uri.fromFile(file).toString()
                updateFileProgress(fileId, 1f, FileTransferState.COMPLETED)
                
                // SharedFile'a URI ekle
                val currentFiles = _uiState.value.sharedFiles.toMutableList()
                val index = currentFiles.indexOfFirst { it.id == fileId }
                if (index != -1) {
                    currentFiles[index] = currentFiles[index].copy(fileUri = fileUri)
                    _uiState.update { it.copy(sharedFiles = currentFiles) }
                }
                
                _uiState.update { it.copy(statusMessage = "Dosya alındı: $fileName") }
            } catch (e: Exception) {
                updateFileProgress(fileId, 0f, FileTransferState.FAILED)
                _uiState.update { it.copy(statusMessage = "Dosya kaydedilemedi: ${e.message}") }
            }
        }
    }
    
    private fun getFileSize(uri: Uri): Long {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun readFileBytes(uri: Uri): ByteArray {
        return contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: ByteArray(0)
    }
    
    fun toggleParticipantMute(participantId: String) {
        val currentList = _participants.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == participantId }
        if (index != -1) {
            val participant = currentList[index]
            currentList[index] = participant.copy(isMuted = !participant.isMuted)
            _participants.value = currentList
            // TODO: WebRTC'de audio track'i mute/unmute et
        }
    }
    
    fun toggleParticipantVideo(participantId: String) {
        val currentList = _participants.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == participantId }
        if (index != -1) {
            val participant = currentList[index]
            currentList[index] = participant.copy(isVideoEnabled = !participant.isVideoEnabled)
            _participants.value = currentList
            // TODO: WebRTC'de video track'i enable/disable et
        }
    }

    fun switchCamera() {
        rtcClient.switchCamera()
    }
    
    fun toggleScreenSharing(resultCode: Int? = null, data: Intent? = null) {
        if (rtcClient.isScreenSharing()) {
            rtcClient.stopScreenCapture()
            _uiState.update { it.copy(isScreenSharing = false) }
        } else {
            // MediaProjection permission request gerekli
            // Bu UI'dan yapılacak, burada sadece state güncellemesi
            if (resultCode != null && data != null) {
                rtcClient.startScreenCapture(resultCode, data)
                _uiState.update { it.copy(isScreenSharing = true) }
            }
        }
    }
    
    fun startScreenSharing(resultCode: Int, data: Intent) {
        rtcClient.startScreenCapture(resultCode, data)
        _uiState.update { it.copy(isScreenSharing = true) }
    }
    
    fun stopScreenSharing() {
        rtcClient.stopScreenCapture()
        _uiState.update { it.copy(isScreenSharing = false) }
    }
    
    // Scheduled Calls Functions
    fun scheduleCall(
        contactName: String,
        contactPhoneNumber: String?,
        scheduledTime: Long,
        roomCode: String? = null,
        notes: String? = null
    ) {
        val scheduledCall = ScheduledCall(
            id = System.currentTimeMillis(),
            contactName = contactName,
            contactPhoneNumber = contactPhoneNumber,
            scheduledTime = scheduledTime,
            roomCode = roomCode,
            notes = notes
        )
        
        val currentList = _scheduledCalls.value.toMutableList()
        currentList.add(scheduledCall)
        _scheduledCalls.value = currentList.sortedBy { it.scheduledTime }
        saveScheduledCalls()
        
        // Takvime ekle
        viewModelScope.launch {
            if (calendarManager.hasCalendarPermission()) {
                val eventId = calendarManager.addEventToCalendar(scheduledCall)
                if (eventId != null) {
                    // Hatırlatıcı ekle (15 dakika önceden)
                    calendarManager.addReminder(eventId, 15)
                }
            }
        }
        
        // Bildirim zamanlaması (gelecek güncelleme)
        // notificationManager.scheduleCallReminder(scheduledCall)
    }
    
    fun cancelScheduledCall(callId: Long) {
        val currentList = _scheduledCalls.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == callId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(isCancelled = true)
            _scheduledCalls.value = currentList
            saveScheduledCalls()
        }
    }
    
    fun completeScheduledCall(callId: Long) {
        val currentList = _scheduledCalls.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == callId }
        if (index != -1) {
            currentList[index] = currentList[index].copy(isCompleted = true)
            _scheduledCalls.value = currentList
            saveScheduledCalls()
        }
    }
    
    fun deleteScheduledCall(callId: Long) {
        val currentList = _scheduledCalls.value.toMutableList()
        currentList.removeAll { it.id == callId }
        _scheduledCalls.value = currentList
        saveScheduledCalls()
    }
    
    fun getUpcomingCalls(): List<ScheduledCall> {
        return _scheduledCalls.value.filter { it.isUpcoming() }
    }
    
    private fun loadScheduledCalls() {
        viewModelScope.launch {
            try {
                val json = preferencesManager.getScheduledCalls()
                if (json != null) {
                    val callsList = parseScheduledCallsFromJson(json)
                    _scheduledCalls.value = callsList.sortedBy { it.scheduledTime }
                }
            } catch (e: Exception) {
                _scheduledCalls.value = emptyList()
            }
        }
    }
    
    private fun syncCalendarEvents() {
        viewModelScope.launch {
            try {
                if (calendarManager.hasCalendarPermission()) {
                    val calendarEvents = calendarManager.readVideoCallEventsFromCalendar()
                    // Takvimden gelen randevuları mevcut randevularla birleştir
                    val existingCalls = _scheduledCalls.value.toMutableList()
                    calendarEvents.forEach { calendarEvent ->
                        if (!existingCalls.any { it.id == calendarEvent.id }) {
                            existingCalls.add(calendarEvent)
                        }
                    }
                    _scheduledCalls.value = existingCalls.sortedBy { it.scheduledTime }
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoCallViewModel", "Takvim senkronizasyonu hatası", e)
            }
        }
    }
    
    private fun calculateCallStatistics(history: List<CallHistory>): CallStatistics {
        val totalCalls = history.size
        val totalDuration = history.sumOf { it.duration }
        val incomingCalls = history.count { it.callType == CallType.INCOMING }
        val outgoingCalls = history.count { it.callType == CallType.OUTGOING }
        val averageDuration = if (totalCalls > 0) totalDuration / totalCalls else 0L
        val longestDuration = history.maxOfOrNull { it.duration } ?: 0L
        
        return CallStatistics(
            totalCalls = totalCalls,
            totalDuration = totalDuration,
            totalIncomingCalls = incomingCalls,
            totalOutgoingCalls = outgoingCalls,
            averageCallDuration = averageDuration,
            longestCallDuration = longestDuration,
            favoriteContact = null // TODO: Calculate favorite contact
        )
    }
    
    private fun saveScheduledCalls() {
        viewModelScope.launch {
            try {
                val json = scheduledCallsToJson(_scheduledCalls.value)
                preferencesManager.saveScheduledCalls(json)
            } catch (e: Exception) {
                // Ignore save errors
            }
        }
    }
    
    private fun scheduledCallsToJson(calls: List<ScheduledCall>): String {
        val jsonArray = JSONArray()
        calls.forEach { call ->
            val jsonObject = JSONObject()
            jsonObject.put("id", call.id)
            jsonObject.put("contactName", call.contactName)
            jsonObject.put("contactPhoneNumber", call.contactPhoneNumber ?: "")
            jsonObject.put("scheduledTime", call.scheduledTime)
            jsonObject.put("roomCode", call.roomCode ?: "")
            jsonObject.put("notes", call.notes ?: "")
            jsonObject.put("isCompleted", call.isCompleted)
            jsonObject.put("isCancelled", call.isCancelled)
            jsonObject.put("createdAt", call.createdAt)
            jsonArray.put(jsonObject)
        }
        return jsonArray.toString()
    }
    
    private fun parseScheduledCallsFromJson(json: String): List<ScheduledCall> {
        val callsList = mutableListOf<ScheduledCall>()
        try {
            val jsonArray = JSONArray(json)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                callsList.add(
                    ScheduledCall(
                        id = jsonObject.getLong("id"),
                        contactName = jsonObject.getString("contactName"),
                        contactPhoneNumber = jsonObject.optString("contactPhoneNumber").takeIf { it.isNotEmpty() },
                        scheduledTime = jsonObject.getLong("scheduledTime"),
                        roomCode = jsonObject.optString("roomCode").takeIf { it.isNotEmpty() },
                        notes = jsonObject.optString("notes").takeIf { it.isNotEmpty() },
                        isCompleted = jsonObject.optBoolean("isCompleted", false),
                        isCancelled = jsonObject.optBoolean("isCancelled", false),
                        createdAt = jsonObject.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return callsList
    }
    
    // Two-Factor Authentication (2FA) Functions
    fun enable2FA(): Pair<String, List<String>> {
        // Secret key oluştur
        val secretKey = TotpManager.generateSecretKey()
        preferencesManager.save2FASecret(secretKey)
        
        // Backup kodları oluştur (10 adet)
        val backupCodes = generateBackupCodes()
        preferencesManager.save2FABackupCodes(backupCodes)
        
        // 2FA'yı aktif et
        preferencesManager.set2FAEnabled(true)
        
        return Pair(secretKey, backupCodes)
    }
    
    fun disable2FA() {
        preferencesManager.set2FAEnabled(false)
        preferencesManager.save2FASecret("")
        preferencesManager.save2FABackupCodes(emptyList())
    }
    
    fun verify2FACode(code: String): Boolean {
        val secretKey = preferencesManager.get2FASecret() ?: return false
        
        // TOTP kodu doğrula
        if (TotpManager.verifyTotp(secretKey, code)) {
            return true
        }
        
        // Backup kod kontrolü
        if (preferencesManager.use2FABackupCode(code)) {
            return true
        }
        
        return false
    }
    
    fun get2FAStatus(): TwoFactorAuth {
        return TwoFactorAuth(
            isEnabled = preferencesManager.is2FAEnabled(),
            secretKey = preferencesManager.get2FASecret(),
            backupCodes = preferencesManager.get2FABackupCodes(),
            createdAt = null
        )
    }
    
    fun generate2FAQRCodeUrl(accountName: String): String? {
        val secretKey = preferencesManager.get2FASecret() ?: return null
        val phoneNumber = preferencesManager.getPhoneNumber() ?: accountName
        return TotpManager.generateOtpauthUrl(secretKey, phoneNumber)
    }
    
    /**
     * 2FA QR kod bitmap'i oluşturur (async)
     */
    suspend fun generate2FAQRCodeBitmapAsync(accountName: String): android.graphics.Bitmap? {
        val otpauthUrl = generate2FAQRCodeUrl(accountName) ?: return null
        return try {
            qrCodeManager.create2FAQRCode(otpauthUrl)
        } catch (e: Exception) {
            android.util.Log.e("VideoCallViewModel", "2FA QR kod oluşturulamadı", e)
            null
        }
    }
    
    fun regenerateBackupCodes(): List<String> {
        val newCodes = generateBackupCodes()
        preferencesManager.save2FABackupCodes(newCodes)
        return newCodes
    }
    
    // Offline Mode Functions
    fun enableOfflineMode() {
        networkDiscovery.enableOfflineMode()
        preferencesManager.setOfflineModeEnabled(true)
    }
    
    fun disableOfflineMode() {
        networkDiscovery.disableOfflineMode()
        preferencesManager.setOfflineModeEnabled(false)
    }
    
    fun discoverLocalPeers() {
        networkDiscovery.discoverPeers()
        networkDiscovery.scanLocalNetwork()
    }
    
    fun connectToLocalPeer(deviceAddress: String) {
        networkDiscovery.connectToPeer(deviceAddress)
    }
    
    fun getLocalNetworkInfo(): String {
        val localIp = localIpAddress.value
        return if (localIp != null) {
            "Yerel IP: $localIp"
        } else {
            "Yerel IP alınamadı"
        }
    }
    
    // Bluetooth Audio Functions
    fun setAudioRoute(route: AudioRoute) {
        bluetoothAudioManager.setAudioRoute(route)
    }
    
    fun enableAutoAudioRouting() {
        bluetoothAudioManager.enableAutoRouting()
    }
    
    fun optimizeAudioQuality() {
        bluetoothAudioManager.optimizeAudioQuality()
    }
    
    fun startBluetoothSco() {
        bluetoothAudioManager.startBluetoothSco()
    }
    
    fun stopBluetoothSco() {
        bluetoothAudioManager.stopBluetoothSco()
    }
    
    // Voice Commands Functions
    fun enableVoiceCommands() {
        voiceCommandManager.enable()
        preferencesManager.setVoiceCommandsEnabled(true)
    }
    
    fun disableVoiceCommands() {
        voiceCommandManager.disable()
        preferencesManager.setVoiceCommandsEnabled(false)
    }
    
    fun startVoiceListening() {
        if (voiceCommandManager.isEnabled.value) {
            voiceCommandManager.startListening()
        }
    }
    
    fun stopVoiceListening() {
        voiceCommandManager.stopListening()
    }
    
    private fun handleVoiceCommand(command: VoiceCommand) {
        viewModelScope.launch {
            when (command) {
                VoiceCommand.TOGGLE_MICROPHONE -> {
                    toggleMicrophone()
                }
                VoiceCommand.TOGGLE_CAMERA -> {
                    toggleCamera()
                }
                VoiceCommand.HANG_UP -> {
                    hangUp()
                }
                VoiceCommand.SWITCH_TO_SPEAKER -> {
                    setAudioRoute(AudioRoute.SPEAKER)
                }
                VoiceCommand.SWITCH_TO_BLUETOOTH -> {
                    if (isBluetoothConnected.value) {
                        setAudioRoute(AudioRoute.BLUETOOTH)
                    }
                }
                VoiceCommand.OPEN_CHAT -> {
                    // Chat açma
                    _uiState.update { it.copy(isChatVisible = true) }
                }
                VoiceCommand.TOGGLE_SCREEN_SHARING -> {
                    // Ekran paylaşımı
                    if (_uiState.value.isScreenSharing) {
                        stopScreenSharing()
                    }
                }
                VoiceCommand.TOGGLE_AUDIO_ONLY -> {
                    toggleAudioOnlyMode()
                }
            }
        }
    }
    
    private fun generateBackupCodes(): List<String> {
        val codes = mutableListOf<String>()
        val random = java.security.SecureRandom()
        
        repeat(10) {
            val code = StringBuilder()
            repeat(8) {
                code.append(random.nextInt(10))
            }
            codes.add(code.toString())
        }
        
        return codes
    }

    /**
     * Güvenli QR kod oluştur (sunucusuz bağlantı için)
     * Aynı ağda bağlantı için local IP kullanır, farklı ağlarda public IP kullanır
     */
    fun createSecureQRCode(sharedSecret: String? = null) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(statusMessage = "QR kod hazırlanıyor...") }
                ensureLocalSignalingServer()
                
                // Önce local IP'yi dene (aynı ağ için)
                var ipToUse = networkDiscovery.getLocalIpAddress()
                val isLocalNetwork = ipToUse != null && _networkState.value.networkType == com.videocall.app.data.NetworkType.WIFI
                
                // Local IP yoksa veya farklı ağlarda bağlanmak için public IP kullan
                if (ipToUse.isNullOrBlank()) {
                    ipToUse = NetworkUtils.getPublicIP()
                    if (ipToUse.isNullOrBlank()) {
                        _uiState.update { it.copy(statusMessage = "IP adresi alınamadı. İnternet bağlantınızı kontrol edin.") }
                        return@launch
                    }
                }
                
                val port = localSignalingServer?.getPort() ?: localServerPort
                val token = localServerToken ?: securityManager.generateNonce().also { localServerToken = it }
                val phoneNumber = preferencesManager.getPhoneNumber()

                val qrResult = qrCodeManager.createSecureQR(
                    publicIp = ipToUse,
                    port = port,
                    token = token,
                    phoneNumber = phoneNumber
                )
                _qrCodeBitmap.value = qrResult.bitmap
                _connectionInfoJson.value = qrResult.info.toJson()
                
                val networkType = if (isLocalNetwork) "Yerel ağ" else "Genel ağ"
                _uiState.update { it.copy(statusMessage = "QR kod oluşturuldu ($networkType: $ipToUse:$port)") }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "QR kod oluşturulamadı: ${e.message}") }
                _qrCodeBitmap.value = null
                _connectionInfoJson.value = null
            }
        }
    }

    /**
     * QR kod içeriğini doğrula ve bağlantı kur
     * QR'dan IP+port+token alınır, SDP WebSocket üzerinden gönderilir
     */
    /**
     * Kişi listesini QR kod olarak oluştur
     */
    fun generateContactsQRCode() {
        viewModelScope.launch {
            try {
                val contactsJson = contactsToJson(_addedContacts.value)
                val qrBitmap = qrCodeManager.createContactsQRCode(contactsJson)
                _qrCodeBitmap.value = qrBitmap
                _uiState.update { it.copy(statusMessage = "Kişi listesi QR kodu oluşturuldu") }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "QR kod oluşturulamadı: ${e.message}") }
                _qrCodeBitmap.value = null
            }
        }
    }
    
    fun processQRCode(qrData: String, expectedSecret: String? = null) {
        viewModelScope.launch {
            // Önce kişi listesi QR kodu mu kontrol et
            if (qrData.startsWith("CONTACTS:")) {
                try {
                    val base64Data = qrData.removePrefix("CONTACTS:")
                    val jsonString = String(
                        Base64.decode(base64Data, Base64.NO_WRAP),
                        Charsets.UTF_8
                    )
                    val importedContacts = parseContactsFromJson(jsonString)
                    // Mevcut kişilere ekle (telefon numarasına göre duplicate kontrolü)
                    importedContacts.forEach { contact ->
                        if (!_addedContacts.value.any { it.phoneNumber == contact.phoneNumber }) {
                            addContact(contact)
                        }
                    }
                    _uiState.update { it.copy(statusMessage = "${importedContacts.size} kişi içe aktarıldı") }
                    return@launch
                } catch (e: Exception) {
                    _uiState.update { it.copy(statusMessage = "QR kod okunamadı: ${e.message}") }
                    return@launch
                }
            }
            
            val (info, error) = qrCodeManager.verifyQRContent(qrData)
            
            if (info == null) {
                _uiState.update { it.copy(statusMessage = error ?: "QR kod geçersiz") }
                return@launch
            }
            
            // PeerSignalingClient ile bağlan
            try {
                val phoneNumber = preferencesManager.getPhoneNumber()
                val phoneHash = phoneNumber?.let { securityManager.hashPhoneNumber(it) } ?: ""
                
                _uiState.update { it.copy(statusMessage = "Karşı cihaza bağlanılıyor...") }
                
                peerSignalingClient.connect(
                    host = info.ip,
                    port = info.port,
                    token = info.token,
                    phoneHash = phoneHash
                )
                
                signalingMode = SignalingMode.DIRECT
                directChannel = DirectChannel.CLIENT
                activeRoom = "DIRECT"
                
                _uiState.update { 
                    it.copy(
                        roomCode = "DIRECT",
                        statusMessage = "Bağlandı, teklif bekleniyor..."
                    ) 
                }
                
                // Bağlantı başarılı olduğunda, eğer sunucu tarafından offer gelmezse
                // client tarafından offer gönderebiliriz (startCall çağrıldığında)
                // SDP'yi WebSocket üzerinden alacağız (observePeerMessages zaten dinliyor)
            } catch (e: Exception) {
                _uiState.update { it.copy(statusMessage = "Bağlantı kurulamadı: ${e.message}") }
            }
        }
    }

    private fun observeRtcEvents() {
        viewModelScope.launch {
            rtcClient.events.collect { event ->
                when (event) {
                    is RtcEvent.ConnectionStateChanged -> {
                        val connected = event.state == org.webrtc.PeerConnection.PeerConnectionState.CONNECTED
                        if (connected) {
                            // Görüşme başladı - süre takibini başlat
                            val startTime = System.currentTimeMillis()
                            
                            // Video kaydını başlat (local ve remote video track'ler ile)
                            val localVideoTrack = rtcClient.getLocalVideoTrack()
                            val remoteVideoTrack = rtcClient.getRemoteVideoTrack()
                            val videoRecordingPath = callRecorder.startVideoRecording(
                                localVideoTrack = localVideoTrack,
                                remoteVideoTrack = remoteVideoTrack
                            )
                            
                            // Ses kaydını da başlat (video kaydı ile birleştirilecek)
                            val audioRecordingPath = callRecorder.startRecording(includeVideo = false)
                            
                            if (videoRecordingPath != null || audioRecordingPath != null) {
                                android.util.Log.i("VideoCallViewModel", "Görüşme kaydı başlatıldı - Video: $videoRecordingPath, Audio: $audioRecordingPath")
                            }
                            _uiState.update {
                                it.copy(
                                    isConnected = true,
                                    callStartTime = startTime,
                                    callDuration = 0,
                                    statusMessage = "Bağlandı"
                                )
                            }
                            startCallDurationTimer()
                        } else {
                            // Görüşme sonlandı - süre takibini durdur
                            stopCallDurationTimer()
                            _uiState.update {
                                it.copy(
                                    isConnected = false,
                                    callStartTime = null,
                                    statusMessage = "Bağlantı durumu: ${event.state.name}"
                                )
                            }
                        }
                    }
                    is RtcEvent.IceCandidateGenerated -> sendIceCandidate(event.candidate)
                    is RtcEvent.Error -> _uiState.update { it.copy(statusMessage = event.message) }
                    RtcEvent.LocalVideoStarted -> _uiState.update { it.copy(localVideoVisible = true) }
                    RtcEvent.RemoteVideoAvailable -> _uiState.update { it.copy(remoteVideoVisible = true) }
                }
            }
        }
    }

    private fun observeSignaling() {
        viewModelScope.launch {
            signalingClient.messages.collect { message ->
                handleSignalingMessage(message, SignalingSource.CLOUD)
            }
        }
    }

    private fun observeSignalingStatus() {
        viewModelScope.launch {
            signalingClient.status.collect { status ->
                when (status) {
                    is SignalingStatus.Error -> {
                        val errorMessage = status.throwable.message ?: "Bilinmeyen hata"
                        
                        if (signalingMode == SignalingMode.CLOUD) {
                            // Sunucu çalıştığında normal hata mesajı göster
                            // Yerel ağ önerisi sadece kullanıcı yerel ağ modunu açtığında veya özellikle istediğinde gösterilir
                            val isNetworkError = errorMessage.contains("Unable to resolve host", ignoreCase = true) ||
                                    errorMessage.contains("No address associated with hostname", ignoreCase = true) ||
                                    errorMessage.contains("Connection refused", ignoreCase = true) ||
                                    errorMessage.contains("timeout", ignoreCase = true)
                            
                            if (isNetworkError) {
                                val timeoutMessage = if (errorMessage.contains("timeout", ignoreCase = true)) {
                                    "Sunucuya bağlanılamadı: Read timed out. Sunucu uyku modunda olabilir, lütfen tekrar deneyin."
                                } else {
                                    "Sunucuya bağlanılamadı. İnternet bağlantınızı kontrol edin."
                                }
                                _uiState.update { 
                                    it.copy(
                                        statusMessage = timeoutMessage
                                    )
                                }
                            } else {
                                _uiState.update { 
                                    it.copy(
                                        statusMessage = "Bağlantı hatası: $errorMessage"
                                    )
                                }
                            }
                        }
                    }
                    is SignalingStatus.Connecting -> {
                        // Sunucuya bağlanırken mesaj gösterme, startCall/joinCall zaten mesaj gösteriyor
                        // Gereksiz mesaj kirliliğini önlemek için burada mesaj güncellemesi yapmıyoruz
                        // CLOUD modunda özel bir işlem yapılmıyor
                    }
                    is SignalingStatus.Connected -> {
                        // Sunucuya bağlandığında mesaj gösterme, normal akış devam ediyor
                        // startCall/joinCall zaten "Teklif gönderildi" veya "Bağlandı" mesajı gösteriyor
                        // CLOUD modunda özel bir işlem yapılmıyor
                    }
                    is SignalingStatus.Disconnected -> {
                        // Disconnected durumunda sadece aktif bir görüşme varsa mesaj göster
                        if (signalingMode == SignalingMode.CLOUD && activeRoom != null && _uiState.value.isConnected) {
                            _uiState.update { 
                                it.copy(
                                    statusMessage = "Sunucu bağlantısı kesildi"
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun handleSignalingMessage(message: SignalingMessage, source: SignalingSource) {
        if (source != SignalingSource.CLOUD) {
            signalingMode = SignalingMode.DIRECT
            directChannel = when (source) {
                SignalingSource.DIRECT_CLIENT -> DirectChannel.CLIENT
                SignalingSource.DIRECT_SERVER -> DirectChannel.SERVER
                else -> directChannel // Unreachable due to if check above, but required for exhaustive when
            }
        }
        when (message) {
            is SignalingMessage.Offer -> handleIncomingOffer(source, message)
            is SignalingMessage.Answer -> rtcClient.setRemoteDescription(message.toSessionDescription())
            is SignalingMessage.IceCandidateMessage -> rtcClient.addIceCandidate(message.payload.toIceCandidate())
            is SignalingMessage.Presence -> _uiState.update { it.copy(participants = message.participants) }
            is SignalingMessage.Error -> _uiState.update { it.copy(statusMessage = message.reason) }
            is SignalingMessage.Chat -> handleIncomingChatMessage(message)
            is SignalingMessage.FileShare -> handleIncomingFileShare(message)
            // Yeni mesaj tipleri
            is SignalingMessage.Registered -> {
                android.util.Log.i("VideoCallViewModel", "Kayıt başarılı: phoneNumber=${message.phoneNumber}, name=${message.name}")
                _uiState.update { 
                    it.copy(
                        statusMessage = "Sunucuya bağlandı"
                    ) 
                }
            }
            is SignalingMessage.IncomingCall -> {
                android.util.Log.d("VideoCallViewModel", "Gelen arama alındı: caller=${message.callerPhoneNumber}, groupId=${message.groupId}")
                handleIncomingCall(message)
            }
            is SignalingMessage.CallRequestSent -> {
                android.util.Log.d("VideoCallViewModel", "Arama isteği gönderildi: groupId=${message.groupId}, roomCode=${message.roomCode}")
                _uiState.update { 
                    it.copy(
                        roomCode = message.roomCode ?: "",
                        statusMessage = "Arama başlatıldı, yanıt bekleniyor..."
                    ) 
                }
            }
            is SignalingMessage.CallError -> {
                android.util.Log.e("VideoCallViewModel", "Arama hatası: ${message.reason}, targetPhoneNumber=${message.targetPhoneNumber}")
                _uiState.update { 
                    it.copy(
                        statusMessage = "Arama hatası: ${message.reason}"
                    ) 
                }
                // Arama geçmişinden çıkar (başarısız arama)
                val currentHistory = _callHistory.value.toMutableList()
                currentHistory.removeAll { it.phoneNumber == message.targetPhoneNumber && it.callType == CallType.OUTGOING }
                _callHistory.value = currentHistory
            }
            is SignalingMessage.CallAccepted -> {
                // Arama kabul edildi, room'a bağlan
                activeRoom = message.roomCode
                _uiState.update { 
                    it.copy(
                        roomCode = message.roomCode,
                        statusMessage = "Arama kabul edildi, bağlanılıyor..."
                    ) 
                }
                // Room'a bağlan ve offer gönder
                viewModelScope.launch {
                    signalingClient.connect(message.roomCode)
                    val offer = rtcClient.createOffer(audioOnly = _uiState.value.isAudioOnly)
                    sendOffer(offer)
                }
            }
            is SignalingMessage.CallAcceptedBy -> {
                // Başka biri aramayı kabul etti
                _uiState.update { 
                    it.copy(
                        statusMessage = "${message.name ?: message.phoneNumber} aramayı kabul etti"
                    ) 
                }
            }
            is SignalingMessage.CallRejectedBy -> {
                // Başka biri aramayı reddetti
                _uiState.update { 
                    it.copy(
                        statusMessage = "${message.name ?: message.phoneNumber} aramayı reddetti"
                    ) 
                }
            }
            else -> Unit
        }
    }

    private fun extractPhoneFromOffer(offer: SignalingMessage.Offer): String? {
        // SDP'den telefon numarası çıkarma (basit implementasyon)
        // Gerçek implementasyonda signaling server'dan gelen metadata kullanılabilir
        return null // Şimdilik null, daha sonra geliştirilebilir
    }

    private fun sendOffer(offer: SessionDescription) {
        when (signalingMode) {
            SignalingMode.CLOUD -> signalingClient.sendOffer(offer)
            SignalingMode.DIRECT -> sendDirectMessage(SignalingMessage.Offer(offer.description))
        }
    }

    private fun sendAnswer(answer: SessionDescription) {
        when (signalingMode) {
            SignalingMode.CLOUD -> signalingClient.sendAnswer(answer)
            SignalingMode.DIRECT -> sendDirectMessage(SignalingMessage.Answer(answer.description))
        }
    }

    private fun sendIceCandidate(candidate: IceCandidate) {
        when (signalingMode) {
            SignalingMode.CLOUD -> signalingClient.sendIceCandidate(candidate)
            SignalingMode.DIRECT -> {
                val payload = SignalingMessage.IceCandidatePayload(
                    candidate = candidate.sdp,
                    sdpMid = candidate.sdpMid,
                    sdpMLineIndex = candidate.sdpMLineIndex
                )
                sendDirectMessage(SignalingMessage.IceCandidateMessage(payload))
            }
        }
    }

    private fun sendDirectMessage(message: SignalingMessage) {
        val delivered = peerSignalingClient.isConnected() && peerSignalingClient.send(message)
        if (!delivered) {
            localSignalingServer?.send(message)
        }
    }

    private suspend fun attemptDirectCall(contact: Contact): Boolean {
        val phoneNumber = contact.phoneNumber ?: return false
        _uiState.update { it.copy(statusMessage = "${contact.name} için doğrudan eşleşme aranıyor...") }
        val selfPresence = publishPresence() ?: return false
        val remoteHash = securityManager.hashPhoneNumber(phoneNumber)
        val remotePresence = presenceRepository.fetch(remoteHash)

        if (remotePresence == null) {
            _uiState.update { it.copy(statusMessage = "${contact.name} için hazır cihaz bulunamadı") }
            return false
        }

        if (remotePresence.phoneHash == selfPresence.phoneHash) {
            _uiState.update { it.copy(statusMessage = "Diğer cihaz çevrimiçi değil") }
            return false
        }

        val host = remotePresence.publicIp
        if (host.isNullOrBlank()) {
            _uiState.update { it.copy(statusMessage = "Karşı cihaz IP adresi paylaşmadı") }
            return false
        }

        return runCatching {
            peerSignalingClient.connect(
                host = host,
                port = remotePresence.port,
                token = remotePresence.token,
                phoneHash = selfPresence.phoneHash
            )
            signalingMode = SignalingMode.DIRECT
            directChannel = DirectChannel.CLIENT
            startPeerOffer(contact)
            true
        }.onFailure { error ->
            _uiState.update { it.copy(statusMessage = "Doğrudan bağlantı kurulamadı: ${error.message}") }
        }.getOrDefault(false)
    }

    private suspend fun publishPresence(): PresenceEntry? {
        val phoneNumber = preferencesManager.getPhoneNumber()
        if (phoneNumber.isNullOrBlank()) {
            _uiState.update { it.copy(statusMessage = "Telefon numarası kaydedilmemiş") }
            return null
        }
        ensureLocalSignalingServer()
        val publicIp = NetworkUtils.getPublicIP()
        if (publicIp.isNullOrBlank()) {
            _uiState.update { it.copy(statusMessage = "Genel IP alınamadı") }
            return null
        }
        val token = securityManager.generateNonce()
        localServerToken = token
        val entry = PresenceEntry(
            phoneHash = securityManager.hashPhoneNumber(phoneNumber),
            publicIp = publicIp,
            port = localSignalingServer?.getPort() ?: localServerPort,
            token = token,
            updatedAt = System.currentTimeMillis()
        )
        presenceRepository.publish(entry)
        lastPublishedPresenceHash = entry.phoneHash
        return entry
    }

    private suspend fun startPeerOffer(contact: Contact) {
        try {
            activeRoom = "DIRECT"
            _uiState.update { it.copy(statusMessage = "${contact.name} ile doğrudan bağlantı kuruluyor...", roomCode = "DIRECT") }
            val offer = rtcClient.createOffer()
            sendOffer(offer)
            _uiState.update { it.copy(statusMessage = "Teklif gönderildi, yanıt bekleniyor...") }
            addCallToHistory(
                contactName = contact.name,
                phoneNumber = contact.phoneNumber ?: "",
                callType = CallType.OUTGOING,
                roomCode = "DIRECT"
            )
        } catch (e: Exception) {
            _uiState.update { it.copy(statusMessage = "Doğrudan teklif gönderilemedi: ${e.message}") }
        }
    }

    fun acceptIncomingCall() {
        val call = _incomingCall.value ?: return
        notificationManager.cancelIncomingCallNotification()
        _incomingCall.value = null
        
        // Yeni sistem: call-accept mesajı gönder
        signalingClient.acceptCall(call.groupId)
        
        _uiState.update { it.copy(roomCode = call.roomCode, statusMessage = "Arama kabul edildi") }
        
        // Gelen arama geçmişine ekle
        addCallToHistory(
            contactName = call.callerName,
            phoneNumber = call.callerPhoneNumber,
            callType = CallType.INCOMING,
            roomCode = call.roomCode
        )
    }

    fun rejectIncomingCall() {
        val call = _incomingCall.value
        notificationManager.cancelIncomingCallNotification()
        _incomingCall.value = null
        
        // Yeni sistem: call-reject mesajı gönder
        if (call != null) {
            signalingClient.rejectCall(call.groupId)
        } else {
            signalingClient.leave()
        }
        
        _uiState.update { it.copy(statusMessage = "Arama reddedildi") }
    }

    private fun handleRemoteOffer(description: SessionDescription) {
        viewModelScope.launch {
            runCatching {
                rtcClient.setRemoteDescription(description)
                val isAudioOnly = _uiState.value.isAudioOnly
                val answer = rtcClient.createAnswer(audioOnly = isAudioOnly)
                sendAnswer(answer)
                _uiState.update { it.copy(statusMessage = "Karşı tarafla eşleştirildi") }
            }.onFailure { error ->
                _uiState.update { it.copy(statusMessage = error.message ?: "Yanıt gönderilemedi") }
            }
        }
    }

    private fun generateRoomCode(): String {
        return BuildConfig.APPLICATION_ID.takeLast(4).uppercase(Locale.getDefault()) +
            System.currentTimeMillis().toString(16).takeLast(4).uppercase(Locale.getDefault())
    }

    override fun onCleared() {
        super.onCleared()
        localSignalingServer?.stop()
        localSignalingServer = null
        peerSignalingClient.close()
        viewModelScope.launch {
            lastPublishedPresenceHash?.let { presenceRepository.clear(it) }
        }
        rtcClient.dispose()
        signalingClient.close()
    }

    companion object {
        private const val MAX_FILE_SIZE = 10 * 1024 * 1024L // 10 MB
        private const val CHUNK_SIZE = 60 * 1024 // 60 KB (WebRTC limit ~64KB, güvenli margin)
        
        fun factory(application: Application): ViewModelProvider.Factory {
            val appContext = application.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val rtcClient = RtcClient(appContext)
                    val signalingClient = SignalingClient(BuildConfig.SIGNALING_URL)
                    val contentResolver = appContext.contentResolver
                    val preferencesManager = PreferencesManager(appContext)
                    val networkManager = NetworkManager(appContext)
                    return VideoCallViewModel(rtcClient, signalingClient, contentResolver, preferencesManager, networkManager, appContext) as T
                }
            }
        }
    }
    
    /**
     * Widget'ı günceller
     */
    private fun updateWidget() {
        try {
            VideoCallWidgetProvider.updateAllWidgets(context)
        } catch (e: Exception) {
            android.util.Log.e("VideoCallViewModel", "Widget güncelleme hatası", e)
        }
    }
}

