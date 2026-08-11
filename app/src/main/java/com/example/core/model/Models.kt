package com.example.core.model

import java.io.File

data class Project(
    val id: String,
    val name: String,
    val path: String,
    val type: ProjectType,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ProjectType {
    EMPTY_APP,
    JAVA_APP,
    KOTLIN_APP,
    XML_ACTIVITY,
    BASIC_ACTIVITY,
    EMPTY_ACTIVITY
}

data class FileNode(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val children: List<FileNode> = emptyList(),
    val extension: String = ""
)

data class EditorTab(
    val file: File,
    val isModified: Boolean = false
)

data class SearchResult(
    val file: File,
    val line: Int,
    val text: String,
    val matchRange: IntRange
)
