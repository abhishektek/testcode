package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.ProjectType

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val path: String,
    val type: ProjectType,
    val createdAt: Long
)
