package com.himo.facerecon

import android.content.Context
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AttendanceExporter {

    enum class Scope { PRESENT_ONLY, ABSENT_ONLY, BOTH }

    fun buildCsv(session: AttendanceSession, scope: Scope): String {
        val sb = StringBuilder()
        sb.appendLine("Name,Status,Class,Date,Time")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateStr = dateFormat.format(Date(session.dateMs))

        if (scope == Scope.PRESENT_ONLY || scope == Scope.BOTH) {
            session.presentNames.sorted().forEach { name ->
                val time = session.timestamps[name]?.let { timeFormat.format(Date(it)) } ?: ""
                sb.appendLine("$name,Present,${session.className},$dateStr,$time")
            }
        }
        if (scope == Scope.ABSENT_ONLY || scope == Scope.BOTH) {
            session.absentNames.sorted().forEach { name ->
                val time = session.timestamps[name]?.let { timeFormat.format(Date(it)) } ?: ""
                sb.appendLine("$name,Absent,${session.className},$dateStr,$time")
            }
        }
        return sb.toString()
    }

    fun suggestedFileName(session: AttendanceSession, scope: Scope): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val suffix = when (scope) {
            Scope.PRESENT_ONLY -> "present"
            Scope.ABSENT_ONLY -> "absent"
            Scope.BOTH -> "all"
        }
        return "attendance_${session.className}_${suffix}_${dateFormat.format(Date(session.dateMs))}.csv"
    }

    fun writeToUri(context: Context, uri: Uri, csv: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                stream.write(csv.toByteArray())
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
