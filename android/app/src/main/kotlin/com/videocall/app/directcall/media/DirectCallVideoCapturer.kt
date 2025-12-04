/*
 * Copyright (c) 2025 DirectCall Project
 * 
 * Licensed under the MIT License
 * See LICENSE file for details
 */

package com.videocall.app.directcall.media

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.view.Surface
import android.view.SurfaceView
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * DirectCall Video Capturer
 * 
 * Android Camera2 API kullanarak video capture.
 * Frame buffer yönetimi ile gerçek implementasyon.
 * WebRTC'ye bağımlı değil, kendi implementasyonumuz.
 */
class DirectCallVideoCapturer(context: Context) {
    private val appContext = context.applicationContext
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var surfaceView: SurfaceView? = null
    
    private val isCapturing = AtomicBoolean(false)
    private var currentCameraId: String? = null
    
    // Video settings
    private var width = 640
    private var height = 480
    private var fps = 30
    
    // Frame buffer (thread-safe queue)
    private val frameQueue = ArrayBlockingQueue<ByteArray>(5) // Max 5 frame buffer
    
    /**
     * Renderer'ı ekle ve capture'ı başlat
     */
    fun attachRenderer(surfaceView: SurfaceView) {
        this.surfaceView = surfaceView
    }
    
    /**
     * Capture'ı başlat
     */
    fun startCapture() {
        if (isCapturing.compareAndSet(false, true)) {
            try {
                // Frame queue'yu temizle
                frameQueue.clear()
                openCamera()
            } catch (e: Exception) {
                android.util.Log.e("DirectCallVideoCapturer", "Capture başlatılamadı", e)
                isCapturing.set(false)
            }
        }
    }
    
    /**
     * Capture'ı durdur
     */
    fun stopCapture() {
        if (isCapturing.compareAndSet(true, false)) {
            closeCamera()
            frameQueue.clear()
        }
    }
    
