package com.himo.facerecon

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class AttendanceStorage(context: Context) {

    private val dir = File(context.filesDir, "attendance").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")

    fun loadAll(): List<AttendanceSession> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val json = JSONObject(indexFile.readText())
            val array = json.optJSONArray("sessions") ?: return emptyList()
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(
                        AttendanceSession(
                            id = obj.getString("id"),
                            classId = obj.getString("classId"),
                            className = obj.getString("className"),
                            dateMs = obj.getLong("dateMs"),
                            presentNames = obj.getJSONArray("presentNames").toStringSet(),
                            absentNames = obj.getJSONArray("absentNames").toStringSet(),
                            timestamps = obj.optJSONObject("timestamps")?.toStringMap() ?: emptyMap()
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load sessions", e)
            emptyList()
        }
    }

    fun loadForClass(classId: String): List<AttendanceSession> {
        return loadAll().filter { it.classId == classId }
            .sortedByDescending { it.dateMs }
    }

    fun save(session: AttendanceSession) {
        val all = loadAll().toMutableList()
        all.removeAll { it.id == session.id }
        all.add(session)
        persist(all)
    }

    fun delete(sessionId: String) {
        val all = loadAll().toMutableList()
        all.removeAll { it.id == sessionId }
        persist(all)
    }

    private fun persist(sessions: List<AttendanceSession>) {
        val json = JSONObject()
        val array = JSONArray()
        sessions.forEach { s ->
            array.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("classId", s.classId)
                    put("className", s.className)
                    put("dateMs", s.dateMs)
                    put("presentNames", JSONArray(s.presentNames.toList()))
                    put("absentNames", JSONArray(s.absentNames.toList()))
                    put("timestamps", JSONObject().apply {
                        s.timestamps.forEach { (name, time) -> put(name, time) }
                    })
                }
            )
        }
        json.put("sessions", array)
        indexFile.writeText(json.toString())
    }

    private fun JSONArray.toStringSet(): Set<String> {
        return buildSet {
            for (i in 0 until length()) add(getString(i))
        }
    }

    private fun JSONObject.toStringMap(): Map<String, Long> {
        return buildMap {
            keys().forEach { key -> put(key, getLong(key)) }
        }
    }

    companion object {
        private const val TAG = "AttendanceStorage"
    }
}
