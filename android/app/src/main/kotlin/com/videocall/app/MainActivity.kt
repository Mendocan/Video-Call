package com.videocall.app

import android.app.Application
import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.videocall.app.ui.VideoCallApp
import com.videocall.app.viewmodel.VideoCallViewModel
import com.videocall.app.data.PreferencesManager
import com.videocall.app.utils.LocaleHelper
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

class MainActivity : ComponentActivity() {

    private val viewModel: VideoCallViewModel by viewModels {
        VideoCallViewModel.factory(application)
    }
    
    private var isInPictureInPictureMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Dil ayarını yükle
        val preferencesManager = PreferencesManager(this)
        val languageCode = preferencesManager.getLanguage() ?: "tr"
        val context = LocaleHelper.setLocale(this, LocaleHelper.getValidLanguageCode(languageCode))
        @Suppress("DEPRECATION")
        resources.updateConfiguration(context.resources.configuration, resources.displayMetrics)
        
        // Widget'dan gelen intent'leri işle
        handleWidgetIntent(intent)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            VideoCallApp(viewModel)
        }
        
        // PiP modu değişikliklerini dinle
        lifecycleScope.launch {
            viewModel.uiState.collect { uiState ->
                // Görüşme aktifse ve PiP modunda değilsek, PiP moduna geçiş yapılabilir
                if (uiState.isConnected && !isInPictureInPictureMode && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Kullanıcı uygulamadan çıktığında otomatik PiP moduna geçiş
                    // (Manuel olarak da tetiklenebilir)
                }
            }
        }
    }
    
    /**
     * Picture-in-Picture moduna geçiş yapar
     */
    override fun enterPictureInPictureMode(params: PictureInPictureParams): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                try {
                    super.enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "PiP moduna geçiş hatası", e)
                    false
                }
            } else {
                false
            }
        } else {
            false
        }
    }
    
    /**
     * Picture-in-Picture moduna geçiş yapar (helper method)
     */
    fun enterPictureInPictureModeHelper() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                try {
                    val params = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9)) // 16:9 aspect ratio
                            .build()
                    } else {
                        @Suppress("DEPRECATION")
                        PictureInPictureParams.Builder()
                            .setAspectRatio(Rational(16, 9))
                            .build()
                    }
                    
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "PiP moduna geçiş hatası", e)
                }
            }
        }
    }
    
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Kullanıcı uygulamadan çıktığında (home tuşuna bastığında)
        // Görüşme aktifse PiP moduna geç
        if (viewModel.uiState.value.isConnected && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            enterPictureInPictureModeHelper()
        }
    }
    
    @Deprecated("Deprecated in Android", ReplaceWith("onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)"))
    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        onPictureInPictureModeChanged(isInPictureInPictureMode, resources.configuration)
    }
    
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        this.isInPictureInPictureMode = isInPictureInPictureMode
        
        if (isInPictureInPictureMode) {
            // PiP moduna geçildi
            android.util.Log.d("MainActivity", "Picture-in-Picture moduna geçildi")
            // Görüşme devam ediyor, video renderer'lar çalışmaya devam edecek
        } else {
            // PiP modundan çıkıldı
            android.util.Log.d("MainActivity", "Picture-in-Picture modundan çıkıldı")
            // Normal ekrana dönüldü
        }
    }
    
    // onBackPressed deprecated - OnBackPressedDispatcher kullanılacak (Compose'da BackHandler ile)
    
    /**
     * Widget'dan gelen intent'leri işler
     */
    private fun handleWidgetIntent(intent: Intent?) {
        when (intent?.action) {
            "com.videocall.app.OPEN_CALL" -> {
                // Call ekranına git
                // Navigation Compose'da bu işlem VideoCallApp içinde yapılacak
            }
            "com.videocall.app.OPEN_CONTACTS" -> {
                // Contacts ekranına git
                // Navigation Compose'da bu işlem VideoCallApp içinde yapılacak
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }
}

