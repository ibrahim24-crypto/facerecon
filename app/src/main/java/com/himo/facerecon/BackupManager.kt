package com.himo.facerecon

import android.content.Context
import android.net.Uri
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object BackupManager {

    private val DATA_DIRS = arrayOf("enrolled_faces", "classes", "attendance")

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
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val file = File(context.filesDir, entry.name)
                        if (entry.isDirectory) {
                            file.mkdirs()
                        } else {
                            file.parentFile?.mkdirs()
                            FileOutputStream(file).use { out ->
                                zip.copyTo(out)
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun addDirToZip(zip: ZipOutputStream, dir: File, filesDir: File) {
        dir.walkTopDown().forEach { file ->
            val entryName = file.relativeTo(filesDir).path
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
