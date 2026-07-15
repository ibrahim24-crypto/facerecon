package com.himo.facerecon

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

private const val TAG = "MainScreen"
private val isProcessing = AtomicInteger(0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    faceDetector: FaceDetector,
    cameraExecutor: ExecutorService,
    onCameraProviderReady: (ProcessCameraProvider) -> Unit,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val faceRecognitionHelper = viewModel.faceRecognitionHelper

    val faces by viewModel.faces.collectAsStateWithLifecycle()
    val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
    val registerName by viewModel.registerName.collectAsStateWithLifecycle()
    val enrollmentSamples by viewModel.enrollmentSamples.collectAsStateWithLifecycle()
    val analysisWidth by viewModel.analysisWidth.collectAsStateWithLifecycle()
    val analysisHeight by viewModel.analysisHeight.collectAsStateWithLifecycle()
    val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
    val capturedFaces by viewModel.capturedFaces.collectAsStateWithLifecycle()
    val showCaptureReview by viewModel.showCaptureReview.collectAsStateWithLifecycle()
    val isFrontCamera by viewModel.isFrontCamera.collectAsStateWithLifecycle()
    val isImageMode by viewModel.isImageMode.collectAsStateWithLifecycle()
    val registerCandidates by viewModel.registerCandidates.collectAsStateWithLifecycle()
    val selectedClass by viewModel.selectedClass.collectAsStateWithLifecycle()
    val attendancePresent by viewModel.attendancePresent.collectAsStateWithLifecycle()
    val rosterEnrollmentIndex by viewModel.rosterEnrollmentIndex.collectAsStateWithLifecycle()
    val rosterEnrollmentNames by viewModel.rosterEnrollmentNames.collectAsStateWithLifecycle()
    val latestRosterFace by viewModel.latestRosterFace.collectAsStateWithLifecycle()
    val pendingClassName by viewModel.pendingClassName.collectAsStateWithLifecycle()
    val pendingClassRoster by viewModel.pendingClassRoster.collectAsStateWithLifecycle()

    var pickedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = loadAndOrientBitmap(context, uri)
                if (bitmap != null) {
                    pickedImageBitmap = bitmap
                    viewModel.enterImageMode()
                    processPickedImage(bitmap, faceDetector, faceRecognitionHelper, viewModel)
                } else {
                    Toast.makeText(context, context.getString(R.string.image_load_error), Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load picked image", e)
                Toast.makeText(context, context.getString(R.string.image_load_error), Toast.LENGTH_SHORT).show()
            }
        }
    }

    val previewView = remember {
        PreviewView(context).apply {
            // COMPATIBLE mode often avoids emulator/GL artefacts; leave PERFORMANCE for real devices.
            try {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            } catch (_: Exception) {}
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    LaunchedEffect(lifecycleOwner, isFrontCamera) {
        val cameraProvider = suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                { continuation.resume(future.get()) },
                ContextCompat.getMainExecutor(context)
            )
        }
        onCameraProviderReady(cameraProvider)

        val cameraSelector = if (viewModel.isFrontCamera.value) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        bindCameraUseCases(
            cameraProvider = cameraProvider,
            lifecycleOwner = lifecycleOwner,
            previewView = previewView,
            cameraSelector = cameraSelector,
            cameraExecutor = cameraExecutor,
            faceDetector = faceDetector,
            faceRecognitionHelper = faceRecognitionHelper,
            viewModel = viewModel
        )
    }

    val drawerState = androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    androidx.compose.material3.ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentMode != Mode.ATTENDANCE && currentMode != Mode.SCAN,
        drawerContent = {
            androidx.compose.material3.ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.app_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    androidx.compose.material3.NavigationDrawerItem(
                        icon = { Icon(Icons.Default.School, contentDescription = null) },
                        label = { Text("Classes") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.navigateToClassList()
                        }
                    )
                    androidx.compose.material3.NavigationDrawerItem(
                        icon = { Icon(Icons.Default.People, contentDescription = null) },
                        label = { Text(stringResource(R.string.manage_faces)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.navigateToManageFaces()
                        }
                    )
                    androidx.compose.material3.Divider(modifier = Modifier.padding(vertical = 8.dp))
                    androidx.compose.material3.NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Image, contentDescription = null) },
                        label = { Text(stringResource(R.string.pick_image)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            imagePickerLauncher.launch("image/*")
                        }
                    )
                    androidx.compose.material3.NavigationDrawerItem(
                        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_register)) },
                        selected = currentMode == Mode.REGISTER,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.setMode(Mode.REGISTER)
                        }
                    )
                    androidx.compose.material3.NavigationDrawerItem(
                        icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_camera)) },
                        selected = currentMode == Mode.CAMERA,
                        onClick = {
                            scope.launch { drawerState.close() }
                            viewModel.setMode(Mode.CAMERA)
                            viewModel.exitImageMode()
                        }
                    )
                }
            }
        }
    ) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::navigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (currentMode != Mode.ATTENDANCE && currentMode != Mode.SCAN) {
                PrimaryTabRow(
                    selectedTabIndex = if (currentMode == Mode.CAMERA) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = currentMode == Mode.CAMERA,
                    onClick = { viewModel.setMode(Mode.CAMERA) },
                    text = { Text(stringResource(R.string.tab_camera)) }
                )
                Tab(
                    selected = currentMode == Mode.REGISTER,
                    onClick = { viewModel.setMode(Mode.REGISTER) },
                    text = { Text(stringResource(R.string.tab_register)) }
                )
            }
            }

            Box(modifier = Modifier.weight(1f)) {
                when {
                    isImageMode && pickedImageBitmap != null -> {
                        Image(
                            bitmap = pickedImageBitmap!!.asImageBitmap(),
                            contentDescription = "Picked image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    else -> {
                        AndroidView(
                            factory = { previewView },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                FaceOverlay(
                    faces = faces,
                    analysisWidth = analysisWidth,
                    analysisHeight = analysisHeight,
                    mirrorHorizontally = if (isImageMode) false else isFrontCamera,
                    onUnknownFaceTap = viewModel::requestEnrollUnknown,
                    modifier = Modifier.fillMaxSize(),
                    isRegisterMode = currentMode == Mode.REGISTER || currentMode == Mode.SCAN,
                    showFaceIndex = viewModel.showFaceIndex.collectAsStateWithLifecycle().value
                )

                // Attendance counter overlay
                if (currentMode == Mode.ATTENDANCE && selectedClass != null) {
                    val rosterSize = selectedClass!!.studentNames.size
                    val presentCount = attendancePresent.size
                    val hapticTrigger by viewModel.hapticTrigger.collectAsStateWithLifecycle()
                    var elapsedSeconds by remember { mutableStateOf(0L) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            kotlinx.coroutines.delay(1000)
                            elapsedSeconds++
                        }
                    }
                    LaunchedEffect(hapticTrigger) {
                        if (hapticTrigger > 0) {
                            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                            vibrator?.vibrate(100)
                        }
                    }
                    val minutes = elapsedSeconds / 60
                    val seconds = elapsedSeconds % 60

                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "$presentCount/$rosterSize",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // Roster enrollment guided scan overlay
                if (rosterEnrollmentIndex >= 0 && rosterEnrollmentIndex < rosterEnrollmentNames.size) {
                    val currentName = rosterEnrollmentNames[rosterEnrollmentIndex]
                    val total = rosterEnrollmentNames.size
                    val faceDetected = latestRosterFace != null

                    Card(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Scan: ${rosterEnrollmentIndex + 1} / $total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = currentName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (faceDetected) "Face detected! Confirm or skip." else "Point camera at student...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { viewModel.skipCurrentRosterStudent() },
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Text("Skip")
                                }
                                ElevatedButton(
                                    onClick = { viewModel.confirmCurrentRosterStudent() },
                                    shape = MaterialTheme.shapes.medium,
                                    enabled = faceDetected
                                ) {
                                    Text("Confirm")
                                }
                            }
                        }
                    }

                    ElevatedButton(
                        onClick = { viewModel.finishRosterEnrollment() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Finish Enrollment")
                    }
                }

                if (showCaptureReview) {
                    CaptureReviewSheet(
                        viewModel = viewModel,
                        onDismiss = viewModel::closeCaptureReview,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }

            when (currentMode) {
                Mode.CAMERA -> CameraTabContent(
                    capturedCount = capturedFaces.size,
                    onOpenCaptureReview = viewModel::openCaptureReview,
                    viewModel = viewModel
                )
                Mode.REGISTER -> RegisterTabContent(
                    registerName = registerName,
                    enrollmentSamples = enrollmentSamples,
                    registerCandidates = registerCandidates,
                    onRegisterNameChange = viewModel::setRegisterName,
                    onCaptureSelected = { indices -> viewModel.addSelectedCandidatesToEnrollment(context, indices) },
                    onRemoveSample = viewModel::removeEnrollmentSample,
                    onCommit = { viewModel.commitEnrollment(context) }
                )
                Mode.ATTENDANCE -> AttendanceChecklist(
                    roster = selectedClass?.studentNames ?: emptyList(),
                    presentNames = attendancePresent,
                    onStop = { viewModel.finalizeAttendanceSession() }
                )
                Mode.SCAN -> ScanRosterCard(
                    className = pendingClassName,
                    roster = pendingClassRoster,
                    onSave = { viewModel.saveScannedClass() },
                    onRemove = { viewModel.removePendingStudent(it) }
                )
            }

            if (currentMode != Mode.ATTENDANCE && currentMode != Mode.SCAN) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                if (isImageMode) {
                    Button(
                        onClick = {
                            pickedImageBitmap = null
                            viewModel.exitImageMode()
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(stringResource(R.string.back_to_camera))
                    }
                    OutlinedButton(
                        onClick = {
                            val bmp = pickedImageBitmap
                            if (bmp != null) {
                                viewModel.refresh()
                                processPickedImage(bmp, faceDetector, faceRecognitionHelper, viewModel)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(stringResource(R.string.refresh))
                    }
                } else {
                    ElevatedButton(
                        onClick = viewModel::toggleCamera,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            if (isFrontCamera) stringResource(R.string.front_camera)
                            else stringResource(R.string.rear_camera)
                        )
                    }

                    OutlinedButton(
                        onClick = { viewModel.refresh() },
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(R.string.refresh)
                        )
                    }
                }
            }
            } // end if (!ATTENDANCE) for bottom buttons
        }
    }
    } // end ModalNavigationDrawer
}

