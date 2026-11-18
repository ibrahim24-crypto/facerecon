package com.himo.facerecon

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    private val DATA_DIRS = setOf("enrolled_faces", "classes", "attendance")

    fun createBackup(context: Context): ByteArray {
        val outputStream = java.io.ByteArrayOutputStream()
        ZipOutputStream(outputStream).use { zip ->
            for (dirName in DATA_DIRS) {
                val dir = File(context.filesDir, dirName)
                if (dir.exists()) {
                    addDirToZip(zip, dir, context.filesDir)
                }
            }
        }
        return outputStream.toByteArray()
    }

    fun restoreBackup(context: Context, uri: Uri): Boolean {
        return try {
            val filesDir = context.filesDir.canonicalFile
            val rootPrefix = filesDir.canonicalPath + File.separator
            var restoredAny = false

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val relativePath = safeRelativePath(entry.name)
                        val file = File(filesDir, relativePath)
                        check(file.canonicalPath.startsWith(rootPrefix)) {
                            "Zip entry escapes app data directory"
                        }
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { out ->
                                zip.copyTo(out)
                            }
                        }
                        restoredAny = true
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            restoredAny
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Rejects absolute paths, traversal segments, and entries outside the three
     * directories this app owns. This prevents a crafted backup from overwriting
     * arbitrary files inside the app sandbox (Zip Slip).
     */
    private fun safeRelativePath(entryName: String): String {
        val normalized = entryName.replace('\\', '/')
        require(!normalized.startsWith("/")) { "Absolute path in backup" }
        val segments = normalized.split('/').filter { it.isNotEmpty() && it != "." }
        require(segments.isNotEmpty()) { "Empty backup path" }
        require(segments.first() in DATA_DIRS) { "Unexpected backup directory" }
        require(segments.none { it == ".." }) { "Traversal in backup path" }
        return segments.joinToString(File.separator)
    }

    private fun addDirToZip(zip: ZipOutputStream, dir: File, filesDir: File) {
        dir.walkTopDown().forEach { file ->
            val entryName = file.relativeTo(filesDir).path.replace(File.separatorChar, '/')
            if (file.isDirectory) {
                zip.putNextEntry(ZipEntry("$entryName/"))
                zip.closeEntry()
            } else {
                zip.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
    }
}
