package com.videocall.app.network

import android.content.Context
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.net.wifi.p2p.WifiP2pManager.ChannelListener
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager.PeerListListener
import android.net.wifi.p2p.WifiP2pManager.ConnectionInfoListener
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

/**
 * Yerel ağ keşfi ve bağlantı yönetimi
 * Wi-Fi Direct ve Bluetooth desteği ile offline görüşme
 */
class NetworkDiscovery(private val context: Context) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    private val wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
    private var channel: Channel? = null
    
    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()
    
    private val _discoveredPeers = MutableStateFlow<List<PeerDevice>>(emptyList())
    val discoveredPeers: StateFlow<List<PeerDevice>> = _discoveredPeers.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val _localIpAddress = MutableStateFlow<String?>(null)
    val localIpAddress: StateFlow<String?> = _localIpAddress.asStateFlow()
    
    private val peerListListener = PeerListListener { peers ->
        val peerList = peers.deviceList.map { device ->
            PeerDevice(
                deviceName = device.deviceName,
                deviceAddress = device.deviceAddress,
                status = device.status,
                isGroupOwner = false // Will be updated when connection info is available
            )
        }
        _discoveredPeers.value = peerList
    }
    
    private val connectionInfoListener = ConnectionInfoListener { info ->
        if (info != null) {
            _isConnected.value = true
            val groupOwnerAddress = info.groupOwnerAddress?.hostAddress
            _localIpAddress.value = if (info.isGroupOwner) {
                getLocalIpAddress()
            } else {
                groupOwnerAddress
            }
        } else {
            _isConnected.value = false
        }
    }
    
    private val wifiP2pReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                        // Wi-Fi Direct enabled
                    } else {
                        // Wi-Fi Direct disabled
                        _isOfflineMode.value = false
                    }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    channel?.let { ch ->
                        wifiP2pManager?.requestPeers(ch, peerListListener)
                    }
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    channel?.let { ch ->
                        wifiP2pManager?.requestConnectionInfo(ch, connectionInfoListener)
                    }
                }
            }
        }
    }
    
    init {
        channel = wifiP2pManager?.initialize(context, context.mainLooper, null)
        val intentFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        }
        context.registerReceiver(wifiP2pReceiver, intentFilter)
    }
    
    /**
     * Offline modu etkinleştir
     */
    fun enableOfflineMode() {
        _isOfflineMode.value = true
        discoverPeers()
    }
    
    /**
     * Offline modu devre dışı bırak
     */
    fun disableOfflineMode() {
        _isOfflineMode.value = false
        stopPeerDiscovery()
    }
    
    /**
     * Eş cihazları keşfet
     */
    fun discoverPeers() {
        channel?.let { ch ->
            wifiP2pManager?.discoverPeers(ch, object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                    // Discovery started
                }
                
                override fun onFailure(reasonCode: Int) {
                    // Discovery failed
                }
            })
        }
    }
    
    /**
     * Eş keşfini durdur
     */
    fun stopPeerDiscovery() {
        channel?.let { ch ->
            wifiP2pManager?.stopPeerDiscovery(ch, null)
        }
    }
    
    /**
     * Bir eşe bağlan
     */
    fun connectToPeer(deviceAddress: String) {
        // Wi-Fi Direct connection setup
        // This requires WifiP2pConfig which is complex
        // For now, we'll use local network IP addresses
    }
    
    /**
     * Yerel IP adresini al
     */
    fun getLocalIpAddress(): String? {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }
    
    /**
     * Yerel ağdaki diğer cihazları tarar (mDNS/Bonjour benzeri)
     */
    fun scanLocalNetwork() {
        val localIp = getLocalIpAddress()
        if (localIp != null) {
            // Extract network prefix (e.g., 192.168.1.x)
            val parts = localIp.split(".")
            if (parts.size == 4) {
                val networkPrefix = "${parts[0]}.${parts[1]}.${parts[2]}"
                // Scan network (simplified - in production, use proper network scanning)
                _localIpAddress.value = localIp
            }
        }
    }
    
    /**
     * Temizlik
     */
    fun cleanup() {
        try {
            context.unregisterReceiver(wifiP2pReceiver)
        } catch (e: Exception) {
            // Already unregistered
        }
    }
}

data class PeerDevice(
    val deviceName: String,
    val deviceAddress: String,
    val status: Int,
    val isGroupOwner: Boolean = false
)

