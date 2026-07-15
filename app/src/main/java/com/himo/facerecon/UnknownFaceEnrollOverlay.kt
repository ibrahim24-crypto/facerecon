package com.himo.facerecon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun UnknownFaceEnrollOverlay(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val showDialog by viewModel.showUnknownDialog.collectAsStateWithLifecycle()
    val promptName by viewModel.unknownPromptName.collectAsStateWithLifecycle()
    val faceBitmap by viewModel.unknownFaceBitmap.collectAsStateWithLifecycle()

    if (showDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissUnknownPrompt,
            title = { Text(stringResource(R.string.who_is_this)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    faceBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Detected face",
                            modifier = Modifier
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                    OutlinedTextField(
                        value = promptName,
                        onValueChange = viewModel::setUnknownPromptName,
                        label = { Text(stringResource(R.string.enter_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.enrollUnknownFace(context) }) {
                    Text(stringResource(R.string.enroll))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissUnknownPrompt) {
                    Text(stringResource(R.string.ignore))
                }
            }
        )
    }
}