    /**
     * Kamerayı aç
     */
    private fun openCamera() {
        try {
            // Kamera ID'yi bul (ön kamera)
            val cameraIds = cameraManager.cameraIdList
            currentCameraId = cameraIds.find { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                facing == CameraCharacteristics.LENS_FACING_FRONT
            } ?: cameraIds.firstOrNull()
            
            if (currentCameraId == null) {
                throw IllegalStateException("Kamera bulunamadı")
            }
            
            // Kamera özelliklerini al
            val characteristics = cameraManager.getCameraCharacteristics(currentCameraId!!)
            val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            
            // Uygun çözünürlüğü bul
            val sizes = map?.getOutputSizes(ImageFormat.YUV_420_888)
            if (sizes != null && sizes.isNotEmpty()) {
                // 640x480'e en yakın boyutu bul
                val targetSize = sizes.minByOrNull { 
                    kotlin.math.abs(it.width - width) + kotlin.math.abs(it.height - height)
                }
                if (targetSize != null) {
                    width = targetSize.width
                    height = targetSize.height
                }
            }
            
            // ImageReader oluştur (YUV420_888 format)
            imageReader = ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 3).apply {
                setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage()
                    image?.let {
                        try {
                            // Frame'i işle ve buffer'a ekle
                            processFrame(it)
                        } finally {
                            it.close()
                        }
                    }
                }, null)
            }
            
            // Surface oluştur (preview için)
            val surfaces = mutableListOf<Surface>()
            surfaceView?.holder?.surface?.let { surfaces.add(it) }
            imageReader?.surface?.let { surfaces.add(it) }
            
            // Kamera aç
            cameraManager.openCamera(currentCameraId!!, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    createCaptureSession(surfaces)
                }
                
                override fun onDisconnected(camera: CameraDevice) {
                    closeCamera()
                }
                
                override fun onError(camera: CameraDevice, error: Int) {
                    android.util.Log.e("DirectCallVideoCapturer", "Kamera hatası: $error")
                    closeCamera()
                }
            }, null)
            
        } catch (e: Exception) {
            android.util.Log.e("DirectCallVideoCapturer", "Kamera açılamadı", e)
            throw e
        }
    }
    
    /**
     * Capture session oluştur
     */
    @Suppress("DEPRECATION") // createCaptureSession deprecated ama tüm API seviyelerinde çalışıyor
    private fun createCaptureSession(surfaces: List<Surface>) {
        val callback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                startRepeatingRequest()
            }
            
            override fun onConfigureFailed(session: CameraCaptureSession) {
                android.util.Log.e("DirectCallVideoCapturer", "Capture session yapılandırılamadı")
                isCapturing.set(false)
            }
        }
        
        // Deprecated API kullanıyoruz ama tüm API seviyelerinde çalışıyor
        // API 28+ için yeni API var ama Kotlin parametre eşleştirmesi sorunlu
        // Bu yüzden deprecated API'yi kullanıp uyarıyı bastırıyoruz
        cameraDevice?.createCaptureSession(
            surfaces,
            callback,
            null // Handler null - callback main thread'de çalışacak
        )
    }
    
    /**
     * Tekrarlayan capture request başlat
     */
    private fun startRepeatingRequest() {
        val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        surfaceView?.holder?.surface?.let {
            captureRequestBuilder?.addTarget(it)
        }
        imageReader?.surface?.let {
            captureRequestBuilder?.addTarget(it)
        }
        
        captureRequestBuilder?.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
        captureRequestBuilder?.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
        captureRequestBuilder?.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
        
        try {
            captureSession?.setRepeatingRequest(
                captureRequestBuilder?.build()!!,
                null,
                null
            )
            android.util.Log.d("DirectCallVideoCapturer", "Capture başlatıldı: ${width}x${height}")
        } catch (e: Exception) {
            android.util.Log.e("DirectCallVideoCapturer", "Capture request başlatılamadı", e)
            isCapturing.set(false)
        }
    }
    
    /**
     * Frame'i işle ve buffer'a ekle
     */
    private fun processFrame(image: android.media.Image) {
        if (!isCapturing.get()) return
        
        try {
            // Frame'i YUV420 formatına çevir
            val yuvFrame = imageToYuv420(image)
            
            // Buffer'a ekle (queue doluysa en eski frame'i at)
            if (!frameQueue.offer(yuvFrame)) {
                // Queue dolu, en eski frame'i çıkar ve yenisini ekle
                frameQueue.poll()
                frameQueue.offer(yuvFrame)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("DirectCallVideoCapturer", "Frame işleme hatası", e)
        }
    }
    
    /**
     * Image'i YUV420 formatına çevir
     */
    private fun imageToYuv420(image: android.media.Image): ByteArray {
        val planes = image.planes
        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer
        
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()
        
        val yuv = ByteArray(ySize + uSize + vSize)
        
        yBuffer.get(yuv, 0, ySize)
        uBuffer.get(yuv, ySize, uSize)
        vBuffer.get(yuv, ySize + uSize, vSize)
        
        return yuv
    }
    
    /**
     * Bir sonraki frame'i al (DirectCallEngine için)
     * @return YUV420 formatında frame veya null (frame yoksa)
     */
    fun getNextFrame(): ByteArray? {
        return frameQueue.poll() // Non-blocking, null döndürürse frame yok
    }
    
    /**
     * Frame buffer'ın dolu olup olmadığını kontrol et
     */
    fun hasFrame(): Boolean {
        return frameQueue.isNotEmpty()
    }
    
    /**
     * Kamerayı kapat
     */
    private fun closeCamera() {
        try {
            captureSession?.stopRepeating()
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            imageReader?.close()
            imageReader = null
        } catch (e: Exception) {
            android.util.Log.e("DirectCallVideoCapturer", "Kamera kapatma hatası", e)
        }
    }
    
    /**
     * Kamerayı değiştir (ön/arka)
     */
    fun switchCamera() {
        if (!isCapturing.get()) return
        
        stopCapture()
        
        // Kamera ID'yi değiştir
        val cameraIds = cameraManager.cameraIdList
        val currentIndex = cameraIds.indexOf(currentCameraId)
        if (currentIndex >= 0 && cameraIds.size > 1) {
            // Diğer kameraya geç
            currentCameraId = cameraIds[(currentIndex + 1) % cameraIds.size]
        }
        
        startCapture()
    }
    
    /**
     * Enable/disable
     */
    fun setEnabled(enabled: Boolean) {
        if (enabled && !isCapturing.get()) {
            startCapture()
        } else if (!enabled && isCapturing.get()) {
            stopCapture()
        }
    }
    
    /**
     * Video boyutlarını al
     */
    fun getVideoSize(): Pair<Int, Int> {
        return Pair(width, height)
    }
    
    /**
     * Kaynakları temizle
     */
    fun dispose() {
        stopCapture()
        surfaceView = null
        frameQueue.clear()
    }
}
