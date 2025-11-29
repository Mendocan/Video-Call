@file:Suppress("UNUSED_PARAMETER", "UNUSED_ANONYMOUS_PARAMETER")

package com.videocall.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.automirrored.filled.Note
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.videocall.app.model.Contact
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import android.net.Uri
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import com.videocall.app.utils.ImagePicker
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight

@Composable
fun ContactsScreen(
    permissionGranted: Boolean,
    contacts: List<Contact>,
    addedContacts: List<Contact>,
    onPermissionResult: (Boolean) -> Unit,
    onContactAdd: (Contact) -> Unit,
    onContactUpdate: (Contact) -> Unit = {},
    onGenerateQRCode: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }
    var newContactEmail by remember { mutableStateOf("") }
    
    val filteredContacts = remember(contacts, searchQuery) {
        if (searchQuery.isBlank()) {
            contacts
        } else {
            val query = searchQuery.lowercase()
            contacts.filter { contact ->
                contact.name.lowercase().contains(query) ||
                contact.phoneNumber?.lowercase()?.contains(query) == true ||
                contact.email?.lowercase()?.contains(query) == true ||
                contact.notes?.lowercase()?.contains(query) == true
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = onPermissionResult
    )

    fun requestPermission() {
        val alreadyGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            onPermissionResult(true)
        } else {
            launcher.launch(Manifest.permission.READ_CONTACTS)
        }
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
            Text(
                text = "Kişiler",
                style = MaterialTheme.typography.headlineSmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onGenerateQRCode != null && addedContacts.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onGenerateQRCode,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("QR Kod")
                    }
                }
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text("Kişi Ekle")
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Yeni Kişi Ekle") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = newContactName,
                            onValueChange = { newContactName = it },
                            label = { Text("İsim *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newContactPhone,
                            onValueChange = { newContactPhone = it },
                            label = { Text("Telefon Numarası *") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = newContactEmail,
                            onValueChange = { newContactEmail = it },
                            label = { Text("E-posta") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newContactName.isNotBlank() && newContactPhone.isNotBlank()) {
                                val newContact = Contact(
                                    id = System.currentTimeMillis(),
                                    name = newContactName,
                                    phoneNumber = newContactPhone,
                                    email = newContactEmail.takeIf { it.isNotBlank() },
                                    photoUri = null
                                )
                                onContactAdd(newContact)
                                newContactName = ""
                                newContactPhone = ""
                                newContactEmail = ""
                                showAddDialog = false
                            }
                        },
                        enabled = newContactName.isNotBlank() && newContactPhone.isNotBlank(),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Ekle")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("İptal")
                    }
                }
            )
        }

        if (!permissionGranted) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { requestPermission() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("Rehber İznini Talep Et")
                    }
                }
            }
        }

        if (permissionGranted && contacts.isNotEmpty()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Rehberdeki Kişiler",
                        style = MaterialTheme.typography.titleMedium
                    )
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("İsim, telefon veya e-posta ile ara...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Ara",
                                tint = com.videocall.app.ui.theme.Teal
                            )
                        },
                        singleLine = true
                    )
                    if (filteredContacts.isEmpty() && searchQuery.isNotBlank()) {
                        Text(
                            text = "Arama sonucu bulunamadı.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredContacts) { contact ->
                                val isAdded = addedContacts.any { it.id == contact.id }
                                val addedContact = if (isAdded) addedContacts.first { it.id == contact.id } else contact
                                ContactItem(
                                    contact = addedContact,
                                    isAdded = isAdded,
                                    onClick = { onContactAdd(contact) },
                                    onEdit = onContactUpdate
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactItem(
    contact: Contact,
    isAdded: Boolean = false,
    onClick: () -> Unit,
    onEdit: ((Contact) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditNoteDialog by remember { mutableStateOf(false) }
    var showPhotoPickerDialog by remember { mutableStateOf(false) }
    var noteText by remember { mutableStateOf(contact.notes ?: "") }
    
    val imagePickerLauncher = ImagePicker.rememberImagePickerLauncher { uri ->
        if (uri != null) {
            val fileName = "contact_${contact.id}_${System.currentTimeMillis()}.jpg"
            val copiedUri = ImagePicker.copyUriToAppStorage(context, uri, fileName)
            if (copiedUri != null) {
                val updatedContact = contact.copy(photoUri = copiedUri.toString())
                onEdit?.invoke(updatedContact)
            }
        }
        showPhotoPickerDialog = false
    }
    
    LaunchedEffect(contact.notes) {
        noteText = contact.notes ?: ""
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isAdded) {
                com.videocall.app.ui.theme.Teal.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profil fotoğrafı veya avatar
            if (contact.photoUri != null && contact.photoUri.isNotBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(context)
                            .data(contact.photoUri)
                            .crossfade(true)
                            .build()
                    ),
                    contentDescription = contact.name,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(2.dp, com.videocall.app.ui.theme.Teal, CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Varsayılan avatar (baş harfler)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(com.videocall.app.ui.theme.Teal.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ImagePicker.generateAvatarInitials(contact.name),
                        style = MaterialTheme.typography.bodyLarge,
                        color = com.videocall.app.ui.theme.Teal,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
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
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = contact.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (contact.notes != null && contact.notes.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Note,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Text(
                            text = if (contact.notes.length > 50) "${contact.notes.take(50)}..." else contact.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1
                        )
                    }
                }
            }
            if (isAdded && onEdit != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showPhotoPickerDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Fotoğraf Ekle",
                            tint = com.videocall.app.ui.theme.Teal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = { showEditNoteDialog = true },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Not Düzenle",
                            tint = com.videocall.app.ui.theme.Teal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else if (!isAdded) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ekle",
                    tint = com.videocall.app.ui.theme.Teal,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
    
    // Not Düzenleme Dialog'u
    if (showEditNoteDialog) {
        AlertDialog(
            onDismissRequest = { 
                showEditNoteDialog = false
                noteText = contact.notes ?: ""
            },
            title = { Text("${contact.name} - Not Ekle/Düzenle") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Not") },
                        placeholder = { Text("Örn: Doğum günü yaklaşıyor, önemli proje hakkında konuş") },
                        maxLines = 4,
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedContact = contact.copy(notes = noteText.takeIf { it.isNotBlank() })
                        onEdit?.invoke(updatedContact)
                        showEditNoteDialog = false
                    }
                ) {
                    Text("Kaydet")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showEditNoteDialog = false
                    noteText = contact.notes ?: ""
                }) {
                    Text("İptal")
                }
            }
        )
    }
}

