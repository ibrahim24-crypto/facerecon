package com.himo.facerecon

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.FileDownload
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceReviewScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val session by viewModel.attendanceSession.collectAsStateWithLifecycle()
    val capturedFaces by viewModel.capturedFaces.collectAsStateWithLifecycle()
    var pendingScope by remember { mutableStateOf<AttendanceExporter.Scope?>(null) }
    var namingCapturedId by remember { mutableStateOf<String?>(null) }
    var namingCapturedInput by remember { mutableStateOf("") }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val scope = pendingScope
        val s = session
        if (uri != null && scope != null && s != null) {
            val csv = AttendanceExporter.buildCsv(s, scope)
            if (AttendanceExporter.writeToUri(context, uri, csv)) {
                Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Save failed", Toast.LENGTH_SHORT).show()
            }
        }
        pendingScope = null
    }

    // Naming dialog for captured unknown faces
    if (namingCapturedId != null) {
        val sample = capturedFaces.find { it.id == namingCapturedId }
        AlertDialog(
            onDismissRequest = { namingCapturedId = null; namingCapturedInput = "" },
            title = { Text("Who is this?") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (sample != null) {
                        Image(
                            bitmap = sample.bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp).clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    }
                    OutlinedTextField(
                        value = namingCapturedInput,
                        onValueChange = { namingCapturedInput = it },
                        label = { Text("Student name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        namingCapturedId?.let { id ->
                            viewModel.enrollCapturedFaceInSession(id, namingCapturedInput.trim(), context)
                        }
                        namingCapturedId = null
                        namingCapturedInput = ""
                    },
                    enabled = namingCapturedInput.isNotBlank()
                ) { Text("Enroll & Mark Present") }
            },
            dismissButton = {
                TextButton(onClick = { namingCapturedId = null; namingCapturedInput = "" }) { Text("Skip") }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Attendance Review")
                        session?.let { s ->
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                            Text(
                                text = "${s.className} — ${dateFormat.format(Date(s.dateMs))}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.exitAttendance() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (session != null) {
                val present = session!!.presentNames.sorted()
                val absent = session!!.absentNames.sorted()
                val timestamps = session!!.timestamps
                val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = present.size.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Present", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = absent.size.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text("Absent", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Unknown faces captured during attendance
                if (capturedFaces.isNotEmpty()) {
                    Text(
                        text = "Unknown Faces (${capturedFaces.size}) — tap to identify",
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
                                    .border(2.dp, MaterialTheme.colorScheme.tertiary, MaterialTheme.shapes.small)
                                    .clickable {
                                        namingCapturedId = sample.id
                                        namingCapturedInput = ""
                                    }
                            ) {
                                Image(
                                    bitmap = sample.bitmap.asImageBitmap(),
                                    contentDescription = "Unknown face",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { viewModel.dismissCapturedFace(sample.id) },
                                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Cancel,
                                        contentDescription = "Dismiss",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Tap a name to toggle Present/Absent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                var reviewSearch by remember { mutableStateOf("") }
                OutlinedTextField(
                    value = reviewSearch,
                    onValueChange = { reviewSearch = it },
                    placeholder = { Text("Search students...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                val filteredPresent = remember(present, reviewSearch) {
                    if (reviewSearch.isBlank()) present
                    else present.filter { it.contains(reviewSearch, ignoreCase = true) }
                }
                val filteredAbsent = remember(absent, reviewSearch) {
                    if (reviewSearch.isBlank()) absent
                    else absent.filter { it.contains(reviewSearch, ignoreCase = true) }
                }

                if (filteredPresent.isNotEmpty()) {
                    Text(
                        text = "Present (${filteredPresent.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredPresent, key = { it }) { name ->
                            ReviewRow(
                                name = name,
                                isPresent = true,
                                timeLabel = timestamps[name]?.let { timeFormat.format(Date(it)) } ?: "",
                                onToggle = { viewModel.toggleAttendanceStatus(name) }
                            )
                        }
                    }
                }

                if (filteredAbsent.isNotEmpty()) {
                    Text(
                        text = "Absent (${filteredAbsent.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredAbsent, key = { it }) { name ->
                            ReviewRow(
                                name = name,
                                isPresent = false,
                                timeLabel = timestamps[name]?.let { timeFormat.format(Date(it)) } ?: "",
                                onToggle = { viewModel.toggleAttendanceStatus(name) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            pendingScope = AttendanceExporter.Scope.PRESENT_ONLY
                            session?.let { saveLauncher.launch(AttendanceExporter.suggestedFileName(it, AttendanceExporter.Scope.PRESENT_ONLY)) }
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        enabled = present.isNotEmpty()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Present")
                    }
                    OutlinedButton(
                        onClick = {
                            pendingScope = AttendanceExporter.Scope.ABSENT_ONLY
                            session?.let { saveLauncher.launch(AttendanceExporter.suggestedFileName(it, AttendanceExporter.Scope.ABSENT_ONLY)) }
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        enabled = absent.isNotEmpty()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Absent")
                    }
                    ElevatedButton(
                        onClick = {
                            pendingScope = AttendanceExporter.Scope.BOTH
                            session?.let { saveLauncher.launch(AttendanceExporter.suggestedFileName(it, AttendanceExporter.Scope.BOTH)) }
                        },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                        Text("Both")
                    }
                }

                ElevatedButton(
                    onClick = { viewModel.resumeAttendanceForLateStudent() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Scan Late Student")
                }
            }
        }
    }
}

@Composable
private fun ReviewRow(
    name: String,
    isPresent: Boolean,
    timeLabel: String,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isPresent) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            }
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isPresent) Icons.Default.CheckCircle else Icons.Default.Cancel,
                contentDescription = null,
                tint = if (isPresent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(end = 8.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, style = MaterialTheme.typography.bodyMedium)
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
                    contentDescription = "Toggle",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}
