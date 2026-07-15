package com.himo.facerecon

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.face.FaceDetector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassListScreen(
    viewModel: MainViewModel,
    faceDetector: FaceDetector? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val classes by viewModel.classes.collectAsStateWithLifecycle()
    val pendingScanCandidates by viewModel.pendingScanCandidates.collectAsStateWithLifecycle()
    val pendingClassName by viewModel.pendingClassName.collectAsStateWithLifecycle()
    val pendingClassRoster by viewModel.pendingClassRoster.collectAsStateWithLifecycle()
    val classHistory by viewModel.classHistory.collectAsStateWithLifecycle()

    var showNameDialog by remember { mutableStateOf(false) }
    var editingClass by remember { mutableStateOf<ClassGroup?>(null) }
    var classNameInput by remember { mutableStateOf("") }
    var namingIndex by remember { mutableIntStateOf(-1) }
    var namingInput by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var expandedClassId by remember { mutableStateOf<String?>(null) }
    var historyClassId by remember { mutableStateOf<String?>(null) }
    var addToClassId by remember { mutableStateOf<String?>(null) }
    var studentSearch by remember { mutableStateOf("") }
    var renameStudent by remember { mutableStateOf<Pair<String, String>?>(null) }
    // ^ Pair<classId, studentName> for the rename dialog
    var importRosterForClass by remember { mutableStateOf<String?>(null) }
    // ^ classId for import, null = new class
    var importRosterText by remember { mutableStateOf("") }
    var postImportInfo by remember { mutableStateOf<Triple<String, List<String>, String?>?>(null) }
    // ^ Triple<className, parsedNames, existingClassId?> for post-import scan dialog

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val targetClassId = addToClassId
        val targetClass = classes.find { it.id == targetClassId }
        val existingStudents = targetClass?.studentNames ?: emptyList()
        val name = targetClass?.name ?: classNameInput.trim()

        if (uri != null && name.isNotBlank()) {
            val bitmap = loadAndOrientBitmap(context, uri)
            if (bitmap != null && faceDetector != null) {
                viewModel.processImageForClass(name, bitmap, faceDetector, targetClassId, existingStudents)
                showNameDialog = false
                addToClassId = null
            }
        }
    }

    // Naming dialog for image candidates
    if (namingIndex >= 0) {
        AlertDialog(
            onDismissRequest = { namingIndex = -1 },
            title = { Text("Name this student") },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val bmp = pendingScanCandidates.getOrNull(namingIndex)
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp).clip(MaterialTheme.shapes.medium),
                            contentScale = ContentScale.Crop
                        )
                    }
                    OutlinedTextField(
                        value = namingInput,
                        onValueChange = { namingInput = it },
                        label = { Text("Student name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (namingInput.isNotBlank()) {
                            viewModel.enrollCandidateAsStudent(namingIndex, namingInput.trim(), context)
                            namingIndex = -1
                            namingInput = ""
                        }
                    },
                    enabled = namingInput.isNotBlank()
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { namingIndex = -1; namingInput = "" }) { Text("Skip") }
            }
        )
    }

    // History dialog
    if (historyClassId != null) {
        val histClass = classes.find { it.id == historyClassId }
        val sessions = classHistory.filter { it.classId == historyClassId }
        val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

        AlertDialog(
            onDismissRequest = { historyClassId = null },
            title = { Text("History: ${histClass?.name ?: ""}") },
            text = {
                if (sessions.isEmpty()) {
                    Text("No sessions yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(sessions, key = { it.id }) { session ->
                            Card(
                                shape = MaterialTheme.shapes.small,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = dateFormat.format(Date(session.dateMs)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text(
                                            "${session.presentNames.size} present",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "${session.absentNames.size} absent",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            historyClassId?.let { viewModel.clearAttendanceHistory(it) }
                            historyClassId = null
                        }
                    ) { Text("Clear", color = MaterialTheme.colorScheme.error) }
                    TextButton(onClick = { historyClassId = null }) { Text("Close") }
                }
            }
        )
    }

    // Create/edit dialog
    val editing = editingClass
    if (showNameDialog || editing != null) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false; editingClass = null },
            title = { Text(if (editing != null) "Edit Class" else "New Class") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = if (editing != null) editing.name else classNameInput,
                        onValueChange = { classNameInput = it },
                        label = { Text("Class name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (editing != null) {
                        Text("${editing.studentNames.size} student(s)", style = MaterialTheme.typography.bodySmall)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(editing.studentNames, key = { it }) { name ->
                                Card(
                                    shape = MaterialTheme.shapes.small,
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Text(name, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (editing != null) {
                    TextButton(onClick = {
                        viewModel.saveClass(editing.copy(name = classNameInput.trim()))
                        editingClass = null
                    }) { Text("Save") }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                if (classNameInput.isNotBlank()) {
                                    viewModel.startScanForClass(classNameInput.trim())
                                    showNameDialog = false
                                    classNameInput = ""
                                }
                            },
                            enabled = classNameInput.isNotBlank()
                        ) { Text("Scan") }
                        TextButton(
                            onClick = {
                                addToClassId = null
                                imagePickerLauncher.launch("image/*")
                            },
                            enabled = classNameInput.isNotBlank()
                        ) { Text("From Image") }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false; editingClass = null }) { Text("Cancel") }
            }
        )
    }

    // Rename student dialog
    if (renameStudent != null) {
        var renameInput by remember(renameStudent) { mutableStateOf(renameStudent!!.second) }
        AlertDialog(
            onDismissRequest = { renameStudent = null },
            title = { Text("Rename Student") },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("New name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameStudentInClass(renameStudent!!.first, renameStudent!!.second, renameInput.trim())
                        renameStudent = null
                    },
                    enabled = renameInput.isNotBlank()
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { renameStudent = null }) { Text("Cancel") }
            }
        )
    }

    // Import roster dialog
    if (importRosterForClass != null || importRosterText.isNotEmpty()) {
        val targetId = importRosterForClass
        val targetClass = classes.find { it.id == targetId }
        AlertDialog(
            onDismissRequest = { importRosterForClass = null; importRosterText = "" },
            title = { Text(if (targetClass != null) "Import to ${targetClass.name}" else "Import Roster") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Paste student names (one per line or comma-separated):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = importRosterText,
                        onValueChange = { importRosterText = it },
                        label = { Text("Student names") },
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = targetClass?.name ?: classNameInput.ifBlank { "Imported Class" }
                        val parsedNames = importRosterText.lines()
                            .flatMap { it.split(",", ";", "\t") }
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .distinct()
                        viewModel.importRosterFromClass(name, importRosterText, targetId)
                        postImportInfo = Triple(name, parsedNames, targetId)
                        importRosterForClass = null
                        importRosterText = ""
                    },
                    enabled = importRosterText.isNotBlank()
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { importRosterForClass = null; importRosterText = "" }) { Text("Cancel") }
            }
        )
    }

    // Post-import: ask to scan faces or skip
    if (postImportInfo != null) {
        val (className, names, classId) = postImportInfo!!
        AlertDialog(
            onDismissRequest = { postImportInfo = null },
            title = { Text("${names.size} students imported") },
            text = {
                Text("Do you want to scan faces for each student now? This links their face to their name for attendance recognition.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startRosterFaceEnrollment(className, names, classId)
                    postImportInfo = null
                }) { Text("Scan Faces") }
            },
            dismissButton = {
                TextButton(onClick = { postImportInfo = null }) { Text("Skip") }
            }
        )
    }

    // Filter classes
    val filteredClasses = remember(classes, searchQuery) {
        if (searchQuery.isBlank()) classes
        else classes.filter { cg ->
            cg.name.contains(searchQuery, ignoreCase = true) ||
                cg.studentNames.any { it.contains(searchQuery, ignoreCase = true) }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Classes") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateToMain() }) {
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (pendingScanCandidates.isNotEmpty()) {
                // Image candidate naming UI
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Class: $pendingClassName", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tap a face to name it (${pendingScanCandidates.size} remaining, ${pendingClassRoster.size} added)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
                            items(pendingScanCandidates.size) { index ->
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                                        .clickable { namingIndex = index; namingInput = "" }
                                ) {
                                    Image(
                                        bitmap = pendingScanCandidates[index].asImageBitmap(),
                                        contentDescription = "Face ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        if (pendingClassRoster.isNotEmpty()) {
                            Text(
                                "Added: ${pendingClassRoster.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.exitScanMode() },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium
                            ) { Text("Cancel") }
                            ElevatedButton(
                                onClick = { viewModel.saveScannedClass() },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.medium,
                                enabled = pendingClassRoster.isNotEmpty()
                            ) { Text("Save (${pendingClassRoster.size})") }
                        }
                    }
                }
            } else {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search classes or students...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                ElevatedButton(
                    onClick = { classNameInput = ""; showNameDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Create Class")
                }

                if (filteredClasses.isEmpty()) {
                    Text(
                        text = if (classes.isEmpty()) "No classes yet." else "No matches for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(vertical = 32.dp)
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredClasses, key = { it.id }) { classGroup ->
                            val isExpanded = expandedClassId == classGroup.id
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large,
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f).clickable {
                                            expandedClassId = if (isExpanded) null else classGroup.id
                                        }) {
                                            Text(classGroup.name, style = MaterialTheme.typography.titleMedium)
                                            Text(
                                                "${classGroup.studentNames.size} student(s)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                viewModel.getClassAttendanceRate(classGroup.id),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = {
                                            historyClassId = classGroup.id
                                            viewModel.loadClassHistory(classGroup.id)
                                        }) {
                                            Icon(Icons.Default.History, contentDescription = "History")
                                        }
                                        IconButton(onClick = { editingClass = classGroup; classNameInput = classGroup.name }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                                        }
                                        IconButton(onClick = { viewModel.deleteClass(classGroup.id) }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    // Expandable student list
                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (classGroup.studentNames.isNotEmpty()) {
                                                // Student search within class
                                                OutlinedTextField(
                                                    value = studentSearch,
                                                    onValueChange = { studentSearch = it },
                                                    placeholder = { Text("Search students...") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true,
                                                    shape = MaterialTheme.shapes.small
                                                )

                                                val filtered = remember(classGroup.studentNames, studentSearch) {
                                                    if (studentSearch.isBlank()) classGroup.studentNames
                                                    else classGroup.studentNames.filter { it.contains(studentSearch, ignoreCase = true) }
                                                }

                                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    items(filtered, key = { it }) { name ->
                                                        Card(
                                                            shape = MaterialTheme.shapes.small,
                                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                                            onClick = { renameStudent = Pair(classGroup.id, name) }
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                if (viewModel.getSampleCount(name) < 3) {
                                                                    Icon(
                                                                        Icons.Default.Warning,
                                                                        contentDescription = "Low samples",
                                                                        modifier = Modifier.size(14.dp).padding(end = 4.dp),
                                                                        tint = MaterialTheme.colorScheme.tertiary
                                                                    )
                                                                }
                                                                Text(name, style = MaterialTheme.typography.bodySmall)
                                                                IconButton(
                                                                    onClick = { viewModel.removeStudentFromClass(classGroup.id, name) },
                                                                    modifier = Modifier.size(20.dp)
                                                                ) {
                                                                    Icon(
                                                                        Icons.Default.Cancel,
                                                                        contentDescription = "Remove",
                                                                        modifier = Modifier.size(14.dp),
                                                                        tint = MaterialTheme.colorScheme.error
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Add students buttons
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.startScanForClass(classGroup.name, classGroup.id, classGroup.studentNames)
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = MaterialTheme.shapes.medium
                                                ) {
                                                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                                    Text("Scan Add")
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        addToClassId = classGroup.id
                                                        imagePickerLauncher.launch("image/*")
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = MaterialTheme.shapes.medium
                                                ) {
                                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                                    Text("Image Add")
                                                }
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    importRosterForClass = classGroup.id
                                                    importRosterText = ""
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = MaterialTheme.shapes.medium
                                            ) {
                                                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                                Text("Import Roster")
                                            }
                                        }
                                    }

                                    if (!isExpanded) {
                                        Icon(
                                            Icons.Default.ExpandMore,
                                            contentDescription = "Expand",
                                            modifier = Modifier.size(20.dp).align(Alignment.CenterHorizontally)
                                                .clickable { expandedClassId = classGroup.id },
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        )
                                    }

                                    ElevatedButton(
                                        onClick = { viewModel.startAttendanceSession(classGroup) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                        Text("Start Attendance")
                                    }
                                }
                            }
                        }

                        item {
                            OutlinedButton(
                                onClick = { viewModel.clearAllAttendanceHistory() },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 8.dp))
                                Text("Clear All Attendance History", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
