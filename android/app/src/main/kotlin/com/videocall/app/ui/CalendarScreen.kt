package com.videocall.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videocall.app.model.Contact
import com.videocall.app.model.ScheduledCall
import com.videocall.app.ui.theme.Teal
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarScreen(
    scheduledCalls: List<ScheduledCall>,
    contacts: List<Contact>,
    onScheduleCall: (String, String?, Long, String?, String?) -> Unit,
    onCancelCall: (Long) -> Unit,
    onCompleteCall: (Long) -> Unit,
    onDeleteCall: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedCall by remember { mutableStateOf<ScheduledCall?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Başlık ve Yeni Randevu butonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Randevular",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Teal
            )
            
            FloatingActionButton(
                onClick = { showScheduleDialog = true },
                containerColor = Teal,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Yeni Randevu",
                    tint = Color.White
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Randevu listesi
        if (scheduledCalls.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Henüz randevu yok",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray
                    )
                    Text(
                        text = "Yeni randevu oluşturmak için + butonuna tıklayın",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(scheduledCalls) { call ->
                    ScheduledCallItem(
                        call = call,
                        onCancel = { onCancelCall(call.id) },
                        onComplete = { onCompleteCall(call.id) },
                        onDelete = { onDeleteCall(call.id) },
                        onEdit = { selectedCall = call }
                    )
                }
            }
        }
    }
    
    // Randevu oluşturma/düzenleme dialog'u
    if (showScheduleDialog || selectedCall != null) {
        ScheduleCallDialog(
            contact = selectedCall?.let { call ->
                contacts.find { it.phoneNumber == call.contactPhoneNumber }
            },
            initialCall = selectedCall,
            contacts = contacts,
            onDismiss = {
                showScheduleDialog = false
                selectedCall = null
            },
            onSchedule = { contactName, phoneNumber, scheduledTime, roomCode, notes ->
                onScheduleCall(contactName, phoneNumber, scheduledTime, roomCode, notes)
                showScheduleDialog = false
                selectedCall = null
            }
        )
    }
}

@Composable
fun ScheduledCallItem(
    call: ScheduledCall,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUpcoming = call.isUpcoming()
    val isPast = call.isPast()
    val isCompleted = call.isCompleted
    val isCancelled = call.isCancelled
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isCancelled -> Color.Gray.copy(alpha = 0.2f)
                isCompleted -> Color.Green.copy(alpha = 0.1f)
                isPast -> Color(0xFFFF9800).copy(alpha = 0.1f) // Orange
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Başlık satırı
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Teal,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = call.contactName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Durum badge
                Surface(
                    color = when {
                        isCancelled -> Color.Gray
                        isCompleted -> Color.Green
                        isPast -> Color(0xFFFF9800) // Orange
                        else -> Teal
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = when {
                            isCancelled -> "İptal"
                            isCompleted -> "Tamamlandı"
                            isPast -> "Geçti"
                            else -> "Yaklaşıyor"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            // Tarih ve saat
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = call.getFormattedDateTime(),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            // Telefon numarası
            if (call.contactPhoneNumber != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = call.contactPhoneNumber,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            // Notlar
            if (call.notes != null && call.notes.isNotBlank()) {
                Text(
                    text = call.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Kalan süre (yaklaşan randevular için)
            if (isUpcoming) {
                val timeUntil = call.getTimeUntilCall()
                val hours = timeUntil / (1000 * 60 * 60)
                val minutes = (timeUntil % (1000 * 60 * 60)) / (1000 * 60)
                
                Text(
                    text = if (hours > 0) {
                        "$hours saat $minutes dakika sonra"
                    } else {
                        "$minutes dakika sonra"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Teal,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            // Aksiyon butonları
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isUpcoming) {
                    TextButton(
                        onClick = onComplete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.Green
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Tamamla",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tamamla")
                    }
                    
                    TextButton(
                        onClick = onCancel,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFFF9800) // Orange
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "İptal Et",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("İptal Et")
                    }
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Sil",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleCallDialog(
    contact: Contact? = null,
    initialCall: ScheduledCall? = null,
    contacts: List<Contact>,
    onDismiss: () -> Unit,
    onSchedule: (String, String?, Long, String?, String?) -> Unit
) {
    var selectedContact by remember { mutableStateOf<Contact?>(contact) }
    var selectedDate by remember { mutableStateOf<Date?>(initialCall?.let { Date(it.scheduledTime) }) }
    var selectedHour by remember { mutableStateOf(initialCall?.let {
        Calendar.getInstance().apply { timeInMillis = it.scheduledTime }.get(Calendar.HOUR_OF_DAY)
    } ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(initialCall?.let {
        Calendar.getInstance().apply { timeInMillis = it.scheduledTime }.get(Calendar.MINUTE)
    } ?: 0) }
    var roomCode by remember { mutableStateOf(initialCall?.roomCode ?: "") }
    var notes by remember { mutableStateOf(initialCall?.notes ?: "") }
    var showContactPicker by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCall != null) "Randevu Düzenle" else "Yeni Randevu",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Kişi seçimi
                OutlinedTextField(
                    value = selectedContact?.name ?: "",
                    onValueChange = { },
                    label = { Text("Kişi") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showContactPicker = true }) {
                            Icon(Icons.Default.Person, contentDescription = "Kişi Seç")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Tarih seçimi
                OutlinedTextField(
                    value = selectedDate?.let {
                        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(it)
                    } ?: "",
                    onValueChange = { },
                    label = { Text("Tarih") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            // TODO: DatePicker dialog aç
                        }) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Tarih Seç")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Saat seçimi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Saat
                    OutlinedTextField(
                        value = String.format("%02d", selectedHour),
                        onValueChange = { value ->
                            value.toIntOrNull()?.takeIf { it in 0..23 }?.let {
                                selectedHour = it
                            }
                        },
                        label = { Text("Saat") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    // Dakika
                    OutlinedTextField(
                        value = String.format("%02d", selectedMinute),
                        onValueChange = { value ->
                            value.toIntOrNull()?.takeIf { it in 0..59 }?.let {
                                selectedMinute = it
                            }
                        },
                        label = { Text("Dakika") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                // Oda kodu (opsiyonel)
                OutlinedTextField(
                    value = roomCode,
                    onValueChange = { roomCode = it },
                    label = { Text("Oda Kodu (Opsiyonel)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Notlar
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notlar (Opsiyonel)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedContact != null && selectedDate != null) {
                        val calendar = Calendar.getInstance().apply {
                            time = selectedDate!!
                            set(Calendar.HOUR_OF_DAY, selectedHour)
                            set(Calendar.MINUTE, selectedMinute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        
                        onSchedule(
                            selectedContact!!.name,
                            selectedContact!!.phoneNumber,
                            calendar.timeInMillis,
                            roomCode.takeIf { it.isNotBlank() },
                            notes.takeIf { it.isNotBlank() }
                        )
                    }
                },
                enabled = selectedContact != null && selectedDate != null
            ) {
                Text(if (initialCall != null) "Güncelle" else "Oluştur")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("İptal")
            }
        }
    )
    
    // Kişi seçici dialog
    if (showContactPicker) {
        AlertDialog(
            onDismissRequest = { showContactPicker = false },
            title = { Text("Kişi Seç") },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    items(contacts) { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedContact = contact
                                    showContactPicker = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Teal
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (contact.phoneNumber != null) {
                                    Text(
                                        text = contact.phoneNumber,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContactPicker = false }) {
                    Text("İptal")
                }
            }
        )
    }
}

