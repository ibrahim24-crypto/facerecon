package com.himo.facerecon

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ClassStorage(context: Context) {

    private val dir = File(context.filesDir, "classes").apply { mkdirs() }
    private val indexFile = File(dir, "index.json")

    fun loadAll(): List<ClassGroup> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val json = JSONObject(indexFile.readText())
            val array = json.optJSONArray("classes") ?: return emptyList()
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val names = obj.optJSONArray("studentNames") ?: JSONArray()
                    add(
                        ClassGroup(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            studentNames = buildList {
                                for (j in 0 until names.length()) add(names.getString(j))
                            },
                            group = obj.optString("group", "")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load classes", e)
            emptyList()
        }
    }

    fun save(group: ClassGroup) {
        val all = loadAll().toMutableList()
        all.removeAll { it.id == group.id }
        all.add(group)
        persist(all)
    }

    fun delete(classId: String) {
        val all = loadAll().toMutableList()
        all.removeAll { it.id == classId }
        persist(all)
    }

    private fun persist(classes: List<ClassGroup>) {
        val json = JSONObject()
        val array = JSONArray()
        classes.forEach { group ->
            array.put(
                JSONObject().apply {
                    put("id", group.id)
                    put("name", group.name)
                    put("studentNames", JSONArray(group.studentNames))
                    put("group", group.group)
                }
            )
        }
        json.put("classes", array)
        indexFile.writeText(json.toString())
    }

    companion object {
        private const val TAG = "ClassStorage"
    }
}
