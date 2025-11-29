package com.videocall.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
        // Başlık
        Text(
            text = "Sohbetler",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
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
        }
        
        HorizontalDivider()
        
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
                    isFromMe = message.isFromMe
                )
            }
        }
        
        HorizontalDivider()
        
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
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isFromMe) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
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
                Spacer(Modifier.height(4.dp))
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
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

