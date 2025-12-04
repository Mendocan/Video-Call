/*
 * Copyright (c) 2025 DirectCall Project
 * 
 * Licensed under the MIT License
 * See LICENSE file for details
 */

package com.videocall.app.directcall.media

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.concurrent.atomic.AtomicInteger

/**
 * DirectCall Video Renderer
 * 
 * Video frame'lerini render eder.
 * Optimize YUV420 to RGB conversion ile gerçek implementasyon.
 * WebRTC'ye bağımlı değil, kendi implementasyonumuz.
 */
class DirectCallVideoRenderer {
    private var surfaceView: SurfaceView? = null
    private var surfaceHolder: SurfaceHolder? = null
    
    // Frame boyutları
    private var frameWidth = 640
    private var frameHeight = 480
    
    // Render kontrolü (frame rate limiting)
    private var lastRenderTime = 0L
    private val minRenderInterval = 33_333_333L // 30 fps için nanoseconds
    
    // Bitmap cache (tekrar kullanım için)
    private var cachedBitmap: Bitmap? = null
    
    /**
     * Renderer'ı ekle
     */
    fun attachRenderer(surfaceView: SurfaceView) {
        this.surfaceView = surfaceView
        this.surfaceHolder = surfaceView.holder
    }
    
    /**
     * Frame boyutlarını ayarla
     */
    fun setFrameSize(width: Int, height: Int) {
        frameWidth = width
        frameHeight = height
        // Cache'i temizle
        cachedBitmap?.recycle()
        cachedBitmap = null
    }
    
    /**
     * Frame'i render et
     * @param frame YUV420 formatında frame
     */
    fun renderFrame(frame: ByteArray) {
        // Frame rate kontrolü
        val currentTime = System.nanoTime()
        if (currentTime - lastRenderTime < minRenderInterval) {
            return // Çok sık render, atla
        }
        lastRenderTime = currentTime
        
        surfaceHolder?.let { holder ->
            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    // YUV420'yi RGB'ye çevir ve render et
                    val bitmap = yuv420ToBitmap(frame, frameWidth, frameHeight)
                    if (bitmap != null) {
                        // Canvas boyutlarına göre scale et
                        val canvasWidth = canvas.width
                        val canvasHeight = canvas.height
                        
                        if (canvasWidth > 0 && canvasHeight > 0) {
                            val matrix = Matrix()
                            val scaleX = canvasWidth.toFloat() / frameWidth
                            val scaleY = canvasHeight.toFloat() / frameHeight
                            val scale = kotlin.math.min(scaleX, scaleY) // Aspect ratio koru
                            
                            val scaledWidth = frameWidth * scale
                            val scaledHeight = frameHeight * scale
                            val offsetX = (canvasWidth - scaledWidth) / 2
                            val offsetY = (canvasHeight - scaledHeight) / 2
                            
                            matrix.setScale(scale, scale)
                            matrix.postTranslate(offsetX, offsetY)
                            
                            canvas.drawColor(android.graphics.Color.BLACK) // Arka plan
                            canvas.drawBitmap(bitmap, matrix, null)
                        } else {
                            canvas.drawBitmap(bitmap, 0f, 0f, null)
                        }
                    }
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }
    }
    
    /**
     * YUV420'yi Bitmap'e çevir (optimize edilmiş)
     */
    private fun yuv420ToBitmap(yuv: ByteArray, width: Int, height: Int): Bitmap? {
        try {
            // Cache kontrolü
            if (cachedBitmap == null || cachedBitmap!!.width != width || cachedBitmap!!.height != height) {
                cachedBitmap?.recycle()
                cachedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            }
            
            val bitmap = cachedBitmap!!
            val pixels = IntArray(width * height)
            
            val ySize = width * height
            val uSize = ySize / 4
            val vSize = uSize
            
            // YUV420 format: Y plane, U plane, V plane
            var yIndex = 0
            var uIndex = ySize
            var vIndex = ySize + uSize
            
            for (j in 0 until height) {
                for (i in 0 until width) {
                    // YUV değerlerini al
                    val y = (yuv[yIndex].toInt() and 0xFF) - 16
                    val u = (yuv[uIndex].toInt() and 0xFF) - 128
                    val v = (yuv[vIndex].toInt() and 0xFF) - 128
                    
                    // RGB'ye çevir (ITU-R BT.601)
                    var r = (y + 1.402 * v).toInt()
                    var g = (y - 0.344 * u - 0.714 * v).toInt()
                    var b = (y + 1.772 * u).toInt()
                    
                    // Clamp değerleri
                    r = r.coerceIn(0, 255)
                    g = g.coerceIn(0, 255)
                    b = b.coerceIn(0, 255)
                    
                    pixels[yIndex] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    
                    yIndex++
                    
                    // UV index'i güncelle (her 2x2 blok için bir UV değeri)
                    if (i % 2 == 1 && j % 2 == 0) {
                        uIndex++
                        vIndex++
                    }
                }
                
                // Satır sonunda UV index'i düzelt
                if (j % 2 == 1) {
                    uIndex -= width / 2
                    vIndex -= width / 2
                }
            }
            
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            return bitmap
        } catch (e: Exception) {
            android.util.Log.e("DirectCallVideoRenderer", "YUV420 to Bitmap hatası", e)
            return null
        }
    }
    
    /**
     * Kaynakları temizle
     */
    fun dispose() {
        cachedBitmap?.recycle()
        cachedBitmap = null
        surfaceView = null
        surfaceHolder = null
    }
}
