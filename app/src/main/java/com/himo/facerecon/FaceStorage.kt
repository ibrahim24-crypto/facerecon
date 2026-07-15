package com.himo.facerecon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

data class StoredFaceSample(
    val id: String,
    val name: String,
    val imagePath: String,
    val embedding: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StoredFaceSample) return false
        return id == other.id && name == other.name && imagePath == other.imagePath
    }

    override fun hashCode(): Int = id.hashCode()
}

class FaceStorage(context: Context) {

    private val rootDir = File(context.filesDir, "enrolled_faces").apply { mkdirs() }
    private val indexFile = File(rootDir, "index.json")

    fun loadAll(): List<StoredFaceSample> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val json = JSONObject(indexFile.readText())
            val samplesArray = json.optJSONArray("samples") ?: return emptyList()
            buildList {
                for (i in 0 until samplesArray.length()) {
                    val item = samplesArray.getJSONObject(i)
                    val embeddingArray = item.getJSONArray("embedding")
                    val embedding = FloatArray(embeddingArray.length()) { index ->
                        embeddingArray.getDouble(index).toFloat()
                    }
                    add(
                        StoredFaceSample(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            imagePath = item.getString("imagePath"),
                            embedding = embedding
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load face index", e)
            emptyList()
        }
    }

    fun saveSample(name: String, bitmap: Bitmap, embedding: FloatArray): StoredFaceSample {
        val personDir = File(rootDir, sanitizeName(name)).apply { mkdirs() }
        val id = UUID.randomUUID().toString()
        val imageFile = File(personDir, "$id.jpg")
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        val sample = StoredFaceSample(
            id = id,
            name = name,
            imagePath = imageFile.absolutePath,
            embedding = embedding
        )
        val all = loadAll().toMutableList()
        all.add(sample)
        persistIndex(all)
        return sample
    }

    fun deleteSample(sampleId: String) {
        val all = loadAll().toMutableList()
        val sample = all.find { it.id == sampleId } ?: return
        File(sample.imagePath).delete()
        all.removeAll { it.id == sampleId }
        persistIndex(all)
        cleanupEmptyPersonDir(sample.name)
    }

    fun deletePerson(name: String) {
        val all = loadAll().toMutableList()
        all.filter { it.name == name }.forEach { File(it.imagePath).delete() }
        all.removeAll { it.name == name }
        persistIndex(all)
        File(rootDir, sanitizeName(name)).deleteRecursively()
    }

    fun renamePerson(oldName: String, newName: String) {
        val trimmedNew = newName.trim()
        if (oldName == trimmedNew) return

        val all = loadAll().toMutableList()
        val newDir = File(rootDir, sanitizeName(trimmedNew)).apply { mkdirs() }
        val oldDir = File(rootDir, sanitizeName(oldName))

        val updated = all.map { sample ->
            if (sample.name != oldName) {
                sample
            } else {
                val oldFile = File(sample.imagePath)
                val newFile = File(newDir, "${sample.id}.jpg")
                if (oldFile.exists()) {
                    oldFile.copyTo(newFile, overwrite = true)
                    oldFile.delete()
                }
                sample.copy(name = trimmedNew, imagePath = newFile.absolutePath)
            }
        }
        persistIndex(updated)
        if (oldDir.exists() && oldDir.absolutePath != newDir.absolutePath) {
            oldDir.deleteRecursively()
        }
        Log.d(TAG, "Renamed '$oldName' to '$trimmedNew'")
    }

    fun loadThumbnail(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load thumbnail: $path", e)
            null
        }
    }

    private fun persistIndex(samples: List<StoredFaceSample>) {
        val json = JSONObject()
        val array = JSONArray()
        samples.forEach { sample ->
            array.put(
                JSONObject().apply {
                    put("id", sample.id)
                    put("name", sample.name)
                    put("imagePath", sample.imagePath)
                    put("embedding", JSONArray(sample.embedding.toList()))
                }
            )
        }
        json.put("samples", array)
        indexFile.writeText(json.toString())
    }

    private fun cleanupEmptyPersonDir(name: String) {
        val personDir = File(rootDir, sanitizeName(name))
        if (personDir.exists() && personDir.listFiles()?.isEmpty() != false) {
            personDir.deleteRecursively()
        }
    }

    private fun sanitizeName(name: String): String {
        return name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    companion object {
        private const val TAG = "FaceStorage"
    }
}
