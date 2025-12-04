/*
 * Copyright (c) 2025 DirectCall Project
 * 
 * Licensed under the MIT License
 * See LICENSE file for details
 */

package com.videocall.app.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.videocall.app.MainActivity

/**
 * Foreground Service - WebSocket bağlantısını canlı tutmak için
 * Android Doze Mode ve App Standby'dan korunmak için gerekli
 */
class ConnectionService : Service() {
    
    private var wakeLock: PowerManager.WakeLock? = null
    private val CHANNEL_ID = "connection_service_channel"
    private val NOTIFICATION_ID = 1
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            startForeground(NOTIFICATION_ID, createNotification())
            isRunning = true
            android.util.Log.d("ConnectionService", "onStartCommand: Servis başlatıldı")
        } catch (e: Exception) {
            android.util.Log.e("ConnectionService", "onStartCommand hatası", e)
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY // Servis öldürülse bile yeniden başlat
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        releaseWakeLock()
        android.util.Log.d("ConnectionService", "onDestroy: Servis durduruldu")
    }
    
    /**
     * Notification channel oluştur (Android 8.0+ için zorunlu)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bağlantı Servisi",
                NotificationManager.IMPORTANCE_LOW // Düşük öncelik - kullanıcıyı rahatsız etmez
            ).apply {
                description = "WebSocket bağlantısını canlı tutmak için"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Foreground service için notification oluştur
     */
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Video Call")
            .setContentText("Bağlantı aktif")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Kullanıcı kapatamaz
            .setPriority(NotificationCompat.PRIORITY_LOW) // Düşük öncelik - ANR önlemek için
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSilent(true) // Sessiz - kullanıcıyı rahatsız etmez
            .setShowWhen(false) // Zaman gösterme
            .build()
    }
    
    /**
     * Wake Lock al - Doze mode'da network işlemlerini sürdürmek için
     */
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "VideoCall::ConnectionWakeLock"
        ).apply {
            acquire(10 * 60 * 60 * 1000L /*10 hours*/) // 10 saat - yeterince uzun
        }
    }
    
    /**
     * Wake Lock serbest bırak
     */
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        wakeLock = null
    }
    
    companion object {
        @Volatile
        private var isRunning: Boolean = false
        
        /**
         * Servisi başlat (sadece çalışmıyorsa)
         */
        fun start(context: Context) {
            if (isRunning) {
                android.util.Log.d("ConnectionService", "Servis zaten çalışıyor, tekrar başlatılmıyor")
                return
            }
            
            val intent = Intent(context, ConnectionService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                isRunning = true
                android.util.Log.d("ConnectionService", "Servis başlatıldı")
            } catch (e: Exception) {
                android.util.Log.e("ConnectionService", "Servis başlatılamadı", e)
                isRunning = false
            }
        }
        
        /**
         * Servisi durdur
         */
        fun stop(context: Context) {
            if (!isRunning) {
                android.util.Log.d("ConnectionService", "Servis zaten durdurulmuş")
                return
            }
            
            val intent = Intent(context, ConnectionService::class.java)
            try {
                context.stopService(intent)
                isRunning = false
                android.util.Log.d("ConnectionService", "Servis durduruldu")
            } catch (e: Exception) {
                android.util.Log.e("ConnectionService", "Servis durdurulamadı", e)
            }
        }
    }
}

