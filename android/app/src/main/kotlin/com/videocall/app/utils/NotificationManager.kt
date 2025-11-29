package com.videocall.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager as AndroidNotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.videocall.app.MainActivity

class NotificationManager(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as AndroidNotificationManager
    
    companion object {
        private const val CHANNEL_ID_INCOMING_CALL = "incoming_call_channel"
        private const val CHANNEL_ID_MESSAGES = "messages_channel"
        private const val CHANNEL_ID_GENERAL = "general_channel"
        
        private const val NOTIFICATION_ID_INCOMING_CALL = 1001
        private const val NOTIFICATION_ID_MESSAGES = 1002
        private const val NOTIFICATION_ID_GENERAL = 1003
    }
    
    init {
        createNotificationChannels()
    }
    
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Gelen arama bildirimi kanalı
            val incomingCallChannel = NotificationChannel(
                CHANNEL_ID_INCOMING_CALL,
                "Gelen Aramalar",
                AndroidNotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Gelen video görüşme bildirimleri"
                enableVibration(true)
                enableLights(true)
            }
            
            // Mesaj bildirimi kanalı
            val messagesChannel = NotificationChannel(
                CHANNEL_ID_MESSAGES,
                "Mesajlar",
                AndroidNotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Gelen mesaj bildirimleri"
            }
            
            // Genel bildirim kanalı
            val generalChannel = NotificationChannel(
                CHANNEL_ID_GENERAL,
                "Genel Bildirimler",
                AndroidNotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Genel uygulama bildirimleri"
            }
            
            notificationManager.createNotificationChannel(incomingCallChannel)
            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }
    
    /**
     * Gelen arama bildirimi gösterir
     */
    fun showIncomingCallNotification(
        callerName: String,
        callerPhoneNumber: String,
        roomCode: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("action", "incoming_call")
            putExtra("room_code", roomCode)
            putExtra("caller_phone", callerPhoneNumber)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_INCOMING_CALL)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Gelen Arama")
            .setContentText("$callerName aradı")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_INCOMING_CALL, notification)
    }
    
    /**
     * Mesaj bildirimi gösterir
     */
    fun showMessageNotification(
        senderName: String,
        message: String,
        conversationId: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("action", "open_message")
            conversationId?.let { putExtra("conversation_id", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_MESSAGES)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(senderName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_MESSAGES, notification)
    }
    
    /**
     * Genel bildirim gösterir
     */
    fun showGeneralNotification(
        title: String,
        message: String,
        action: String? = null
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            action?.let { putExtra("action", it) }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID_GENERAL, notification)
    }
    
    /**
     * Gelen arama bildirimini iptal eder
     */
    fun cancelIncomingCallNotification() {
        notificationManager.cancel(NOTIFICATION_ID_INCOMING_CALL)
    }
    
    /**
     * Tüm bildirimleri iptal eder
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }
}

