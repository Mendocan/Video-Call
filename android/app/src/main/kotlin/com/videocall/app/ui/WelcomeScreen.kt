package com.videocall.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.videocall.app.R
import com.videocall.app.ui.theme.Teal

@Composable
fun WelcomeScreen(
    onLoginSuccess: (String, Boolean) -> Unit,
    onNavigateToLegal: () -> Unit,
    savedPhoneNumber: String? = null,
    modifier: Modifier = Modifier
) {
    var phoneNumber by remember { mutableStateOf(savedPhoneNumber ?: "") }
    var termsAccepted by remember { mutableStateOf(false) }
    var rememberMe by remember { mutableStateOf(true) }
    var phoneError by remember { mutableStateOf<String?>(null) }

    fun validatePhoneNumber(phone: String): Boolean {
        // Türkiye telefon numarası formatı: +90XXXXXXXXXX veya 0XXXXXXXXXX
        val cleaned = phone.replace(Regex("[^0-9+]"), "")
        return when {
            cleaned.isEmpty() -> {
                phoneError = "Telefon numarası gereklidir"
                false
            }
            cleaned.startsWith("+90") && cleaned.length == 13 -> {
                phoneError = null
                true
            }
            cleaned.startsWith("0") && cleaned.length == 11 -> {
                phoneError = null
                true
            }
            cleaned.startsWith("90") && cleaned.length == 12 -> {
                phoneError = null
                true
            }
            else -> {
                phoneError = "Geçerli bir telefon numarası giriniz (örn: +905551234567 veya 05551234567)"
                false
            }
        }
    }

    fun handleLogin(rememberMe: Boolean) {
        if (validatePhoneNumber(phoneNumber) && termsAccepted) {
            onLoginSuccess(phoneNumber, rememberMe)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Video Call Logo",
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Hoşgeldiniz mesajı
        Text(
            text = "Hoş Geldiniz",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Uygulamaya erişmek için lütfen giriş yapın",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Giriş Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { 
                        phoneNumber = it
                        phoneError = null
                    },
                    label = { Text("Telefon Numarası") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = phoneError != null,
                    supportingText = if (phoneError != null) {
                        { Text(phoneError!!) }
                    } else null
                )

                // Kullanıcı Sözleşmesi checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Checkbox(
                        checked = termsAccepted,
                        onCheckedChange = { termsAccepted = it }
                    )
                    Text(
                        text = "Kullanıcı sözleşmesini ve gizlilik politikasını kabul ediyorum",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Beni Hatırla checkbox
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = { rememberMe = it }
                    )
                    Text(
                        text = "Beni Hatırla",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Sözleşmeyi Görüntüle linki
                TextButton(
                    onClick = onNavigateToLegal,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Kullanıcı Sözleşmesi ve Gizlilik Politikası",
                        style = MaterialTheme.typography.bodySmall,
                        color = Teal
                    )
                }

                // Giriş Butonu
                Button(
                    onClick = { handleLogin(rememberMe) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    enabled = phoneNumber.isNotBlank() && termsAccepted
                ) {
                    Text(
                        text = "Giriş Yap",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

