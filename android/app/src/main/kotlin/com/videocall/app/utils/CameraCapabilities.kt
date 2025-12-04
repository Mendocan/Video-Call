package com.videocall.app.utils

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.graphics.ImageFormat

/**
 * Kamera yeteneklerini kontrol etmek için utility fonksiyonlar
 */
object CameraCapabilities {
    /**
     * Telefonun 4K (UHD) video çözünürlüğünü destekleyip desteklemediğini kontrol et
     */
    fun supports4K(context: Context): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIds = cameraManager.cameraIdList
            
            cameraIds.any { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                
                // 4K çözünürlüğü kontrol et (3840x2160)
                val sizes = map?.getOutputSizes(ImageFormat.YUV_420_888)
                sizes?.any { size ->
                    size.width >= 3840 && size.height >= 2160
                } == true
            }
        } catch (e: Exception) {
            android.util.Log.e("CameraCapabilities", "4K desteği kontrol edilemedi", e)
            false
        }
    }
    
    /**
     * Telefonun 1080p@60fps video çözünürlüğünü destekleyip desteklemediğini kontrol et
     */
    fun supports1080p60fps(context: Context): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameraIds = cameraManager.cameraIdList
            
            cameraIds.any { cameraId ->
                val characteristics = cameraManager.getCameraCharacteristics(cameraId)
                val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                
                // 1080p çözünürlüğü kontrol et (1920x1080)
                val sizes = map?.getOutputSizes(ImageFormat.YUV_420_888)
                val supports1080p = sizes?.any { size ->
                    size.width >= 1920 && size.height >= 1080
                } == true
                
                // 60fps desteği kontrol et
                val fpsRanges = characteristics.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                val supports60fps = fpsRanges?.any { range ->
                    range.upper >= 60
                } == true
                
                supports1080p && supports60fps
            }
        } catch (e: Exception) {
            android.util.Log.e("CameraCapabilities", "1080p@60fps desteği kontrol edilemedi", e)
            false
        }
    }
}

