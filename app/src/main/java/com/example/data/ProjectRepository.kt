package com.example.data

import com.example.core.model.Project
import com.example.core.model.ProjectType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProjectRepository(private val projectDao: ProjectDao) {
    val allProjects: Flow<List<Project>> = projectDao.getAllProjects().map { entities ->
        entities.map { it.toProject() }
    }

    suspend fun createProject(name: String, path: String, type: ProjectType): Project {
        val project = Project(
            id = UUID.randomUUID().toString(),
            name = name,
            path = path,
            type = type
        )
        projectDao.insertProject(project.toEntity())
        return project
    }

    suspend fun deleteProject(project: Project) {
        projectDao.deleteProject(project.toEntity())
    }

    private fun ProjectEntity.toProject() = Project(id, name, path, type, createdAt)
    private fun Project.toEntity() = ProjectEntity(id, name, path, type, createdAt)
}
