package com.example.core.filesystem

import com.example.core.model.FileNode
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

object FileSystemManager {

    fun listFiles(dir: File): List<FileNode> {
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles()?.map { file ->
            FileNode(
                name = file.name,
                path = file.absolutePath,
                isDirectory = file.isDirectory,
                extension = file.extension,
                children = if (file.isDirectory) emptyList() else emptyList() // Lazy load children
            )
        }?.sortedBy { !it.isDirectory } ?: emptyList()
    }

    fun createFile(parent: File, name: String): File {
        val file = File(parent, name)
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }

    fun createDirectory(parent: File, name: String): File {
        val dir = File(parent, name)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun delete(file: File): Boolean {
        return if (file.isDirectory) {
            file.deleteRecursively()
        } else {
            file.delete()
        }
    }

    fun rename(file: File, newName: String): File {
        val newFile = File(file.parentFile, newName)
        file.renameTo(newFile)
        return newFile
    }

    fun readFile(file: File): String {
        return try {
            file.readText()
        } catch (e: Exception) {
            "Error reading file: ${e.message}"
        }
    }

    fun writeFile(file: File, content: String) {
        try {
            file.writeText(content)
        } catch (e: Exception) {
            // Log error
        }
    }

    fun copy(source: File, destination: File) {
        if (source.isDirectory) {
            source.copyRecursively(destination, overwrite = true)
        } else {
            source.copyTo(destination, overwrite = true)
        }
    }
}
