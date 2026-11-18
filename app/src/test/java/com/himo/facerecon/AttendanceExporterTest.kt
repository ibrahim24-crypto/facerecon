package com.himo.facerecon

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceExporterTest {

    private fun session() = AttendanceSession(
        classId = "class-1",
        className = "Math, 101",
        dateMs = 1_700_000_000_000L,
        presentNames = setOf("Charlie", "Alice"),
        absentNames = setOf("Bob"),
        unsureNames = setOf("Dana"),
        timestamps = mapOf("Alice" to 1_700_000_060_000L)
    )

    @Test
    fun bothScopeWritesEveryStatusInSortedOrder() {
        val lines = AttendanceExporter.buildCsv(session(), AttendanceExporter.Scope.BOTH).trimEnd().lines()

        assertEquals("Name,Status,Class,Date,Time", lines.first())
        assertEquals(5, lines.size)
        assertTrue(lines[1].startsWith("Alice,Present"))
        assertTrue(lines[2].startsWith("Charlie,Present"))
        assertTrue(lines[3].startsWith("Bob,Absent"))
        assertTrue(lines[4].startsWith("Dana,UNSURE"))
        assertTrue(lines[1].contains("\"Math, 101\""))
    }

    @Test
    fun unsureScopeOnlyWritesUnsureRows() {
        val csv = AttendanceExporter.buildCsv(session(), AttendanceExporter.Scope.UNSURE_ONLY)

        assertEquals(2, csv.trimEnd().lines().size)
        assertTrue(csv.contains("Dana,UNSURE"))
        assertFalse(csv.contains("Alice"))
    }

    @Test
    fun presentScopeIgnoresAbsentAndUnsure() {
        val csv = AttendanceExporter.buildCsv(session(), AttendanceExporter.Scope.PRESENT_ONLY)

        assertEquals(3, csv.trimEnd().lines().size)
        assertTrue(csv.contains("Alice,Present"))
        assertFalse(csv.contains("Absent"))
        assertFalse(csv.contains("UNSURE"))
    }

    @Test
    fun csvEscapesQuotesAndCommasInNames() {
        val sample = session().copy(presentNames = setOf("Doe, \"John\""))

        val csv = AttendanceExporter.buildCsv(sample, AttendanceExporter.Scope.PRESENT_ONLY)

        assertTrue(csv.contains("\"Doe, \"\"John\"\"\""))
    }

    @Test
    fun legacySessionWithoutUnsureNamesExportsCleanly() {
        val legacy = AttendanceSession(
            classId = "class-1",
            className = "Legacy",
            dateMs = 0L,
            presentNames = setOf("Alice"),
            absentNames = emptySet()
        )

        val csv = AttendanceExporter.buildCsv(legacy, AttendanceExporter.Scope.BOTH)

        assertEquals(2, csv.trimEnd().lines().size)
        assertFalse(csv.contains("UNSURE"))
    }

    @Test
    fun unsureFileNameUsesUnsureSuffix() {
        val name = AttendanceExporter.suggestedFileName(session(), AttendanceExporter.Scope.UNSURE_ONLY)

        assertTrue(name.startsWith("attendance_"))
        assertTrue(name.contains("unsure"))
        assertTrue(name.endsWith(".csv"))
    }
}
