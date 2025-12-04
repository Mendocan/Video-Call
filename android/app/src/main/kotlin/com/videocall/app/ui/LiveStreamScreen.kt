package com.videocall.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.videocall.app.model.Contact
import com.videocall.app.viewmodel.VideoCallViewModel

@Composable
@Suppress("UNUSED_VALUE")
fun LiveStreamSelectionScreen(
    viewModel: VideoCallViewModel,
    addedContacts: List<Contact>,
    groups: List<com.videocall.app.signaling.SignalingMessage.GroupListItem>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Kişiler, 1 = Gruplar
    var selectedContacts by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedGroups by remember { mutableStateOf<Set<String>>(emptySet()) }
    var liveTitle by remember { mutableStateOf("") }
    val showStartDialog = remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                    text = "Canlı Yayın Başlat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Başlık girişi
        OutlinedTextField(
            value = liveTitle,
            onValueChange = { liveTitle = it },
            label = { Text("Yayın Başlığı (Opsiyonel)") },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Örn: Canlı Yayın") }
        )
        
        Spacer(Modifier.height(16.dp))
        
        // Tab seçimi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("Kişiler") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Gruplar") },
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Seçim listesi
        if (selectedTab == 0) {
            // Kişiler listesi
            if (addedContacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Eklenmiş kişi yok",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(addedContacts) { contact ->
                        ContactSelectionItem(
                            contact = contact,
                            isSelected = selectedContacts.contains(contact.phoneNumber),
                            onToggle = {
                                val newSet = selectedContacts.toMutableSet()
                                if (newSet.contains(contact.phoneNumber)) {
                                    newSet.remove(contact.phoneNumber)
                                } else {
                                    contact.phoneNumber?.let { newSet.add(it) }
                                }
                                selectedContacts = newSet
                            }
                        )
                    }
                }
            }
        } else {
            // Gruplar listesi
            if (groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Grup yok",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(groups) { group ->
                        GroupSelectionItem(
                            group = group,
                            isSelected = selectedGroups.contains(group.groupId),
                            onToggle = {
                                val newSet = selectedGroups.toMutableSet()
                                if (newSet.contains(group.groupId)) {
                                    newSet.remove(group.groupId)
                                } else {
                                    newSet.add(group.groupId)
                                }
                                selectedGroups = newSet
                            }
                        )
                    }
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        // Başlat butonu
        Button(
            onClick = {
                if (selectedContacts.isNotEmpty() || selectedGroups.isNotEmpty()) {
                    showStartDialog.value = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedContacts.isNotEmpty() || selectedGroups.isNotEmpty()
        ) {
            Icon(
                imageVector = Icons.Default.Videocam,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Canlı Yayını Başlat")
        }
    }
    
    // Başlatma onay dialog'u
    if (showStartDialog.value) {
        AlertDialog(
            onDismissRequest = { showStartDialog.value = false },
            title = { Text("Canlı Yayını Başlat") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Seçilen ${selectedContacts.size} kişi ve ${selectedGroups.size} gruba canlı yayın bildirimi gönderilecek.")
                    if (liveTitle.isNotBlank()) {
                        Text("Başlık: $liveTitle")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedContactList = addedContacts.filter { 
                            it.phoneNumber != null && selectedContacts.contains(it.phoneNumber)
                        }
                        viewModel.startLiveStream(
                            title = liveTitle.takeIf { it.isNotBlank() },
                            selectedContacts = selectedContactList,
                            selectedGroupIds = selectedGroups.toList()
                        )
                        // State güncellemesi - Compose UI'ı yeniden render eder
                        showStartDialog.value = false
                        onBack()
                    }
                ) {
                    Text("Başlat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDialog.value = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

@Composable
private fun ContactSelectionItem(
    contact: Contact,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
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
                if (contact.phoneNumber != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = contact.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Seçili",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Seçili değil",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GroupSelectionItem(
    group: com.videocall.app.signaling.SignalingMessage.GroupListItem,
    isSelected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
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
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${group.memberCount} üye",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Seçili",
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Seçili değil",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
