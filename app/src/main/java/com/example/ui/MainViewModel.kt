package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.filesystem.ProjectTemplateManager
import com.example.core.model.Project
import com.example.core.model.ProjectType
import com.example.data.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(
    private val repository: ProjectRepository,
    private val filesDir: File
) : ViewModel() {

    private val _searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val projects: StateFlow<List<Project>> = kotlinx.coroutines.flow.combine(
        repository.allProjects,
        _searchQuery
    ) { projects, query ->
        if (query.isBlank()) projects
        else projects.filter { it.name.contains(query, ignoreCase = true) }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun createProject(name: String, type: ProjectType) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val projectPath = File(filesDir, "projects/$name").absolutePath
                ProjectTemplateManager.createProjectFromTemplate(File(filesDir, "projects"), name, type)
                repository.createProject(name, projectPath, type)
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteProject(project)
                File(project.path).deleteRecursively()
            }
        }
    }
}
