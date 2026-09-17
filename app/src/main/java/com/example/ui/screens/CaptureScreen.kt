package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.util.Log
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.outlined.Cameraswitch
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.R
import com.example.ui.theme.AmberWarning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private val ScannerAccentColor = Color.White
private val ScannerSubtleWhite = Color.White.copy(alpha = 0.35f)

@Composable
fun CaptureScreen(
    onNavigateBack: () -> Unit,
    onImageSelected: (Uri) -> Unit,
    onPresetSelected: (String) -> Unit,
    onClearSelection: () -> Unit = {},
    onProceedToAnalyze: () -> Unit,
    selectedImageUri: Uri?,
    selectedPreset: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Camera permission check & request
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // CameraX instance references
    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCameraLoading by remember { mutableStateOf(true) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    var hasTorch by remember { mutableStateOf(false) }
    var isCapturingPhoto by remember { mutableStateOf(false) }

    // Reusable PreviewView across recompositions
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Rebind camera whenever lensFacing or camera permissions change
    LaunchedEffect(lensFacing, hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect
        isCameraLoading = true
        try {
            val cameraProvider = withContext(Dispatchers.IO) {
                ProcessCameraProvider.getInstance(context).get()
            }

            val targetSelector = if (cameraProvider.hasCamera(lensFacing)) {
                lensFacing
            } else if (cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else {
                lensFacing
            }

            val previewUseCase = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val captureUseCase = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            cameraProvider.unbindAll()
            val boundCamera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                targetSelector,
                previewUseCase,
                captureUseCase
            )

            camera = boundCamera
            imageCapture = captureUseCase
            isCameraLoading = false
            cameraError = null

            if (hasTorch && boundCamera.cameraInfo.hasFlashUnit()) {
                boundCamera.cameraControl.enableTorch(true)
            } else {
                hasTorch = false
            }
        } catch (exc: Exception) {
            Log.e("CaptureScreen", "Camera bind failed for $lensFacing", exc)
            cameraError = exc.localizedMessage ?: "Camera binding error"
            isCameraLoading = false
        }
    }

    // Hardware Flashlight controller
    fun setTorch(enable: Boolean) {
        hasTorch = enable
        val cam = camera
        if (cam != null && cam.cameraInfo.hasFlashUnit()) {
            try {
                cam.cameraControl.enableTorch(enable)
                return
            } catch (e: Exception) {
                Log.e("CaptureScreen", "Torch enable via CameraX failed", e)
            }
        }
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            if (cameraManager != null) {
                val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    try {
                        val chars = cameraManager.getCameraCharacteristics(id)
                        chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    } catch (_: Exception) {
                        false
                    }
                }
                if (cameraId != null) {
                    cameraManager.setTorchMode(cameraId, enable)
                }
            }
        } catch (e: Exception) {
            Log.e("CaptureScreen", "Torch enable via CameraManager failed", e)
        }
    }

    // Turn off hardware flashlight when screen is dismissed
    DisposableEffect(Unit) {
        onDispose {
            try {
                camera?.cameraControl?.enableTorch(false)
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                cameraManager?.cameraIdList?.forEach { id ->
                    try {
                        val chars = cameraManager.getCameraCharacteristics(id)
                        if (chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                            cameraManager.setTorchMode(id, false)
                        }
                    } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    val isPresetOrImageActive = selectedImageUri != null || selectedPreset != null

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
            onProceedToAnalyze()
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "reticle_pulse")
    val reticleAlpha by pulseTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Fullscreen Camera Layout
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F10))
    ) {
        // 1. Edge-to-Edge Camera / Preview Layer
        Box(modifier = Modifier.fillMaxSize()) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = "Selected batch preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (selectedPreset == "GRADE_A") {
                Image(
                    painter = painterResource(id = R.drawable.img_sample_grade_a),
                    contentDescription = "Grade A sample preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (selectedPreset == "MIXED") {
                Image(
                    painter = painterResource(id = R.drawable.img_sample_mixed),
                    contentDescription = "Mixed sample preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (hasCameraPermission) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { previewView }
                )

                // Flashlight subtle screen glow
                if (hasTorch) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.12f))
                    )
                }

                // Camera Sensor Initializing / Fallback State
                if (cameraError != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF141416).copy(alpha = 0.92f))
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoCamera,
                            contentDescription = "Camera Sensor",
                            tint = AmberWarning,
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Camera Sensor Initializing",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "In virtual emulator environments, you can test with pre-calibrated batches or choose from your gallery.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White,
                            modifier = Modifier
                                .clickable { onPresetSelected("GRADE_A") }
                                .testTag("fallback_load_sample")
                        ) {
                            Text(
                                text = "Load Grade A Sample",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                        }
                    }
                } else if (isCameraLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0F0F10)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = ScannerAccentColor,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Initializing camera viewfinder...",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }
            } else {
                // Camera Permission Required
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF141416))
                        .padding(28.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = "Camera Permission",
                        tint = ScannerAccentColor,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Akshi requires camera access to scan onion produce and evaluate defect proportions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = ScannerAccentColor,
                        modifier = Modifier
                            .clickable { permissionLauncher.launch(Manifest.permission.CAMERA) }
                            .testTag("grant_camera_permission_button")
                    ) {
                        Text(
                            text = "Allow Camera Access",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF18181B),
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
                        )
                    }
                }
            }

            // Subtle dark translucent overlay over the camera preview
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.20f))
            )
        }

        // 2. Top Bar: Back arrow, Title, Flash toggle
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .size(48.dp)
                    .testTag("camera_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Text(
                text = "Start Inspection",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                ),
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )

            IconButton(
                onClick = { setTorch(!hasTorch) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(48.dp)
                    .testTag("camera_flash_button")
            ) {
                Icon(
                    imageVector = if (hasTorch) Icons.Filled.FlashOn else Icons.Outlined.FlashOn,
                    contentDescription = if (hasTorch) "Flash On" else "Flash Off",
                    tint = if (hasTorch) Color(0xFFFFD600) else Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        // 3. Center Scanning Frame with 4 White Rounded Corner Brackets + Status Pill
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(250.dp)
            ) {
                WhiteScannerCornerBrackets(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = reticleAlpha },
                    bracketColor = Color.White
                )
            }

            Spacer(modifier = Modifier.height(110.dp))

            // Status Badge: Dark rounded pill with white indicator dot and "Detecting..."
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.65f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Detecting...",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = Color.White
                    )
                }
            }
        }

        // 4. Bottom Controls & Text
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Bottom-left: rounded translucent square button with gallery icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("gallery_picker_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = "Gallery",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Bottom-center: large circular shutter button matching reference
                val shutterScale by animateFloatAsState(
                    targetValue = if (isCapturingPhoto) 0.92f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "shutter_scale"
                )

                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .graphicsLayer {
                            scaleX = shutterScale
                            scaleY = shutterScale
                        }
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable {
                            if (isCapturingPhoto) return@clickable

                            if (isPresetOrImageActive) {
                                onProceedToAnalyze()
                            } else if (imageCapture != null && hasCameraPermission) {
                                isCapturingPhoto = true
                                val inspectionsDir = File(context.filesDir, "inspections").apply {
                                    if (!exists()) mkdirs()
                                }
                                val photoFile = File(
                                    inspectionsDir,
                                    "onion_scan_${System.currentTimeMillis()}.jpg"
                                )
                                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                                imageCapture?.takePicture(
                                    outputOptions,
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                            isCapturingPhoto = false
                                            val savedUri = Uri.fromFile(photoFile)
                                            onImageSelected(savedUri)
                                            onProceedToAnalyze()
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            Log.e("CaptureScreen", "Photo capture failed: ${exception.message}", exception)
                                            isCapturingPhoto = false
                                            onProceedToAnalyze()
                                        }
                                    }
                                )
                            } else {
                                onProceedToAnalyze()
                            }
                        }
                        .testTag("capture_shutter_button")
                )

                // Bottom-right: rounded translucent circular button with camera switch icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable {
                            lensFacing = if (lensFacing == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                        }
                        .testTag("switch_camera_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Bottom text: "AI will detect and grade onions automatically"
            Text(
                text = "AI will detect and grade onions automatically",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }

        // 5. Capturing Photo Overlay
        if (isCapturingPhoto) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Analyzing onion produce...",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                }
            }
        }
    }
}

