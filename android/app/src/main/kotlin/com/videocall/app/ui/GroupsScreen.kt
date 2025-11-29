package com.videocall.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GroupsScreen(
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Gruplar",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Button(
            onClick = onNavigateToCreateGroup,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Yeni Grup Oluştur")
        }
        
        Spacer(Modifier.height(16.dp))
        
        Button(
            onClick = onNavigateToBlockedUsers,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Block, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Engellenenler")
        }
        
        Spacer(Modifier.height(16.dp))
        
        Text(
            text = "Grup listesi burada gösterilecek",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

