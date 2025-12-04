package com.videocall.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.videocall.app.model.ChatMessage
import com.videocall.app.model.Contact
import com.videocall.app.viewmodel.VideoCallViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    viewModel: VideoCallViewModel,
    addedContacts: List<Contact>,
    modifier: Modifier = Modifier
) {
    val chatMessagesByContact by viewModel.chatMessagesByContact.collectAsState()
    
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var messageText by remember { mutableStateOf("") }
    
    // Seçili kişiye göre mesajları al
    val filteredMessages = if (selectedContact?.phoneNumber != null) {
        chatMessagesByContact[selectedContact!!.phoneNumber] ?: emptyList()
    } else {
        emptyList()
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Başlık ve Arama İkonları
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sohbetler",
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Sesli Arama ve Görüntülü Arama İkonları (Ana sayfada görünür)
            if (selectedContact == null) {
                var showContactSelectionDialog by remember { mutableStateOf(false) }
                var callType by remember { mutableStateOf<Boolean?>(null) } // true = audio, false = video
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sesli Arama İkonu
                    IconButton(
                        onClick = {
                            if (addedContacts.isEmpty()) {
                                return@IconButton
                            }
                            if (addedContacts.size == 1) {
                                // Tek kişi varsa direkt ara
                                viewModel.startCallWithContact(addedContacts.first(), audioOnly = true)
                            } else {
                                // Birden fazla kişi varsa dialog göster
                                callType = true
                                showContactSelectionDialog = true
                            }
                        },
                        enabled = addedContacts.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = "Sesli Arama",
                            tint = if (addedContacts.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    }
                    
                    // Görüntülü Arama İkonu
                    IconButton(
                        onClick = {
                            if (addedContacts.isEmpty()) {
                                return@IconButton
                            }
                            if (addedContacts.size == 1) {
                                // Tek kişi varsa direkt ara
                                viewModel.startCallWithContact(addedContacts.first(), audioOnly = false)
                            } else {
                                // Birden fazla kişi varsa dialog göster
                                callType = false
                                showContactSelectionDialog = true
                            }
                        },
                        enabled = addedContacts.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Görüntülü Arama",
                            tint = if (addedContacts.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            }
                        )
                    }
                }
                
                // Kişi Seçim Dialog'u
                if (showContactSelectionDialog && callType != null) {
                    AlertDialog(
                        onDismissRequest = { 
                            showContactSelectionDialog = false
                            callType = null
                        },
                        title = {
                            Text(
                                text = if (callType == true) "Sesli Arama" else "Görüntülü Arama",
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        text = {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 400.dp)
                            ) {
                                items(addedContacts) { contact ->
                                    Card(
                                        onClick = {
                                            viewModel.startCallWithContact(
                                                contact, 
                                                audioOnly = callType == true
                                            )
                                            showContactSelectionDialog = false
                                            callType = null
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = contact.name,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (contact.phoneNumber != null) {
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        text = contact.phoneNumber,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = if (callType == true) Icons.Default.Phone else Icons.Default.Videocam,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = { 
                                    showContactSelectionDialog = false
                                    callType = null
                                }
                            ) {
                                Text("İptal")
                            }
                        }
                    )
                }
            }
        }
        
        if (selectedContact == null) {
            // Kişi listesi
            if (addedContacts.isEmpty()) {
                Text(
                    text = "Sohbet yapmak için önce bir kişi ekleyin",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyColumn {
                    items(addedContacts) { contact ->
                        ChatContactItem(
                            contact = contact,
                            lastMessage = chatMessagesByContact[contact.phoneNumber]?.lastOrNull(),
                            onClick = {
                                selectedContact = contact
                            }
                        )
                    }
                }
            }
        } else {
            // Chat ekranı
            ChatDetailScreen(
                contact = selectedContact!!,
                messages = filteredMessages,
                messageText = messageText,
                onMessageTextChange = { messageText = it },
                onSendMessage = {
                    val phoneNumber = selectedContact?.phoneNumber
                    if (messageText.isNotBlank() && phoneNumber != null) {
                        // Chat mesajı gönder (kişiye özel)
                        viewModel.sendChatMessage(phoneNumber, messageText)
                        messageText = ""
                    }
                },
                onBack = {
                    selectedContact = null
                },
                onStartAudioCall = {
                    viewModel.startCallWithContact(selectedContact!!, audioOnly = true)
                },
                onStartVideoCall = {
                    viewModel.startCallWithContact(selectedContact!!, audioOnly = false)
                },
                onEditMessage = { messageId, newMessage ->
                    val phoneNumber = selectedContact?.phoneNumber
                    if (phoneNumber != null) {
                        viewModel.editChatMessage(messageId, phoneNumber, newMessage)
                    }
                },
                onDeleteMessage = { messageId, targetPhoneNumber ->
                    viewModel.deleteChatMessage(messageId, targetPhoneNumber)
                },
                onDeleteMessageFromOther = { messageId, targetPhoneNumber ->
                    viewModel.deleteChatMessageFromOther(messageId, targetPhoneNumber)
                },
                onSaveChat = {
                    val phoneNumber = selectedContact?.phoneNumber
                    if (phoneNumber != null) {
                        viewModel.saveChatToPhone(phoneNumber)
                    }
                }
            )
        }
    }
}

@Composable
fun ChatContactItem(
    contact: Contact,
    lastMessage: ChatMessage?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp)
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
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (lastMessage != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = lastMessage.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            if (lastMessage != null) {
                Text(
                    text = formatTime(lastMessage.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ChatDetailScreen(
    contact: Contact,
    messages: List<ChatMessage>,
    messageText: String,
    onMessageTextChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onBack: () -> Unit,
    onStartAudioCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onEditMessage: ((String, String) -> Unit)? = null, // messageId, newMessage
    onDeleteMessage: ((String, String) -> Unit)? = null, // messageId, targetPhoneNumber
    onDeleteMessageFromOther: ((String, String) -> Unit)? = null, // messageId, targetPhoneNumber
    onSaveChat: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferencesManager = remember { com.videocall.app.data.PreferencesManager(context) }
    val chatBackgroundColor = remember { 
        mutableStateOf(preferencesManager.getChatBackgroundColor() ?: "#FFFFFF")
    }
    
    var showChatSettingsMenu by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Color(
                    android.graphics.Color.parseColor(chatBackgroundColor.value)
                )
            )
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri"
                    )
                }
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Chat Ayarları, Sesli Arama ve Görüntülü Arama İkonları
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Chat Ayarları Butonu
                IconButton(onClick = { showChatSettingsMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Chat Ayarları",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                // Sesli Arama İkonu
                IconButton(
                    onClick = onStartAudioCall,
                    enabled = contact.phoneNumber != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = "Sesli Arama",
                        tint = if (contact.phoneNumber != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                }
                
                // Görüntülü Arama İkonu
                IconButton(
                    onClick = onStartVideoCall,
                    enabled = contact.phoneNumber != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Görüntülü Arama",
                        tint = if (contact.phoneNumber != null) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        }
                    )
                }
            }
        }
        
        HorizontalDivider()
        
        // Chat Ayarları Menu
        if (showChatSettingsMenu) {
            AlertDialog(
                onDismissRequest = { showChatSettingsMenu = false },
                title = { Text("Chat Ayarları") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onSaveChat != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSaveChat()
                                        showChatSettingsMenu = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Chat'i Telefona Kaydet")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChatSettingsMenu = false }) {
                        Text("Kapat")
                    }
                }
            )
        }
        
        // Mesaj listesi
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            items(messages) { message ->
                ChatMessageBubble(
                    message = message,
                    isFromMe = message.isFromMe,
                    onEditClick = onEditMessage?.let { { it(message.id, message.message) } },
                    onDeleteClick = onDeleteMessage?.let { { it(message.id, contact.phoneNumber ?: "") } },
                    onDeleteFromOtherClick = onDeleteMessageFromOther?.let { { it(message.id, contact.phoneNumber ?: "") } }
                )
            }
        }
        
        HorizontalDivider()
        
        // Chat Ayarları Menu
        if (showChatSettingsMenu) {
            AlertDialog(
                onDismissRequest = { showChatSettingsMenu = false },
                title = { Text("Chat Ayarları") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (onSaveChat != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSaveChat()
                                        showChatSettingsMenu = false
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Chat'i Telefona Kaydet")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChatSettingsMenu = false }) {
                        Text("Kapat")
                    }
                }
            )
        }
        
        // Mesaj gönderme alanı
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = onMessageTextChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mesaj yazın...") },
                singleLine = true
            )
            IconButton(
                onClick = onSendMessage,
                enabled = messageText.isNotBlank()
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Gönder",
                    tint = if (messageText.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    isFromMe: Boolean,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onDeleteFromOtherClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(message.message) }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 280.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isFromMe) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (!isFromMe && message.senderName != null) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = message.message,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (message.isEdited) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Düzenlendi",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Mesaj durumu göstergesi (sadece kendi mesajlarımız için)
                    if (isFromMe) {
                        MessageStatusIcon(status = message.status)
                    }
                }
                
                // Mesaj işlemleri (uzun basma menüsü)
                if (isFromMe) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (onEditClick != null) {
                            TextButton(onClick = { showEditDialog = true }) {
                                Text("Düzenle", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        if (onDeleteClick != null || onDeleteFromOtherClick != null) {
                            TextButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Sil",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Silme dialog'u
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Mesajı Sil") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Mesajı nasıl silmek istersiniz?")
                    if (onDeleteClick != null) {
                        TextButton(
                            onClick = {
                                onDeleteClick()
                                showDeleteDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Benden Sil")
                        }
                    }
                    if (onDeleteFromOtherClick != null) {
                        TextButton(
                            onClick = {
                                onDeleteFromOtherClick()
                                showDeleteDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Karşıdan Sil")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
    
    // Düzenleme dialog'u
    if (showEditDialog && onEditClick != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Mesajı Düzenle") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("Mesaj") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editText.isNotBlank() && editText != message.message) {
                            onEditClick()
                            showEditDialog = false
                        }
                    },
                    enabled = editText.isNotBlank() && editText != message.message
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
fun MessageStatusIcon(status: com.videocall.app.model.MessageStatus) {
    val iconColor = when (status) {
        com.videocall.app.model.MessageStatus.SENDING,
        com.videocall.app.model.MessageStatus.SENT -> 
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) // Gri
        com.videocall.app.model.MessageStatus.DELIVERED,
        com.videocall.app.model.MessageStatus.READ -> 
            androidx.compose.ui.graphics.Color(0xFF2196F3) // Mavi
        com.videocall.app.model.MessageStatus.FAILED -> 
            MaterialTheme.colorScheme.error
    }
    
    val icon = when (status) {
        com.videocall.app.model.MessageStatus.SENDING -> 
            Icons.Default.Schedule // Saat ikonu (gönderiliyor)
        com.videocall.app.model.MessageStatus.SENT -> 
            Icons.Default.Check // Tek çentik (gönderildi)
        com.videocall.app.model.MessageStatus.DELIVERED -> 
            Icons.Default.DoneAll // Çift çentik (ulaştı)
        com.videocall.app.model.MessageStatus.READ -> 
            Icons.Default.DoneAll // Çift çentik (okundu - mavi)
        com.videocall.app.model.MessageStatus.FAILED -> 
            Icons.Default.Error // Hata ikonu
    }
    
    Icon(
        imageVector = icon,
        contentDescription = status.name,
        tint = iconColor,
        modifier = Modifier.size(16.dp)
    )
}

private fun formatTime(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - timestamp
    
    return when {
        diff < 60000 -> "Şimdi" // 1 dakikadan az
        diff < 3600000 -> "${diff / 60000} dk önce" // 1 saatten az
        diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date) // Bugün
        else -> SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(date) // Daha eski
    }
}

