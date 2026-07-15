package com.himo.facerecon

import android.graphics.Bitmap
import java.util.UUID

data class EnrollmentSample(
    val id: String = UUID.randomUUID().toString(),
    val bitmap: Bitmap,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EnrollmentSample) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
