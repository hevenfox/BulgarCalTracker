package com.bulgarcal.tracker.ui.scan

import android.util.Size
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bulgarcal.tracker.domain.Barcodes
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Camera preview that reports the first trustworthy retail barcode it sees.
 *
 * A code whose GS1 check digit is correct is accepted at once; one that fails it is
 * almost always a glare artefact, so the same code has to be read twice before it is
 * accepted. [active] stops the work while a dialog, the log sheet or the product form
 * is on screen, and the camera is released with the composable.
 */
@Composable
fun ScannerCamera(
    modifier: Modifier,
    active: Boolean,
    torch: Boolean,
    onCode: (String) -> Unit,
    onCameraUnavailable: () -> Unit,
    onFlashAvailability: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val scanner: BarcodeScanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E,
                    Barcode.FORMAT_CODE_128,
                    Barcode.FORMAT_CODE_39,
                    Barcode.FORMAT_ITF
                )
                .build()
        )
    }
    val handled = remember { AtomicBoolean(false) }
    val suspect = remember { AtomicReference<String?>(null) }
    val isActive by rememberUpdatedState(active)

    var analysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var camera by remember { mutableStateOf<Camera?>(null) }

    // A new scan starts fresh: the code that was already handled must not block it.
    LaunchedEffect(active) {
        if (active) {
            handled.set(false)
            suspect.set(null)
        }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setAspectRatioStrategy(
                                AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
                            )
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    Size(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                                )
                            )
                            .build()
                    )
                    .build()
                imageAnalysis.setAnalyzer(analyzerExecutor) { proxy ->
                    analyze(
                        proxy = proxy,
                        scanner = scanner,
                        active = isActive,
                        handled = handled,
                        suspect = suspect,
                        onCode = onCode
                    )
                }
                provider.unbindAll()
                val bound = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
                camera = bound
                analysis = imageAnalysis
                onFlashAvailability(bound.cameraInfo.hasFlashUnit())
            } catch (t: Throwable) {
                // No camera, or one another app is holding: the screen falls back to
                // manual entry instead of crashing.
                onCameraUnavailable()
            }
        }
        providerFuture.addListener(listener, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                providerFuture.get().unbindAll()
            } catch (t: Throwable) {
                // already unbound or the provider never came up
            }
            analysis?.clearAnalyzer()
            scanner.close()
            analyzerExecutor.shutdown()
        }
    }

    LaunchedEffect(torch, camera) {
        camera?.cameraControl?.enableTorch(torch && (camera?.cameraInfo?.hasFlashUnit() == true))
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

private fun analyze(
    proxy: ImageProxy,
    scanner: BarcodeScanner,
    active: Boolean,
    handled: AtomicBoolean,
    suspect: AtomicReference<String?>,
    onCode: (String) -> Unit
) {
    val mediaImage = proxy.image
    if (!active || handled.get() || mediaImage == null) {
        proxy.close()
        return
    }
    val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
    scanner.process(image)
        .addOnSuccessListener { barcodes ->
            val codes = barcodes.asSequence().mapNotNull { Barcodes.normalize(it.rawValue) }.toList()
            val trusted = codes.firstOrNull { Barcodes.hasValidCheckDigit(it) }
            val accepted = trusted ?: codes.firstOrNull()?.takeIf { candidate ->
                // Same suspicious read twice in a row: good enough for a package that
                // carries a non-standard code.
                suspect.getAndSet(candidate) == candidate
            }
            if (accepted != null && handled.compareAndSet(false, true)) onCode(accepted)
        }
        .addOnCompleteListener { proxy.close() }
}
