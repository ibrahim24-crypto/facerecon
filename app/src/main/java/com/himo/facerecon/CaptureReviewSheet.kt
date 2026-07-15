package com.himo.facerecon

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CaptureReviewSheet(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val capturedFaces by viewModel.capturedFaces.collectAsStateWithLifecycle()
    val targetName by viewModel.captureSessionTargetName.collectAsStateWithLifecycle()
    val trainName by viewModel.captureTrainName.collectAsStateWithLifecycle()
    val isCommitting by viewModel.isCommitting.collectAsStateWithLifecycle()

    val needsName = targetName == "Unknown" || targetName.isNullOrBlank()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.review_captured_faces),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = if (needsName) {
                    stringResource(R.string.capture_unknown_hint)
                } else {
                    stringResource(R.string.capture_known_hint, targetName ?: "")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            EnrollmentSampleList(
                samples = capturedFaces,
                onDelete = viewModel::removeCapturedFace,
                modifier = Modifier.fillMaxWidth()
            )

            if (needsName) {
                OutlinedTextField(
                    value = trainName,
                    onValueChange = viewModel::setCaptureTrainName,
                    label = { Text(stringResource(R.string.enter_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.discardCapturedFaces()
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.discard_captures))
                }

                Button(
                    onClick = { viewModel.commitCapturedFaces(context) },
                    modifier = Modifier.weight(1f),
                    enabled = !isCommitting && capturedFaces.isNotEmpty() && (!needsName || trainName.isNotBlank())
                ) {
                    if (isCommitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(stringResource(R.string.submit_for_training))
                }
            }
        }
    }
}