@Composable
private fun CameraTabContent(
    capturedCount: Int,
    onOpenCaptureReview: () -> Unit,
    viewModel: MainViewModel
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        UnknownFaceEnrollOverlay(
            viewModel = viewModel
        )
        if (capturedCount > 0) {
            BadgedBox(
                badge = {
                    Badge { Text(capturedCount.toString()) }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(onClick = onOpenCaptureReview) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = stringResource(R.string.review_captured_faces)
                    )
                }
            }
        }
    }
}

@Composable
private fun RegisterTabContent(
    registerName: String,
    enrollmentSamples: List<EnrollmentSample>,
    registerCandidates: List<Bitmap>,
    onRegisterNameChange: (String) -> Unit,
    onCaptureSelected: (Set<Int>) -> Unit,
    onRemoveSample: (String) -> Unit,
    onCommit: () -> Unit
) {
    var selectedCandidates by remember(registerCandidates.size) {
        mutableStateOf((0 until registerCandidates.size).toSet())
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (registerCandidates.isNotEmpty()) {
                    Text(
                        text = "${registerCandidates.size} face(s) detected — tap to select",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(registerCandidates.size) { index ->
                            val isSelected = index in selectedCandidates
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .then(
                                        if (isSelected) Modifier.border(
                                            3.dp,
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.shapes.small
                                        ) else Modifier.border(
                                            1.dp,
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            MaterialTheme.shapes.small
                                        )
                                    )
                                    .clickable {
                                        selectedCandidates = if (isSelected) {
                                            selectedCandidates - index
                                        } else {
                                            selectedCandidates + index
                                        }
                                    }
                            ) {
                                Image(
                                    bitmap = registerCandidates[index].asImageBitmap(),
                                    contentDescription = "Detected face ${index + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (!isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.5f))
                                    )
                                }
                            }
                        }
                    }
                    ElevatedButton(
                        onClick = { onCaptureSelected(selectedCandidates) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        enabled = selectedCandidates.isNotEmpty()
                    ) {
                        Text("Add ${selectedCandidates.size} face(s) to samples")
                    }
                } else {
                    Text(
                        text = "Point camera at face(s) to detect",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                if (enrollmentSamples.isNotEmpty()) {
                    Text(
                        text = "Samples (${enrollmentSamples.size})",
                        style = MaterialTheme.typography.titleSmall
                    )
                    EnrollmentSampleList(
                        samples = enrollmentSamples,
                        onDelete = onRemoveSample,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = registerName,
                    onValueChange = onRegisterNameChange,
                    label = { Text(stringResource(R.string.enter_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedButton(
                    onClick = onCommit,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enrollmentSamples.isNotEmpty() && registerName.isNotBlank(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(R.string.register_face))
                }
            }
        }
    }
}

@Composable
private fun AttendanceChecklist(
    roster: List<String>,
    presentNames: Set<String>,
    onStop: () -> Unit
) {
    val sorted = remember(roster, presentNames) { roster.sorted() }
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${presentNames.size} / ${roster.size} present",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    items(sorted, key = { it }) { name ->
                        val isPresent = name in presentNames
                        Card(
                            shape = MaterialTheme.shapes.small,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPresent) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Text(
                                text = name,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isPresent) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                }
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Stop & Review (${presentNames.size}/${roster.size})")
                }
            }
        }
    }
}

