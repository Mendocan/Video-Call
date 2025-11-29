@file:Suppress("DEPRECATION")

package com.videocall.app.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import org.webrtc.*
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.videocall.app.model.BackgroundMode
import com.videocall.app.model.FilterType

/**
 * WebRTC VideoSink kullanarak video frame'lerini işleyen sınıf
 * Arka plan değiştirme ve filtreler uygular
 */
class VideoProcessor(
    private val context: android.content.Context,
    private val eglBase: EglBase,
    private val scope: CoroutineScope,
    private val backgroundMode: AtomicReference<BackgroundMode>,
    private val filterType: AtomicReference<FilterType>
) {
    private var renderScript: RenderScript? = null
    private var blurScript: ScriptIntrinsicBlur? = null
    private var processedVideoTrack: VideoTrack? = null
    private var processedVideoSource: VideoSource? = null
    private val processedSinks = mutableListOf<VideoSink>()
    
    companion object {
        private const val TAG = "VideoProcessor"
        private const val BLUR_RADIUS = 25f // Blur yarıçapı
    }
    
    init {
        try {
            @Suppress("DEPRECATION")
            val rs = RenderScript.create(context)
            renderScript = rs
            @Suppress("DEPRECATION")
            val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
            blurScript = blur
            blur?.setRadius(BLUR_RADIUS)
        } catch (e: Exception) {
            Log.e(TAG, "RenderScript oluşturulamadı", e)
        }
    }
    
    /**
     * İşlenmiş video track'i döndürür
     */
    fun getProcessedVideoTrack(
        peerConnectionFactory: PeerConnectionFactory,
        originalTrack: VideoTrack
    ): VideoTrack {
        if (processedVideoTrack == null) {
            processedVideoSource = peerConnectionFactory.createVideoSource(false)
            processedVideoTrack = peerConnectionFactory.createVideoTrack(
                "PROCESSED_VIDEO",
                processedVideoSource
            )
            
            // Orijinal track'ten frame'leri al ve işle
            originalTrack.addSink(createProcessingSink())
        }
        
        return processedVideoTrack!!
    }
    
    /**
     * Video işleme sink'i oluşturur
     */
    private fun createProcessingSink(): VideoSink {
        return object : VideoSink {
            override fun onFrame(frame: VideoFrame) {
                scope.launch(Dispatchers.Default) {
                    try {
                        val processedFrame = processFrame(frame)
                        processedFrame?.let {
                            // İşlenmiş frame'i tüm sink'lere gönder
                            processedSinks.forEach { sink ->
                                try {
                                    sink.onFrame(it)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Frame gönderim hatası", e)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Frame işleme hatası", e)
                    }
                }
            }
        }
    }
    
    /**
     * Video frame'ini işler (arka plan değiştirme ve filtreler)
     */
    private fun processFrame(frame: VideoFrame): VideoFrame? {
        val bgMode = backgroundMode.get()
        val filter = filterType.get()
        
        // Herhangi bir işleme yoksa orijinal frame'i döndür
        if (bgMode == BackgroundMode.NONE && filter == FilterType.NONE) {
            return frame
        }
        
        try {
            // Frame'i Bitmap'e dönüştür
            val bitmap = frameToBitmap(frame) ?: return frame
            
            // Arka plan işleme
            val bgProcessedBitmap = when (bgMode) {
                BackgroundMode.BLUR -> applyBlur(bitmap)
                BackgroundMode.COLOR -> applyColorBackground(bitmap)
                BackgroundMode.NONE -> bitmap
            }
            
            // Filtre uygula
            val filteredBitmap = when (filter) {
                FilterType.SEPIA -> applySepiaFilter(bgProcessedBitmap)
                FilterType.BLACK_WHITE -> applyBlackWhiteFilter(bgProcessedBitmap)
                FilterType.VINTAGE -> applyVintageFilter(bgProcessedBitmap)
                FilterType.NONE -> bgProcessedBitmap
            }
            
            // Bitmap'i tekrar VideoFrame'e dönüştür
            return bitmapToFrame(filteredBitmap, frame)
        } catch (e: Exception) {
            Log.e(TAG, "Frame işleme hatası", e)
            return frame
        }
    }
    
    /**
     * VideoFrame'i Bitmap'e dönüştürür
     */
    private fun frameToBitmap(frame: VideoFrame): Bitmap? {
        return try {
            val i420Buffer = frame.buffer.toI420() ?: return null
            val width = i420Buffer.width
            val height = i420Buffer.height
            val yBuffer = i420Buffer.dataY
            val uBuffer = i420Buffer.dataU
            val vBuffer = i420Buffer.dataV
            
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            
            val yPlane = ByteArray(yBuffer.remaining())
            yBuffer.get(yPlane)
            val uPlane = ByteArray(uBuffer.remaining())
            uBuffer.get(uPlane)
            val vPlane = ByteArray(vBuffer.remaining())
            vBuffer.get(vPlane)
            
            // YUV to RGB dönüşümü
            var yIndex = 0
            var uvIndex = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val yValue = (yPlane[yIndex].toInt() and 0xFF).toFloat()
                    val uValue = (uPlane[uvIndex].toInt() and 0xFF) - 128
                    val vValue = (vPlane[uvIndex].toInt() and 0xFF) - 128
                    
                    val r = (yValue + 1.402f * vValue).toInt().coerceIn(0, 255)
                    val g = (yValue - 0.344f * uValue - 0.714f * vValue).toInt().coerceIn(0, 255)
                    val b = (yValue + 1.772f * uValue).toInt().coerceIn(0, 255)
                    
                    pixels[yIndex] = android.graphics.Color.rgb(r, g, b)
                    yIndex++
                    if (x % 2 == 1) uvIndex++
                }
                if (y % 2 == 0) uvIndex -= width / 2
            }
            
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            i420Buffer.release()
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Frame to Bitmap dönüşüm hatası", e)
            null
        }
    }
    
    /**
     * Bitmap'i VideoFrame'e dönüştürür
     */
    private fun bitmapToFrame(bitmap: Bitmap, originalFrame: VideoFrame): VideoFrame? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            
            // RGB to YUV dönüşümü
            val yPlane = ByteArray(width * height)
            val uPlane = ByteArray(width * height / 4)
            val vPlane = ByteArray(width * height / 4)
            
            var yIndex = 0
            var uvIndex = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = pixels[y * width + x]
                    val r = android.graphics.Color.red(pixel)
                    val g = android.graphics.Color.green(pixel)
                    val b = android.graphics.Color.blue(pixel)
                    
                    val yValue = (0.299f * r + 0.587f * g + 0.114f * b).toInt().coerceIn(0, 255)
                    yPlane[yIndex++] = yValue.toByte()
                    
                    if (y % 2 == 0 && x % 2 == 0) {
                        val uValue = ((-0.169f * r - 0.331f * g + 0.5f * b) + 128).toInt().coerceIn(0, 255)
                        val vValue = ((0.5f * r - 0.419f * g - 0.081f * b) + 128).toInt().coerceIn(0, 255)
                        uPlane[uvIndex] = uValue.toByte()
                        vPlane[uvIndex] = vValue.toByte()
                        uvIndex++
                    }
                }
            }
            
            val yBuffer = JavaI420Buffer.wrap(
                width, height,
                ByteBuffer.wrap(yPlane),
                width,
                ByteBuffer.wrap(uPlane),
                width / 2,
                ByteBuffer.wrap(vPlane),
                width / 2,
                null
            )
            
            VideoFrame(yBuffer, originalFrame.rotation, originalFrame.timestampNs)
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap to Frame dönüşüm hatası", e)
            null
        }
    }
    
    /**
     * Blur efekti uygular
     */
    private fun applyBlur(bitmap: Bitmap): Bitmap {
        return try {
            val rs = renderScript ?: return bitmap
            val script = blurScript ?: return bitmap
            
            @Suppress("DEPRECATION")
            val inputAllocation = Allocation.createFromBitmap(rs, bitmap)
            @Suppress("DEPRECATION")
            val outputAllocation = Allocation.createTyped(
                rs,
                inputAllocation.type
            )
            
            script.setInput(inputAllocation)
            script.forEach(outputAllocation)
            outputAllocation.copyTo(bitmap)
            
            inputAllocation.destroy()
            outputAllocation.destroy()
            
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Blur uygulama hatası", e)
            bitmap
        }
    }
    
    /**
     * Renk arka plan uygular (basit implementasyon - chroma key benzeri)
     */
    private fun applyColorBackground(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        // Basit arka plan değiştirme - yeşil ekran benzeri
        // Gerçek implementasyonda ML Kit Segmentation kullanılmalı
        canvas.drawColor(android.graphics.Color.parseColor("#00B8D4")) // Teal renk
        
        val paint = Paint()
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return output
    }
    
    /**
     * Sepia filtresi uygular
     */
    private fun applySepiaFilter(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return output
    }
    
    /**
     * Siyah-beyaz filtresi uygular
     */
    private fun applyBlackWhiteFilter(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f) // Saturation'ı 0 yaparak siyah-beyaz yap
        }
        
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return output
    }
    
    /**
     * Vintage filtresi uygular
     */
    private fun applyVintageFilter(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        
        // Vintage = Sepia + Saturation azaltma + Kontrast artırma
        val colorMatrix = ColorMatrix().apply {
            // Sepia
            set(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            // Saturation azalt
            postConcat(ColorMatrix().apply { setSaturation(0.5f) })
            // Kontrast artır
            postConcat(ColorMatrix().apply { setScale(1.1f, 1.1f, 1.1f, 1f) })
        }
        
        val paint = Paint()
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return output
    }
    
    /**
     * İşlenmiş video track'e sink ekler
     */
    fun addSink(sink: VideoSink) {
        processedSinks.add(sink)
    }
    
    /**
     * İşlenmiş video track'ten sink kaldırır
     */
    fun removeSink(sink: VideoSink) {
        processedSinks.remove(sink)
    }
    
    /**
     * Video processor'ı temizler
     */
    fun dispose() {
        try {
            @Suppress("DEPRECATION")
            blurScript?.destroy()
            @Suppress("DEPRECATION")
            renderScript?.destroy()
            processedVideoSource?.dispose()
            processedSinks.clear()
        } catch (e: Exception) {
            Log.e(TAG, "Dispose hatası", e)
        }
    }
}

