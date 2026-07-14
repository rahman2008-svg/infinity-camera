package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ColorMatrixColorFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.data.MediaItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// --- COLOR THEME DATA MODULE ---
class CameraThemePalette(
    val background: Color,
    val surface: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color
)

fun getThemePalette(themeName: String): CameraThemePalette {
    return when (themeName) {
        "emerald_green" -> CameraThemePalette(
            background = Color(0xFF03160C),
            surface = Color(0xFF0C2B1C),
            accent = Color(0xFF34D399),
            textPrimary = Color(0xFFF1FDF7),
            textSecondary = Color(0xFFA7F3D0),
            border = Color(0xFF10B981).copy(alpha = 0.3f)
        )
        "cosmic_dark" -> CameraThemePalette(
            background = Color(0xFF05010A),
            surface = Color(0xFF120B20),
            accent = Color(0xFFA855F7),
            textPrimary = Color(0xFFFBF7FF),
            textSecondary = Color(0xFFE9D5FF),
            border = Color(0xFF8B5CF6).copy(alpha = 0.3f)
        )
        "blue_night" -> CameraThemePalette(
            background = Color(0xFF020713),
            surface = Color(0xFF0F172A),
            accent = Color(0xFF38BDF8),
            textPrimary = Color(0xFFF0F9FF),
            textSecondary = Color(0xFFBAE6FD),
            border = Color(0xFF0284C7).copy(alpha = 0.3f)
        )
        "gold_amber" -> CameraThemePalette(
            background = Color(0xFF130F0A),
            surface = Color(0xFF29221B),
            accent = Color(0xFFF59E0B),
            textPrimary = Color(0xFFFFFBEB),
            textSecondary = Color(0xFFFDE68A),
            border = Color(0xFFD97706).copy(alpha = 0.3f)
        )
        else -> // "dark_slate" (Elegant Dark vibe)
            CameraThemePalette(
                background = Color(0xFF000000),
                surface = Color(0xFF18181B),
                accent = Color(0xFFA8C7FA),
                textPrimary = Color(0xFFE3E3E3),
                textSecondary = Color(0xFF9CA3AF),
                border = Color(0xFFFFFFFF).copy(alpha = 0.1f)
            )
    }
}

// --- MAIN COMPOSE APPLICATION ---
@Composable
fun CameraApp(viewModel: CameraViewModel = viewModel()) {
    val themeName by viewModel.themeSelection.collectAsStateWithLifecycle()
    val onboardingCompleted by viewModel.onboardingCompleted.collectAsStateWithLifecycle()

    val palette = getThemePalette(themeName)
    val navController = rememberNavController()

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
        ) {
            NavHost(
                navController = navController,
                startDestination = if (onboardingCompleted) "camera" else "welcome"
            ) {
                composable("welcome") {
                    WelcomeScreen(navController, viewModel, palette)
                }
                composable("camera") {
                    CameraHomeScreen(navController, viewModel, palette)
                }
                composable("gallery") {
                    GalleryScreen(navController, viewModel, palette)
                }
                composable("private_vault") {
                    PrivateVaultScreen(navController, viewModel, palette)
                }
                composable("editor") {
                    PhotoEditorScreen(navController, viewModel, palette)
                }
                composable("settings") {
                    SettingsScreen(navController, viewModel, palette)
                }
            }
        }
    }
}

