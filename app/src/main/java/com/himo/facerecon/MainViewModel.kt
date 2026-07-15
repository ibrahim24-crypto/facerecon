package com.himo.facerecon

import android.app.Application
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val faceRecognitionHelper = FaceRecognitionHelper(application)
    private val themePreferences = ThemePreferences(application)
    private val faceStabilizer = FaceRecognitionStabilizer()
    private val classStorage = ClassStorage(application)
    private val attendanceStorage = AttendanceStorage(application)

    private val _faces = MutableStateFlow<List<FaceData>>(emptyList())
    val faces: StateFlow<List<FaceData>> = _faces.asStateFlow()

    private val _currentMode = MutableStateFlow(Mode.CAMERA)
    val currentMode: StateFlow<Mode> = _currentMode.asStateFlow()

    private val _registerName = MutableStateFlow("")
    val registerName: StateFlow<String> = _registerName.asStateFlow()

    private val _enrollmentSamples = MutableStateFlow<List<EnrollmentSample>>(emptyList())
    val enrollmentSamples: StateFlow<List<EnrollmentSample>> = _enrollmentSamples.asStateFlow()

    private val _analysisWidth = MutableStateFlow(720)
    val analysisWidth: StateFlow<Int> = _analysisWidth.asStateFlow()

    private val _analysisHeight = MutableStateFlow(1280)
    val analysisHeight: StateFlow<Int> = _analysisHeight.asStateFlow()

    private val _darkTheme = MutableStateFlow(isSystemDarkTheme(application))
    val darkTheme: StateFlow<Boolean> = _darkTheme.asStateFlow()

    private val _showFaceIndex = MutableStateFlow(false)
    val showFaceIndex: StateFlow<Boolean> = _showFaceIndex.asStateFlow()

    private val _devDisableSubmit = MutableStateFlow(false)
    val devDisableSubmit: StateFlow<Boolean> = _devDisableSubmit.asStateFlow()

    private val _developerName = MutableStateFlow("")
    val developerName: StateFlow<String> = _developerName.asStateFlow()

    private val _autoCaptureEnabled = MutableStateFlow(false)
    val autoCaptureEnabled: StateFlow<Boolean> = _autoCaptureEnabled.asStateFlow()

    private val _autoCaptureInterval = MutableStateFlow(1)
    val autoCaptureInterval: StateFlow<Int> = _autoCaptureInterval.asStateFlow()

    private val _currentLanguage = MutableStateFlow("system")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.MAIN)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val backStack = mutableListOf<AppScreen>()

    private val _selectedPerson = MutableStateFlow<String?>(null)
    val selectedPerson: StateFlow<String?> = _selectedPerson.asStateFlow()

    private val _knownNames = MutableStateFlow<List<String>>(emptyList())
    val knownNames: StateFlow<List<String>> = _knownNames.asStateFlow()

    private val _personSamples = MutableStateFlow<List<StoredFaceSample>>(emptyList())
    val personSamples: StateFlow<List<StoredFaceSample>> = _personSamples.asStateFlow()

    private val _showUnknownDialog = MutableStateFlow(false)
    val showUnknownDialog: StateFlow<Boolean> = _showUnknownDialog.asStateFlow()

    private val _unknownPromptName = MutableStateFlow("")
    val unknownPromptName: StateFlow<String> = _unknownPromptName.asStateFlow()

    private val _unknownFaceBitmap = MutableStateFlow<Bitmap?>(null)
    val unknownFaceBitmap: StateFlow<Bitmap?> = _unknownFaceBitmap.asStateFlow()

    private val _capturedFaces = MutableStateFlow<List<EnrollmentSample>>(emptyList())
    val capturedFaces: StateFlow<List<EnrollmentSample>> = _capturedFaces.asStateFlow()

    private val _isCaptureSessionActive = MutableStateFlow(false)
    val isCaptureSessionActive: StateFlow<Boolean> = _isCaptureSessionActive.asStateFlow()

    private val _captureSessionTargetName = MutableStateFlow<String?>(null)
    val captureSessionTargetName: StateFlow<String?> = _captureSessionTargetName.asStateFlow()

    private val _captureTrainName = MutableStateFlow("")
    val captureTrainName: StateFlow<String> = _captureTrainName.asStateFlow()

    private val _showCaptureReview = MutableStateFlow(false)
    val showCaptureReview: StateFlow<Boolean> = _showCaptureReview.asStateFlow()

    private val _isCommitting = MutableStateFlow(false)
    val isCommitting: StateFlow<Boolean> = _isCommitting.asStateFlow()

    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()

    private val _isImageMode = MutableStateFlow(false)
    val isImageMode: StateFlow<Boolean> = _isImageMode.asStateFlow()

    private val _registerCandidates = MutableStateFlow<List<Bitmap>>(emptyList())
    val registerCandidates: StateFlow<List<Bitmap>> = _registerCandidates.asStateFlow()

    private val _classes = MutableStateFlow<List<ClassGroup>>(emptyList())
    val classes: StateFlow<List<ClassGroup>> = _classes.asStateFlow()

    private val _selectedClass = MutableStateFlow<ClassGroup?>(null)
    val selectedClass: StateFlow<ClassGroup?> = _selectedClass.asStateFlow()

    private val _attendancePresent = MutableStateFlow<Set<String>>(emptySet())
    val attendancePresent: StateFlow<Set<String>> = _attendancePresent.asStateFlow()

    private val _attendanceTimestamps = MutableStateFlow<Map<String, Long>>(emptyMap())
    val attendanceTimestamps: StateFlow<Map<String, Long>> = _attendanceTimestamps.asStateFlow()

    private val _hapticTrigger = MutableStateFlow(0)
    val hapticTrigger: StateFlow<Int> = _hapticTrigger.asStateFlow()

    private val _attendanceSession = MutableStateFlow<AttendanceSession?>(null)
    val attendanceSession: StateFlow<AttendanceSession?> = _attendanceSession.asStateFlow()

    private val _classHistory = MutableStateFlow<List<AttendanceSession>>(emptyList())
    val classHistory: StateFlow<List<AttendanceSession>> = _classHistory.asStateFlow()

    private val _pendingClassName = MutableStateFlow("")
    val pendingClassName: StateFlow<String> = _pendingClassName.asStateFlow()

    private val _pendingClassRoster = MutableStateFlow<List<String>>(emptyList())
    val pendingClassRoster: StateFlow<List<String>> = _pendingClassRoster.asStateFlow()

    private val _pendingScanCandidates = MutableStateFlow<List<Bitmap>>(emptyList())
    val pendingScanCandidates: StateFlow<List<Bitmap>> = _pendingScanCandidates.asStateFlow()

    private val _rosterEnrollmentNames = MutableStateFlow<List<String>>(emptyList())
    val rosterEnrollmentNames: StateFlow<List<String>> = _rosterEnrollmentNames.asStateFlow()

    private val _rosterEnrollmentIndex = MutableStateFlow(-1)
    val rosterEnrollmentIndex: StateFlow<Int> = _rosterEnrollmentIndex.asStateFlow()

    private val _rosterEnrollmentClassInfo = MutableStateFlow<Pair<String, String?>?>(null)
    // ^ Pair<className, existingClassId?>

    private val _latestRosterFace = MutableStateFlow<Pair<Bitmap, FloatArray>?>(null)
    val latestRosterFace: StateFlow<Pair<Bitmap, FloatArray>?> = _latestRosterFace.asStateFlow()

    private var pendingClassId: String? = null

    private var burstFaceId: String? = null
    private var burstRemaining = 0
    private var lastBurstCaptureTimeMs = 0L

    private var lastDetectedBitmap: Bitmap? = null
    private var pendingUnknownBitmap: Bitmap? = null
    private var pendingUnknownEmbedding: FloatArray? = null

    init {
        refreshKnownNames()
        refreshClasses()
        viewModelScope.launch {
            val stored = themePreferences.readDarkTheme()
            _darkTheme.value = stored ?: isSystemDarkTheme(application)

            // load new prefs
            val sf = themePreferences.readShowFaceIndex()
            _showFaceIndex.value = sf ?: false
            val dev = themePreferences.readDevDisableSubmit()
            _devDisableSubmit.value = dev ?: false
            val dn = themePreferences.readDeveloperName()
            _developerName.value = dn ?: "Ibrahim Ezzine"

            val ac = themePreferences.readAutoCaptureEnabled()
            _autoCaptureEnabled.value = ac ?: false
            val ai = themePreferences.readAutoCaptureInterval()
            _autoCaptureInterval.value = ai ?: 1

            val lang = themePreferences.readLanguage()
            _currentLanguage.value = lang ?: "system"
        }
    }

    fun launchFrameWork(block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            block()
        }
    }

    fun stabilizeRecognition(faceId: String, result: RecognitionResult): String {
        return faceStabilizer.stabilize(
            faceId = faceId,
            rawName = result.name,
            distance = result.bestDistance,
            threshold = FaceRecognitionHelper.DEFAULT_THRESHOLD
        )
    }

    fun pruneFaceTracks(activeIds: Set<String>) {
        faceStabilizer.pruneActiveIds(activeIds)
    }

    fun updateFaces(newFaces: List<FaceData>) {
        _faces.value = newFaces
    }

    fun updateAnalysisResolution(width: Int, height: Int) {
        if (_analysisWidth.value != width || _analysisHeight.value != height) {
            _analysisWidth.value = width
            _analysisHeight.value = height
        }
    }

    fun setMode(mode: Mode) {
        _currentMode.value = mode
        updateFaces(emptyList())
        faceStabilizer.clear()
        _registerCandidates.value = emptyList()
        if (mode == Mode.CAMERA) {
            lastDetectedBitmap = null
        }
    }

    fun setRegisterName(name: String) {
        _registerName.value = name
    }

    fun setDarkTheme(enabled: Boolean) {
        _darkTheme.value = enabled
        viewModelScope.launch {
            themePreferences.saveDarkTheme(enabled)
        }
    }

    fun toggleDarkTheme() {
        setDarkTheme(!_darkTheme.value)
    }

    fun setShowFaceIndex(enabled: Boolean) {
        _showFaceIndex.value = enabled
        viewModelScope.launch {
            themePreferences.saveShowFaceIndex(enabled)
        }
    }

    fun setDevDisableSubmit(enabled: Boolean) {
        _devDisableSubmit.value = enabled
        viewModelScope.launch {
            themePreferences.saveDevDisableSubmit(enabled)
        }
    }

    fun setDeveloperName(name: String) {
        _developerName.value = name
        viewModelScope.launch {
            themePreferences.saveDeveloperName(name)
        }
    }

    fun setAutoCaptureEnabled(enabled: Boolean) {
        _autoCaptureEnabled.value = enabled
        viewModelScope.launch { themePreferences.saveAutoCaptureEnabled(enabled) }
    }

    fun setAutoCaptureInterval(seconds: Int) {
        _autoCaptureInterval.value = seconds
        viewModelScope.launch { themePreferences.saveAutoCaptureInterval(seconds.coerceAtLeast(1)) }
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
        viewModelScope.launch { themePreferences.saveLanguage(lang) }
    }

    private fun pushScreen(screen: AppScreen) {
        backStack.add(_currentScreen.value)
        _currentScreen.value = screen
    }

    fun navigateToManageFaces() {
        refreshKnownNames()
        pushScreen(AppScreen.MANAGE_FACES)
    }

    fun navigateToMain() {
        backStack.clear()
        _currentScreen.value = AppScreen.MAIN
        _selectedPerson.value = null
    }

    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            _currentScreen.value = backStack.removeAt(backStack.lastIndex)
            return true
        }
        return false
    }

    private val lastAutoCaptureTimes = mutableMapOf<String, Long>()

    fun tryCaptureBurstSample(
        faceId: String,
        stableName: String,
        bitmap: Bitmap,
        embedding: FloatArray
    ) {
        val mode = _currentMode.value
        if (mode != Mode.CAMERA && mode != Mode.ATTENDANCE) return
        if (!_isCaptureSessionActive.value && _capturedFaces.value.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val copyBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val copyEmbedding = embedding.copyOf()

        if (_isCaptureSessionActive.value) {
            if (burstFaceId == faceId && burstRemaining > 0) {
                if (now - lastBurstCaptureTimeMs < BURST_DELAY_MS) return
                lastBurstCaptureTimeMs = now
                addCapturedSample(copyBitmap, copyEmbedding)
                burstRemaining--
                if (burstRemaining <= 0) {
                    finishBurstCapture()
                }
            }
            return
        }

        _isCaptureSessionActive.value = true
        burstFaceId = faceId
        burstRemaining = BURST_COUNT - 1
        lastBurstCaptureTimeMs = now
        _captureSessionTargetName.value = stableName
        _captureTrainName.value = if (stableName == "Unknown") "" else stableName
        addCapturedSample(copyBitmap, copyEmbedding)
        if (burstRemaining <= 0) {
            finishBurstCapture()
        }
    }

    private fun addCapturedSample(bitmap: Bitmap, embedding: FloatArray) {
        val sample = EnrollmentSample(bitmap = bitmap, embedding = embedding)
        _capturedFaces.value = _capturedFaces.value + sample
    }

    private fun finishBurstCapture() {
        _isCaptureSessionActive.value = false
        burstFaceId = null
        burstRemaining = 0
        lastBurstCaptureTimeMs = 0L
    }

    fun removeCapturedFace(id: String) {
        _capturedFaces.value = _capturedFaces.value.filterNot { it.id == id }
        if (_capturedFaces.value.isEmpty()) {
            resetCaptureSession()
        }
    }

    fun maybeAutoCapture(faceId: String, stableName: String, bitmap: Bitmap, embedding: FloatArray) {
        if (!autoCaptureEnabled.value) return
        val dev = developerName.value.trim()
        if (dev.isBlank()) return
        if (stableName != dev) return

        val now = System.currentTimeMillis()
        val last = lastAutoCaptureTimes[faceId] ?: 0L
        val intervalMs = (autoCaptureInterval.value.coerceAtLeast(1) * 1000L)
        if (now - last < intervalMs) return
        lastAutoCaptureTimes[faceId] = now

        try {
            val copyBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            val copyEmbedding = embedding.copyOf()
            addCapturedSample(copyBitmap, copyEmbedding)
        } catch (e: Exception) {
            Log.w(TAG, "maybeAutoCapture failed", e)
        }
    }

    fun discardCapturedFaces() {
        resetCaptureSession()
        _showCaptureReview.value = false
    }

    fun openCaptureReview() {
        if (_capturedFaces.value.isNotEmpty()) {
            _showCaptureReview.value = true
        }
    }

    fun closeCaptureReview() {
        _showCaptureReview.value = false
    }

    fun toggleCamera() {
        _isFrontCamera.value = !_isFrontCamera.value
        exitImageMode()
    }

    fun enterImageMode() {
        _isImageMode.value = true
        updateFaces(emptyList())
        faceStabilizer.clear()
    }

    fun exitImageMode() {
        _isImageMode.value = false
        updateFaces(emptyList())
        faceStabilizer.clear()
    }

    fun refresh() {
        updateFaces(emptyList())
        faceStabilizer.clear()
        lastDetectedBitmap = null
        _registerCandidates.value = emptyList()
    }

    fun setCaptureTrainName(name: String) {
        _captureTrainName.value = name
    }

    fun enrollCapturedFaceInSession(sampleId: String, name: String, context: Context) {
        val sample = _capturedFaces.value.find { it.id == sampleId } ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                faceRecognitionHelper.addFaceWithBitmap(name, sample.bitmap, sample.embedding)
            }
            refreshKnownNames()

            // Add to attendance session as present
            val session = _attendanceSession.value
            if (session != null && name !in session.presentNames) {
                val now = System.currentTimeMillis()
                _attendancePresent.value = _attendancePresent.value + name
                _attendanceTimestamps.value = _attendanceTimestamps.value + (name to now)
                val updated = session.copy(
                    presentNames = session.presentNames + name,
                    timestamps = session.timestamps + (name to now)
                )
                _attendanceSession.value = updated
                attendanceStorage.save(updated)
            }

            // Remove from captured list
            _capturedFaces.value = _capturedFaces.value.filterNot { it.id == sampleId }
            Toast.makeText(context, "Enrolled $name", Toast.LENGTH_SHORT).show()
        }
    }

    fun dismissCapturedFace(sampleId: String) {
        _capturedFaces.value = _capturedFaces.value.filterNot { it.id == sampleId }
    }

    fun commitCapturedFaces(context: Context) {
        val samples = _capturedFaces.value
        if (samples.isEmpty()) return

        val target = _captureSessionTargetName.value
        val name = when {
            target != null && target != "Unknown" -> target
            else -> _captureTrainName.value.trim()
        }
        if (name.isBlank()) {
            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
            return
        }
        if (target == "Unknown" && faceRecognitionHelper.getAllKnownNames().contains(name)) {
            Toast.makeText(context, "Name already exists", Toast.LENGTH_SHORT).show()
            return
        }

        viewModelScope.launch {
            _isCommitting.value = true
            withContext(Dispatchers.Default) {
                samples.forEach { sample ->
                    faceRecognitionHelper.addFaceWithBitmap(name, sample.bitmap, sample.embedding)
                }
            }
            refreshKnownNames()
            faceStabilizer.clear()
            resetCaptureSession()
            _showCaptureReview.value = false
            _isCommitting.value = false
            Toast.makeText(
                context,
                "Trained $name with ${samples.size} sample(s)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun resetCaptureSession() {
        _capturedFaces.value = emptyList()
        _captureSessionTargetName.value = null
        _captureTrainName.value = ""
        _isCaptureSessionActive.value = false
        burstFaceId = null
        burstRemaining = 0
        lastBurstCaptureTimeMs = 0L
    }

    fun navigateToPersonDetail(name: String) {
        _selectedPerson.value = name
        pushScreen(AppScreen.PERSON_DETAIL)
        _personSamples.value = faceRecognitionHelper.getSamplesForName(name)
    }

    fun refreshKnownNames() {
        _knownNames.value = faceRecognitionHelper.getKnownNames()
    }

    private fun refreshPersonSamples() {
        val name = _selectedPerson.value ?: return
        _personSamples.value = faceRecognitionHelper.getSamplesForName(name)
    }

    fun updateLastDetectedBitmap(bitmap: Bitmap) {
        lastDetectedBitmap = bitmap
    }

    fun clearLastDetectedBitmap() {
        if (_currentMode.value != Mode.CAMERA) {
            lastDetectedBitmap = null
        }
    }

    fun requestEnrollUnknown(faceId: String) {
        if (_showUnknownDialog.value) return
        val face = _faces.value.find { it.id == faceId && it.name == "Unknown" } ?: return
        val bitmap = face.enrollmentBitmap ?: return
        val embedding = face.enrollmentEmbedding ?: return

        pendingUnknownBitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
        pendingUnknownEmbedding = embedding.copyOf()
        _unknownFaceBitmap.value = pendingUnknownBitmap
        _unknownPromptName.value = ""
        _showUnknownDialog.value = true
    }

    fun setUnknownPromptName(name: String) {
        _unknownPromptName.value = name
    }

    fun enrollUnknownFace(context: Context) {
        val name = _unknownPromptName.value.trim()
        if (name.isBlank()) {
            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
            return
        }
        val bitmap = pendingUnknownBitmap
        val embedding = pendingUnknownEmbedding
        if (bitmap == null || embedding == null) {
            dismissUnknownPrompt()
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                faceRecognitionHelper.addFaceWithBitmap(name, bitmap, embedding)
            }
            refreshKnownNames()
            faceStabilizer.clear()
            if (_currentMode.value == Mode.SCAN && name !in _pendingClassRoster.value) {
                _pendingClassRoster.value = _pendingClassRoster.value + name
            }
            // Late student: add to active attendance session during review
            val session = _attendanceSession.value
            if (session != null && name !in session.presentNames && name !in session.absentNames) {
                val now = System.currentTimeMillis()
                _attendancePresent.value = _attendancePresent.value + name
                _attendanceTimestamps.value = _attendanceTimestamps.value + (name to now)
                val updated = session.copy(
                    presentNames = session.presentNames + name,
                    timestamps = session.timestamps + (name to now)
                )
                _attendanceSession.value = updated
                attendanceStorage.save(updated)
            }
            Toast.makeText(context, "Enrolled $name", Toast.LENGTH_SHORT).show()
            dismissUnknownPrompt()
        }
    }

    fun dismissUnknownPrompt() {
        _showUnknownDialog.value = false
        _unknownPromptName.value = ""
        _unknownFaceBitmap.value = null
        pendingUnknownBitmap = null
        pendingUnknownEmbedding = null
    }

    fun updateRegisterCandidates(bitmaps: List<Bitmap>) {
        _registerCandidates.value = bitmaps
    }

    fun addSelectedCandidatesToEnrollment(context: Context, selectedIndices: Set<Int>) {
        val candidates = _registerCandidates.value
        if (selectedIndices.isEmpty() || candidates.isEmpty()) {
            Toast.makeText(context, "No faces selected", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            var added = 0
            for (index in selectedIndices.sorted()) {
                val bitmap = candidates.getOrNull(index) ?: continue
                val copy = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
                val embedding = withContext(Dispatchers.Default) {
                    faceRecognitionHelper.getEmbedding(copy)
                }
                val sample = EnrollmentSample(bitmap = copy, embedding = embedding)
                _enrollmentSamples.value = _enrollmentSamples.value + sample
                added++
            }
            Toast.makeText(
                context,
                "Added $added face(s)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun addEnrollmentSample(context: Context) {
        val bitmap = lastDetectedBitmap
        if (bitmap == null) {
            Toast.makeText(context, "No face detected", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val embedding = withContext(Dispatchers.Default) {
                faceRecognitionHelper.getEmbedding(bitmap)
            }
            val sample = EnrollmentSample(
                bitmap = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true),
                embedding = embedding
            )
            _enrollmentSamples.value = _enrollmentSamples.value + sample
            Toast.makeText(context, "Face captured (${_enrollmentSamples.value.size})", Toast.LENGTH_SHORT).show()
        }
    }

    fun removeEnrollmentSample(id: String) {
        _enrollmentSamples.value = _enrollmentSamples.value.filterNot { it.id == id }
    }

    fun commitEnrollment(context: Context) {
        val name = _registerName.value.trim()
        if (name.isBlank()) {
            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
            return
        }
        val samples = _enrollmentSamples.value
        if (samples.isEmpty()) {
            Toast.makeText(context, "Capture at least one face", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                samples.forEach { sample ->
                    faceRecognitionHelper.addFaceWithBitmap(name, sample.bitmap, sample.embedding)
                }
            }
            refreshKnownNames()
            Toast.makeText(
                context,
                "Registered $name with ${samples.size} sample(s)",
                Toast.LENGTH_SHORT
            ).show()
            _enrollmentSamples.value = emptyList()
            _registerName.value = ""
            lastDetectedBitmap = null
        }
    }

    fun removeStoredSample(sampleId: String, context: Context) {
        faceRecognitionHelper.removeSample(sampleId)
        refreshKnownNames()
        refreshPersonSamples()
        Toast.makeText(context, "Sample removed", Toast.LENGTH_SHORT).show()
    }

    fun removePerson(name: String, context: Context) {
        faceRecognitionHelper.removePerson(name)
        refreshKnownNames()
        if (_selectedPerson.value == name) {
            navigateToManageFaces()
        }
        Toast.makeText(context, "Removed $name", Toast.LENGTH_SHORT).show()
    }

    fun renamePerson(oldName: String, newName: String, context: Context) {
        val trimmedNew = newName.trim()
        if (trimmedNew.isBlank()) {
            Toast.makeText(context, "Please enter a name", Toast.LENGTH_SHORT).show()
            return
        }
        if (oldName == trimmedNew) return
        if (faceRecognitionHelper.getAllKnownNames().contains(trimmedNew)) {
            Toast.makeText(context, "Name already exists", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                faceRecognitionHelper.renamePerson(oldName, trimmedNew)
            }
            refreshKnownNames()
            if (_selectedPerson.value == oldName) {
                _selectedPerson.value = trimmedNew
                refreshPersonSamples()
            }
            _faces.value = _faces.value.map { face ->
                if (face.name == oldName) face.copy(name = trimmedNew) else face
            }
            if (_registerName.value == oldName) {
                _registerName.value = trimmedNew
            }
            faceStabilizer.clear()
            Toast.makeText(context, "Renamed to $trimmedNew", Toast.LENGTH_SHORT).show()
        }
    }

    fun startAddSampleForPerson(name: String) {
        _registerName.value = name
        _enrollmentSamples.value = emptyList()
        _currentMode.value = Mode.REGISTER
        _currentScreen.value = AppScreen.MAIN
        lastDetectedBitmap = null
    }

    fun addPersonFromImage(
        personName: String,
        bitmap: Bitmap,
        faceDetector: FaceDetector,
        context: Context
    ) {
        viewModelScope.launch {
            try {
                val faces = withContext(Dispatchers.Default) {
                    detectFacesInBitmap(bitmap, faceDetector)
                }
                if (faces.isEmpty()) {
                    Toast.makeText(context, "No faces found in image", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                var added = 0
                withContext(Dispatchers.Default) {
                    for (face in faces) {
                        val enrollmentBitmap = face.enrollmentBitmap ?: continue
                        val embedding = face.enrollmentEmbedding ?: continue
                        faceRecognitionHelper.addFaceWithBitmap(personName, enrollmentBitmap, embedding)
                        added++
                    }
                }
                refreshKnownNames()
                if (_selectedPerson.value == personName) {
                    _personSamples.value = faceRecognitionHelper.getSamplesForName(personName)
                }
                Toast.makeText(
                    context,
                    "Added $added face(s) for $personName",
                    Toast.LENGTH_SHORT
                ).show()
            } catch (e: Exception) {
                Log.e(TAG, "addPersonFromImage failed", e)
                Toast.makeText(context, "Failed to add faces from image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun detectFacesInBitmap(bitmap: Bitmap, faceDetector: FaceDetector): List<FaceData> {
        return suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            faceDetector.process(inputImage)
                .addOnSuccessListener { detectedFaces ->
                    val faceList = mutableListOf<FaceData>()
                    for (face in detectedFaces) {
                        try {
                            val bounds = face.boundingBox
                            val left = bounds.left.coerceIn(0, bitmap.width - 1)
                            val top = bounds.top.coerceIn(0, bitmap.height - 1)
                            val width = (bounds.right - bounds.left).coerceIn(1, bitmap.width - left)
                            val height = (bounds.bottom - bounds.top).coerceIn(1, bitmap.height - top)
                            val faceBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)

                            val embedding = faceRecognitionHelper.getEmbedding(faceBitmap)
                            val result = faceRecognitionHelper.recognize(embedding)
                            val faceId = FaceData.stableId(bounds)
                            val stableName = stabilizeRecognition(faceId, result)

                            if (stableName == "Unknown") {
                                faceList.add(
                                    FaceData(
                                        bounds = bounds,
                                        name = "Unknown",
                                        id = faceId,
                                        enrollmentBitmap = faceBitmap.copy(
                                            faceBitmap.config ?: Bitmap.Config.ARGB_8888, true
                                        ),
                                        enrollmentEmbedding = embedding.copyOf()
                                    )
                                )
                            } else {
                                faceList.add(FaceData(bounds = bounds, name = stableName, id = faceId))
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Face detection on frame failed", e)
                        }
                    }
                    continuation.resume(faceList)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Face detection failed on bitmap", e)
                    continuation.resume(emptyList())
                }
        }
    }

    fun refreshClasses() {
        _classes.value = classStorage.loadAll()
    }

    fun getSampleCount(name: String): Int {
        return faceRecognitionHelper.getKnownFaceCount(name)
    }

    fun getClassAttendanceRate(classId: String): String {
        val sessions = attendanceStorage.loadForClass(classId)
        if (sessions.isEmpty()) return "No sessions"
        val rates = sessions.map { s ->
            val total = s.presentNames.size + s.absentNames.size
            if (total > 0) s.presentNames.size.toFloat() / total else 0f
        }
        val avg = rates.sum() / rates.size
        return "${String.format("%.0f", avg * 100)}% (${sessions.size} sessions)"
    }

    fun importRosterFromClass(className: String, text: String, existingClassId: String? = null) {
        val names = text.lines()
            .flatMap { it.split(",", ";", "\t") }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (names.isEmpty()) return
        val id = existingClassId ?: java.util.UUID.randomUUID().toString()
        val existing = classStorage.loadAll().find { it.id == id }
        val merged = if (existing != null) {
            (existing.studentNames + names).distinct()
        } else {
            names
        }
        classStorage.save(ClassGroup(id = id, name = className, studentNames = merged.sorted()))
        refreshClasses()
    }

    fun startRosterFaceEnrollment(className: String, names: List<String>, existingClassId: String? = null) {
        _rosterEnrollmentNames.value = names
        _rosterEnrollmentIndex.value = 0
        _rosterEnrollmentClassInfo.value = Pair(className, existingClassId)
        _currentMode.value = Mode.SCAN
        pushScreen(AppScreen.MAIN)
        updateFaces(emptyList())
        faceStabilizer.clear()
    }

    fun confirmCurrentRosterStudent() {
        val face = _latestRosterFace.value ?: return
        val index = _rosterEnrollmentIndex.value
        val names = _rosterEnrollmentNames.value
        if (index < 0 || index >= names.size) return
        val name = names[index]

        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                faceRecognitionHelper.addFaceWithBitmap(name, face.first, face.second)
            }
            refreshKnownNames()
            _latestRosterFace.value = null
            advanceRosterEnrollment()
        }
    }

    fun skipCurrentRosterStudent() {
        advanceRosterEnrollment()
    }

    private fun advanceRosterEnrollment() {
        val nextIndex = _rosterEnrollmentIndex.value + 1
        if (nextIndex >= _rosterEnrollmentNames.value.size) {
            finishRosterEnrollment()
        } else {
            _rosterEnrollmentIndex.value = nextIndex
            updateFaces(emptyList())
            faceStabilizer.clear()
        }
    }

    fun finishRosterEnrollment() {
        _rosterEnrollmentNames.value = emptyList()
        _rosterEnrollmentIndex.value = -1
        _rosterEnrollmentClassInfo.value = null
        _currentMode.value = Mode.CAMERA
        navigateBack()
        updateFaces(emptyList())
        faceStabilizer.clear()
    }

    fun getCurrentRosterStudentName(): String? {
        val index = _rosterEnrollmentIndex.value
        val names = _rosterEnrollmentNames.value
        return if (index in names.indices) names[index] else null
    }

    fun updateLatestRosterFace(bitmap: Bitmap, embedding: FloatArray) {
        _latestRosterFace.value = Pair(bitmap, embedding)
    }

    fun saveClass(group: ClassGroup) {
        classStorage.save(group)
        refreshClasses()
    }

    fun deleteClass(classId: String) {
        classStorage.delete(classId)
        refreshClasses()
    }

    fun removeStudentFromClass(classId: String, studentName: String) {
        val classes = classStorage.loadAll()
        val group = classes.find { it.id == classId } ?: return
        val updated = group.copy(studentNames = group.studentNames.filter { it != studentName })
        classStorage.save(updated)
        refreshClasses()
    }

    fun renameStudentInClass(classId: String, oldName: String, newName: String) {
        if (newName.isBlank() || oldName == newName) return
        val classes = classStorage.loadAll()
        val group = classes.find { it.id == classId } ?: return
        if (newName in group.studentNames) return
        val updated = group.copy(
            studentNames = group.studentNames.map { if (it == oldName) newName else it }
        )
        classStorage.save(updated)
        refreshClasses()
    }

    fun clearAttendanceHistory(classId: String) {
        val sessions = attendanceStorage.loadForClass(classId)
        sessions.forEach { attendanceStorage.delete(it.id) }
        _classHistory.value = emptyList()
    }

    fun clearAllAttendanceHistory() {
        val all = attendanceStorage.loadAll()
        all.forEach { attendanceStorage.delete(it.id) }
        _classHistory.value = emptyList()
    }

    fun navigateToSettings() {
        pushScreen(AppScreen.SETTINGS)
    }

    fun deleteAllData() {
        faceRecognitionHelper.getAllKnownNames().forEach { name ->
            faceRecognitionHelper.removePerson(name)
        }
        refreshKnownNames()
        classStorage.loadAll().forEach { classStorage.delete(it.id) }
        refreshClasses()
        clearAllAttendanceHistory()
        updateFaces(emptyList())
        faceStabilizer.clear()
    }

    fun navigateToClassList() {
        refreshClasses()
        pushScreen(AppScreen.CLASS_LIST)
    }

    fun startAttendanceSession(classGroup: ClassGroup) {
        _selectedClass.value = classGroup
        _attendancePresent.value = emptySet()
        _attendanceTimestamps.value = emptyMap()
        _currentMode.value = Mode.ATTENDANCE
        pushScreen(AppScreen.ATTENDANCE_SESSION)
        updateFaces(emptyList())
        faceStabilizer.clear()
    }

    fun markAttendanceRecognized(name: String) {
        val roster = _selectedClass.value?.studentNames ?: return
        if (name !in roster) return
        if (name in _attendancePresent.value) return
        _attendancePresent.value = _attendancePresent.value + name
        _attendanceTimestamps.value = _attendanceTimestamps.value + (name to System.currentTimeMillis())
        _hapticTrigger.value = _hapticTrigger.value + 1
    }

    fun finalizeAttendanceSession() {
        val classGroup = _selectedClass.value ?: return
        val present = _attendancePresent.value
        val absent = classGroup.studentNames.toSet() - present
        val session = AttendanceSession(
            classId = classGroup.id,
            className = classGroup.name,
            dateMs = System.currentTimeMillis(),
            presentNames = present,
            absentNames = absent,
            timestamps = _attendanceTimestamps.value
        )
        attendanceStorage.save(session)
        _attendanceSession.value = session
        pushScreen(AppScreen.ATTENDANCE_REVIEW)
    }

    fun toggleAttendanceStatus(name: String) {
        val session = _attendanceSession.value ?: return
        val now = System.currentTimeMillis()
        val updatedTimestamps = session.timestamps + (name to now)
        if (name in session.presentNames) {
            val updated = session.copy(
                presentNames = session.presentNames - name,
                absentNames = session.absentNames + name,
                timestamps = updatedTimestamps
            )
            _attendanceSession.value = updated
            attendanceStorage.save(updated)
        } else {
            val updated = session.copy(
                presentNames = session.presentNames + name,
                absentNames = session.absentNames - name,
                timestamps = updatedTimestamps
            )
            _attendanceSession.value = updated
            attendanceStorage.save(updated)
        }
    }

    fun loadClassHistory(classId: String) {
        _classHistory.value = attendanceStorage.loadForClass(classId)
    }

    fun resumeAttendanceForLateStudent() {
        _currentMode.value = Mode.ATTENDANCE
        navigateBack()
        updateFaces(emptyList())
        faceStabilizer.clear()
    }

    fun exitAttendance() {
        _selectedClass.value = null
        _attendancePresent.value = emptySet()
        _attendanceTimestamps.value = emptyMap()
        _attendanceSession.value = null
        _currentMode.value = Mode.CAMERA
        backStack.clear()
        _currentScreen.value = AppScreen.MAIN
        updateFaces(emptyList())
        faceStabilizer.clear()
    }

    fun startScanForClass(className: String, existingClassId: String? = null, existingStudents: List<String> = emptyList()) {
        _pendingClassName.value = className
        _pendingClassRoster.value = existingStudents
        pendingClassId = existingClassId
        _currentMode.value = Mode.SCAN
        pushScreen(AppScreen.MAIN)
        updateFaces(emptyList())
        faceStabilizer.clear()
    }

    fun saveScannedClass() {
        val name = _pendingClassName.value.trim()
        val roster = _pendingClassRoster.value
        if (name.isBlank() || roster.isEmpty()) return
        val id = pendingClassId ?: java.util.UUID.randomUUID().toString()
        classStorage.save(ClassGroup(id = id, name = name, studentNames = roster.sorted()))
        refreshClasses()
        exitScanMode()
    }

    fun removePendingStudent(name: String) {
        _pendingClassRoster.value = _pendingClassRoster.value.filter { it != name }
    }

    fun exitScanMode() {
        _pendingClassName.value = ""
        _pendingClassRoster.value = emptyList()
        _pendingScanCandidates.value = emptyList()
        pendingClassId = null
        _currentMode.value = Mode.CAMERA
        navigateBack()
        updateFaces(emptyList())
        faceStabilizer.clear()
    }

    fun processImageForClass(className: String, bitmap: Bitmap, faceDetector: FaceDetector, existingClassId: String? = null, existingStudents: List<String> = emptyList()) {
        _pendingClassName.value = className
        _pendingClassRoster.value = existingStudents
        pendingClassId = existingClassId
        viewModelScope.launch {
            val faces = withContext(Dispatchers.Default) {
                suspendCancellableCoroutine { continuation ->
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    faceDetector.process(inputImage)
                        .addOnSuccessListener { detectedFaces ->
                            val crops = mutableListOf<Bitmap>()
                            for (face in detectedFaces) {
                                val bounds = face.boundingBox
                                val left = bounds.left.coerceIn(0, bitmap.width - 1)
                                val top = bounds.top.coerceIn(0, bitmap.height - 1)
                                val w = (bounds.right - bounds.left).coerceIn(1, bitmap.width - left)
                                val h = (bounds.bottom - bounds.top).coerceIn(1, bitmap.height - top)
                                try {
                                    crops.add(Bitmap.createBitmap(bitmap, left, top, w, h))
                                } catch (_: Exception) {}
                            }
                            continuation.resume(crops)
                        }
                        .addOnFailureListener { continuation.resume(emptyList<Bitmap>()) }
                }
            }
            _pendingScanCandidates.value = faces
            if (faces.isEmpty()) {
                Toast.makeText(getApplication(), "No faces found in image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun enrollCandidateAsStudent(index: Int, name: String, context: Context) {
        val candidates = _pendingScanCandidates.value
        val bitmap = candidates.getOrNull(index) ?: return
        if (name.isBlank()) return
        if (name in _pendingClassRoster.value) {
            Toast.makeText(context, "Name already in class", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch {
            val copy = bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, true)
            val embedding = withContext(Dispatchers.Default) {
                faceRecognitionHelper.getEmbedding(copy)
            }
            faceRecognitionHelper.addFaceWithBitmap(name, copy, embedding)
            refreshKnownNames()
            _pendingClassRoster.value = _pendingClassRoster.value + name
            _pendingScanCandidates.value = candidates.toMutableList().apply { removeAt(index) }
            Toast.makeText(context, "Added $name", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCleared() {
        super.onCleared()
        faceRecognitionHelper.close()
    }

    companion object {
        private const val TAG = "MainViewModel"
        private const val BURST_COUNT = 4
        private const val BURST_DELAY_MS = 400L

        private fun isSystemDarkTheme(application: Application): Boolean {
            val nightMode = application.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return nightMode == Configuration.UI_MODE_NIGHT_YES
        }
    }
}
