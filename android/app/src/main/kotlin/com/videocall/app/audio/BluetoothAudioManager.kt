@file:Suppress("DEPRECATION")

package com.videocall.app.audio

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.AudioDeviceInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bluetooth kulaklık yönetimi ve ses yönlendirme
 */
class BluetoothAudioManager(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    
    private val _isBluetoothConnected = MutableStateFlow(false)
    val isBluetoothConnected: StateFlow<Boolean> = _isBluetoothConnected.asStateFlow()
    
    private val _bluetoothDeviceName = MutableStateFlow<String?>(null)
    val bluetoothDeviceName: StateFlow<String?> = _bluetoothDeviceName.asStateFlow()
    
    private val _audioRoute = MutableStateFlow<AudioRoute>(AudioRoute.SPEAKER)
    val audioRoute: StateFlow<AudioRoute> = _audioRoute.asStateFlow()
    
    private var bluetoothProfile: BluetoothProfile? = null
    private val bluetoothProfileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothProfile = proxy
                checkBluetoothConnection()
            }
        }
        
        override fun onServiceDisconnected(profile: Int) {
            if (profile == BluetoothProfile.HEADSET) {
                bluetoothProfile = null
                _isBluetoothConnected.value = false
                _bluetoothDeviceName.value = null
            }
        }
    }
    
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1)
                    when (state) {
                        BluetoothHeadset.STATE_CONNECTED -> {
                            checkBluetoothConnection()
                        }
                        BluetoothHeadset.STATE_DISCONNECTED -> {
                            _isBluetoothConnected.value = false
                            _bluetoothDeviceName.value = null
                            if (_audioRoute.value == AudioRoute.BLUETOOTH) {
                                _audioRoute.value = AudioRoute.SPEAKER
                            }
                        }
                    }
                }
                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> {
                    // Ses çıkışı değişti (örneğin kulaklık çıkarıldı)
                    updateAudioRoute()
                }
            }
        }
    }
    
    init {
        registerBluetoothProfile()
        registerReceivers()
        updateAudioRoute()
    }
    
    /**
     * Bluetooth profile'ı kaydet
     */
    private fun registerBluetoothProfile() {
        bluetoothAdapter?.getProfileProxy(
            context,
            bluetoothProfileListener,
            BluetoothProfile.HEADSET
        )
    }
    
    /**
     * Broadcast receiver'ları kaydet
     */
    private fun registerReceivers() {
        val filter = IntentFilter().apply {
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            addAction(AudioManager.ACTION_HEADSET_PLUG)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }
    
    /**
     * Bluetooth bağlantısını kontrol et
     */
    private fun checkBluetoothConnection() {
        val headset = bluetoothProfile as? BluetoothHeadset
        val connectedDevices = headset?.connectedDevices ?: emptyList()
        
        if (connectedDevices.isNotEmpty()) {
            val device = connectedDevices[0]
            _isBluetoothConnected.value = true
            _bluetoothDeviceName.value = device.name ?: "Bluetooth Cihazı"
            
            // Otomatik olarak Bluetooth'a yönlendir
            if (_audioRoute.value != AudioRoute.BLUETOOTH) {
                setAudioRoute(AudioRoute.BLUETOOTH)
            }
        } else {
            _isBluetoothConnected.value = false
            _bluetoothDeviceName.value = null
        }
    }
    
    /**
     * Mevcut ses yönlendirmesini güncelle
     */
    private fun updateAudioRoute() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        var currentRoute = AudioRoute.SPEAKER
        
        for (device in devices) {
            when (device.type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> {
                    if (_isBluetoothConnected.value) {
                        currentRoute = AudioRoute.BLUETOOTH
                    }
                }
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                    currentRoute = AudioRoute.HEADSET
                }
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> {
                    currentRoute = AudioRoute.EARPIECE
                }
            }
        }
        
        _audioRoute.value = currentRoute
    }
    
    /**
     * Ses yönlendirmesini ayarla
     */
    fun setAudioRoute(route: AudioRoute) {
        when (route) {
            AudioRoute.BLUETOOTH -> {
                if (_isBluetoothConnected.value) {
                    audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                    @Suppress("DEPRECATION")
                    audioManager.isBluetoothScoOn = true
                    @Suppress("DEPRECATION")
                    audioManager.startBluetoothSco()
                    _audioRoute.value = AudioRoute.BLUETOOTH
                }
            }
            AudioRoute.SPEAKER -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = true
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
                _audioRoute.value = AudioRoute.SPEAKER
            }
            AudioRoute.EARPIECE -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
                _audioRoute.value = AudioRoute.EARPIECE
            }
            AudioRoute.HEADSET -> {
                audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
                audioManager.isSpeakerphoneOn = false
                @Suppress("DEPRECATION")
                audioManager.isBluetoothScoOn = false
                @Suppress("DEPRECATION")
                audioManager.stopBluetoothSco()
                _audioRoute.value = AudioRoute.HEADSET
            }
        }
    }
    
    /**
     * Otomatik ses yönlendirmesini etkinleştir
     */
    fun enableAutoRouting() {
        // Bluetooth bağlandığında otomatik yönlendirme zaten yapılıyor
        updateAudioRoute()
    }
    
    /**
     * Bluetooth SCO bağlantısını başlat
     */
    fun startBluetoothSco() {
        if (_isBluetoothConnected.value) {
            @Suppress("DEPRECATION")
            audioManager.startBluetoothSco()
        }
    }
    
    /**
     * Bluetooth SCO bağlantısını durdur
     */
    fun stopBluetoothSco() {
        @Suppress("DEPRECATION")
        audioManager.stopBluetoothSco()
    }
    
    /**
     * Ses kalitesini optimize et
     */
    fun optimizeAudioQuality() {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        
        // Ses parametrelerini optimize et
        if (_audioRoute.value == AudioRoute.BLUETOOTH) {
            // Bluetooth için özel ayarlar
            @Suppress("DEPRECATION")
            audioManager.isBluetoothScoOn = true
        }
        
        // Gürültü azaltma (eğer destekleniyorsa)
        // audioManager.setParameters("noise_suppression=on")
    }
    
    /**
     * Temizlik
     */
    fun cleanup() {
        try {
            context.unregisterReceiver(bluetoothReceiver)
            bluetoothAdapter?.closeProfileProxy(BluetoothProfile.HEADSET, bluetoothProfile)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
}

enum class AudioRoute {
    SPEAKER,      // Hoparlör
    EARPIECE,     // Kulaklık (telefon)
    HEADSET,      // Kablolu kulaklık
    BLUETOOTH     // Bluetooth kulaklık
}