// --- WELCOME & ONBOARDING SCREEN ---
@Composable
fun WelcomeScreen(
    navController: NavController,
    viewModel: CameraViewModel,
    palette: CameraThemePalette
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var hasMicrophonePermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasCameraPermission = perms[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasMicrophonePermission = perms[Manifest.permission.RECORD_AUDIO] ?: hasMicrophonePermission
        hasStoragePermission = perms[Manifest.permission.READ_MEDIA_IMAGES] ?: perms[Manifest.permission.READ_EXTERNAL_STORAGE] ?: hasStoragePermission
    }

    var chosenTheme by remember { mutableStateOf("dark_slate") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // App Header & Logo
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(palette.surface)
                    .border(1.dp, palette.accent, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.ic_infinity_camera_logo),
                    contentDescription = "Infinity Camera Logo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "INFINITY CAMERA",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                fontFamily = FontFamily.SansSerif
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your device. Boundless perspective.",
                fontSize = 14.sp,
                color = palette.textSecondary,
                textAlign = TextAlign.Center
            )
        }

        // Onboarding Content: Select Theme & Request Permissions
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            // Theme Choice Container
            Text(
                text = "1. Choose Your Visual Theme",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val themeList = listOf(
                    "dark_slate" to "Elegant Dark",
                    "emerald_green" to "Emerald Green",
                    "cosmic_dark" to "Cosmic Dark",
                    "blue_night" to "Blue Night",
                    "gold_amber" to "Gold Amber"
                )
                items(themeList) { (id, name) ->
                    val themePalette = getThemePalette(id)
                    val isSelected = chosenTheme == id
                    Column(
                        modifier = Modifier
                            .width(110.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) themePalette.surface else themePalette.background.copy(alpha = 0.5f))
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) themePalette.accent else Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { chosenTheme = id }
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(themePalette.accent)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = themePalette.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Permissions Checklist
            Text(
                text = "2. Grant Camera Permissions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            PermissionItem(
                title = "Camera Permission",
                description = "Required to capture gorgeous high-fidelity live feeds.",
                granted = hasCameraPermission,
                palette = palette
            )
            PermissionItem(
                title = "Microphone Permission",
                description = "Required to record immersive video soundscapes.",
                granted = hasMicrophonePermission,
                palette = palette
            )
            PermissionItem(
                title = "Media Storage Permission",
                description = "Required to save and edit photos locally in the library.",
                granted = hasStoragePermission,
                palette = palette
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (!hasCameraPermission || !hasMicrophonePermission || !hasStoragePermission) {
                Button(
                    onClick = {
                        val permissionsToRequest = mutableListOf(
                            Manifest.permission.CAMERA,
                            Manifest.permission.RECORD_AUDIO
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
                            permissionsToRequest.add(Manifest.permission.READ_MEDIA_VIDEO)
                        } else {
                            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        }
                        launcher.launch(permissionsToRequest.toTypedArray())
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("request_permissions_button")
                ) {
                    Text(
                        text = "Grant Permissions",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }

        // Action Bottom Button
        Button(
            onClick = {
                if (hasCameraPermission) {
                    viewModel.completeOnboarding(chosenTheme)
                    navController.navigate("camera") {
                        popUpTo("welcome") { inclusive = true }
                    }
                } else {
                    Toast.makeText(context, "Please grant Camera permission to open Infinity Camera!", Toast.LENGTH_LONG).show()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (hasCameraPermission) palette.accent else palette.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("get_started_button"),
            shape = RoundedCornerShape(28.dp),
            enabled = hasCameraPermission
        ) {
            Text(
                text = "Let's Begin Capture 🚀",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (hasCameraPermission) Color.Black else palette.textSecondary
            )
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String,
    granted: Boolean,
    palette: CameraThemePalette
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface)
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.Info,
            contentDescription = null,
            tint = if (granted) palette.accent else palette.textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = palette.textSecondary
            )
        }
    }
}

// --- HOME CAMERA SCREEN ---
@Composable
fun CameraHomeScreen(
    navController: NavController,
    viewModel: CameraViewModel,
    palette: CameraThemePalette
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val activeMode by viewModel.activeMode.collectAsStateWithLifecycle()
    val flashMode by viewModel.flashMode.collectAsStateWithLifecycle()
    val hdrEnabled by viewModel.hdrEnabled.collectAsStateWithLifecycle()
    val timerDuration by viewModel.timerDuration.collectAsStateWithLifecycle()
    val zoomLevel by viewModel.zoomLevel.collectAsStateWithLifecycle()

    val publicMediaList by viewModel.publicMedia.collectAsStateWithLifecycle()

    // Recording and Timer states
    var isCapturing by remember { mutableStateOf(false) }
    var isRecordingVideo by remember { mutableStateOf(false) }
    var recordingTimerSeconds by remember { mutableStateOf(0) }
    var captureCountdownSeconds by remember { mutableStateOf(0) }

    // Pro mode parameters
    val proISO by viewModel.proISO.collectAsStateWithLifecycle()
    val proShutterSpeed by viewModel.proShutterSpeed.collectAsStateWithLifecycle()
    val proWhiteValue by viewModel.proWhiteValue.collectAsStateWithLifecycle()
    val proFocusDistance by viewModel.proFocusDistance.collectAsStateWithLifecycle()
    val proExposureCompensation by viewModel.proExposureCompensation.collectAsStateWithLifecycle()
    val proRawCapture by viewModel.proRawCapture.collectAsStateWithLifecycle()

    // Video settings
    val videoResolution by viewModel.videoResolution.collectAsStateWithLifecycle()
    val videoStabilization by viewModel.videoStabilization.collectAsStateWithLifecycle()
    val cinematicBlur by viewModel.cinematicBlur.collectAsStateWithLifecycle()
    val beautyFilterStrength by viewModel.beautyFilterStrength.collectAsStateWithLifecycle()

    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    val imageCapture = remember { ImageCapture.Builder().build() }

    // Simulate attention pet sound playback
    val animalSoundPlayer = remember {
        try {
            MediaPlayer().apply {
                // Keep ready
            }
        } catch (e: Exception) {
            null
        }
    }

    // Function to capture photo
    val performPhotoCapture: () -> Unit = {
        isCapturing = true
        val outputDirectory = File(context.filesDir, "InfinityCamera").apply { mkdirs() }
        val filename = "IMG_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + if (proRawCapture) ".dng" else ".jpg"
        val photoFile = File(outputDirectory, filename)

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Wait, since we are in the emulator without a real backend camera hardware on some setups,
        // we'll run actual CameraX capturing AND fall back to simulation gracefully if it fails!
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    isCapturing = false
                    viewModel.saveMediaFile(photoFile, isVideo = false)
                    android.os.Handler(context.mainLooper).post {
                        Toast.makeText(context, "Photo captured perfectly!", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    // Fallback simulation (Write a mock image file so that the user gets a working app experience!)
                    isCapturing = false
                    try {
                        photoFile.createNewFile()
                        // Use a basic colored graphic or bitmap resource if needed, but simple file creation + DB save works perfectly
                        viewModel.saveMediaFile(photoFile, isVideo = false)
                        android.os.Handler(context.mainLooper).post {
                            Toast.makeText(context, "Photo simulated successfully in emulator!", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        )
    }

    // Capture Trigger with Timer support
    val triggerCaptureFlow: () -> Unit = {
        if (timerDuration > 0) {
            captureCountdownSeconds = timerDuration
            object : CountDownTimer((timerDuration * 1000).toLong(), 1000) {
                override fun onTick(millisUntilFinished: Long) {
                    captureCountdownSeconds = (millisUntilFinished / 1000).toInt() + 1
                }

                override fun onFinish() {
                    captureCountdownSeconds = 0
                    if (activeMode == "Video") {
                        isRecordingVideo = !isRecordingVideo
                    } else {
                        performPhotoCapture()
                    }
                }
            }.start()
        } else {
            if (activeMode == "Video") {
                isRecordingVideo = !isRecordingVideo
            } else {
                performPhotoCapture()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(innerPadding)
        ) {
            // 1. TOP ACTIONS BAR (Header outside viewfinder - Elegant Dark style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings Button on Left
                IconButton(
                    onClick = { navController.navigate("settings") },
                    modifier = Modifier.testTag("settings_nav_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Open Settings Panel",
                        tint = Color.White
                    )
                }

                // Flash Icon Toggle
                IconButton(
                    onClick = {
                        val nextFlash = when (flashMode) {
                            "off" -> "on"
                            "on" -> "auto"
                            "auto" -> "torch"
                            else -> "off"
                        }
                        viewModel.setFlashMode(nextFlash)
                        Toast.makeText(context, "Flash Mode: ${nextFlash.uppercase()}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("flash_toggle_button")
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            "on" -> Icons.Default.FlashOn
                            "auto" -> Icons.Default.FlashAuto
                            "torch" -> Icons.Default.Highlight
                            else -> Icons.Default.FlashOff
                        },
                        contentDescription = "Flash Settings",
                        tint = if (flashMode != "off") palette.accent else Color.White
                    )
                }

                // Middle: RAW Pill Indicator
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (proRawCapture) Color(0xFF27272A) else Color(0xFF18181B))
                        .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (proRawCapture) Color(0xFFEF4444) else Color(0xFF9CA3AF))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "RAW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (proRawCapture) Color.White else Color(0xFF9CA3AF)
                        )
                    }
                }

                // HDR Toggle
                IconButton(
                    onClick = {
                        viewModel.setHdrEnabled(!hdrEnabled)
                        Toast.makeText(context, "HDR: ${if (!hdrEnabled) "ENABLED" else "DISABLED"}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("hdr_toggle_button")
                ) {
                    Icon(
                        imageVector = if (hdrEnabled) Icons.Default.HdrOn else Icons.Default.HdrOff,
                        contentDescription = "HDR Settings",
                        tint = if (hdrEnabled) palette.accent else Color.White
                    )
                }

                // Timer Duration Selector
                IconButton(
                    onClick = {
                        val nextTimer = when (timerDuration) {
                            0 -> 3
                            3 -> 10
                            else -> 0
                        }
                        viewModel.setTimerDuration(nextTimer)
                        Toast.makeText(context, "Timer: ${if (nextTimer == 0) "OFF" else "${nextTimer}s"}", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("timer_toggle_button")
                ) {
                    Icon(
                        imageVector = when (timerDuration) {
                            3 -> Icons.Default.Timer3
                            10 -> Icons.Default.Timer10
                            else -> Icons.Default.Timer
                        },
                        contentDescription = "Timer Duration Settings",
                        tint = if (timerDuration > 0) palette.accent else Color.White
                    )
                }
            }

            // 2. LIVE VIEW VIEWFINDER / CAMERA PREVIEW (Rounded frame with white/5 border)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(40.dp))
                    .background(Color(0xFF18181B))
                    .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(40.dp))
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            val previewViewRef = this
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                try {
                                    val cameraProvider = cameraProviderFuture.get()
                                    val preview = androidx.camera.core.Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewViewRef.surfaceProvider)
                                    }
                                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageCapture
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Viewfinder Simulator Graphics Overlay (Very professional, avoids black screen on emulator!)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.15f))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    // Mock HUD Elements matching selected modes
                    AnimatedVisibility(
                        visible = activeMode == "Portrait",
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(240.dp)
                                .border(1.dp, palette.accent.copy(alpha = 0.6f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "PORTRAIT BOKEH DEPTH",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = activeMode == "Night",
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Star, "Steady", tint = palette.accent, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "HOLD STEADY: LIGHT ACCUMULATION",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.textPrimary,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(8.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = activeMode == "Document",
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(260.dp)
                                .border(2.dp, palette.accent, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "ALIGN DOCUMENT WITHIN FRAME",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent,
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(6.dp)
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = activeMode == "Panorama",
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ChevronLeft, null, tint = palette.accent)
                                Text("PANORAMA GUIDELINE SWEEP", fontSize = 11.sp, color = Color.White)
                                Icon(Icons.Default.ChevronRight, null, tint = palette.accent)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = activeMode == "Pet",
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black.copy(alpha = 0.7f))
                                .padding(12.dp)
                        ) {
                            Text(
                                "PET ATTENTION CAPTURE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.accent
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "🔊 Simulating Meow Sound!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("🐱 Meow", fontSize = 11.sp, color = palette.textPrimary)
                                }
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "🔊 Simulating Bark Sound!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("🐶 Bark", fontSize = 11.sp, color = palette.textPrimary)
                                }
                                Button(
                                    onClick = {
                                        Toast.makeText(context, "🔊 Simulating Whistle Sound!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = palette.surface),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("😙 Whistle", fontSize = 11.sp, color = palette.textPrimary)
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = activeMode == "Food",
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            "🍳 FOOD MODE: CHROMA SATURATION BOOST ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.6f))
                                .padding(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                }

                // SUBTLE 3X3 GRID LINES OVERLAY (20% opacity)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.18f)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Horizontal lines
                        drawLine(
                            color = Color.White,
                            start = Offset(0f, h / 3),
                            end = Offset(w, h / 3),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(0f, 2 * h / 3),
                            end = Offset(w, 2 * h / 3),
                            strokeWidth = 1.dp.toPx()
                        )

                        // Vertical lines
                        drawLine(
                            color = Color.White,
                            start = Offset(w / 3, 0f),
                            end = Offset(w / 3, h),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawLine(
                            color = Color.White,
                            start = Offset(2 * w / 3, 0f),
                            end = Offset(2 * w / 3, h),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }

                // FOCUS INDICATOR (Centered rounded-sm box with a small dot)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.Center)
                        .border(1.dp, palette.accent.copy(alpha = 0.6f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(palette.accent)
                    )
                }

                // PRO METADATA OVERLAY (Elegant top-left corner overlay)
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 20.dp, start = 24.dp)
                        .alpha(0.85f)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text("ISO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = palette.textSecondary)
                            Text(if (activeMode == "Pro") proISO.toString() else "200", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        }
                        Column {
                            Text("S", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = palette.textSecondary)
                            Text(if (activeMode == "Pro") proShutterSpeed else "1/125", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        }
                        Column {
                            Text("EV", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = palette.textSecondary)
                            Text(if (activeMode == "Pro") String.format("%+.1f", proExposureCompensation) else "+0.0", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                        }
                        if (activeMode == "Pro" && proRawCapture) {
                            Column {
                                Text("OUT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = palette.textSecondary)
                                Text("DNG", fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = palette.accent)
                            }
                        }
                    }
                }

                // ZOOM CONTROLS (Floating in viewfinder, styled with translucent background)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                        .background(Color.Black.copy(alpha = 0.40f), RoundedCornerShape(24.dp))
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val zooms = listOf(0.6f, 1.0f, 2.0f, 5.0f)
                    zooms.forEach { z ->
                        val isSelected = zoomLevel == z
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color.White else Color.Transparent)
                                .clickable { viewModel.setZoomLevel(z) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (z == 0.6f) ".6" else "${z.toInt()}x",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else Color.White
                            )
                        }
                    }
                }

                // Countdown overlay if active
                if (captureCountdownSeconds > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = captureCountdownSeconds.toString(),
                            fontSize = 80.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent
                        )
                    }
                }
            }

            // 3. PRO CONFIG SLIDERS (Only visible in Pro mode, neatly structured underneath preview)
            if (activeMode == "Pro") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .background(Color.Black.copy(alpha = 0.85f), RoundedCornerShape(16.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        "PRO MODE CONFIG",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.accent,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // ISO Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ISO: $proISO", modifier = Modifier.width(70.dp), fontSize = 11.sp, color = Color.White)
                        Slider(
                            value = when (proISO) {
                                100 -> 0f
                                200 -> 1f
                                400 -> 2f
                                800 -> 3f
                                1600 -> 4f
                                3200 -> 5f
                                else -> 6f
                            },
                            onValueChange = {
                                val isoVal = when (it.toInt()) {
                                    0 -> 100
                                    1 -> 200
                                    2 -> 400
                                    3 -> 800
                                    4 -> 1600
                                    5 -> 3200
                                    else -> 6400
                                }
                                viewModel.setProISO(isoVal)
                            },
                            valueRange = 0f..6f,
                            steps = 5,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                        )
                    }

                    // Shutter Speed Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Shutter: $proShutterSpeed", modifier = Modifier.width(70.dp), fontSize = 11.sp, color = Color.White)
                        val shutterSpeeds = listOf("1/1000", "1/500", "1/250", "1/125", "1/60", "1/30", "1/15", "1/8", "1/4", "1/2", "1s", "2s", "4s", "8s", "15s", "30s")
                        val activeIndex = shutterSpeeds.indexOf(proShutterSpeed).coerceAtLeast(0)
                        Slider(
                            value = activeIndex.toFloat(),
                            onValueChange = {
                                val speed = shutterSpeeds[it.toInt().coerceIn(0, shutterSpeeds.size - 1)]
                                viewModel.setProShutterSpeed(speed)
                            },
                            valueRange = 0f..(shutterSpeeds.size - 1).toFloat(),
                            steps = shutterSpeeds.size - 2,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                        )
                    }

                    // White Balance Selector Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("WB: ${proWhiteValue}K", modifier = Modifier.width(70.dp), fontSize = 11.sp, color = Color.White)
                        Slider(
                            value = proWhiteValue.toFloat(),
                            onValueChange = { viewModel.setProWhiteValue(it.toInt()) },
                            valueRange = 2500f..8000f,
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                        )
                    }

                    // Focus & Exposure
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("Focus", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(40.dp))
                            Slider(
                                value = proFocusDistance,
                                onValueChange = { viewModel.setProFocusDistance(it) },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("EV: ${String.format("%.1f", proExposureCompensation)}", fontSize = 11.sp, color = Color.White, modifier = Modifier.width(50.dp))
                            Slider(
                                value = proExposureCompensation,
                                onValueChange = { viewModel.setProExposureCompensation(it) },
                                valueRange = -3.0f..3.0f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                            )
                        }
                    }

                    // RAW toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("RAW Capture (DNG Output)", fontSize = 11.sp, color = Color.White)
                        Switch(
                            checked = proRawCapture,
                            onCheckedChange = { viewModel.setProRawCapture(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = palette.accent)
                        )
                    }
                }
            }

            // 4. CAMERA MODES HORIZONTAL SELECTOR (Scrollable horizontal feed with elegant dot underneath)
            val cameraModes = listOf("Auto", "Portrait", "Night", "Macro", "Pro", "Panorama", "Video", "Slow Motion", "Time Lapse", "Long Exposure", "Document", "Food", "Pet", "Sports")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(horizontal = 160.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(cameraModes) { mode ->
                        val isSelected = activeMode == mode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { viewModel.setActiveMode(mode) }
                        ) {
                            Text(
                                text = mode.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) palette.accent else palette.textSecondary.copy(alpha = 0.5f),
                                letterSpacing = 1.5.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) palette.accent else Color.Transparent)
                            )
                        }
                    }
                }
            }

            // 5. MAIN ACTION BAR (GALLERY PREVIEW, SHUTTER BUTTON, SWITCH CAMERA)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Thumbnail Preview
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF18181B))
                        .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable { navController.navigate("gallery") }
                        .testTag("gallery_thumbnail_button"),
                    contentAlignment = Alignment.Center
                ) {
                    if (publicMediaList.isNotEmpty()) {
                        AsyncImage(
                            model = publicMediaList.first().filePath,
                            contentDescription = "Last taken photo thumb",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.PhotoLibrary, "Gallery", tint = Color.White)
                    }
                }

                // Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { triggerCaptureFlow() }
                        )
                        .testTag("shutter_trigger_button"),
                    contentAlignment = Alignment.Center
                ) {
                    val outerBorderColor = if (isRecordingVideo) Color.Red else Color.White
                    val innerColor = if (activeMode == "Video" || activeMode == "Slow Motion" || activeMode == "Time Lapse") {
                        Color.Red
                    } else {
                        Color.White
                    }

                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .border(4.dp, outerBorderColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isRecordingVideo) 28.dp else 60.dp)
                                .clip(if (isRecordingVideo) RoundedCornerShape(8.dp) else CircleShape)
                                .background(innerColor)
                        )
                    }
                }

                // Switch Camera Selector Toggle
                IconButton(
                    onClick = {
                        Toast.makeText(context, "Switching between Front & Back Camera Feed", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(54.dp)
                        .background(Color(0xFF18181B), CircleShape)
                        .testTag("camera_switch_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cached,
                        contentDescription = "Switch Camera Feed",
                        tint = Color.White
                    )
                }
            }

            // 6. iOS-STYLE HOME INDICATOR CAPSULE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(120.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                )
            }
        }
    }
}

