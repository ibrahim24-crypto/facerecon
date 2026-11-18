package com.himo.facerecon

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ReviewStatus(@StringRes val labelRes: Int) {
    PRESENT(R.string.attendance_present),
    ABSENT(R.string.attendance_absent),
    UNSURE(R.string.attendance_unsure)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceReviewScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val session by viewModel.attendanceSession.collectAsStateWithLifecycle()
    val capturedFaces by viewModel.capturedFaces.collectAsStateWithLifecycle()
    var pendingScope by remember { mutableStateOf<AttendanceExporter.Scope?>(null) }
    var namingCapturedId by remember { mutableStateOf<String?>(null) }
    var namingCapturedInput by remember { mutableStateOf("") }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val scope = pendingScope
        val current = session
        if (uri != null && scope != null && current != null) {
            val csv = AttendanceExporter.buildCsv(current, scope)
            val messageRes = if (AttendanceExporter.writeToUri(context, uri, csv)) {
                R.string.attendance_saved
            } else {
                R.string.attendance_save_failed
            }
            Toast.makeText(context, context.getString(messageRes), Toast.LENGTH_SHORT).show()
        }
        pendingScope = null
    }

    // Naming dialog for captured unknown faces
    if (namingCapturedId != null) {
        val sample = capturedFaces.find { it.id == namingCapturedId }
        AlertDialog(
            onDismissRequest = {
                namingCapturedId = null
                namingCapturedInput = ""
            },
            title = { Text(stringResource(R.string.who_is_this)) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (sample != null) {
                        Image(
                            bitmap = sample.bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.attendance_unknown_face),
                            modifier = Modifier.size(120.dp).clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    }
                    OutlinedTextField(
                        value = namingCapturedInput,
                        onValueChange = { namingCapturedInput = it },
                        label = { Text(stringResource(R.string.student_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        namingCapturedId?.let { id ->
                            viewModel.enrollCapturedFaceInSession(
                                id,
                                namingCapturedInput.trim(),
                                context
                            )
                        }
                        namingCapturedId = null
                        namingCapturedInput = ""
                    },
                    enabled = namingCapturedInput.isNotBlank()
                ) { Text(stringResource(R.string.enroll_mark_present)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    namingCapturedId = null
                    namingCapturedInput = ""
                }) { Text(stringResource(R.string.skip)) }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.attendance_review_title))
                        val current = session
                        if (current != null) {
                            val dateFormat = SimpleDateFormat(
                                "MMM dd, yyyy HH:mm",
                                Locale.getDefault()
                            )
                            Text(
                                text = stringResource(
                                    R.string.attendance_session_subtitle,
                                    current.className,
                                    dateFormat.format(Date(current.dateMs))
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.exitAttendance() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val current = session
            if (current != null) {
                val present = current.presentNames.sorted()
                val absent = current.absentNames.sorted()
                val unsure = current.unsureNames.sorted()
                val timestamps = current.timestamps
                val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryCard(
                        count = present.size,
                        label = stringResource(R.string.attendance_present),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        count = absent.size,
                        label = stringResource(R.string.attendance_absent),
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        count = unsure.size,
                        label = stringResource(R.string.attendance_unsure),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Unknown faces captured during attendance
                if (capturedFaces.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.attendance_unknown_faces, capturedFaces.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp)
                    ) {
                        items(capturedFaces.size) { index ->
                            val sample = capturedFaces[index]
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.tertiary,
                                        MaterialTheme.shapes.small
                                    )
                                    .clickable {
                                        namingCapturedId = sample.id
                                        namingCapturedInput = ""
                                    }
                            ) {
                                Image(
                                    bitmap = sample.bitmap.asImageBitmap(),
                                    contentDescription = stringResource(R.string.attendance_unknown_face),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { viewModel.dismissCapturedFace(sample.id) },
                                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Cancel,
                                        contentDescription = stringResource(R.string.dismiss),
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.attendance_toggle_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                var reviewSearch by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = reviewSearch,
                    onValueChange = { reviewSearch = it },
                    placeholder = { Text(stringResource(R.string.attendance_search)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                fun filter(names: List<String>): List<String> = if (reviewSearch.isBlank()) {
                    names
                } else {
                    names.filter { it.contains(reviewSearch, ignoreCase = true) }
                }

                fun timeLabel(name: String): String =
                    timestamps[name]?.let { timeFormat.format(Date(it)) } ?: ""

                val filteredPresent = filter(present)
                val filteredAbsent = filter(absent)
                val filteredUnsure = filter(unsure)

                if (filteredPresent.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.attendance_present_count, filteredPresent.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(filteredPresent, key = { it }) { name ->
                            ReviewRow(
                                name = name,
                                status = ReviewStatus.PRESENT,
                                timeLabel = timeLabel(name),
                                onToggle = { viewModel.toggleAttendanceStatus(name) }
                            )
                        }
                    }
                }

                if (filteredAbsent.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.attendance_absent_count, filteredAbsent.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(filteredAbsent, key = { it }) { name ->
                            ReviewRow(
                                name = name,
                                status = ReviewStatus.ABSENT,
                                timeLabel = timeLabel(name),
                                onToggle = { viewModel.toggleAttendanceStatus(name) }
                            )
                        }
                    }
                }

                if (filteredUnsure.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.attendance_unsure_count, filteredUnsure.size),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(filteredUnsure, key = { it }) { name ->
                            ReviewRow(
                                name = name,
                                status = ReviewStatus.UNSURE,
                                timeLabel = timeLabel(name),
                                onToggle = { viewModel.toggleAttendanceStatus(name) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportButton(
                        label = stringResource(R.string.attendance_export_present),
                        scope = AttendanceExporter.Scope.PRESENT_ONLY,
                        enabled = present.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        onClick = { scope ->
                            pendingScope = scope
                            saveLauncher.launch(
                                AttendanceExporter.suggestedFileName(current, scope)
                            )
                        }
                    )
                    ExportButton(
                        label = stringResource(R.string.attendance_export_absent),
                        scope = AttendanceExporter.Scope.ABSENT_ONLY,
                        enabled = absent.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        onClick = { scope ->
                            pendingScope = scope
                            saveLauncher.launch(
                                AttendanceExporter.suggestedFileName(current, scope)
                            )
                        }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportButton(
                        label = stringResource(R.string.attendance_export_unsure),
                        scope = AttendanceExporter.Scope.UNSURE_ONLY,
                        enabled = unsure.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        onClick = { scope ->
                            pendingScope = scope
                            saveLauncher.launch(
                                AttendanceExporter.suggestedFileName(current, scope)
                            )
                        }
                    )
                    ExportButton(
                        label = stringResource(R.string.attendance_export_all),
                        scope = AttendanceExporter.Scope.BOTH,
                        enabled = present.isNotEmpty() || absent.isNotEmpty() || unsure.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        onClick = { scope ->
                            pendingScope = scope
                            saveLauncher.launch(
                                AttendanceExporter.suggestedFileName(current, scope)
                            )
                        }
                    )
                }

                ElevatedButton(
                    onClick = { viewModel.resumeAttendanceForLateStudent() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    Text(stringResource(R.string.attendance_scan_late))
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    count: Int,
    label: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineMedium,
                color = contentColor
            )
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ExportButton(
    label: String,
    scope: AttendanceExporter.Scope,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: (AttendanceExporter.Scope) -> Unit
) {
    OutlinedButton(
        onClick = { onClick(scope) },
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        enabled = enabled
    ) {
        Icon(
            Icons.Default.FileDownload,
            contentDescription = null,
            modifier = Modifier.padding(end = 4.dp)
        )
        Text(label)
    }
}

@Composable
private fun ReviewRow(
    name: String,
    status: ReviewStatus,
    timeLabel: String,
    onToggle: () -> Unit
) {
    val containerColor = when (status) {
        ReviewStatus.PRESENT -> MaterialTheme.colorScheme.primaryContainer
        ReviewStatus.ABSENT -> MaterialTheme.colorScheme.errorContainer
        ReviewStatus.UNSURE -> MaterialTheme.colorScheme.tertiaryContainer
    }
    val contentColor = when (status) {
        ReviewStatus.PRESENT -> MaterialTheme.colorScheme.primary
        ReviewStatus.ABSENT -> MaterialTheme.colorScheme.error
        ReviewStatus.UNSURE -> MaterialTheme.colorScheme.tertiary
    }
    val icon = when (status) {
        ReviewStatus.PRESENT -> Icons.Default.CheckCircle
        ReviewStatus.ABSENT -> Icons.Default.Cancel
        ReviewStatus.UNSURE -> Icons.Default.HelpOutline
    }

    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = stringResource(status.labelRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor
                )
                if (timeLabel.isNotEmpty()) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            IconButton(onClick = onToggle) {
                Icon(
                    Icons.Default.SwapHoriz,
                    contentDescription = stringResource(R.string.toggle),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
