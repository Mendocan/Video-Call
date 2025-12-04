package com.videocall.app.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.videocall.app.ui.theme.Teal

@Composable
fun OTPScreen(
    phoneNumber: String,
    onOTPVerified: (String) -> Unit,
    onBack: () -> Unit,
    onRequestOTP: () -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null,
    testOTP: String? = null, // Development için test OTP
    modifier: Modifier = Modifier
) {
    var otp by remember { mutableStateOf("") }
    var countdown by remember { mutableIntStateOf(0) }
    var otpSent by remember { mutableStateOf(false) }

    // Test OTP varsa otomatik doldur (development)
    LaunchedEffect(testOTP) {
        if (testOTP != null && !otpSent) {
            otp = testOTP
            otpSent = true
        }
    }

    // Countdown timer
    LaunchedEffect(otpSent) {
        if (otpSent) {
            countdown = 60
            while (countdown > 0) {
                kotlinx.coroutines.delay(1000)
                countdown--
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Geri dön butonu
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Geri"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Başlık
        Text(
            text = "Doğrulama Kodu",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Telefon numaranıza gönderilen 6 haneli doğrulama kodunu giriniz",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Telefon numarası gösterimi
        Text(
            text = phoneNumber,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = Teal,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // OTP Kartı
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // OTP Input
                OutlinedTextField(
                    value = otp,
                    onValueChange = { newValue ->
                        // Sadece rakam ve maksimum 6 karakter
                        if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                            otp = newValue
                        }
                    },
                    label = { Text("Doğrulama Kodu") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage != null,
                    supportingText = if (errorMessage != null) {
                        { Text(errorMessage!!) }
                    } else {
                        { Text("6 haneli kod") }
                    },
                    enabled = !isLoading
                )

                // Test OTP gösterimi (development)
                if (testOTP != null) {
                    Text(
                        text = "Test OTP: $testOTP",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Hata mesajı
                if (errorMessage != null && !otp.isEmpty()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // OTP Tekrar Gönder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(
                        onClick = {
                            if (countdown == 0) {
                                onRequestOTP()
                                otpSent = true
                                otp = ""
                            }
                        },
                        enabled = countdown == 0 && !isLoading
                    ) {
                        if (countdown > 0) {
                            Text("Kodu tekrar gönder (${countdown}s)")
                        } else {
                            Text("Kodu tekrar gönder")
                        }
                    }
                }

                // Doğrula Butonu
                Button(
                    onClick = {
                        if (otp.length == 6) {
                            onOTPVerified(otp)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Teal),
                    enabled = otp.length == 6 && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(
                            text = "Doğrula",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

