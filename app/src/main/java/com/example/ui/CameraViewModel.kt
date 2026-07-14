package com.example.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MediaDatabase
import com.example.data.MediaItem
import com.example.data.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MediaRepository

    // Onboarding & General Settings State
    private val _themeSelection = MutableStateFlow("dark_slate") // dark_slate, emerald_green, cosmic_dark, blue_night, gold_amber
    val themeSelection: StateFlow<String> = _themeSelection.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(false)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    // Camera Operations State
    private val _activeMode = MutableStateFlow("Auto")
    val activeMode: StateFlow<String> = _activeMode.asStateFlow()

    private val _flashMode = MutableStateFlow("off") // off, on, auto, torch
    val flashMode: StateFlow<String> = _flashMode.asStateFlow()

    private val _hdrEnabled = MutableStateFlow(false)
    val hdrEnabled: StateFlow<Boolean> = _hdrEnabled.asStateFlow()

    private val _timerDuration = MutableStateFlow(0) // 0 (off), 3, 10 seconds
    val timerDuration: StateFlow<Int> = _timerDuration.asStateFlow()

    private val _zoomLevel = MutableStateFlow(1.0f) // 0.6f to 10.0f
    val zoomLevel: StateFlow<Float> = _zoomLevel.asStateFlow()

    private val _videoResolution = MutableStateFlow("1080p") // 1080p, 4K
    val videoResolution: StateFlow<String> = _videoResolution.asStateFlow()

    private val _videoStabilization = MutableStateFlow(true)
    val videoStabilization: StateFlow<Boolean> = _videoStabilization.asStateFlow()

    private val _cinematicBlur = MutableStateFlow(false)
    val cinematicBlur: StateFlow<Boolean> = _cinematicBlur.asStateFlow()

    private val _beautyFilterStrength = MutableStateFlow(0.0f) // 0.0 to 1.0
    val beautyFilterStrength: StateFlow<Float> = _beautyFilterStrength.asStateFlow()

    // Pro Mode States
    private val _proISO = MutableStateFlow(100) // 100, 200, 400, 800, 1600, 3200, 6400
    val proISO: StateFlow<Int> = _proISO.asStateFlow()

    private val _proShutterSpeed = MutableStateFlow("1/125") // 1/1000 to 30s
    val proShutterSpeed: StateFlow<String> = _proShutterSpeed.asStateFlow()

    private val _proWhiteValue = MutableStateFlow(5000) // 2500K to 8000K
    val proWhiteValue: StateFlow<Int> = _proWhiteValue.asStateFlow()

    private val _proFocusDistance = MutableStateFlow(1.0f) // 0.0f (macro) to 1.0f (infinity)
    val proFocusDistance: StateFlow<Float> = _proFocusDistance.asStateFlow()

    private val _proExposureCompensation = MutableStateFlow(0.0f) // -3.0f to +3.0f
    val proExposureCompensation: StateFlow<Float> = _proExposureCompensation.asStateFlow()

    private val _proRawCapture = MutableStateFlow(false)
    val proRawCapture: StateFlow<Boolean> = _proRawCapture.asStateFlow()

    // Private Vault Security
    private val _savedPin = MutableStateFlow("")
    val savedPin: StateFlow<String> = _savedPin.asStateFlow()

    private val _isPinSetupRequired = MutableStateFlow(true)
    val isPinSetupRequired: StateFlow<Boolean> = _isPinSetupRequired.asStateFlow()

    // Backup State
    private val _isCloudBackupEnabled = MutableStateFlow(false)
    val isCloudBackupEnabled: StateFlow<Boolean> = _isCloudBackupEnabled.asStateFlow()

    private val _backupStatus = MutableStateFlow("No backups performed yet")
    val backupStatus: StateFlow<String> = _backupStatus.asStateFlow()

    // Media Lists from Database
    val publicMedia: StateFlow<List<MediaItem>>
    val privateMedia: StateFlow<List<MediaItem>>
    val favorites: StateFlow<List<MediaItem>>

    // Media Details & Editing State
    private val _selectedMediaItem = MutableStateFlow<MediaItem?>(null)
    val selectedMediaItem: StateFlow<MediaItem?> = _selectedMediaItem.asStateFlow()

    // Photo Editor State Variables
    val editRotation = MutableStateFlow(0f)
    val editCropRatio = MutableStateFlow("free") // free, 1:1, 4:3, 16:9
    val editFilter = MutableStateFlow("none") // none, vivid, noir, cinematic, retro, warm, cool, cyberpunk
    val editBrightness = MutableStateFlow(1.0f) // 0.5f to 1.5f
    val editContrast = MutableStateFlow(1.0f) // 0.5f to 1.5f
    val editSaturation = MutableStateFlow(1.0f) // 0.0f to 2.0f
    val editSharpen = MutableStateFlow(0.0f) // 0.0f to 1.0f
    val editBlur = MutableStateFlow(0.0f) // 0.0f to 1.0f
    val editAddedText = MutableStateFlow("")
    val editTextColor = MutableStateFlow(0xFFFFFFFF) // ARGB color
    val editTextSize = MutableStateFlow(18f)
    val editAddedSticker = MutableStateFlow<String?>(null) // e.g. "watermark", "star", "lens_flare"

    init {
        val database = MediaDatabase.getDatabase(application)
        repository = MediaRepository(database.mediaDao())

        publicMedia = repository.publicMedia.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        privateMedia = repository.privateMedia.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        favorites = repository.favorites.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        // Load Pin settings from Prefs
        val sharedPrefs = application.getSharedPreferences("infinity_camera_prefs", Context.MODE_PRIVATE)
        _onboardingCompleted.value = sharedPrefs.getBoolean("onboarding_completed", false)
        _themeSelection.value = sharedPrefs.getString("theme_selection", "dark_slate") ?: "dark_slate"
        val pin = sharedPrefs.getString("vault_pin", "") ?: ""
        _savedPin.value = pin
        _isPinSetupRequired.value = pin.isEmpty()
    }

    // Settings Updates
    fun completeOnboarding(selectedTheme: String) {
        _themeSelection.value = selectedTheme
        _onboardingCompleted.value = true
        val sharedPrefs = getApplication<Application>().getSharedPreferences("infinity_camera_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putBoolean("onboarding_completed", true)
            putString("theme_selection", selectedTheme)
            apply()
        }
    }

    fun updateTheme(themeName: String) {
        _themeSelection.value = themeName
        val sharedPrefs = getApplication<Application>().getSharedPreferences("infinity_camera_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("theme_selection", themeName).apply()
    }

    fun setActiveMode(mode: String) {
        _activeMode.value = mode
    }

    fun setFlashMode(flash: String) {
        _flashMode.value = flash
    }

    fun setHdrEnabled(enabled: Boolean) {
        _hdrEnabled.value = enabled
    }

    fun setTimerDuration(duration: Int) {
        _timerDuration.value = duration
    }

    fun setZoomLevel(zoom: Float) {
        _zoomLevel.value = zoom.coerceIn(0.6f, 10.0f)
    }

    fun setVideoResolution(res: String) {
        _videoResolution.value = res
    }

    fun setVideoStabilization(enabled: Boolean) {
        _videoStabilization.value = enabled
    }

    fun setCinematicBlur(enabled: Boolean) {
        _cinematicBlur.value = enabled
    }

    fun setBeautyFilterStrength(strength: Float) {
        _beautyFilterStrength.value = strength.coerceIn(0.0f, 1.0f)
    }

    // Pro Settings Updates
    fun setProISO(iso: Int) {
        _proISO.value = iso
    }

    fun setProShutterSpeed(speed: String) {
        _proShutterSpeed.value = speed
    }

    fun setProWhiteValue(temp: Int) {
        _proWhiteValue.value = temp
    }

    fun setProFocusDistance(distance: Float) {
        _proFocusDistance.value = distance.coerceIn(0.0f, 1.0f)
    }

    fun setProExposureCompensation(compensation: Float) {
        _proExposureCompensation.value = compensation.coerceIn(-3.0f, 3.0f)
    }

    fun setProRawCapture(enabled: Boolean) {
        _proRawCapture.value = enabled
    }

    // PIN Management
    fun setupPin(newPin: String) {
        _savedPin.value = newPin
        _isPinSetupRequired.value = false
        val sharedPrefs = getApplication<Application>().getSharedPreferences("infinity_camera_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("vault_pin", newPin).apply()
    }

    fun resetPin() {
        _savedPin.value = ""
        _isPinSetupRequired.value = true
        val sharedPrefs = getApplication<Application>().getSharedPreferences("infinity_camera_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().remove("vault_pin").apply()
    }

    // Cloud Backup Simulation
    fun setCloudBackupEnabled(enabled: Boolean) {
        _isCloudBackupEnabled.value = enabled
    }

    fun triggerSync() {
        viewModelScope.launch {
            _backupStatus.value = "Backing up metadata..."
            withContext(Dispatchers.IO) {
                // Mock network delay
                Thread.sleep(1500)
            }
            _backupStatus.value = "Backup completed! Sync date: Just now"
        }
    }

    // Media DB Operations
    fun saveMediaFile(file: File, isVideo: Boolean) {
        viewModelScope.launch {
            val type = if (isVideo) "VIDEO" else "PHOTO"
            val item = MediaItem(
                filePath = file.absolutePath,
                fileName = file.name,
                mediaType = type,
                iso = if (_activeMode.value == "Pro") _proISO.value else null,
                shutterSpeed = if (_activeMode.value == "Pro") _proShutterSpeed.value else null,
                whiteBalance = if (_activeMode.value == "Pro") _proWhiteValue.value else null,
                exposureCompensation = if (_activeMode.value == "Pro") _proExposureCompensation.value else null
            )
            repository.insert(item)
        }
    }

    fun toggleFavorite(item: MediaItem) {
        viewModelScope.launch {
            val updated = item.copy(isFavorite = !item.isFavorite)
            repository.update(updated)
            if (_selectedMediaItem.value?.id == item.id) {
                _selectedMediaItem.value = updated
            }
        }
    }

    fun togglePrivateVault(item: MediaItem) {
        viewModelScope.launch {
            val updated = item.copy(isPrivate = !item.isPrivate)
            repository.update(updated)
            if (_selectedMediaItem.value?.id == item.id) {
                _selectedMediaItem.value = updated
            }
        }
    }

    fun deleteMediaItem(item: MediaItem) {
        viewModelScope.launch {
            repository.delete(item)
            val file = File(item.filePath)
            if (file.exists()) {
                file.delete()
            }
            if (_selectedMediaItem.value?.id == item.id) {
                _selectedMediaItem.value = null
            }
        }
    }

    fun selectMediaItem(item: MediaItem?) {
        _selectedMediaItem.value = item
        // Reset Editor state
        if (item != null) {
            editRotation.value = 0f
            editCropRatio.value = "free"
            editFilter.value = "none"
            editBrightness.value = 1.0f
            editContrast.value = 1.0f
            editSaturation.value = 1.0f
            editSharpen.value = 0.0f
            editBlur.value = 0.0f
            editAddedText.value = ""
            editTextColor.value = 0xFFFFFFFF
            editAddedSticker.value = null
        }
    }

    // Save edited photo
    fun saveEditedPhoto(onSuccess: () -> Unit) {
        val currentItem = _selectedMediaItem.value ?: return
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val originalBitmap = BitmapFactory.decodeFile(currentItem.filePath) ?: return@withContext
                    var processed = originalBitmap

                    // 1. Rotate
                    if (editRotation.value != 0f) {
                        val matrix = Matrix()
                        matrix.postRotate(editRotation.value)
                        processed = Bitmap.createBitmap(
                            processed, 0, 0, processed.width, processed.height, matrix, true
                        )
                    }

                    // 2. We can simulate editing. In real device, we will create a file with "_edited" suffix
                    val originalFile = File(currentItem.filePath)
                    val parentDir = originalFile.parentFile
                    val editedFile = File(parentDir, "edited_${System.currentTimeMillis()}_${originalFile.name}")

                    FileOutputStream(editedFile).use { out ->
                        processed.compress(Bitmap.CompressFormat.JPEG, 90, out)
                    }

                    // Save to Database
                    val newMediaItem = MediaItem(
                        filePath = editedFile.absolutePath,
                        fileName = editedFile.name,
                        mediaType = "PHOTO",
                        isFavorite = currentItem.isFavorite,
                        isPrivate = currentItem.isPrivate,
                        iso = currentItem.iso,
                        shutterSpeed = currentItem.shutterSpeed,
                        whiteBalance = currentItem.whiteBalance,
                        exposureCompensation = currentItem.exposureCompensation
                    )
                    repository.insert(newMediaItem)

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            onSuccess()
        }
    }
}
