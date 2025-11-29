package com.videocall.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.videocall.app.model.Contact

@Composable
fun CreateGroupScreen(
    addedContacts: List<Contact>,
    onCreateGroup: (String, List<String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var groupName by remember { mutableStateOf("") }
    var selectedMembers by remember { mutableStateOf<Set<String>>(emptySet()) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
            }
            Text(
                text = "Yeni Grup",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(Modifier.width(48.dp))
        }
        
        Spacer(Modifier.height(16.dp))
        
        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("Grup Adı") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "Üyeleri Seçin",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyColumn {
            items(addedContacts) { contact ->
                val isSelected = selectedMembers.contains(contact.phoneNumber)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = contact.phoneNumber ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            selectedMembers = if (checked) {
                                selectedMembers + (contact.phoneNumber ?: "")
                            } else {
                                selectedMembers - (contact.phoneNumber ?: "")
                            }
                        }
                    )
                }
            }
        }
        
        Spacer(Modifier.weight(1f))
        
        Button(
            onClick = {
                if (groupName.isNotBlank() && selectedMembers.isNotEmpty()) {
                    onCreateGroup(groupName, selectedMembers.toList())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = groupName.isNotBlank() && selectedMembers.isNotEmpty()
        ) {
            Text("Grup Oluştur")
        }
    }
}

