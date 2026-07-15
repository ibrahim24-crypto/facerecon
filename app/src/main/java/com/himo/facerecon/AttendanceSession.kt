package com.himo.facerecon

import java.util.UUID

data class AttendanceSession(
    val id: String = UUID.randomUUID().toString(),
    val classId: String,
    val className: String,
    val dateMs: Long,
    val presentNames: Set<String>,
    val absentNames: Set<String>,
    val timestamps: Map<String, Long> = emptyMap()
    // ^ Maps student name → timestamp (ms) of when they were marked present or toggled.
    //   Set by detection (first sighting) or manual toggle in review.
)
