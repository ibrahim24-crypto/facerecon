package com.himo.facerecon

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun EnrollmentSampleList(
    samples: List<EnrollmentSample>,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (samples.isEmpty()) {
        Text(
            text = "No faces captured yet",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = modifier.padding(vertical = 8.dp)
        )
        return
    }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(samples, key = { it.id }) { sample ->
            Surface(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.small,
                tonalElevation = 2.dp
            ) {
                Box {
                    Image(
                        bitmap = sample.bitmap.asImageBitmap(),
                        contentDescription = "Captured face",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    TextButton(
                        onClick = { onDelete(sample.id) },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "✕",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}
