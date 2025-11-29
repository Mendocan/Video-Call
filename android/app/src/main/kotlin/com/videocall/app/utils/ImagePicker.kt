package com.videocall.app.utils

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImagePicker {
    /**
     * Galeriden veya kameradan fotoğraf seçmek için launcher oluşturur
     */
    @Composable
    fun rememberImagePickerLauncher(
        onImageSelected: (Uri?) -> Unit
    ) = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        onImageSelected(uri)
    }
    
    /**
     * Kameradan fotoğraf çekmek için launcher oluşturur
     */
    @Composable
    fun rememberCameraLauncher(
        onImageCaptured: (Uri?) -> Unit
    ) = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        // TakePicture contract'ı success boolean döner, URI'yi manuel oluşturmamız gerekir
        // Bu durumda, kullanıcıya önce bir temp file URI'si vermemiz gerekir
        // Şimdilik basit bir implementasyon yapıyoruz
        onImageCaptured(null) // TODO: Temp file URI oluştur
    }
    
    /**
     * URI'yi dosya sistemine kopyalar ve yeni URI döner
     */
    fun copyUriToAppStorage(context: Context, uri: Uri, fileName: String): Uri? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            inputStream?.use { input ->
                val appDir = File(context.filesDir, "contact_photos")
                if (!appDir.exists()) {
                    appDir.mkdirs()
                }
                val file = File(appDir, fileName)
                val outputStream = FileOutputStream(file)
                outputStream.use { output ->
                    input.copyTo(output)
                }
                Uri.fromFile(file)
            }
        } catch (e: Exception) {
            android.util.Log.e("ImagePicker", "Error copying image: ${e.message}")
            null
        }
    }
    
    /**
     * İlk harfi kullanarak basit bir avatar oluşturur
     */
    fun generateAvatarInitials(name: String): String {
        return if (name.isNotBlank()) {
            name.trim().split(" ").mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .take(2)
                .joinToString("")
        } else {
            "?"
        }
    }
}

