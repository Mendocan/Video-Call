package com.videocall.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.videocall.app.MainActivity
import com.videocall.app.R
import com.videocall.app.data.PreferencesManager
import com.videocall.app.data.SubscriptionManager

/**
 * Video Call uygulaması için widget provider
 * Ana ekranda hızlı erişim ve görüşme durumu gösterimi sağlar
 */
class VideoCallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        // İlk widget eklendiğinde
        android.util.Log.d("VideoCallWidget", "Widget etkinleştirildi")
    }

    override fun onDisabled(context: Context) {
        // Son widget kaldırıldığında
        android.util.Log.d("VideoCallWidget", "Widget devre dışı bırakıldı")
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        // Widget güncelleme istekleri
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE ||
            intent.action == ACTION_UPDATE_WIDGET) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, VideoCallWidgetProvider::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        private const val ACTION_UPDATE_WIDGET = "com.videocall.app.UPDATE_WIDGET"
        private const val ACTION_OPEN_CALL = "com.videocall.app.OPEN_CALL"
        private const val ACTION_OPEN_CONTACTS = "com.videocall.app.OPEN_CONTACTS"

        /**
         * Widget'ı günceller
         */
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val preferencesManager = PreferencesManager(context)
            val subscriptionManager = SubscriptionManager(context)
            
            // Widget layout'unu yükle
            val views = RemoteViews(context.packageName, R.layout.widget_layout)
            
            // Durum bilgisi
            val hasActiveSubscription = subscriptionManager.hasActiveSubscription()
            val statusText = if (hasActiveSubscription) {
                context.getString(R.string.widget_active)
            } else {
                context.getString(R.string.widget_subscription_required)
            }
            views.setTextViewText(R.id.widget_status, statusText)
            
            // Buton metinleri
            views.setTextViewText(R.id.widget_call_button, context.getString(R.string.widget_call_button))
            views.setTextViewText(R.id.widget_contacts_button, context.getString(R.string.widget_contacts_button))
            
            // Ana uygulamayı açmak için intent
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val mainPendingIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Widget'a tıklanınca ana uygulamayı aç
            views.setOnClickPendingIntent(R.id.widget_title, mainPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_logo, mainPendingIntent)
            
            // "Ara" butonu - Call ekranına git
            val callIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = ACTION_OPEN_CALL
            }
            val callPendingIntent = PendingIntent.getActivity(
                context,
                1,
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_call_button, callPendingIntent)
            
            // "Kişiler" butonu - Contacts ekranına git
            val contactsIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = ACTION_OPEN_CONTACTS
            }
            val contactsPendingIntent = PendingIntent.getActivity(
                context,
                2,
                contactsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_contacts_button, contactsPendingIntent)
            
            // Widget'ı güncelle
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        /**
         * Tüm widget'ları günceller
         */
        fun updateAllWidgets(context: Context) {
            val intent = Intent(context, VideoCallWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            }
            context.sendBroadcast(intent)
        }
    }
}

