package com.videocall.app.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import java.util.Locale

/**
 * Dil yönetimi için yardımcı sınıf
 */
object LocaleHelper {
    private const val LANGUAGE_TURKISH = "tr"
    private const val LANGUAGE_ENGLISH = "en"
    
    /**
     * Context'e göre locale ayarlar
     */
    fun setLocale(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }
    
    /**
     * Mevcut dil kodunu döndürür
     */
    fun getCurrentLanguage(context: Context): String {
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale
        }
        return locale.language
    }
    
    /**
     * Dil kodunu doğrular ve varsayılan dil döndürür
     */
    fun getValidLanguageCode(languageCode: String?): String {
        return when (languageCode) {
            LANGUAGE_TURKISH -> LANGUAGE_TURKISH
            LANGUAGE_ENGLISH -> LANGUAGE_ENGLISH
            else -> LANGUAGE_TURKISH // Varsayılan Türkçe
        }
    }
    
    /**
     * Dil kodundan dil adını döndürür
     */
    fun getLanguageName(languageCode: String): String {
        return when (languageCode) {
            LANGUAGE_TURKISH -> "Türkçe"
            LANGUAGE_ENGLISH -> "English"
            else -> "Türkçe"
        }
    }
    
    /**
     * Desteklenen dilleri döndürür
     */
    fun getSupportedLanguages(): List<Pair<String, String>> {
        return listOf(
            Pair(LANGUAGE_TURKISH, "Türkçe"),
            Pair(LANGUAGE_ENGLISH, "English")
        )
    }
}