@Composable
private fun ScanRosterCard(
    className: String,
    roster: List<String>,
    onSave: () -> Unit,
    onRemove: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Scanning: $className",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Tap a face to name and add to class (${roster.size} added)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                if (roster.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(roster, key = { it }) { name ->
                            Card(
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    IconButton(
                                        onClick = { onRemove(name) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove",
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                ElevatedButton(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    enabled = roster.isNotEmpty()
                ) {
                    Text("Save Class (${roster.size} students)")
                }
            }
        }
    }
}

private fun processFrame(
    imageProxy: ImageProxy,
    faceDetector: FaceDetector,
    faceRecognitionHelper: FaceRecognitionHelper,
    viewModel: MainViewModel
) {
    if (!isProcessing.compareAndSet(0, 1)) {
        imageProxy.close()
        return
    }

    var analysisStarted = false
    try {
        val rgbaBitmap = extractRgbaBitmap(imageProxy)
        if (rgbaBitmap == null) {
            Log.e(TAG, "Failed to extract RGBA bitmap")
            isProcessing.set(0)
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val uprightBitmap = rotateBitmapForAnalysis(rgbaBitmap, rotationDegrees)
        viewModel.updateAnalysisResolution(uprightBitmap.width, uprightBitmap.height)

        val inputImage = InputImage.fromBitmap(uprightBitmap, 0)
        val mode = viewModel.currentMode.value
        analysisStarted = true

        faceDetector.process(inputImage)
            .addOnSuccessListener { detectedFaces ->
                try {
                    if (detectedFaces.isEmpty()) {
                        viewModel.updateFaces(emptyList())
                        viewModel.pruneFaceTracks(emptySet())
                        viewModel.clearLastDetectedBitmap()
                        isProcessing.set(0)
                        return@addOnSuccessListener
                    }

                    viewModel.launchFrameWork {
                        val faceList = mutableListOf<FaceData>()
                        try {
                            for (face in detectedFaces) {
                                try {
                                    val bounds = face.boundingBox
                                    val faceBitmap = cropBitmap(uprightBitmap, bounds) ?: continue

                                    when (mode) {
                                        Mode.CAMERA, Mode.SCAN -> {
                                            val embedding = faceRecognitionHelper.getEmbedding(faceBitmap)
                                            val result = faceRecognitionHelper.recognize(embedding)
                                            val faceId = FaceData.stableId(bounds)
                                            val stableName = viewModel.stabilizeRecognition(faceId, result)

                                            if (stableName == "Unknown") {
                                                faceList.add(
                                                    FaceData(
                                                        bounds = bounds,
                                                        name = "Unknown",
                                                        id = faceId,
                                                        enrollmentBitmap = faceBitmap.copy(
                                                            faceBitmap.config ?: Bitmap.Config.ARGB_8888,
                                                            true
                                                        ),
                                                        enrollmentEmbedding = embedding.copyOf()
                                                    )
                                                )
                                            } else {
                                                faceList.add(
                                                    FaceData(
                                                        bounds = bounds,
                                                        name = stableName,
                                                        id = faceId,
                                                        confidence = result.bestDistance
                                                    )
                                                )
                                            }

                                            if (detectedFaces.size == 1) {
                                                viewModel.tryCaptureBurstSample(
                                                    faceId = faceId,
                                                    stableName = stableName,
                                                    bitmap = faceBitmap,
                                                    embedding = embedding
                                                )
                                            }

                                            // Auto-capture for developer if enabled
                                            viewModel.maybeAutoCapture(
                                                faceId = faceId,
                                                stableName = stableName,
                                                bitmap = faceBitmap,
                                                embedding = embedding
                                            )

                                            // Capture face for roster enrollment
                                            if (viewModel.rosterEnrollmentIndex.value >= 0) {
                                                viewModel.updateLatestRosterFace(
                                                    faceBitmap.copy(faceBitmap.config ?: Bitmap.Config.ARGB_8888, true),
                                                    embedding.copyOf()
                                                )
                                            }
                                        }
                                        Mode.REGISTER -> {
                                            val candidateBitmaps = mutableListOf<Bitmap>()
                                            for (f in detectedFaces) {
                                                val cropped = cropBitmap(uprightBitmap, f.boundingBox) ?: continue
                                                candidateBitmaps.add(cropped)
                                                faceList.add(
                                                    FaceData(
                                                        bounds = f.boundingBox,
                                                        name = "",
                                                        id = FaceData.stableId(f.boundingBox)
                                                    )
                                                )
                                            }
                                            withContext(Dispatchers.Main) {
                                                viewModel.updateRegisterCandidates(candidateBitmaps)
                                            }
                                        }
                                        Mode.ATTENDANCE -> {
                                            val embedding = faceRecognitionHelper.getEmbedding(faceBitmap)
                                            val result = faceRecognitionHelper.recognize(embedding)
                                            val faceId = FaceData.stableId(bounds)
                                            val stableName = viewModel.stabilizeRecognition(faceId, result)

                                            if (stableName != "Unknown") {
                                                viewModel.markAttendanceRecognized(stableName)
                                            }

                                            faceList.add(
                                                FaceData(
                                                    bounds = bounds,
                                                    name = stableName,
                                                    id = faceId,
                                                    confidence = result.bestDistance
                                                )
                                            )

                                            if (detectedFaces.size == 1 && stableName == "Unknown") {
                                                viewModel.tryCaptureBurstSample(
                                                    faceId = faceId,
                                                    stableName = stableName,
                                                    bitmap = faceBitmap,
                                                    embedding = embedding
                                                )
                                            }

                                            // also allow auto-capture during attendance
                                            viewModel.maybeAutoCapture(
                                                faceId = faceId,
                                                stableName = stableName,
                                                bitmap = faceBitmap,
                                                embedding = embedding
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to process a single face", e)
                                }
                            }
                        } finally {
                            withContext(Dispatchers.Main) {
                                viewModel.pruneFaceTracks(faceList.map { it.id }.toSet())
                                viewModel.updateFaces(faceList)
                            }
                            isProcessing.set(0)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Face detection success handler failed", e)
                    isProcessing.set(0)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Face detection failed", e)
                isProcessing.set(0)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } catch (e: Exception) {
        Log.e(TAG, "Frame processing exception", e)
        isProcessing.set(0)
        if (!analysisStarted) {
            imageProxy.close()
        }
    }
}

private fun extractRgbaBitmap(imageProxy: ImageProxy): Bitmap? {
    return try {
        val planes = imageProxy.planes
        if (planes.isEmpty()) return null

        val buffer = planes[0].buffer
        buffer.rewind()
        val rowStride = planes[0].rowStride
        val pixelStride = planes[0].pixelStride
        val rowPadding = rowStride - pixelStride * imageProxy.width
        val bitmapWidth = imageProxy.width + rowPadding / pixelStride
        val bitmap = createBitmap(bitmapWidth, imageProxy.height)
        bitmap.copyPixelsFromBuffer(buffer)
        bitmap
    } catch (e: Exception) {
        Log.e(TAG, "RGBA extraction failed", e)
        null
    }
}

private fun rotateBitmapForAnalysis(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun cropBitmap(bitmap: Bitmap, bounds: android.graphics.Rect): Bitmap? {
    return try {
        val left = bounds.left.coerceIn(0, bitmap.width - 1)
        val top = bounds.top.coerceIn(0, bitmap.height - 1)
        val width = (bounds.right - bounds.left).coerceIn(1, bitmap.width - left)
        val height = (bounds.bottom - bounds.top).coerceIn(1, bitmap.height - top)

        Bitmap.createBitmap(bitmap, left, top, width, height)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to crop bitmap", e)
        null
    }
}

private fun bindCameraUseCases(
    cameraProvider: ProcessCameraProvider,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    cameraSelector: CameraSelector,
    cameraExecutor: ExecutorService,
    faceDetector: FaceDetector,
    faceRecognitionHelper: FaceRecognitionHelper,
    viewModel: MainViewModel
) {
    // Ensure preview resolution matches analysis to avoid low-resolution pixelated preview on some devices/AVDs
    val rotation = previewView.display?.rotation ?: Surface.ROTATION_0

    val preview = Preview.Builder()
        .setTargetResolution(Size(720, 1280))
        .setTargetRotation(rotation)
        .build()
        .also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

    @Suppress("DEPRECATION")
    val imageAnalyzer = ImageAnalysis.Builder()
        .setTargetResolution(Size(720, 1280))
        .setTargetRotation(rotation)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .build()
        .also { analysis ->
            analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                processFrame(
                    imageProxy = imageProxy,
                    faceDetector = faceDetector,
                    faceRecognitionHelper = faceRecognitionHelper,
                    viewModel = viewModel
                )
            }
        }

    try {
        // Some devices/AVDs may not report all lenses; prefer checking availability before binding.
        var selectorToUse = cameraSelector
        try {
            if (!cameraProvider.hasCamera(selectorToUse)) {
                Log.w(TAG, "Requested camera not available, trying alternate")
                val alternate = if (selectorToUse == CameraSelector.DEFAULT_FRONT_CAMERA) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
                if (cameraProvider.hasCamera(alternate)) {
                    // update viewModel toggle so UI reflects actual camera
                    viewModel.toggleCamera()
                    selectorToUse = alternate
                    Log.i(TAG, "Switched camera selector to alternate facing to match device")
                } else {
                    Log.e(TAG, "No suitable camera found on device")
                    return
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "hasCamera check failed, proceeding to bind and letting bind throw if incompatible", e)
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            selectorToUse,
            preview,
            imageAnalyzer
        )
    } catch (e: Exception) {
        Log.e(TAG, "Failed to bind camera", e)
    }
}

private fun processPickedImage(
    bitmap: Bitmap,
    faceDetector: FaceDetector,
    faceRecognitionHelper: FaceRecognitionHelper,
    viewModel: MainViewModel
) {
    viewModel.updateAnalysisResolution(bitmap.width, bitmap.height)
    val inputImage = InputImage.fromBitmap(bitmap, 0)
    Log.d(TAG, "processPickedImage: ${bitmap.width}x${bitmap.height}")

    val mode = viewModel.currentMode.value

    faceDetector.process(inputImage)
        .addOnSuccessListener { detectedFaces ->
            Log.d(TAG, "Picked image: detected ${detectedFaces.size} face(s)")
            viewModel.launchFrameWork {
                val faceList = mutableListOf<FaceData>()
                try {
                    if (mode == Mode.REGISTER) {
                        val candidateBitmaps = mutableListOf<Bitmap>()
                        for (face in detectedFaces) {
                            val cropped = cropBitmap(bitmap, face.boundingBox) ?: continue
                            candidateBitmaps.add(cropped)
                            faceList.add(
                                FaceData(
                                    bounds = face.boundingBox,
                                    name = "",
                                    id = FaceData.stableId(face.boundingBox)
                                )
                            )
                        }
                        withContext(Dispatchers.Main) {
                            viewModel.updateRegisterCandidates(candidateBitmaps)
                            viewModel.pruneFaceTracks(faceList.map { it.id }.toSet())
                            viewModel.updateFaces(faceList)
                        }
                    } else {
                        for (face in detectedFaces) {
                            try {
                                val bounds = face.boundingBox
                                val faceBitmap = cropBitmap(bitmap, bounds) ?: continue
                                val embedding = faceRecognitionHelper.getEmbedding(faceBitmap)
                                val result = faceRecognitionHelper.recognize(embedding)
                                val faceId = FaceData.stableId(bounds)
                                val stableName = viewModel.stabilizeRecognition(faceId, result)

                                if (stableName == "Unknown") {
                                    faceList.add(
                                        FaceData(
                                            bounds = bounds,
                                            name = "Unknown",
                                            id = faceId,
                                            enrollmentBitmap = faceBitmap.copy(
                                                faceBitmap.config ?: Bitmap.Config.ARGB_8888,
                                                true
                                            ),
                                            enrollmentEmbedding = embedding.copyOf()
                                        )
                                    )
                                } else {
                                    faceList.add(
                                        FaceData(
                                            bounds = bounds,
                                            name = stableName,
                                            id = faceId
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to process picked image face", e)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            viewModel.pruneFaceTracks(faceList.map { it.id }.toSet())
                            viewModel.updateFaces(faceList)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "processPickedImage failed", e)
                }
            }
        }
        .addOnFailureListener { e ->
            Log.e(TAG, "Face detection on picked image failed", e)
        }
}

internal fun loadAndOrientBitmap(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        val orientation = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        } ?: ExifInterface.ORIENTATION_NORMAL

        val options = BitmapFactory.Options().apply {
            inSampleSize = 1
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }

        val maxDim = maxOf(options.outWidth, options.outHeight)
        if (maxDim > MAX_IMAGE_DIM) {
            var sample = 1
            while (maxDim / sample > MAX_IMAGE_DIM) sample *= 2
            options.inSampleSize = sample
        }
        options.inJustDecodeBounds = false

        val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        } ?: return null

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
        }

        if (matrix.isIdentity) bitmap
        else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } catch (e: Exception) {
        Log.e(TAG, "loadAndOrientBitmap failed", e)
        null
    }
}

private const val MAX_IMAGE_DIM = 1280