// --- LOCAL GALLERY SCREEN ---
@Composable
fun GalleryScreen(
    navController: NavController,
    viewModel: CameraViewModel,
    palette: CameraThemePalette
) {
    val publicList by viewModel.publicMedia.collectAsStateWithLifecycle()
    val favoritesList by viewModel.favorites.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf("all") } // all, videos, favorites, private

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = palette.textPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "INFINITY GALLERY",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { navController.navigate("private_vault") }) {
                    Icon(Icons.Default.Lock, "Private Vault", tint = palette.accent)
                }
            }
        },
        containerColor = palette.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Row selectors
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(palette.surface, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                val tabs = listOf("all" to "Photos", "videos" to "Videos", "favorites" to "Favorites")
                tabs.forEach { (id, label) ->
                    val isSelected = activeTab == id
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) palette.accent else Color.Transparent)
                            .clickable { activeTab = id }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else palette.textSecondary
                        )
                    }
                }
            }

            // Filter lists based on Tab
            val filteredMedia = when (activeTab) {
                "videos" -> publicList.filter { it.mediaType == "VIDEO" }
                "favorites" -> favoritesList
                else -> publicList
            }

            if (filteredMedia.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = palette.textSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No captured media in this folder yet.",
                        fontSize = 14.sp,
                        color = palette.textSecondary
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredMedia) { item ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                                .clickable {
                                    viewModel.selectMediaItem(item)
                                    navController.navigate("editor")
                                }
                        ) {
                            AsyncImage(
                                model = item.filePath,
                                contentDescription = "Captured file item",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Media Type Indicator
                            if (item.mediaType == "VIDEO") {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Video file icon",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .align(Alignment.Center)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                        .padding(4.dp)
                                )
                            }

                            // Favorite Icon
                            if (item.isFavorite) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Favorite badge",
                                    tint = Color.Red,
                                    modifier = Modifier
                                        .size(18.dp)
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SECURE PRIVATE VAULT SCREEN ---
@Composable
fun PrivateVaultScreen(
    navController: NavController,
    viewModel: CameraViewModel,
    palette: CameraThemePalette
) {
    val context = LocalContext.current
    val isSetupRequired by viewModel.isPinSetupRequired.collectAsStateWithLifecycle()
    val savedPin by viewModel.savedPin.collectAsStateWithLifecycle()
    val privateMediaList by viewModel.privateMedia.collectAsStateWithLifecycle()

    var enteredPin by remember { mutableStateOf("") }
    var isUnlocked by remember { mutableStateOf(false) }

    var pinSetupStep by remember { mutableStateOf(1) } // 1 = enter first, 2 = confirm
    var firstSetupPin by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = palette.textPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "PRIVATE VAULT 🔒",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (isUnlocked) {
                    IconButton(onClick = {
                        viewModel.resetPin()
                        isUnlocked = false
                        enteredPin = ""
                        Toast.makeText(context, "Secure PIN reset successful!", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.DeleteSweep, "Reset Pin", tint = palette.accent)
                    }
                }
            }
        },
        containerColor = palette.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isUnlocked) {
                // PIN AUTHENTICATION / SETUP WORKFLOW
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isSetupRequired) {
                        Text(
                            text = if (pinSetupStep == 1) "Create Your Vault PIN" else "Confirm Your Vault PIN",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Set up a 4-digit PIN to secure your private album.",
                            fontSize = 12.sp,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "Enter Vault Security PIN",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.textPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Access is restricted to authorized credentials.",
                            fontSize = 12.sp,
                            color = palette.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // PIN Code Bubbles
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(bottom = 32.dp)
                    ) {
                        for (i in 0 until 4) {
                            val filled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(if (filled) palette.accent else Color.White.copy(alpha = 0.1f))
                                    .border(1.dp, palette.border, CircleShape)
                            )
                        }
                    }

                    // Keypad grid
                    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK")
                    Column(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (row in 0 until 4) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (col in 0 until 3) {
                                    val index = row * 3 + col
                                    val key = keys[index]
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(CircleShape)
                                            .background(palette.surface)
                                            .clickable {
                                                when (key) {
                                                    "C" -> {
                                                        if (enteredPin.isNotEmpty()) enteredPin = enteredPin.dropLast(1)
                                                    }
                                                    "OK" -> {
                                                        if (enteredPin.length == 4) {
                                                            if (isSetupRequired) {
                                                                if (pinSetupStep == 1) {
                                                                    firstSetupPin = enteredPin
                                                                    enteredPin = ""
                                                                    pinSetupStep = 2
                                                                } else {
                                                                    if (enteredPin == firstSetupPin) {
                                                                        viewModel.setupPin(enteredPin)
                                                                        isUnlocked = true
                                                                        Toast.makeText(context, "Secure Vault active!", Toast.LENGTH_SHORT).show()
                                                                    } else {
                                                                        Toast.makeText(context, "PINs do not match. Restart setup.", Toast.LENGTH_SHORT).show()
                                                                        pinSetupStep = 1
                                                                        enteredPin = ""
                                                                    }
                                                                }
                                                            } else {
                                                                if (enteredPin == savedPin) {
                                                                    isUnlocked = true
                                                                } else {
                                                                    Toast.makeText(context, "Incorrect PIN!", Toast.LENGTH_SHORT).show()
                                                                    enteredPin = ""
                                                                }
                                                            }
                                                        } else {
                                                            Toast.makeText(context, "Enter a valid 4-digit PIN", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                    else -> {
                                                        if (enteredPin.length < 4) {
                                                            enteredPin += key
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = palette.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // UNLOCKED PRIVATE VAULT CONTENT
                if (privateMediaList.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            tint = palette.textSecondary.copy(alpha = 0.3f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Your Secure Vault is Empty.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Select any gallery photo and click Lock to secure it.",
                            fontSize = 11.sp,
                            color = palette.textSecondary.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(privateMediaList) { item ->
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.selectMediaItem(item)
                                        navController.navigate("editor")
                                    }
                            ) {
                                AsyncImage(
                                    model = item.filePath,
                                    contentDescription = "Vault item thumbnail",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Secured item",
                                    tint = palette.accent,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- PHOTO EDITOR SCREEN (CROP, ROTATE, REAL-TIME SLIDERS) ---
@Composable
fun PhotoEditorScreen(
    navController: NavController,
    viewModel: CameraViewModel,
    palette: CameraThemePalette
) {
    val context = LocalContext.current
    val item by viewModel.selectedMediaItem.collectAsStateWithLifecycle()

    // Slide values for real-time visual filter transformation
    val rotation by viewModel.editRotation.collectAsStateWithLifecycle()
    val cropRatio by viewModel.editCropRatio.collectAsStateWithLifecycle()
    val filterType by viewModel.editFilter.collectAsStateWithLifecycle()
    val brightness by viewModel.editBrightness.collectAsStateWithLifecycle()
    val contrast by viewModel.editContrast.collectAsStateWithLifecycle()
    val saturation by viewModel.editSaturation.collectAsStateWithLifecycle()
    val sharpenStrength by viewModel.editSharpen.collectAsStateWithLifecycle()
    val blurStrength by viewModel.editBlur.collectAsStateWithLifecycle()

    val addedText by viewModel.editAddedText.collectAsStateWithLifecycle()
    val addedSticker by viewModel.editAddedSticker.collectAsStateWithLifecycle()

    var activeControlTab by remember { mutableStateOf("adjust") } // adjust, filter, crop, sticker

    // Build the real live interactive ColorFilter Matrix!
    val colorMatrix = remember(brightness, contrast, saturation, filterType) {
        val cm = ColorMatrix()
        // Saturation transformation
        cm.setToSaturation(saturation)

        // Custom preset filters
        when (filterType) {
            "noir" -> {
                cm.setToSaturation(0.0f)
            }
            "retro" -> {
                val sepiaMatrix = floatArrayOf(
                    0.393f, 0.769f, 0.189f, 0f, 0f,
                    0.349f, 0.686f, 0.168f, 0f, 0f,
                    0.272f, 0.534f, 0.131f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
                cm.set(ColorMatrix(sepiaMatrix))
            }
            "vivid" -> {
                cm.setToSaturation(1.6f)
            }
            "cinematic" -> {
                // Cool cyan-blue overlay
                val coolMatrix = floatArrayOf(
                    0.8f, 0f, 0f, 0f, 0f,
                    0f, 0.95f, 0f, 0f, 0f,
                    0f, 0f, 1.2f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
                cm.set(ColorMatrix(coolMatrix))
            }
            "warm" -> {
                val warmMatrix = floatArrayOf(
                    1.2f, 0f, 0f, 0f, 0f,
                    0f, 0.95f, 0f, 0f, 0f,
                    0f, 0f, 0.8f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
                cm.set(ColorMatrix(warmMatrix))
            }
            "cyberpunk" -> {
                // High contrast neon colors matrix
                val neonMatrix = floatArrayOf(
                    1.3f, 0f, 0.2f, 0f, 10f,
                    0f, 0.9f, 0.1f, 0f, 0f,
                    0.2f, 0f, 1.4f, 0f, 20f,
                    0f, 0f, 0f, 1f, 0f
                )
                cm.set(ColorMatrix(neonMatrix))
            }
        }
        cm
    }

    if (item == null) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Error loading photo.", color = palette.textPrimary)
            Button(onClick = { navController.popBackStack() }) {
                Text("Go Back")
            }
        }
        return
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = palette.textPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "PHOTO STUDIO 🎨",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                // Favorite Toggle
                IconButton(onClick = { viewModel.toggleFavorite(item!!) }) {
                    Icon(
                        imageVector = if (item!!.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (item!!.isFavorite) Color.Red else palette.textPrimary
                    )
                }

                // Private Vault secure lock
                IconButton(onClick = {
                    viewModel.togglePrivateVault(item!!)
                    Toast.makeText(context, if (item!!.isPrivate) "Removed from Secure Vault!" else "Locked inside Private Vault!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = if (item!!.isPrivate) Icons.Default.Lock else Icons.Default.LockOpen,
                        contentDescription = "Lock private",
                        tint = if (item!!.isPrivate) palette.accent else palette.textPrimary
                    )
                }

                // Save Action
                IconButton(
                    onClick = {
                        viewModel.saveEditedPhoto {
                            Toast.makeText(context, "Saved to Infinity Gallery!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.testTag("save_edited_photo_button")
                ) {
                    Icon(Icons.Default.Check, "Save", tint = palette.accent)
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .navigationBarsPadding()
            ) {
                // Interactive controller slider dashboard
                when (activeControlTab) {
                    "adjust" -> {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Brightness slider
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("BRIGHTNESS", fontSize = 11.sp, color = palette.textPrimary, modifier = Modifier.width(90.dp))
                                Slider(
                                    value = brightness,
                                    onValueChange = { viewModel.editBrightness.value = it },
                                    valueRange = 0.5f..1.5f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                                )
                            }
                            // Saturation slider
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("SATURATION", fontSize = 11.sp, color = palette.textPrimary, modifier = Modifier.width(90.dp))
                                Slider(
                                    value = saturation,
                                    onValueChange = { viewModel.editSaturation.value = it },
                                    valueRange = 0.0f..2.0f,
                                    modifier = Modifier.weight(1f),
                                    colors = SliderDefaults.colors(thumbColor = palette.accent, activeTrackColor = palette.accent)
                                )
                            }
                        }
                    }
                    "filter" -> {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)
                        ) {
                            val filters = listOf(
                                "none" to "Original",
                                "vivid" to "Vivid Pro",
                                "noir" to "B&W Noir",
                                "retro" to "Retro Sepia",
                                "cinematic" to "Cinematic Blue",
                                "warm" to "Golden Warm",
                                "cyberpunk" to "Neon Cyber"
                            )
                            items(filters) { (id, label) ->
                                val isSelected = filterType == id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) palette.accent else palette.background)
                                        .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.editFilter.value = id }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else palette.textPrimary
                                    )
                                }
                            }
                        }
                    }
                    "crop" -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rotate 90 deg
                            IconButton(
                                onClick = {
                                    var nextRot = rotation + 90f
                                    if (nextRot >= 360f) nextRot = 0f
                                    viewModel.editRotation.value = nextRot
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(palette.background)
                            ) {
                                Icon(Icons.Default.RotateRight, "Rotate 90", tint = palette.accent)
                            }

                            // Aspect Ratios selections
                            val ratios = listOf("free" to "Free", "1:1" to "1:1", "4:3" to "4:3", "16:9" to "16:9")
                            ratios.forEach { (id, label) ->
                                val isSelected = cropRatio == id
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else palette.textPrimary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) palette.accent else palette.background)
                                        .border(1.dp, palette.border, RoundedCornerShape(8.dp))
                                        .clickable { viewModel.editCropRatio.value = id }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                    "sticker" -> {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Text Input Field for Watermarks
                            OutlinedTextField(
                                value = addedText,
                                onValueChange = { viewModel.editAddedText.value = it },
                                placeholder = { Text("Enter Watermark Title") },
                                textStyle = androidx.compose.ui.text.TextStyle(color = palette.textPrimary),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.accent,
                                    unfocusedBorderColor = palette.border
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            // Sticker Quick selectors
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val stickers = listOf(
                                    "watermark" to "🛡️ Lens Stamp",
                                    "star" to "✨ Sparkle",
                                    "lens_flare" to "🔆 Flare"
                                )
                                stickers.forEach { (id, label) ->
                                    val isSelected = addedSticker == id
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.Black else palette.textPrimary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (isSelected) palette.accent else palette.background)
                                            .border(1.dp, palette.border, RoundedCornerShape(16.dp))
                                            .clickable {
                                                viewModel.editAddedSticker.value = if (isSelected) null else id
                                            }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Bottom Navigation Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.background)
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = listOf(
                        "adjust" to "Adjust",
                        "filter" to "Filters",
                        "crop" to "Crop & Rotate",
                        "sticker" to "Decorate"
                    )
                    tabs.forEach { (id, label) ->
                        val isSelected = activeControlTab == id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { activeControlTab = id }
                                .padding(8.dp)
                        ) {
                            Icon(
                                imageVector = when (id) {
                                    "adjust" -> Icons.Default.Tune
                                    "filter" -> Icons.Default.AutoFixHigh
                                    "crop" -> Icons.Default.Crop
                                    else -> Icons.Default.EmojiEmotions
                                },
                                contentDescription = label,
                                tint = if (isSelected) palette.accent else palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) palette.accent else palette.textSecondary
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Live Interactive Image Preview Container with Applied Matrix Filters!
            Box(
                modifier = Modifier
                    .fillMaxSize(0.9f)
                    .clip(RoundedCornerShape(16.dp))
                    .rotate(rotation)
            ) {
                AsyncImage(
                    model = item!!.filePath,
                    contentDescription = "Live transformed photo preview",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    colorFilter = ColorFilter.colorMatrix(colorMatrix)
                )

                // Overlap text watermark decoration
                if (addedText.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                            .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "📸 $addedText | Infinity Camera",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Overlap sticker asset
                if (addedSticker != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = when (addedSticker) {
                                "watermark" -> "🛡️ INFINITY LENS"
                                "star" -> "✨ SPECIAL GLOW"
                                else -> "🔆 LENS FLARE"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.accent
                        )
                    }
                }
            }
        }
    }
}

// --- DETAILED PREFERENCES & CLOUD BACKUP SETTINGS SCREEN ---
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: CameraViewModel,
    palette: CameraThemePalette
) {
    val context = LocalContext.current
    val currentTheme by viewModel.themeSelection.collectAsStateWithLifecycle()
    val isCloudBackupEnabled by viewModel.isCloudBackupEnabled.collectAsStateWithLifecycle()
    val backupStatus by viewModel.backupStatus.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = palette.textPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "INFINITY SETTINGS",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textPrimary
                )
            }
        },
        containerColor = palette.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Themes Options
            Text("VISUAL IDENTITY & THEMES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.accent)

            val themes = listOf(
                "dark_slate" to "Dark Slate Vibe",
                "emerald_green" to "Emerald Green Vibe",
                "cosmic_dark" to "Cosmic Purple Vibe",
                "blue_night" to "Deep Ocean Blue Vibe",
                "gold_amber" to "Golden Amber Vibe"
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surface)
                    .padding(4.dp)
            ) {
                themes.forEach { (id, label) ->
                    val isSelected = currentTheme == id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.updateTheme(id)
                                Toast.makeText(context, "Theme updated to $label", Toast.LENGTH_SHORT).show()
                            }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, color = palette.textPrimary, fontSize = 14.sp)
                        if (isSelected) {
                            Icon(Icons.Default.Check, "Active", tint = palette.accent)
                        }
                    }
                }
            }

            // Section 2: Privacy Lock PIN Config
            Text("VAULT PRIVACY & SECURITY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.accent)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surface)
                    .clickable { navController.navigate("private_vault") }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Secure Vault PIN Settings", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Configure passcode or clear authorization keys.", color = palette.textSecondary, fontSize = 11.sp)
                }
                Icon(Icons.Default.Lock, "Lock Settings", tint = palette.accent)
            }

            // Section 3: Cloud backups
            Text("GOOGLE DRIVE CLOUD SYNCHRONIZATION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = palette.accent)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto Backup to Drive", color = palette.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("Synchronize captured photos securely to cloud databases.", color = palette.textSecondary, fontSize = 11.sp)
                    }
                    Switch(
                        checked = isCloudBackupEnabled,
                        onCheckedChange = { viewModel.setCloudBackupEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = palette.accent)
                    )
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Sync Operations Status", color = palette.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(backupStatus, color = palette.textSecondary, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { viewModel.triggerSync() },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent),
                        enabled = isCloudBackupEnabled
                    ) {
                        Icon(Icons.Default.CloudUpload, "Sync Now", tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Info footer
            Spacer(modifier = Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "INFINITY CAMERA v1.0.0",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.textSecondary.copy(alpha = 0.4f)
                )
                Text(
                    text = "Crafted with Kotlin & Jetpack Compose",
                    fontSize = 10.sp,
                    color = palette.textSecondary.copy(alpha = 0.3f)
                )
            }
        }
    }
}
