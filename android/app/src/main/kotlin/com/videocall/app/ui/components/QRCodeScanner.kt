package com.videocall.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Build
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.Result
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

/**
 * QR kod tarayıcı composable
 */
@Composable
fun QRCodeScanner(
    onQRCodeScanned: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasPermission by remember { mutableStateOf(false) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    
    // Kamera izni launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
    }
    
    // Kamera izni kontrolü
    LaunchedEffect(Unit) {
        val permission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        hasPermission = permission
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    if (!hasPermission) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Kamera izni gerekli",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
                Text(
                    text = "QR kod okumak için kamera iznine ihtiyacımız var",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }
                ) {
                    Text("İzin Ver")
                }
            }
        }
        return
    }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                    .also {
                        var isProcessing = false
                        it.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            if (!isProcessing) {
                                isProcessing = true
                                processImageProxy(imageProxy, ctx) { qrContent ->
                                    // Aynı QR kod tekrar okunmasın
                                    if (lastScannedCode != qrContent) {
                                        lastScannedCode = qrContent
                                        // Titreşim feedback
                                        try {
                                            val vibratorManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                ctx.getSystemService(android.os.VibratorManager::class.java)
                                            } else {
                                                null
                                            }
                                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                vibratorManager?.defaultVibrator
                                            } else {
                                                @Suppress("DEPRECATION")
                                                ctx.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
                                            }
                                            if (vibrator != null) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                                                } else {
                                                    @Suppress("DEPRECATION")
                                                    (vibrator as? Vibrator)?.vibrate(100)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            // Vibrator yoksa devam et
                                        }
                                        isProcessing = false
                                        onQRCodeScanned(qrContent)
                                    } else {
                                        isProcessing = false
                                    }
                                }
                                isProcessing = false
                            } else {
                                imageProxy.close()
                            }
                        }
                    }
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    // Hata yönetimi
                }
            }, ContextCompat.getMainExecutor(ctx))
            
            previewView
        },
        modifier = modifier.fillMaxSize()
    )
}

private fun processImageProxy(
    imageProxy: androidx.camera.core.ImageProxy,
    context: android.content.Context,
    onQRCodeFound: (String) -> Unit
) {
    try {
        // YUV_420_888 formatında Y düzlemi (luminance) QR kod okuma için yeterli
        val yBuffer = imageProxy.planes[0].buffer
        val ySize = yBuffer.remaining()
        val yStride = imageProxy.planes[0].rowStride
        val width = imageProxy.width
        val height = imageProxy.height
        
        // Y düzlemini oku
        val yData = ByteArray(ySize)
        yBuffer.get(yData)
        
        // PlanarYUVLuminanceSource için Y düzlemini kullan
        // YUV_420_888'de Y düzlemi tam genişlikte, U ve V düzlemleri yarım genişlikte
        val source = PlanarYUVLuminanceSource(
            yData,
            yStride,
            height,
            0,
            0,
            width,
            height,
            false
        )
        
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        val reader = MultiFormatReader()
        
        // QR kod okuma için hint ekle
        val hints = java.util.HashMap<com.google.zxing.DecodeHintType, Any>().apply {
            put(com.google.zxing.DecodeHintType.POSSIBLE_FORMATS, listOf(com.google.zxing.BarcodeFormat.QR_CODE))
            put(com.google.zxing.DecodeHintType.TRY_HARDER, true)
        }
        reader.setHints(hints)
        
        val result: Result = reader.decode(binaryBitmap)
        if (com.videocall.app.BuildConfig.DEBUG) {
            android.util.Log.d("QRCodeScanner", "QR kod okundu: ${result.text}")
        }
        onQRCodeFound(result.text)
    } catch (e: com.google.zxing.NotFoundException) {
        // QR kod bulunamadı, normal durum - devam et
    } catch (e: Exception) {
        // Hata logları release'de de faydalı olabilir, ama sadece önemli hatalar
        if (com.videocall.app.BuildConfig.DEBUG) {
            android.util.Log.e("QRCodeScanner", "QR kod okuma hatası: ${e.message}", e)
        }
    } finally {
        imageProxy.close()
    }
}

