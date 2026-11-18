package com.himo.facerecon

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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

/**
 * Face images and embeddings are encrypted with an AES-GCM key held by the
 * Android Keystore. Plaintext files written by older builds are migrated on
 * read and rewritten encrypted; an image is only removed after its encrypted
 * replacement decodes successfully, so a failed migration never loses data.
 */
class FaceStorage(context: Context) {

    private val rootDir = File(context.filesDir, "enrolled_faces").apply { mkdirs() }
    private val indexFile = File(rootDir, "index.json")
    private val key: SecretKey by lazy { getOrCreateKey() }

    @Synchronized
    fun loadAll(): List<StoredFaceSample> {
        if (!indexFile.exists()) return emptyList()
        return try {
            val json = JSONObject(String(readMaybeEncrypted(indexFile), StandardCharsets.UTF_8))
            val samplesArray = json.optJSONArray("samples") ?: return emptyList()
            val samples = buildList {
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
            val migrated = samples.map(::migrateLegacyImage)
            if (migrated.indices.any { index -> migrated[index].imagePath != samples[index].imagePath }) {
                persistIndex(migrated)
            }
            migrated
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load face index", e)
            emptyList()
        }
    }

    @Synchronized
    fun saveSample(name: String, bitmap: Bitmap, embedding: FloatArray): StoredFaceSample {
        val personDir = File(rootDir, sanitizeName(name)).apply { mkdirs() }
        val id = UUID.randomUUID().toString()
        val imageFile = File(personDir, "$id.enc")
        val jpegBytes = ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.toByteArray()
        }
        imageFile.writeBytes(encrypt(jpegBytes))

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

    @Synchronized
    fun deleteSample(sampleId: String) {
        val all = loadAll().toMutableList()
        val sample = all.find { it.id == sampleId } ?: return
        deleteImageFile(sample.imagePath)
        all.removeAll { it.id == sampleId }
        persistIndex(all)
        cleanupEmptyPersonDir(sample.name)
    }

    @Synchronized
    fun deletePerson(name: String) {
        val all = loadAll().toMutableList()
        all.filter { it.name == name }.forEach { deleteImageFile(it.imagePath) }
        all.removeAll { it.name == name }
        persistIndex(all)
        val personDir = File(rootDir, sanitizeName(name))
        if (isInsideRoot(personDir)) personDir.deleteRecursively()
    }

    @Synchronized
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
                val newFile = File(newDir, "${sample.id}.enc")
                if (oldFile.exists() && isInsideRoot(oldFile)) {
                    newFile.writeBytes(encrypt(readMaybeEncrypted(oldFile)))
                    oldFile.delete()
                }
                sample.copy(name = trimmedNew, imagePath = newFile.absolutePath)
            }
        }
        persistIndex(updated)
        if (oldDir.exists() && oldDir.absolutePath != newDir.absolutePath && isInsideRoot(oldDir)) {
            oldDir.deleteRecursively()
        }
        Log.d(TAG, "Renamed '$oldName' to '$trimmedNew'")
    }

    @Synchronized
    fun loadThumbnail(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists() || !isInsideRoot(file)) return null
        return try {
            val bytes = readMaybeEncrypted(file)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
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
        val tmp = File(rootDir, INDEX_TMP)
        tmp.writeBytes(encrypt(json.toString().toByteArray(StandardCharsets.UTF_8)))
        if (!tmp.renameTo(indexFile)) {
            indexFile.writeBytes(tmp.readBytes())
            tmp.delete()
        }
    }

    private fun migrateLegacyImage(sample: StoredFaceSample): StoredFaceSample {
        if (sample.imagePath.endsWith(".enc")) return sample
        val oldFile = File(sample.imagePath)
        if (!oldFile.exists() || !isInsideRoot(oldFile)) return sample
        val newFile = File(oldFile.parentFile, "${sample.id}.enc")
        return try {
            newFile.writeBytes(encrypt(oldFile.readBytes()))
            val decrypted = decrypt(newFile.readBytes())
            if (BitmapFactory.decodeByteArray(decrypted, 0, decrypted.size) == null) {
                newFile.delete()
                return sample
            }
            oldFile.delete()
            sample.copy(imagePath = newFile.absolutePath)
        } catch (e: Exception) {
            Log.w(TAG, "Could not migrate image for ${sample.id}, keeping original", e)
            newFile.delete()
            sample
        }
    }

    private fun cleanupEmptyPersonDir(name: String) {
        val personDir = File(rootDir, sanitizeName(name))
        if (isInsideRoot(personDir) && personDir.exists() &&
            personDir.listFiles()?.isEmpty() != false
        ) {
            personDir.deleteRecursively()
        }
    }

    private fun deleteImageFile(path: String) {
        val file = File(path)
        if (isInsideRoot(file)) file.delete()
    }

    private fun isInsideRoot(file: File): Boolean {
        val canonicalRoot = rootDir.canonicalPath + File.separator
        return file.canonicalPath.startsWith(canonicalRoot)
    }

    private fun readMaybeEncrypted(file: File): ByteArray {
        val bytes = file.readBytes()
        return if (bytes.copyOf(MAGIC.size).contentEquals(MAGIC)) decrypt(bytes) else bytes
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        // Android Keystore keys use randomized encryption by default, so the IV
        // must be generated by the Cipher rather than supplied by the caller.
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, key)
        }
        val iv = cipher.iv
        return MAGIC + iv + cipher.doFinal(plain)
    }

    private fun decrypt(encrypted: ByteArray): ByteArray {
        val iv = encrypted.copyOfRange(MAGIC.size, MAGIC.size + IV_BYTES)
        val cipherText = encrypted.copyOfRange(MAGIC.size + IV_BYTES, encrypted.size)
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }.doFinal(cipherText)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setKeySize(KEY_BITS)
        }.build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private fun sanitizeName(name: String): String {
        return name.trim().replace(Regex("[^a-zA-Z0-9._-]"), "_")
    }

    private companion object {
        const val TAG = "FaceStorage"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "facerecon_face_data_key"
        const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
        const val MAGIC_PREFIX = "FREC1"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
        const val KEY_BITS = 256
        const val INDEX_TMP = "index.json.tmp"
        val MAGIC = MAGIC_PREFIX.toByteArray(StandardCharsets.US_ASCII)
    }
}