// Four White Rounded Corner Brackets Frame (Visual guide only, does not crop image)
@Composable
private fun WhiteScannerCornerBrackets(
    modifier: Modifier = Modifier,
    bracketColor: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cornerArm = 38.dp.toPx()
        val cornerRadius = 22.dp.toPx()
        val strokeWidth = 3.5.dp.toPx()

        // 1. Top-Left Corner
        val pathTL = Path().apply {
            moveTo(0f, cornerArm)
            lineTo(0f, cornerRadius)
            quadraticTo(0f, 0f, cornerRadius, 0f)
            lineTo(cornerArm, 0f)
        }
        drawPath(
            path = pathTL,
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 2. Top-Right Corner
        val pathTR = Path().apply {
            moveTo(w - cornerArm, 0f)
            lineTo(w - cornerRadius, 0f)
            quadraticTo(w, 0f, w, cornerRadius)
            lineTo(w, cornerArm)
        }
        drawPath(
            path = pathTR,
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 3. Bottom-Right Corner
        val pathBR = Path().apply {
            moveTo(w, h - cornerArm)
            lineTo(w, h - cornerRadius)
            quadraticTo(w, h, w - cornerRadius, h)
            lineTo(w - cornerArm, h)
        }
        drawPath(
            path = pathBR,
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 4. Bottom-Left Corner
        val pathBL = Path().apply {
            moveTo(cornerArm, h)
            lineTo(cornerRadius, h)
            quadraticTo(0f, h, 0f, h - cornerRadius)
            lineTo(0f, h - cornerArm)
        }
        drawPath(
            path = pathBL,
            color = bracketColor,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}
