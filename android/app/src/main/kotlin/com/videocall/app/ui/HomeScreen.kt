package com.videocall.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.painterResource
import com.videocall.app.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.videocall.app.model.Contact
import com.videocall.app.model.UserStatus
import com.videocall.app.ui.theme.Teal

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    addedContacts: List<Contact>,
    onNavigateToSettings: () -> Unit,
    onStartCallWithContact: (Contact) -> Unit,
    onRemoveContact: (Contact) -> Unit,
    onContactClick: (Contact) -> Unit = {}
) {
    val complianceItems = listOf(
        "Tüm medya ve sinyalleşme trafiği TLS 1.3 üzerinden uçtan uca şifrelenir.",
        "Rehber verileri yalnızca davet sürecinde okunur, sunucuya kaydedilmez.",
        "KVKK ve GDPR kapsamında veri sahibinin silme ve bilgi alma hakları desteklenir.",
        "Görüntülü görüşmeler cihaz üzerinde işlenir; bulutta kalıcı kayıt tutulmaz."
    )

    val context = LocalContext.current
    val contactsPermissionGranted = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Logo - büyütüldü (40dp -> 60dp, 1.5x)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Video Call Logo",
                modifier = Modifier.size(60.dp)
            )
        }

        // İzin uyarısı (izin yoksa)
        if (!contactsPermissionGranted) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "⚠️ İzin Gerekli",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        text = "Kişileri eklemek ve görüntülemek için Rehber izni gereklidir. Lütfen Ayarlar'dan izinleri verin.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Button(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.fillMaxWidth(),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Ayarlara Git")
                    }
                }
            }
        }

        // ActionCard kaldırıldı - Tab bar üzerinden erişilebilir

        if (addedContacts.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ekli Kişiler",
                        style = MaterialTheme.typography.titleMedium
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(addedContacts) { contact ->
                            AddedContactItem(
                                contact = contact,
                                onCallClick = { onStartCallWithContact(contact) },
                                onRemoveClick = { onRemoveContact(contact) },
                                onClick = { onContactClick(contact) }
                            )
                        }
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Güvenlik ve Uyumluluk",
                    style = MaterialTheme.typography.titleMedium
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(complianceItems) { item ->
                        Text("• $item", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

// StatusCard ve ActionCard kaldırıldı - Tab bar üzerinden erişilebilir

@Composable
fun AddedContactItem(
    modifier: Modifier = Modifier,
    contact: Contact,
    onCallClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onClick: () -> Unit = {}
) {
    // Uygulamayı kullanan kişilerin çerçevesi turuncu olacak
    val isAppUser = contact.status == UserStatus.ONLINE
    val borderColor = if (isAppUser) {
        androidx.compose.ui.graphics.Color(0xFFFF9800) // Turuncu
    } else {
        Teal
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isAppUser) {
                    Modifier.border(2.dp, borderColor, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.padding(4.dp),
                tint = Teal
            )
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
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = contact.phoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(onClick = onCallClick) {
                Icon(
                    imageVector = Icons.Default.VideoCall,
                    contentDescription = "Görüşme Başlat",
                    tint = Teal
                )
            }
            IconButton(onClick = onRemoveClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Kaldır",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

