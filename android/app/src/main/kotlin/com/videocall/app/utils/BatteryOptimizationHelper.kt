/*
 * Copyright (c) 2025 DirectCall Project
 * 
 * Licensed under the MIT License
 * See LICENSE file for details
 */

package com.videocall.app.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Battery Optimization muafiyeti için helper
 * Kullanıcıdan uygulamanın battery optimization'dan muaf tutulmasını ister
 */
object BatteryOptimizationHelper {
    
    /**
     * Battery optimization muafiyeti kontrolü
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true // API 23 öncesi için gerekli değil
        }
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
    
    /**
     * Battery optimization muafiyeti isteği dialog'u aç
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return // API 23 öncesi için gerekli değil
        }
        
        if (isIgnoringBatteryOptimizations(context)) {
            return // Zaten muaf
        }
        
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: Settings sayfasına yönlendir
            try {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                context.startActivity(intent)
            } catch (e2: Exception) {
                android.util.Log.e("BatteryOptimizationHelper", "Battery optimization ayarları açılamadı", e2)
            }
        }
    }
}

