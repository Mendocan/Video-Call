package com.videocall.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sesli komut yönetimi ve işleme
 */
class VoiceCommandManager(private val context: Context) {
    private val speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    
    private val _lastCommand = MutableStateFlow<String?>(null)
    val lastCommand: StateFlow<String?> = _lastCommand.asStateFlow()
    
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled.asStateFlow()
    
    private var commandListener: ((VoiceCommand) -> Unit)? = null
    
    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
        }
        
        override fun onBeginningOfSpeech() {
            // Konuşma başladı
        }
        
        override fun onRmsChanged(rmsdB: Float) {
            // Ses seviyesi değişti
        }
        
        override fun onBufferReceived(buffer: ByteArray?) {
            // Ses buffer'ı alındı
        }
        
        override fun onEndOfSpeech() {
            _isListening.value = false
        }
        
        override fun onError(error: Int) {
            _isListening.value = false
            when (error) {
                SpeechRecognizer.ERROR_AUDIO -> {
                    // Ses kaydı hatası
                }
                SpeechRecognizer.ERROR_CLIENT -> {
                    // İstemci hatası
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                    // İzin hatası
                }
                SpeechRecognizer.ERROR_NETWORK -> {
                    // Ağ hatası
                }
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    // Ağ zaman aşımı
                }
                SpeechRecognizer.ERROR_NO_MATCH -> {
                    // Eşleşme bulunamadı
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    // Tanıyıcı meşgul
                }
                SpeechRecognizer.ERROR_SERVER -> {
                    // Sunucu hatası
                }
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    // Konuşma zaman aşımı
                }
            }
        }
        
        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (matches != null && matches.isNotEmpty()) {
                val recognizedText = matches[0].lowercase()
                _lastCommand.value = recognizedText
                
                // Komutu işle
                val command = parseCommand(recognizedText)
                if (command != null) {
                    commandListener?.invoke(command)
                }
            }
            _isListening.value = false
        }
        
        override fun onPartialResults(partialResults: Bundle?) {
            // Kısmi sonuçlar
        }
        
        override fun onEvent(eventType: Int, params: Bundle?) {
            // Olay işleme
        }
    }
    
    init {
        speechRecognizer.setRecognitionListener(recognitionListener)
    }
    
    /**
     * Sesli komutları etkinleştir
     */
    fun enable() {
        _isEnabled.value = true
    }
    
    /**
     * Sesli komutları devre dışı bırak
     */
    fun disable() {
        _isEnabled.value = false
        stopListening()
    }
    
    /**
     * Dinlemeyi başlat
     */
    fun startListening() {
        if (!_isEnabled.value) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR") // Türkçe
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        speechRecognizer.startListening(intent)
    }
    
    /**
     * Dinlemeyi durdur
     */
    fun stopListening() {
        if (_isListening.value) {
            speechRecognizer.stopListening()
            _isListening.value = false
        }
    }
    
    /**
     * Komut dinleyicisini ayarla
     */
    fun setCommandListener(listener: (VoiceCommand) -> Unit) {
        commandListener = listener
    }
    
    /**
     * Tanınan metni komuta dönüştür
     */
    private fun parseCommand(text: String): VoiceCommand? {
        val lowerText = text.lowercase()
        
        return when {
            // Mikrofon komutları
            lowerText.contains("mikrofon") && (lowerText.contains("kapat") || lowerText.contains("kapatı") || lowerText.contains("sessiz")) -> {
                VoiceCommand.TOGGLE_MICROPHONE
            }
            lowerText.contains("mikrofon") && (lowerText.contains("aç") || lowerText.contains("açı")) -> {
                VoiceCommand.TOGGLE_MICROPHONE
            }
            
            // Kamera komutları
            lowerText.contains("kamera") && (lowerText.contains("kapat") || lowerText.contains("kapatı")) -> {
                VoiceCommand.TOGGLE_CAMERA
            }
            lowerText.contains("kamera") && (lowerText.contains("aç") || lowerText.contains("açı")) -> {
                VoiceCommand.TOGGLE_CAMERA
            }
            
            // Görüşme sonlandırma
            lowerText.contains("görüşme") && (lowerText.contains("sonlandır") || lowerText.contains("bitir") || lowerText.contains("kapat")) -> {
                VoiceCommand.HANG_UP
            }
            lowerText.contains("kapat") && lowerText.contains("görüşme") -> {
                VoiceCommand.HANG_UP
            }
            
            // Ses yönlendirme
            lowerText.contains("hoparlör") || lowerText.contains("ses") && lowerText.contains("aç") -> {
                VoiceCommand.SWITCH_TO_SPEAKER
            }
            lowerText.contains("bluetooth") || lowerText.contains("kulaklık") -> {
                VoiceCommand.SWITCH_TO_BLUETOOTH
            }
            
            // Chat
            lowerText.contains("chat") && (lowerText.contains("aç") || lowerText.contains("göster")) -> {
                VoiceCommand.OPEN_CHAT
            }
            lowerText.contains("mesaj") && (lowerText.contains("gönder") || lowerText.contains("yaz")) -> {
                VoiceCommand.OPEN_CHAT
            }
            
            // Ekran paylaşımı
            lowerText.contains("ekran") && (lowerText.contains("paylaş") || lowerText.contains("göster")) -> {
                VoiceCommand.TOGGLE_SCREEN_SHARING
            }
            
            // Sesli arama modu
            lowerText.contains("sesli") && lowerText.contains("arama") -> {
                VoiceCommand.TOGGLE_AUDIO_ONLY
            }
            lowerText.contains("video") && lowerText.contains("kapat") -> {
                VoiceCommand.TOGGLE_AUDIO_ONLY
            }
            
            else -> null
        }
    }
    
    /**
     * Temizlik
     */
    fun cleanup() {
        stopListening()
        speechRecognizer.destroy()
    }
}

/**
 * Sesli komut türleri
 */
enum class VoiceCommand {
    TOGGLE_MICROPHONE,      // Mikrofonu aç/kapat
    TOGGLE_CAMERA,          // Kamerayı aç/kapat
    HANG_UP,                // Görüşmeyi sonlandır
    SWITCH_TO_SPEAKER,      // Hoparlöre geç
    SWITCH_TO_BLUETOOTH,    // Bluetooth'a geç
    OPEN_CHAT,              // Chat'i aç
    TOGGLE_SCREEN_SHARING,  // Ekran paylaşımını aç/kapat
    TOGGLE_AUDIO_ONLY       // Sesli arama moduna geç
}

