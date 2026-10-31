package com.himo.facerecon

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.Locale
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var faceDetector: FaceDetector
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var cameraProvider: ProcessCameraProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setMinFaceSize(0.15f)
            .build()
        faceDetector = FaceDetection.getClient(options)

        setContent {
            val viewModel: MainViewModel = viewModel()
            val darkTheme by viewModel.darkTheme.collectAsStateWithLifecycle()
            val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
            val currentMode by viewModel.currentMode.collectAsStateWithLifecycle()
            val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
            var showExitDialog by remember { mutableStateOf(false) }

            val context = LocalContext.current
            val localeConfig = remember(currentLanguage) {
                val locale = when (currentLanguage) {
                    "en" -> Locale.ENGLISH
                    "fr" -> Locale.FRENCH
                    else -> Locale.getDefault()
                }
                val config = Configuration(context.resources.configuration)
                config.setLocales(android.os.LocaleList(locale))
                config
            }
            val localeContext = remember(localeConfig) {
                context.createConfigurationContext(localeConfig)
            }

            CompositionLocalProvider(
                LocalContext provides localeContext,
                LocalActivityResultRegistryOwner provides this@MainActivity
            ) {
            BackHandler {
                if (currentScreen == AppScreen.MAIN && currentMode == Mode.SCAN) {
                    viewModel.exitScanMode()
                } else if (!viewModel.navigateBack()) {
                    showExitDialog = true
                }
            }

            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = { Text(stringResource(R.string.exit_app)) },
                    text = { Text(stringResource(R.string.exit_app_message)) },
                    confirmButton = {
                        TextButton(onClick = { finish() }) {
                            Text(stringResource(R.string.exit))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitDialog = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }

            FaceRecognitionTheme(darkTheme = darkTheme) {
                when (currentScreen) {
                    AppScreen.MAIN -> {
                        MainScreen(
                            faceDetector = faceDetector,
                            cameraExecutor = cameraExecutor,
                            onCameraProviderReady = { provider -> cameraProvider = provider },
                            viewModel = viewModel
                        )
                    }
                    AppScreen.MANAGE_FACES, AppScreen.PERSON_DETAIL -> {
                        ManageFacesScreen(viewModel = viewModel, faceDetector = faceDetector)
                    }
                    AppScreen.CLASS_LIST -> {
                        ClassListScreen(viewModel = viewModel, faceDetector = faceDetector)
                    }
                    AppScreen.ATTENDANCE_SESSION -> {
                        MainScreen(
                            faceDetector = faceDetector,
                            cameraExecutor = cameraExecutor,
                            onCameraProviderReady = { provider -> cameraProvider = provider },
                            viewModel = viewModel
                        )
                    }
                    AppScreen.ATTENDANCE_REVIEW -> {
                        AttendanceReviewScreen(viewModel = viewModel)
                    }
                    AppScreen.SETTINGS -> {
                        SettingsScreen(viewModel = viewModel)
                    }
                }
            }
            } // CompositionLocalProvider
        }

        checkCameraPermission()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
        }, ContextCompat.getMainExecutor(this))
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        faceDetector.close()
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
}
