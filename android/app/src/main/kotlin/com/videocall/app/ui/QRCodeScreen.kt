package com.videocall.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.videocall.app.ui.components.QRCodeScanner
import com.videocall.app.ui.theme.Teal

/**
 * QR kod oluşturma ve okuma ekranı
 */
@Composable
fun QRCodeScreen(
    qrBitmap: android.graphics.Bitmap?,
    connectionInfoJson: String?,
    onClose: () -> Unit,
    onQRCodeScanned: (String) -> Unit,
    onGenerateQR: (String?) -> Unit,
    onShareConnection: (String) -> Unit,
    onManualConnect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showScanner by remember { mutableStateOf(false) }
    var sharedSecret by remember { mutableStateOf("") }
    var showSecretInput by remember { mutableStateOf(false) }
    var showManualConnect by remember { mutableStateOf(false) }
    var manualConnectionJson by remember { mutableStateOf("") }
    var showConnectionInfo by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Başlık
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "QR Kod Bağlantısı",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kapat"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Seçenekler
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { 
                    showSecretInput = true
                    onGenerateQR(sharedSecret.takeIf { it.isNotBlank() })
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("QR Kod Oluştur")
            }
            
            Button(
                onClick = { showScanner = true },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(4.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = Teal
                )
            ) {
                Icon(
                    imageVector = Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text("QR Kod Oku")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Yardımcı Metin: Uzaktan Bağlantı Rehberi
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            var showHelpText by remember { mutableStateOf(false) }
            
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📱 Uzaktan Bağlantı Nasıl Yapılır?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Teal
                    )
                    IconButton(
                        onClick = { showHelpText = !showHelpText }
                    ) {
                        Icon(
                            imageVector = if (showHelpText) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (showHelpText) "Gizle" else "Göster",
                            tint = Teal
                        )
                    }
                }
                
                if (showHelpText) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Madde 1
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "1️⃣",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "QR Kod Oluştur",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Yukarıdaki 'QR Kod Oluştur' butonuna tıklayın. QR kodunuz ve bağlantı bilgileriniz hazır olacak.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Madde 2
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "2️⃣",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Paylaş Butonuna Tıklayın",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "QR kod oluşturulduktan sonra görünen 'Paylaş' butonuna tıklayın. Bağlantı bilgileriniz SMS, WhatsApp, e-posta veya diğer uygulamalar ile paylaşılabilir hale gelir.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Madde 3
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "3️⃣",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Karşı Tarafa Gönderin",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Bağlantı bilgilerini karşı tarafa gönderin (SMS, WhatsApp, e-posta veya başka bir yöntemle). Karşı taraf bu bilgileri alacak.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Madde 4
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "4️⃣",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Karşı Taraf Bağlansın",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Karşı taraf, aldığı bağlantı bilgilerini ekranın altındaki 'Uzaktan Bağlantı' bölümüne yapıştırıp 'Bağlan' butonuna tıklamalı. Bağlantı otomatik olarak kurulacak.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        // Not
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Teal.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = "💡 İpucu: Yan yana olduğunuzda QR kod okutma daha hızlıdır. Uzaktan bağlantı için yukarıdaki adımları takip edin.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp),
                                color = Teal
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (showSecretInput) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Paylaşılan Şifre (İsteğe Bağlı)",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Ekstra güvenlik için karşı taraf ile paylaştığınız şifreyi girin",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = sharedSecret,
                        onValueChange = { sharedSecret = it },
                        label = { Text("Şifre (opsiyonel)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // QR kod gösterimi
        if (qrBitmap != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "QR Kodunuz",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Kod",
                        modifier = Modifier.size(300.dp)
                    )
                    Text(
                        text = "Yan yana olduğunuzda: Karşı taraf bu QR kodu okusun",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (sharedSecret.isNotBlank()) {
                        Text(
                            text = "Şifre: $sharedSecret",
                            style = MaterialTheme.typography.bodySmall,
                            color = Teal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    // Paylaş butonları
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { 
                                connectionInfoJson?.let { onShareConnection(it) }
                            },
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Teal
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Paylaş")
                        }
                        Button(
                            onClick = { showConnectionInfo = !showConnectionInfo },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Text("Bilgi")
                        }
                    }
                    
                    // Bağlantı bilgileri gösterimi
                    if (showConnectionInfo && connectionInfoJson != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Uzaktan Bağlantı İçin:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Bu bilgileri karşı tarafa gönderin (SMS, WhatsApp, e-posta vb.)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = connectionInfoJson ?: "",
                                    onValueChange = { },
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Bağlantı Bilgileri") },
                                    maxLines = 5
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Manuel bağlantı girişi (uzaktan bağlantı için)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Uzaktan Bağlantı",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showManualConnect = !showManualConnect }
                    ) {
                        Icon(
                            imageVector = if (showManualConnect) Icons.Default.Close else Icons.Default.Link,
                            contentDescription = if (showManualConnect) "Kapat" else "Aç"
                        )
                    }
                }
                Text(
                    text = "Karşı taraftan aldığınız bağlantı bilgilerini buraya yapıştırın",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (showManualConnect) {
                    OutlinedTextField(
                        value = manualConnectionJson,
                        onValueChange = { manualConnectionJson = it },
                        label = { Text("Bağlantı Bilgileri (JSON)") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Bağlantı bilgilerini buraya yapıştırın...") },
                        maxLines = 5
                    )
                    Button(
                        onClick = {
                            if (manualConnectionJson.isNotBlank()) {
                                onManualConnect(manualConnectionJson)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = manualConnectionJson.isNotBlank()
                    ) {
                        Text("Bağlan")
                    }
                }
            }
        }
        
        // QR kod tarayıcı
        if (showScanner) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    QRCodeScanner(
                        onQRCodeScanned = { qrData ->
                            showScanner = false
                            onQRCodeScanned(qrData)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

