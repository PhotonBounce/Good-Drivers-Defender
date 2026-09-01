package com.example.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdaptiveScoreEngine
import com.example.data.TrialManager
import com.example.data.EvidenceRepository
import com.example.data.IncidentRecord
import com.example.data.BillingManager
import com.example.data.ScoreTrend
import com.example.data.SubscriptionState
import com.example.data.RecorderDatabase
import com.example.data.TripPoint
import com.google.android.gms.location.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sqrt
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import com.example.util.saveFileToPublicDownloads
import androidx.core.content.ContextCompat
import android.media.MediaRecorder
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collect
import com.aistudio.driverrecorder.gpxrt.BuildConfig

class RecorderViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val database = RecorderDatabase.getDatabase(application)
    private val repository = EvidenceRepository(database.recorderDao())

    private var billingManager: BillingManager? = null
    private var billingCollectJob: Job? = null

    fun setBillingManager(manager: BillingManager) {
        billingManager = manager
        // Mirror the real Google Play subscription state into our exposed state so that
        // every isPro / subscriptionState consumer reflects actual, acknowledged purchases.
        // MainActivity recreates its BillingManager on every configuration change, so
        // cancel the previous mirror first: stacking collectors leaks each dead manager
        // AND lets the fresh manager's initial isLoading(isPro=false) snapshot flash a
        // paying user back to the free tier mid-rotation.
        billingCollectJob?.cancel()
        billingCollectJob = viewModelScope.launch {
            manager.subscriptionState.collect { state ->
                if (!state.isLoading) {
                    _subscriptionState.value = state
                    // A mid-trip upgrade must immediately disarm the free 30-min cap.
                    if (state.isPro) freeTripCapJob?.cancel()
                } else if (_subscriptionState.value.isLoading) {
                    // Only mirror "still loading" while we never had a real answer.
                    _subscriptionState.value = state
                }
            }
        }
    }

    fun getBillingManager(): BillingManager? {
        return billingManager
    }

    // CameraX Active ImageCapture reference and thread-safe registration
    private var activeImageCapture: ImageCapture? = null
    private var activeVideoCapture: VideoCapture<Recorder>? = null
    // Ensure the internal directory exists when the ViewModel is created
    private val evidenceDir: File = File(getApplication<Application>().filesDir, "evidence_media").apply { if (!exists()) mkdirs() }
    private var activeRecording: Recording? = null

    // ── Adaptive Q-Scoring Engine ────────────────────────────────────────────
    private val adaptiveEngine = AdaptiveScoreEngine(application)
    private val _riskLevel = MutableStateFlow(0f)
    val riskLevel: StateFlow<Float> = _riskLevel.asStateFlow()
    private val _scoreTrend = MutableStateFlow(ScoreTrend.STABLE)
    val scoreTrend: StateFlow<ScoreTrend> = _scoreTrend.asStateFlow()
    private val _adaptiveScore = MutableStateFlow(adaptiveEngine.getAdaptiveScore())
    val adaptiveScore: StateFlow<Float> = _adaptiveScore.asStateFlow()
    private val _recentTripScores = MutableStateFlow(adaptiveEngine.getRecentScores(5))
    val recentTripScores: StateFlow<List<Int>> = _recentTripScores.asStateFlow()
    private var sessionHardBrakeCount = 0
    private var tripStartTimeMs = 0L
    private var lastHardBrakeCountTime = 0L
    // ────────────────────────────────────────────────────────────────────────

    private val _lastVideoFile = MutableStateFlow<File?>(null)
    // Expose list of saved video files for UI
    private val _savedVideos = MutableStateFlow<List<File>>(emptyList())
    val savedVideos: StateFlow<List<File>> = _savedVideos.asStateFlow()
    val lastVideoFile: StateFlow<File?> = _lastVideoFile.asStateFlow()

    fun registerImageCapture(capture: ImageCapture?) {
        synchronized(this) {
            activeImageCapture = capture
        }
    }

    fun registerVideoCapture(capture: VideoCapture<Recorder>?) {
        synchronized(this) {
            activeVideoCapture = capture
        }
    }

    /**
     * Records an evidentiary 10-second MP4 clip using CameraX Recorder.
     * Saved to private evidence_media dir as incident_<id>_video.mp4
     */
    fun recordEvidentiaryVideoClip(incidentId: Long) {
        val capture = synchronized(this) { activeVideoCapture } ?: return
        try {
            val dir = File(getApplication<Application>().filesDir, "evidence_media")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "incident_${incidentId}_video.mp4")
            val outputOptions = FileOutputOptions.Builder(file).build()

            val recording = capture.output
                .prepareRecording(getApplication(), outputOptions)
                .apply {
                    // Audio track only when the runtime grant exists — CameraX throws
                    // SecurityException from withAudioEnabled() otherwise, which used
                    // to silently kill the whole clip for camera-only users.
                    if (ContextCompat.checkSelfPermission(
                            getApplication(), android.Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        withAudioEnabled()
                    }
                }
                .start(ContextCompat.getMainExecutor(getApplication())) { event ->
                    when (event) {
                        is VideoRecordEvent.Start -> { /* recording started */ }
                        is VideoRecordEvent.Finalize -> {
                            // Only a successful clip becomes "last video". Evidence stays in
                            // app-private storage; users export copies explicitly from the
                            // locker (auto-copying to public Downloads contradicted the
                            // "saved to Downloads when you export" privacy promise).
                            if (!event.hasError()) {
                                _lastVideoFile.value = file
                                refreshSavedVideos()
                            }
                        }
                        else -> {}
                    }
                }
            activeRecording = recording

            // Auto-stop after 10 seconds
            viewModelScope.launch {
                delay(10_000L)
                activeRecording?.stop()
                activeRecording = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Background Audio Recording states
    private var mediaRecorder: MediaRecorder? = null
    private var audioFile: File? = null

    // Refresh the list of saved video files from evidence_media directory
    private fun refreshSavedVideos() {
        try {
            val dir = File(getApplication<Application>().filesDir, "evidence_media")
            if (dir.exists()) {
                val videos = dir.listFiles { _, name -> name.endsWith(".mp4", ignoreCase = true) }?.toList() ?: emptyList()
                _savedVideos.value = videos
            } else {
                _savedVideos.value = emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _savedVideos.value = emptyList()
        }
    }

    private fun startAudioRecording(tripId: String) {
        if (!isPro) return
        if (!_isSoundEnabled.value) return
        var recorder: MediaRecorder? = null
        try {
            val dir = File(getApplication<Application>().filesDir, "evidence_media")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            val file = File(dir, "trip_${tripId}_audio.m4a")
            audioFile = file

            recorder = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(getApplication())
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(64000)
            recorder.setAudioSamplingRate(44100)
            recorder.setOutputFile(file.absolutePath)

            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder
        } catch (e: Exception) {
            e.printStackTrace()
            // Release the half-initialized recorder so the mic/hardware isn't leaked.
            runCatching { recorder?.release() }
            mediaRecorder = null
        }
    }

    private fun stopAudioRecording() {
        val recorder = mediaRecorder
        mediaRecorder = null
        if (recorder == null) return
        try {
            // stop() throws RuntimeException when no valid frames were captured
            // (e.g. stopped immediately after start) — release() must still run
            // or the native recorder + mic handle leak for the process lifetime.
            recorder.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            runCatching { recorder.release() }
        }
    }

    private fun captureSnapshotForIncident(id: Long) {
        val imgCapture = synchronized(this) { activeImageCapture }
        if (imgCapture != null) {
            try {
                val dir = File(getApplication<Application>().filesDir, "evidence_media")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, "incident_${id}_snap.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                imgCapture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(getApplication()),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            // Snapshot saved — also trigger 10s video clip evidence
                            recordEvidentiaryVideoClip(id)
                        }
                        override fun onError(exception: ImageCaptureException) {
                            exception.printStackTrace()
                            // Still attempt video even if snapshot failed
                            recordEvidentiaryVideoClip(id)
                        }
                    }
                )
            } catch (e: Exception) {
                e.printStackTrace()
                recordEvidentiaryVideoClip(id)
            }
        } else {
            // No image capture — try video only
            recordEvidentiaryVideoClip(id)
        }
    }

    // UI exposed flows
    val allIncidents: StateFlow<List<IncidentRecord>> = repository.allIncidents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uniqueTrips: StateFlow<List<String>> = repository.uniqueTrips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Navigation and screen management
    private val _currentRoute = MutableStateFlow("dashboard")
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    // ─── Subscription / Monetization ─────────────────────────────────────────
    // Starts as "not Pro, still loading" until BillingManager reports the real state
    // (mirrored in via setBillingManager). Defaulting to not-Pro is the safe gate.
    private val _subscriptionState = MutableStateFlow(SubscriptionState(isPro = false, isLoading = true))
    val subscriptionState: StateFlow<SubscriptionState> = _subscriptionState.asStateFlow()

    // Debug-only Pro override for exercising premium features during development.
    // Has NO effect in release builds — guarded by BuildConfig.DEBUG below.
    private val _debugProOverride = MutableStateFlow(false)
    val debugProOverride: StateFlow<Boolean> = _debugProOverride.asStateFlow()

    fun toggleDebugPro() {
        if (!BuildConfig.DEBUG) return // never grant free Pro in a release build
        _debugProOverride.value = !_debugProOverride.value
        speakText(if (_debugProOverride.value) "Premium Pro Active" else "Premium Pro Inactive")
    }

    // ─── 7-day free VIP trial ────────────────────────────────────────────────
    // Every new install gets full VIP/Pro access for its first 7 days; afterwards
    // it drops to the limited free tier unless a subscription is active.
    private val trialManager = TrialManager(application).also { it.ensureStarted() }
    private val _trialActive = MutableStateFlow(trialManager.isInTrial())
    val trialActive: StateFlow<Boolean> = _trialActive.asStateFlow()
    private val _trialDaysRemaining = MutableStateFlow(trialManager.daysRemaining())
    val trialDaysRemaining: StateFlow<Int> = _trialDaysRemaining.asStateFlow()

    /** Re-evaluate the trial window (call on app resume in case it lapsed while open). */
    fun refreshTrial() {
        _trialActive.value = trialManager.isInTrial()
        _trialDaysRemaining.value = trialManager.daysRemaining()
    }

    init {
        // This app stays foregrounded for hours (FLAG_KEEP_SCREEN_ON dash mount), so a
        // day-7 trial can lapse mid-session; without a periodic re-check isProFlow goes
        // stale and composition gates disagree with the live isPro getter.
        viewModelScope.launch {
            while (true) {
                delay(60_000L)
                refreshTrial()
            }
        }
        // Populate the video gallery from disk — previously only a NEW clip's finalize
        // callback refreshed the list, so every app restart showed "No saved videos".
        viewModelScope.launch(Dispatchers.IO) { refreshSavedVideos() }
    }

    // Pro entitlement = a real, acknowledged Google Play subscription (mirrored from
    // BillingManager) OR an active 7-day VIP trial. The debug override only applies in
    // debug builds. Use this synchronous getter from event handlers (onClick etc.) where
    // the live value is read at the moment of the action.
    val isPro: Boolean get() =
        _subscriptionState.value.isPro || trialManager.isInTrial() || (BuildConfig.DEBUG && _debugProOverride.value)

    // Observable equivalent of [isPro] for Composables: reads taken DURING composition must
    // collect this so gated UI (e.g. the evidence locker) recomposes when Pro status changes
    // mid-session, rather than reading the non-observable getter once.
    val isProFlow: StateFlow<Boolean> =
        combine(_subscriptionState, _debugProOverride, _trialActive) { sub, dbg, trial ->
            sub.isPro || trial || (BuildConfig.DEBUG && dbg)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, _subscriptionState.value.isPro || _trialActive.value)

    private val _speedWarningThreshold = MutableStateFlow(8)
    val speedWarningThreshold: StateFlow<Int> = _speedWarningThreshold.asStateFlow()

    fun setSpeedWarningThreshold(threshold: Int) {
        if (!isPro) {
            speakText("Customizing the Speed Warning Threshold is a premium Pro feature. Opening paywall.")
            navigateTo("upgrade")
            return
        }
        _speedWarningThreshold.value = threshold
        checkViolation(_currentSpeed.value)
    }

    private var freeTripCapJob: kotlinx.coroutines.Job? = null

    // True after any stop that happened while the vehicle was still moving; blocks
    // motion automation from instantly restarting the session (see handleMotionAutomation).
    private var motionRestartSuppressed = false

    // Daily incident count for the free-tier limit. Persisted — in-memory counters made
    // the cap "3 per app launch": any process death (routine for a backgrounded dashcam)
    // silently reset it.
    private val FREE_DAILY_INCIDENT_LIMIT = 3
    private val capPrefs = application.getSharedPreferences("free_caps_v1", Context.MODE_PRIVATE)

    private fun canLogIncident(): Boolean {
        if (isPro) return true
        val today = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
        if (capPrefs.getString("incident_day", "") != today) {
            capPrefs.edit().putString("incident_day", today).putInt("incident_count", 0).apply()
        }
        return capPrefs.getInt("incident_count", 0) < FREE_DAILY_INCIDENT_LIMIT
    }

    private fun incrementDailyCount() {
        capPrefs.edit().putInt("incident_count", capPrefs.getInt("incident_count", 0) + 1).apply()
    }
    // ─────────────────────────────────────────────────────────────────────────

    fun navigateTo(route: String) {
        _currentRoute.value = route
    }

    // Complaint-writer selection, held as an id so it survives Activity recreation;
    // the UI re-resolves the record from allIncidents.
    private val _activeSuitIncidentId = MutableStateFlow<Long?>(null)
    val activeSuitIncidentId: StateFlow<Long?> = _activeSuitIncidentId.asStateFlow()

    fun setActiveSuitIncident(id: Long?) {
        _activeSuitIncidentId.value = id
    }

    // Telemetry and Real-Time State
    private val _currentSpeed = MutableStateFlow(0.0) // MPH
    val currentSpeed: StateFlow<Double> = _currentSpeed.asStateFlow()

    private val _targetSpeedLimit = MutableStateFlow(35) // User configured speed limit in MPH
    val targetSpeedLimit: StateFlow<Int> = _targetSpeedLimit.asStateFlow()

    private val _isSpeedingViolation = MutableStateFlow(false)
    val isSpeedingViolation: StateFlow<Boolean> = _isSpeedingViolation.asStateFlow()

    private val _gForce = MutableStateFlow(0.0) // Real-time Gs
    val gForce: StateFlow<Double> = _gForce.asStateFlow()

    private val _latitude = MutableStateFlow(0.0)
    val latitude: StateFlow<Double> = _latitude.asStateFlow()

    private val _longitude = MutableStateFlow(0.0)
    val longitude: StateFlow<Double> = _longitude.asStateFlow()

    private val _street = MutableStateFlow("Determining Location...")
    val street: StateFlow<String> = _street.asStateFlow()

    private val _city = MutableStateFlow("")
    val city: StateFlow<String> = _city.asStateFlow()

    private val _county = MutableStateFlow("")
    val county: StateFlow<String> = _county.asStateFlow()

    // Offline Override details
    private val _manualOverriddenStreet = MutableStateFlow("")
    val manualOverriddenStreet: StateFlow<String> = _manualOverriddenStreet.asStateFlow()

    private val _manualOverriddenCounty = MutableStateFlow("")
    val manualOverriddenCounty: StateFlow<String> = _manualOverriddenCounty.asStateFlow()

    // Recording status
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingMode = MutableStateFlow("Video Clip Stream") // "Video Clip Stream" or "Stamped Frames"
    val recordingMode: StateFlow<String> = _recordingMode.asStateFlow()

    private val _frameRateFps = MutableStateFlow(8) // configurable 4-12 FPS
    val frameRateFps: StateFlow<Int> = _frameRateFps.asStateFlow()

    private val _isFrontCameraActive = MutableStateFlow(false) // Toggle camera sensor setup
    val isFrontCameraActive: StateFlow<Boolean> = _isFrontCameraActive.asStateFlow()

    private val _isSoundEnabled = MutableStateFlow(true)
    val isSoundEnabled: StateFlow<Boolean> = _isSoundEnabled.asStateFlow()

    // Auto capture triggers (Motion & speed automation)
    private val _autoCaptureEnabled = MutableStateFlow(true)
    val autoCaptureEnabled: StateFlow<Boolean> = _autoCaptureEnabled.asStateFlow()

    private val _isStealthMode = MutableStateFlow(false)
    val isStealthMode: StateFlow<Boolean> = _isStealthMode.asStateFlow()

    private val _hardBrakeWarning = MutableStateFlow(false)
    val hardBrakeWarning: StateFlow<Boolean> = _hardBrakeWarning.asStateFlow()

    private val _activeTripId = MutableStateFlow<String?>(null)
    val activeTripId: StateFlow<String?> = _activeTripId.asStateFlow()

    // Sensor & Geolocation APIs
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var textToSpeech: TextToSpeech? = null

    // Tracking idle stop timers
    private var idleHandler = Handler(Looper.getMainLooper())
    private val idleTimeoutRunnable = Runnable {
        if (_isRecording.value && _autoCaptureEnabled.value && !_isStealthMode.value && _currentSpeed.value < 2.0) {
            stopRecordingSession()
            speakText("Auto-capture ended due to inactivity.")
        }
    }

    // TTS warning cooldowns
    private var lastSpeedWarningTime = 0L
    private var lastHardBrakeWarningTime = 0L

    init {
        // Initialize sensors
        sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // Initialize Native Speech warnings
        textToSpeech = TextToSpeech(application) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
            }
        }

        // Initialize GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
        setupLocationTracker()
    }

    fun setTargetSpeedLimit(limit: Int) {
        _targetSpeedLimit.value = limit
        checkViolation(currentSpeed.value)
    }

    fun setRecordingMode(mode: String) {
        if (!isPro) {
            speakText("Customizing recording format mode is a premium Pro feature. Opening paywall.")
            navigateTo("upgrade")
            return
        }
        _recordingMode.value = mode
    }

    fun setFrameRate(fps: Int) {
        if (!isPro) {
            speakText("Customizing video capture frame rates is a premium Pro feature. Opening paywall.")
            navigateTo("upgrade")
            return
        }
        _frameRateFps.value = fps
    }

    fun toggleCameraSensor() {
        _isFrontCameraActive.value = !_isFrontCameraActive.value
        speakText("Switched to ${if (_isFrontCameraActive.value) "front camera" else "rear camera"}")
    }

    fun toggleSound() {
        _isSoundEnabled.value = !_isSoundEnabled.value
    }

    fun toggleAutoCapture() {
        _autoCaptureEnabled.value = !_autoCaptureEnabled.value
    }

    fun setManualLocationOverrides(street: String, county: String) {
        _manualOverriddenStreet.value = street
        _manualOverriddenCounty.value = county
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            stopRecordingSession()
        } else {
            startRecordingSession()
        }
    }

    fun toggleStealthMode() {
        val nextStealth = !_isStealthMode.value
        _isStealthMode.value = nextStealth
        if (nextStealth) {
            if (!_isRecording.value) {
                startRecordingSession()
            }
            speakText("Stealth protection activated. Viewfinders closed, recording secured.")
        } else {
            speakText("Stealth protection deactivated. Standard HUD display restored.")
        }
    }

    fun recordIncidentNow() {
        // Free tier: cap at 3 incidents per day — Pro users are unlimited
        if (!canLogIncident()) {
            speakText("Daily free limit reached. Upgrade to Defender Pro for unlimited incident logging.")
            navigateTo("upgrade")
            return
        }
        // Count synchronously with the check — incrementing after the suspend point let a
        // burst of taps pass the gate 6 times before the first increment landed.
        incrementDailyCount()
        viewModelScope.launch {
            val streetName = if (_manualOverriddenStreet.value.isNotEmpty()) _manualOverriddenStreet.value else _street.value
            val countyName = if (_manualOverriddenCounty.value.isNotEmpty()) _manualOverriddenCounty.value else _county.value

            val finalStreet = if (streetName.startsWith("Determining")) "Highway / Street Unresolved" else streetName
            val finalCounty = if (countyName.isEmpty()) "County Unresolved" else countyName

            val tripFolder = _activeTripId.value ?: ("SESS_" + System.currentTimeMillis())

            val incident = IncidentRecord(
                speedMph = _currentSpeed.value,
                speedLimitMph = _targetSpeedLimit.value,
                maxGForce = _gForce.value,
                latitude = _latitude.value,
                longitude = _longitude.value,
                streetOrHighway = finalStreet,
                city = _city.value.ifEmpty { "Offline Zone" },
                county = finalCounty,
                // Leave identification fields empty — sentinel strings here previously
                // leaked into generated complaint drafts as the literal defendant
                // ("Owner of Vehicle Plate [MANUAL ACTIVE SNAPSHOT]").
                defendantPlate = "",
                defendantCarModelColor = "",
                recklessBehaviorObserved = "Immediate manual capture",
                extraNotes = "Driver manually triggered an immediate telemetry capture. Snapshot and clip recording were requested at the moment of capture.",
                sessionFrameFolder = tripFolder
            )
            val insertedId = repository.insertIncident(incident)
            captureSnapshotForIncident(insertedId)
            speakText("Incident recorded now! Evidence locked.")
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(getApplication(), permission) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun startRecordingSession() {
        val tripId = "TRIP_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        pruneOldestFilesIfStorageExceeded()
        sessionHardBrakeCount = 0
        tripStartTimeMs = System.currentTimeMillis()
        _activeTripId.value = tripId
        _isRecording.value = true
        motionRestartSuppressed = false
        registerLocationUpdates() // switch GPS to high-rate session tracking
        speakText("Recording started. Telemetry stamped.")
        startAudioRecording(tripId)
        // The FGS declares location|microphone types; starting it without BOTH runtime
        // grants throws SecurityException on Android 14+ (and can crash with
        // RemoteServiceException even when caught inside the service). Only start it
        // when the grants exist — the in-app session still runs without it.
        val fgsPermitted = hasPermission(android.Manifest.permission.RECORD_AUDIO) &&
            (hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
                hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION))
        if (fgsPermitted) {
            try {
                com.example.DefenderService.startService(getApplication())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (!isPro) {
            freeTripCapJob = viewModelScope.launch {
                delay(30 * 60 * 1000L) // 30 minutes free limit
                // Re-check entitlement at fire time — the user may have upgraded (or the
                // billing collector may have already cancelled this job) mid-trip.
                if (_isRecording.value && !isPro) {
                    speakText("Free trip duration limit reached. Upgrade to Pro for unlimited recording.")
                    stopRecordingSession()
                }
            }
        }
    }

    private fun stopRecordingSession() {
        freeTripCapJob?.cancel()
        freeTripCapJob = null
        // Any stop while the vehicle is still moving must suppress motion auto-restart:
        // previously auto-capture re-armed within ~1s of a manual STOP (or of the free
        // 30-min cap firing), so the mic could not actually be turned off above 2 mph.
        motionRestartSuppressed = true
        val stoppedTripId = _activeTripId.value

        // Update adaptive Q-score with this trip's raw score before clearing state
        val tripIncidents = allIncidents.value.filter { stoppedTripId != null && it.sessionFrameFolder == stoppedTripId }
        val hardBrakes = tripIncidents.count { it.maxGForce >= 1.5 }
        val rawScore = com.example.data.rawTripScore(hardBrakes, tripIncidents.size - hardBrakes)
        adaptiveEngine.updateWithTripScore(rawScore)
        _adaptiveScore.value = adaptiveEngine.getAdaptiveScore()
        _scoreTrend.value = adaptiveEngine.getTrend()
        _recentTripScores.value = adaptiveEngine.getRecentScores(5)

        _isRecording.value = false
        _activeTripId.value = null
        registerLocationUpdates() // relax GPS back to idle rate
        speakText("Recording stopped. Exporting telemetry.")
        stopAudioRecording()
        // Auto-export GPS telemetry CSV so every trip has a ready-to-share log
        if (stoppedTripId != null) {
            autoExportTripCsv(stoppedTripId)
        }
        try {
            com.example.DefenderService.stopService(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Writes GPS trip-point telemetry for [tripId] to evidence_media/trip_<id>_telemetry.csv.
     * Called automatically when a recording session ends.
     */
    private fun autoExportTripCsv(tripId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val points = repository.getPointsForTrip(tripId).first()
                if (points.isEmpty()) return@launch
                val dir = File(getApplication<Application>().filesDir, "evidence_media")
                if (!dir.exists()) dir.mkdirs()
                val csvFile = File(dir, "trip_${tripId}_telemetry.csv")
                val csvBuilder = StringBuilder()
                csvBuilder.append("GPS_POINT_ID,TIMESTAMP_UTC,LATITUDE,LONGITUDE,SPEED_MPH,STREET,COUNTY\n")
                points.forEach { pt ->
                    csvBuilder.append("${pt.id},${pt.timestamp},${pt.latitude},${pt.longitude},${pt.speedMph},\"${pt.streetName}\",\"${pt.countyName}\"\n")
                }
                csvFile.writeText(csvBuilder.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Direct manual or safety automatic creation of civil lawsuit evidence bundles
    fun createIncidentReport(
        defendantPlate: String,
        defendantColorModel: String,
        behaviorObserved: String,
        extraNotes: String
    ) {
        viewModelScope.launch {
            val streetName = if (_manualOverriddenStreet.value.isNotEmpty()) _manualOverriddenStreet.value else _street.value
            val countyName = if (_manualOverriddenCounty.value.isNotEmpty()) _manualOverriddenCounty.value else _county.value

            val finalStreet = if (streetName.startsWith("Determining")) "Highway / Street Unresolved" else streetName
            val finalCounty = if (countyName.isEmpty()) "County Unresolved" else countyName

            val tripFolder = _activeTripId.value ?: ("SESS_" + System.currentTimeMillis())

            val incident = IncidentRecord(
                speedMph = _currentSpeed.value,
                speedLimitMph = _targetSpeedLimit.value,
                maxGForce = _gForce.value,
                latitude = _latitude.value,
                longitude = _longitude.value,
                streetOrHighway = finalStreet,
                city = _city.value.ifEmpty { "Offline Zone" },
                county = finalCounty,
                defendantPlate = defendantPlate,
                defendantCarModelColor = defendantColorModel,
                recklessBehaviorObserved = behaviorObserved,
                extraNotes = extraNotes,
                sessionFrameFolder = tripFolder
            )
            val insertedId = repository.insertIncident(incident)
            captureSnapshotForIncident(insertedId)
            speakText("Incident report compiled. Evidence locked.")
        }
    }

    fun deleteIncident(id: Long) {
        viewModelScope.launch {
            repository.deleteIncident(id)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationTracker() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val lastLocation = result.lastLocation ?: return
                
                _latitude.value = lastLocation.latitude
                _longitude.value = lastLocation.longitude

                // Velocity conversion: m/s to mph
                // speed is in meters per second
                val speedMphValue = lastLocation.speed * 2.23694
                _currentSpeed.value = String.format(Locale.US, "%.1f", speedMphValue).toDouble()

                // Check speed warning condition (> 8 mph speed limit)
                checkViolation(_currentSpeed.value)

                // Trigger motion automation
                handleMotionAutomation(speedMphValue)

                // Log real-time location as continuous tracks if session is live
                val currentTrip = _activeTripId.value
                if (currentTrip != null) {
                    viewModelScope.launch {
                        val activeStreet = if (_manualOverriddenStreet.value.isNotEmpty()) _manualOverriddenStreet.value else _street.value
                        val activeCounty = if (_manualOverriddenCounty.value.isNotEmpty()) _manualOverriddenCounty.value else _county.value
                        repository.insertTripPoint(
                            TripPoint(
                                runId = currentTrip,
                                latitude = lastLocation.latitude,
                                longitude = lastLocation.longitude,
                                speedMph = _currentSpeed.value,
                                streetName = activeStreet,
                                countyName = activeCounty
                            )
                        )
                    }
                }

                // Reverse Geocode County, City, Street name
                lookupAddress(lastLocation)
            }
        }

        registerLocationUpdates()
    }

    private fun hasLocationPermission(): Boolean =
        hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ||
            hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)

    /**
     * (Re)register GPS updates at a rate matching the session state. Safe to call
     * repeatedly. High-rate fine tracking runs only while a session records; a relaxed
     * request keeps the idle dashboard live without continuous 2 Hz fine-location
     * collection from app launch (matches the privacy policy's session-scoped wording).
     */
    @SuppressLint("MissingPermission")
    private fun registerLocationUpdates() {
        val callback = locationCallback ?: return
        if (!hasLocationPermission()) {
            _street.value = "GPS Active (Permission Pending)"
            return
        }
        val request = if (_isRecording.value) {
            LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L).apply {
                setMinUpdateIntervalMillis(500L)
                setGranularity(Granularity.GRANULARITY_FINE)
            }.build()
        } else {
            LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 3000L).apply {
                setMinUpdateIntervalMillis(2000L)
            }.build()
        }
        try {
            fusedLocationClient?.removeLocationUpdates(callback)
            fusedLocationClient?.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: Exception) {
            _street.value = "GPS Active (Permission Pending)"
        }
    }

    /**
     * Call when runtime permissions become granted. The init-time registration runs
     * before the permission dialog, gets rejected, and was previously never retried —
     * leaving every fresh install's first session with speed 0 and incidents at (0,0).
     */
    fun onLocationPermissionsGranted() {
        registerLocationUpdates()
    }

    private var lastGeocodeMs = 0L

    // Reverse geocoding goes through the device geocoder, which is network-backed via
    // Play services on most devices — throttle it so coordinates aren't shipped out at
    // GPS rate. The street/county label doesn't need sub-30-second freshness.
    private fun lookupAddress(loc: Location) {
        val now = System.currentTimeMillis()
        val needsInitial = _street.value.isEmpty() ||
            _street.value.startsWith("Determining") || _street.value.startsWith("GPS Active")
        if (!needsInitial && now - lastGeocodeMs < 30_000L) return
        lastGeocodeMs = now
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (Geocoder.isPresent()) {
                    val geocoder = Geocoder(getApplication(), Locale.getDefault())
                    val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        withContext(Dispatchers.Main) {
                            _street.value = addr.thoroughfare ?: addr.getAddressLine(0) ?: "Hwy / Local Road"
                            _city.value = addr.locality ?: addr.subLocality ?: ""
                            _county.value = addr.subAdminArea ?: "Unknown County"
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep default or fallback offline indicator
            }
        }
    }

    private fun checkViolation(speedMph: Double) {
        val limit = _targetSpeedLimit.value
        val threshold = _speedWarningThreshold.value
        val difference = speedMph - limit
        val isViolated = difference >= threshold.toDouble()
        _isSpeedingViolation.value = isViolated

        if (isViolated) {
            val now = System.currentTimeMillis()
            if (now - lastSpeedWarningTime > 15000L) { // speak warning once every 15s to not annoy
                lastSpeedWarningTime = now
                speakText("Warning! Speed exceeds limit by over $threshold miles per hour!")
            }
        }
    }

    private fun handleMotionAutomation(speedMph: Double) {
        if (!_autoCaptureEnabled.value || _isStealthMode.value) return

        if (speedMph >= 2.0) {
            // Cancel idle timer since there is motion
            idleHandler.removeCallbacks(idleTimeoutRunnable)

            // Motion detected - autostart recording if inactive, UNLESS a stop happened
            // while still moving (manual STOP or the free 30-min cap): auto-restart is
            // suppressed until the vehicle actually comes to rest, so stopping sticks.
            if (!_isRecording.value && !motionRestartSuppressed) {
                startRecordingSession()
                speakText("Motion registered. Auto-capture engaged.")
            }
        } else {
            // Vehicle at rest — a completed stop re-arms motion automation.
            motionRestartSuppressed = false
            // Stationary - start the 30-sec countdown to prevent storage exhaustion
            if (_isRecording.value) {
                idleHandler.removeCallbacks(idleTimeoutRunnable)
                idleHandler.postDelayed(idleTimeoutRunnable, 30000L) // 30 seconds
            }
        }
    }

    // Accelerometer checks for sudden braking and crash triggers
    // ─── Parking Sentry: armed impact monitoring (runs while the app is open) ──
    // The screen used to be pure theater — a local "armed" flag wired to nothing,
    // filtering on an isAutoCaptured field no code ever set. Armed state now lives
    // here and real accelerometer spikes create real auto-captured incidents.
    private val _sentryArmed = MutableStateFlow(false)
    val sentryArmed: StateFlow<Boolean> = _sentryArmed.asStateFlow()
    private val _sentrySensitivity = MutableStateFlow("MEDIUM")
    val sentrySensitivity: StateFlow<String> = _sentrySensitivity.asStateFlow()
    private var lastSentryHitMs = 0L

    fun setSentryArmed(armed: Boolean) {
        _sentryArmed.value = armed
        speakText(if (armed) "Parking Sentry armed." else "Parking Sentry disarmed.")
    }

    fun setSentrySensitivity(level: String) {
        _sentrySensitivity.value = level
    }

    private fun sentryThresholdG(): Double = when (_sentrySensitivity.value) {
        "HIGH" -> 1.3  // most sensitive
        "LOW" -> 2.2
        else -> 1.7
    }

    private fun handleSentryImpact(gValue: Double) {
        if (!_sentryArmed.value) return
        val now = System.currentTimeMillis()
        if (gValue < sentryThresholdG() || now - lastSentryHitMs < 10_000L) return
        lastSentryHitMs = now
        viewModelScope.launch {
            val incident = IncidentRecord(
                speedMph = _currentSpeed.value,
                speedLimitMph = _targetSpeedLimit.value,
                maxGForce = kotlin.math.round(gValue * 100.0) / 100.0,
                latitude = _latitude.value,
                longitude = _longitude.value,
                streetOrHighway = _street.value,
                city = _city.value.ifEmpty { "Offline Zone" },
                county = _county.value.ifEmpty { "County Unresolved" },
                defendantPlate = "",
                defendantCarModelColor = "",
                recklessBehaviorObserved = "Parking Sentry impact detection",
                extraNotes = "Impact registered automatically while Parking Sentry was armed.",
                sessionFrameFolder = _activeTripId.value ?: ("SENTRY_" + now),
                isAutoCaptured = true
            )
            val id = repository.insertIncident(incident)
            captureSnapshotForIncident(id)
            speakText("Impact detected. Sentry incident recorded.")
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        if (event.values.size < 3) return // guard against short sensor payloads

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate direct net acceleration vector against Earth's standard gravity
        val currentAccelNorm = sqrt(x * x + y * y + z * z)
        val gValue = (currentAccelNorm / SensorManager.GRAVITY_EARTH).toDouble()
        // Round to 2 decimals for display without the redundant String round-trip.
        _gForce.value = kotlin.math.round(gValue * 100.0) / 100.0

        handleSentryImpact(gValue)

        // Count hard-brake spikes for the adaptive risk level (no incident logging — telemetry only)
        if (gValue >= 1.5 && _isRecording.value) {
            val now = System.currentTimeMillis()
            if (now - lastHardBrakeCountTime > 1500L) {
                sessionHardBrakeCount++
                lastHardBrakeCountTime = now
            }
        }
        val elapsedMins = if (tripStartTimeMs > 0) (System.currentTimeMillis() - tripStartTimeMs) / 60000.0 else 0.0
        val rawRisk = adaptiveEngine.computeRiskLevel(
            _currentSpeed.value, _targetSpeedLimit.value, gValue, sessionHardBrakeCount, elapsedMins
        )
        // EMA-smooth so the dashboard risk halo glides instead of flickering on spikes
        _riskLevel.value = adaptiveEngine.smoothRisk(_riskLevel.value, rawRisk)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-Op
    }

    fun speakText(phrase: String) {
        if (_isSoundEnabled.value) {
            textToSpeech?.speak(phrase, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun sha256Of(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun exportEvidenceZip(context: Context, incidentsToExport: List<IncidentRecord>, onResult: (String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            // Held outside the try so the underlying file/zip stream is closed on EVERY exit
            // path (success, throw, or cancellation) — not just the happy path.
            var streamToClose: java.io.Closeable? = null
            try {
                val cacheDir = context.cacheDir
                val zipFile = java.io.File(cacheDir, "Defender_Evidence_Bundle_${System.currentTimeMillis()}.zip")
                val zipOut = java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile))
                streamToClose = zipOut

                // 1. Write metadata/report file
                val reportBuilder = StringBuilder()
                reportBuilder.append("=====================================================\n")
                reportBuilder.append("GOOD DRIVERS DEFENDER - COMPREHENSIVE EVIDENCE BUNDLE\n")
                reportBuilder.append("=====================================================\n\n")
                reportBuilder.append("Export Time (ISO): ${SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())}\n")
                reportBuilder.append("Total Stamped Incidents Packaged: ${incidentsToExport.size}\n")
                if (incidentsToExport.isNotEmpty()) {
                    reportBuilder.append("Primary Jurisdiction: ${incidentsToExport.first().county.uppercase()} COUNTY, USA\n")
                }
                reportBuilder.append("-----------------------------------------------------\n\n")

                incidentsToExport.forEachIndexed { index, incident ->
                    val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US).format(Date(incident.timestamp))
                    reportBuilder.append(">>> RECORD EVIDENCE COMPLAINT DETAIL #${index + 1} <<<\n")
                    reportBuilder.append("Incident ID Stamped: ${incident.id}\n")
                    reportBuilder.append("UTC Event Time: $dateFormatted\n")
                    reportBuilder.append("Exact GPS Location: Latitude ${incident.latitude} / Longitude ${incident.longitude}\n")
                    reportBuilder.append("Thoroughfare/Highway: ${incident.streetOrHighway}\n")
                    reportBuilder.append("City / County / State: ${incident.city} / ${incident.county} / ${incident.state}\n")
                    reportBuilder.append("Vehicular Velocity: ${incident.speedMph} MPH (Posted Speed Limit: ${incident.speedLimitMph} MPH)\n")
                    reportBuilder.append("Triaxial Accelerometer Peak: ${incident.maxGForce} Gs (Braking Threshold: 1.8 Gs)\n")
                    reportBuilder.append("Defendant Vehicle Specs: Color/Model: ${incident.defendantCarModelColor.ifEmpty { "UNKNOWN" }}\n")
                    reportBuilder.append("Defendant Plate Number: ${incident.defendantPlate.ifEmpty { "UNKNOWN" }}\n")
                    reportBuilder.append("Errant Driving Violations Registered: ${incident.recklessBehaviorObserved}\n")
                    reportBuilder.append("Plaintiff Custody Narrative: ${incident.extraNotes}\n")
                    reportBuilder.append("Chain-of-Custody Folder ID: ${incident.sessionFrameFolder}\n")

                    // Bundle REAL media only. Earlier builds fabricated ASCII-art "camera
                    // frames", a silent placeholder WAV, and a hashCode() labeled as an MD5
                    // checksum — content that would destroy the bundle's credibility (and
                    // the user's) under any scrutiny. Each real file now gets a genuine
                    // SHA-256 digest recorded in the report so recipients can verify the
                    // files were not modified after export.
                    val mediaDir = File(context.filesDir, "evidence_media")
                    val candidates = listOf(
                        Triple(File(mediaDir, "incident_${incident.id}_snap.jpg"), "media/incident_${incident.id}_snap.jpg", "snapshot"),
                        Triple(File(mediaDir, "incident_${incident.id}_video.mp4"), "media/incident_${incident.id}_video.mp4", "video clip"),
                        Triple(File(mediaDir, "trip_${incident.sessionFrameFolder}_audio.m4a"), "media/incident_${incident.id}_audio_witness.m4a", "session audio")
                    )
                    candidates.forEach { (srcFile, entryName, label) ->
                        if (srcFile.exists()) {
                            try {
                                zipOut.putNextEntry(java.util.zip.ZipEntry(entryName))
                                srcFile.inputStream().use { input -> input.copyTo(zipOut) }
                                zipOut.closeEntry()
                                reportBuilder.append("SHA-256 of $entryName ($label, ${srcFile.length()} bytes): ${sha256Of(srcFile)}\n")
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            reportBuilder.append("Note: no $label was captured for this incident.\n")
                        }
                    }
                    reportBuilder.append("-----------------------------------------------------\n\n")
                }

                reportBuilder.append(
                    "INTEGRITY NOTE: The SHA-256 digests above were computed on-device at export\n" +
                    "time. Recompute them with any standard SHA-256 tool to verify that the files\n" +
                    "in this bundle have not been modified since export. This bundle is a plain\n" +
                    "(unencrypted) ZIP of user-recorded logs; it makes no claim of certification.\n\n"
                )

                zipOut.putNextEntry(java.util.zip.ZipEntry("evidence_report.txt"))
                zipOut.write(reportBuilder.toString().toByteArray())
                zipOut.closeEntry()

                val recentPoints = repository.getRecentTripPoints()
                if (recentPoints.isNotEmpty()) {
                    val csvBuilder = StringBuilder()
                    csvBuilder.append("GPS_POINT_ID,TIMESTAMP_UTC,LATITUDE,LONGITUDE,SPEED_MPH,RECORD_STREET_OR_HIGHWAY,COUNTY\n")
                    recentPoints.forEach { pt ->
                        csvBuilder.append("${pt.id},${pt.timestamp},${pt.latitude},${pt.longitude},${pt.speedMph},\"${pt.streetName}\",\"${pt.countyName}\"\n")
                    }
                    zipOut.putNextEntry(java.util.zip.ZipEntry("continuous_gps_telemetry_track.csv"))
                    zipOut.write(csvBuilder.toString().toByteArray())
                    zipOut.closeEntry()
                }

                zipOut.close()
                withContext(Dispatchers.Main) {
                    onResult(zipFile.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onResult(null)
                }
            } finally {
                // Idempotent: the success path already closed it; runCatching swallows
                // the harmless double-close and any close-time error.
                runCatching { streamToClose?.close() }
            }
        }
    }

    fun pruneOldestFilesIfStorageExceeded() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(getApplication<Application>().filesDir, "evidence_media")
                if (!dir.exists()) return@launch
                val files = dir.listFiles() ?: return@launch
                
                var totalSize = files.sumOf { it.length() }
                val limit = 500 * 1024 * 1024L // 500 MB
                
                if (totalSize > limit) {
                    val sortedFiles = files.sortedBy { it.lastModified() }
                    var prunedCount = 0
                    for (file in sortedFiles) {
                        if (totalSize <= limit) break
                        val size = file.length()
                        if (file.delete()) {
                            totalSize -= size
                            prunedCount++
                        }
                    }
                    if (prunedCount > 0) {
                        // Keep the gallery in sync — otherwise deleted files linger as
                        // stale entries that open a broken player when tapped.
                        refreshSavedVideos()
                        withContext(Dispatchers.Main) {
                            speakText("Storage limit reached. Pruned $prunedCount oldest recording files.")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getLastRecordingInfo(): LastRecordingInfo? {
        val dir = File(getApplication<Application>().filesDir, "evidence_media")
        if (!dir.exists()) return null
        val files = dir.listFiles() ?: return null
        if (files.isEmpty()) return null

        val newestFile = files.maxByOrNull { it.lastModified() } ?: return null
        
        var tripId = ""
        val name = newestFile.name
        if (name.startsWith("trip_") && name.endsWith("_audio.m4a")) {
            tripId = name.substringAfter("trip_").substringBefore("_audio.m4a")
        } else if (name.startsWith("incident_") && name.endsWith("_snap.jpg")) {
            val incidentIdStr = name.substringAfter("incident_").substringBefore("_snap.jpg")
            val incidentId = incidentIdStr.toLongOrNull()
            if (incidentId != null) {
                val matchingIncident = allIncidents.value.find { it.id == incidentId }
                if (matchingIncident != null) {
                    tripId = matchingIncident.sessionFrameFolder
                }
            }
        }
        
        if (tripId.isEmpty()) {
            val match = files.find { it.name.startsWith("trip_") }
            if (match != null) {
                tripId = match.name.substringAfter("trip_").substringBefore("_audio.m4a")
            } else {
                return null
            }
        }

        val audioFile = files.find { it.name == "trip_${tripId}_audio.m4a" }
        val matchingIncidents = allIncidents.value.filter { it.sessionFrameFolder == tripId }
        val snapshotFiles = matchingIncidents.mapNotNull { incident ->
            val snap = File(dir, "incident_${incident.id}_snap.jpg")
            if (snap.exists()) snap else null
        }
        val videoFiles = matchingIncidents.mapNotNull { incident ->
            val vid = File(dir, "incident_${incident.id}_video.mp4")
            if (vid.exists()) vid else null
        }

        val formattedDate = if (tripId.startsWith("TRIP_")) {
            val rawDate = tripId.substringAfter("TRIP_")
            try {
                val parser = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                parser.parse(rawDate)?.let { formatter.format(it) } ?: tripId
            } catch (e: Exception) {
                tripId
            }
        } else {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(newestFile.lastModified()))
        }

        return LastRecordingInfo(
            tripId = tripId,
            audioFile = audioFile,
            snapshotFiles = snapshotFiles,
            videoFiles = videoFiles,
            formattedDate = formattedDate
        )
    }

    fun shareSingleSnapshot(context: Context, incidentId: Long) {
        val dir = File(context.filesDir, "evidence_media")
        val snapFile = File(dir, "incident_${incidentId}_snap.jpg")
        if (snapFile.exists()) {
            try {
                val authority = "${context.packageName}.fileprovider"
                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, snapFile)
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Defender - Incident Photo Evidence")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Snapshot JPG"))
                speakText("Sharing snapshot photo.")
            } catch (e: Exception) {
                e.printStackTrace()
                speakText("Failed to share snapshot.")
            }
        } else {
            speakText("No snapshot photo found for this incident.")
        }
    }

    fun shareSingleAudio(context: Context, sessionFolder: String) {
        val dir = File(context.filesDir, "evidence_media")
        val audioFile = File(dir, "trip_${sessionFolder}_audio.m4a")
        if (audioFile.exists()) {
            try {
                val authority = "${context.packageName}.fileprovider"
                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, audioFile)
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "audio/m4a"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Defender - Audio Witness")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Audio Witness"))
                speakText("Sharing ambient audio recording.")
            } catch (e: Exception) {
                e.printStackTrace()
                speakText("Failed to share audio.")
            }
        } else {
            speakText("No audio witness recording found for this session.")
        }
    }

    fun shareSingleTelemetryCsv(context: Context, sessionFolder: String) {
        viewModelScope.launch {
            try {
                val points = repository.getPointsForTrip(sessionFolder).first()
                if (points.isEmpty()) {
                    speakText("No telemetry log points found for this session.")
                    return@launch
                }
                
                withContext(Dispatchers.IO) {
                    val cacheFile = File(context.cacheDir, "telemetry_track_${sessionFolder}.csv")
                    val csvBuilder = StringBuilder()
                    csvBuilder.append("GPS_POINT_ID,TIMESTAMP_UTC,LATITUDE,LONGITUDE,SPEED_MPH,RECORD_STREET_OR_HIGHWAY,COUNTY\n")
                    points.forEach { pt ->
                        csvBuilder.append("${pt.id},${pt.timestamp},${pt.latitude},${pt.longitude},${pt.speedMph},\"${pt.streetName}\",\"${pt.countyName}\"\n")
                    }
                    cacheFile.writeText(csvBuilder.toString())
                    
                    withContext(Dispatchers.Main) {
                        try {
                           val authority = "${context.packageName}.fileprovider"
                           val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, cacheFile)
                           val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                               type = "text/csv"
                               putExtra(android.content.Intent.EXTRA_STREAM, uri)
                               putExtra(android.content.Intent.EXTRA_SUBJECT, "Defender - GPS Telemetry Log")
                               addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                           }
                           context.startActivity(android.content.Intent.createChooser(shareIntent, "Share GPS Telemetry CSV"))
                           speakText("Sharing GPS telemetry log.")
                        } catch (e: Exception) {
                           e.printStackTrace()
                           speakText("Failed to share telemetry logs.")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                speakText("Error generating telemetry log.")
            }
        }
    }

    private fun saveFileToPublicDownloads(
        context: Context,
        sourceFile: File,
        displayName: String,
        mimeType: String
    ): Boolean {
        return try {
            val resolver = context.contentResolver
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/DefenderEvidence")
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        sourceFile.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    true
                } else {
                    false
                }
            } else {
                val targetDir = File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),
                    "DefenderEvidence"
                )
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                val targetFile = File(targetDir, displayName)
                sourceFile.inputStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // The single-file save family runs the MediaStore stream copy on Dispatchers.IO —
    // these are invoked straight from locker onClicks, and copying a multi-MB clip on
    // the main thread meant dropped frames or an ANR (the ZIP path always did this
    // correctly; these three previously didn't).
    fun downloadSingleSnapshot(context: Context, incidentId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(context.filesDir, "evidence_media")
            val snapFile = File(dir, "incident_${incidentId}_snap.jpg")
            val message = if (snapFile.exists()) {
                val displayName = "incident_${incidentId}_snap_${System.currentTimeMillis()}.jpg"
                val success = try { saveFileToPublicDownloads(context, snapFile, displayName, "image/jpeg") } catch (e: Exception) { false }
                if (success) "Snapshot saved to public Downloads." else "Failed to download snapshot."
            } else "No snapshot photo found for this incident."
            withContext(Dispatchers.Main) { speakText(message) }
        }
    }

    fun downloadSingleAudio(context: Context, sessionFolder: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(context.filesDir, "evidence_media")
            val audioFile = File(dir, "trip_${sessionFolder}_audio.m4a")
            val message = if (audioFile.exists()) {
                val displayName = "trip_${sessionFolder}_audio_${System.currentTimeMillis()}.m4a"
                val success = try { saveFileToPublicDownloads(context, audioFile, displayName, "audio/m4a") } catch (e: Exception) { false }
                if (success) "Witness audio saved to public Downloads." else "Failed to download audio."
            } else "No audio witness found for this session."
            withContext(Dispatchers.Main) { speakText(message) }
        }
    }

    fun downloadSingleTelemetryCsv(context: Context, sessionFolder: String) {
        viewModelScope.launch {
            try {
                val points = repository.getPointsForTrip(sessionFolder).first()
                if (points.isEmpty()) {
                    speakText("No telemetry log points found for this session.")
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    val tempFile = File(context.cacheDir, "telemetry_temp_${System.currentTimeMillis()}.csv")
                    val csvBuilder = StringBuilder()
                    csvBuilder.append("GPS_POINT_ID,TIMESTAMP_UTC,LATITUDE,LONGITUDE,SPEED_MPH,RECORD_STREET_OR_HIGHWAY,COUNTY\n")
                    points.forEach { pt ->
                        csvBuilder.append("${pt.id},${pt.timestamp},${pt.latitude},${pt.longitude},${pt.speedMph},\"${pt.streetName}\",\"${pt.countyName}\"\n")
                    }
                    tempFile.writeText(csvBuilder.toString())
                    
                    val displayName = "telemetry_track_${sessionFolder}_${System.currentTimeMillis()}.csv"
                    val success = saveFileToPublicDownloads(context, tempFile, displayName, "text/csv")
                    tempFile.delete()
                    
                    withContext(Dispatchers.Main) {
                        if (success) {
                            speakText("Telemetry CSV track saved to public Downloads.")
                        } else {
                            speakText("Failed to download telemetry logs.")
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                speakText("Error exporting telemetry log.")
            }
        }
    }

    fun downloadSingleVideo(context: Context, incidentId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(context.filesDir, "evidence_media")
            val videoFile = File(dir, "incident_${incidentId}_video.mp4")
            val message = if (videoFile.exists()) {
                val displayName = "incident_${incidentId}_video_${System.currentTimeMillis()}.mp4"
                val success = try { saveFileToPublicDownloads(context, videoFile, displayName, "video/mp4") } catch (e: Exception) { false }
                if (success) "Video clip saved to public Downloads." else "Failed to download video clip."
            } else "No video clip found for this incident."
            withContext(Dispatchers.Main) { speakText(message) }
        }
    }

    fun shareVideo(context: Context, incidentId: Long) {
        val dir = File(context.filesDir, "evidence_media")
        val videoFile = File(dir, "incident_${incidentId}_video.mp4")
        if (videoFile.exists()) {
            try {
                val authority = "${context.packageName}.fileprovider"
                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, videoFile)
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "video/mp4"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, "Defender - Evidence Video Clip")
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Evidence Video"))
                speakText("Sharing evidence video clip.")
            } catch (e: Exception) {
                e.printStackTrace()
                speakText("Failed to share video clip.")
            }
        } else {
            speakText("No video clip found for this incident.")
        }
    }

    fun downloadEvidenceZip(context: Context, incidentsToExport: List<IncidentRecord>, onResult: (Boolean) -> Unit) {
        speakText("Compiling zip bundle.")
        exportEvidenceZip(context, incidentsToExport) { zipPath ->
            if (zipPath != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    val zipFile = File(zipPath)
                    val displayName = "Defender_Evidence_Bundle_${System.currentTimeMillis()}.zip"
                    val success = saveFileToPublicDownloads(context, zipFile, displayName, "application/zip")
                    zipFile.delete()
                    withContext(Dispatchers.Main) {
                        if (success) {
                            speakText("Evidence ZIP bundle saved to public Downloads.")
                        } else {
                            speakText("Failed to save ZIP bundle.")
                        }
                        onResult(success)
                    }
                }
            } else {
                speakText("Failed to compile ZIP bundle.")
                onResult(false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeRecording?.stop()
        activeRecording = null
        stopAudioRecording() // release the audio MediaRecorder + mic if still recording
        sensorManager?.unregisterListener(this)
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        idleHandler.removeCallbacks(idleTimeoutRunnable)
        // All capture lives in this ViewModel, so once it's cleared NOTHING is being
        // recorded — the foreground service must not keep telling the user otherwise
        // (a task-swipe used to leave a permanent "Recording active" notification
        // over a dead pipeline, with its wake lock still held).
        try {
            com.example.DefenderService.stopService(getApplication())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

data class LastRecordingInfo(
    val tripId: String,
    val audioFile: File?,
    val snapshotFiles: List<File>,
    val videoFiles: List<File> = emptyList(),
    val formattedDate: String
)
