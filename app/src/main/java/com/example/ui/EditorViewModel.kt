package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.GeminiManager
import com.example.core.build.*
import com.example.core.filesystem.FileSystemManager
import com.example.core.model.FileNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditorViewModel : ViewModel() {

    private val _fileTree = MutableStateFlow<List<FileNode>>(emptyList())
    val fileTree: StateFlow<List<FileNode>> = _fileTree.asStateFlow()

    private val _openFiles = MutableStateFlow<List<File>>(emptyList())
    val openFiles: StateFlow<List<File>> = _openFiles.asStateFlow()

    private val _activeFile = MutableStateFlow<File?>(null)
    val activeFile: StateFlow<File?> = _activeFile.asStateFlow()

    private val _fileContent = MutableStateFlow("")
    val fileContent: StateFlow<String> = _fileContent.asStateFlow()

    private val _dirtyFiles = MutableStateFlow<Set<File>>(emptySet())
    val dirtyFiles: StateFlow<Set<File>> = _dirtyFiles.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<com.example.core.model.SearchResult>>(emptyList())
    val searchResults: StateFlow<List<com.example.core.model.SearchResult>> = _searchResults.asStateFlow()

    private val _isSearchingProject = MutableStateFlow(false)
    val isSearchingProject: StateFlow<Boolean> = _isSearchingProject.asStateFlow()

    private val _buildLogs = MutableStateFlow<List<BuildLog>>(emptyList())
    val buildLogs: StateFlow<List<BuildLog>> = _buildLogs.asStateFlow()

    private val _buildResult = MutableStateFlow<BuildResult?>(null)
    val buildResult: StateFlow<BuildResult?> = _buildResult.asStateFlow()

    private val _envInfo = MutableStateFlow<EnvironmentInfo?>(null)
    val envInfo: StateFlow<EnvironmentInfo?> = _envInfo.asStateFlow()

    private val _aiResponse = MutableStateFlow("")
    val aiResponse: StateFlow<String> = _aiResponse.asStateFlow()

    private val _isBuilding = MutableStateFlow(false)
    val isBuilding: StateFlow<Boolean> = _isBuilding.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _buildSuccessEvent = MutableSharedFlow<Unit>()
    val buildSuccessEvent = _buildSuccessEvent.asSharedFlow()

    fun dismissBuildResult() {
        _buildResult.value = null
    }

    enum class ExplorerMode { PROJECT, DEVICE }
    private val _explorerMode = MutableStateFlow(ExplorerMode.PROJECT)
    val explorerMode: StateFlow<ExplorerMode> = _explorerMode.asStateFlow()

    private var projectRoot: File? = null

    fun loadProject(path: String) {
        projectRoot = File(path)
        _explorerMode.value = ExplorerMode.PROJECT
        refreshFileTree()
        refreshEnvInfo()
    }

    fun refreshEnvInfo() {
        projectRoot?.let {
            viewModelScope.launch {
                _envInfo.value = BuildManager.getEnvironmentInfo(it.absolutePath)
            }
        }
    }

    fun setExplorerMode(mode: ExplorerMode) {
        _explorerMode.value = mode
        refreshFileTree()
    }

    fun build(variant: String = "debug") {
        projectRoot?.let {
            viewModelScope.launch {
                _isBuilding.value = true
                _buildLogs.value = emptyList()
                _buildResult.value = null
                
                val currentLogs = mutableListOf<BuildLog>()
                var lastUpdateTime = System.currentTimeMillis()
                
                withContext(Dispatchers.IO) {
                    BuildManager.buildProject(it.absolutePath, variant).collect { log ->
                        currentLogs.add(log)
                        val now = System.currentTimeMillis()
                        // Batch updates every 150ms or for non-INFO logs to keep UI responsive but efficient
                        if (now - lastUpdateTime > 150 || log.level != LogLevel.INFO) {
                            _buildLogs.value = ArrayList(currentLogs)
                            lastUpdateTime = now
                        }
                    }
                }
                _buildLogs.value = ArrayList(currentLogs)
                _isBuilding.value = false
                _buildResult.value = BuildManager.getBuildResult(it.absolutePath)
                if (_buildResult.value?.isSuccess == true) {
                    _buildSuccessEvent.emit(Unit)
                }
            }
        }
    }

    fun run() {
        // Run logic is now integrated into the build flow results
        // or can be triggered manually from the build result panel
        build("debug")
    }

    fun askAi(prompt: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponse.value = "AI is thinking..."
            val response = GeminiManager.askAi(prompt)
            _aiResponse.value = response
            _isAiLoading.value = false
        }
    }

    fun refreshFileTree() {
        val root = if (_explorerMode.value == ExplorerMode.PROJECT) {
            projectRoot
        } else {
            // Use external storage root or internal if not available
            android.os.Environment.getExternalStorageDirectory() ?: File("/")
        }
        
        root?.let {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    _fileTree.value = FileSystemManager.listFiles(it)
                } catch (e: Exception) {
                    _fileTree.value = emptyList()
                }
            }
        }
    }

    fun openFile(file: File) {
        if (file.isDirectory) return
        viewModelScope.launch(Dispatchers.IO) {
            val content = FileSystemManager.readFile(file)
            withContext(Dispatchers.Main) {
                if (!_openFiles.value.contains(file)) {
                    _openFiles.value = _openFiles.value + file
                }
                _activeFile.value = file
                _fileContent.value = content
                // When opening, it's not dirty yet (unless it was already open and dirty, which is handled by set logic)
            }
        }
    }

    fun closeFile(file: File) {
        val newFiles = _openFiles.value.filter { it != file }
        _openFiles.value = newFiles
        if (_activeFile.value == file) {
            val nextFile = newFiles.lastOrNull()
            _activeFile.value = nextFile
            if (nextFile != null) {
                viewModelScope.launch(Dispatchers.IO) {
                    val content = FileSystemManager.readFile(nextFile)
                    withContext(Dispatchers.Main) {
                        _fileContent.value = content
                    }
                }
            } else {
                _fileContent.value = ""
            }
        }
    }

    fun onContentChange(newContent: String) {
        _fileContent.value = newContent
        _activeFile.value?.let {
            _dirtyFiles.value = _dirtyFiles.value + it
        }
    }

    private var searchJob: kotlinx.coroutines.Job? = null

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(300) // Debounce search
            performSearch(query)
        }
    }

    fun performSearch(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        _isSearchingProject.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val results = mutableListOf<com.example.core.model.SearchResult>()
            projectRoot?.let { root ->
                searchInDirectory(root, query, results)
            }
            withContext(Dispatchers.Main) {
                _searchResults.value = results
                _isSearchingProject.value = false
            }
        }
    }

    private fun searchInDirectory(directory: File, query: String, results: MutableList<com.example.core.model.SearchResult>) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (!file.name.startsWith(".") && file.name != "build" && file.name != "node_modules") {
                    searchInDirectory(file, query, results)
                }
            } else {
                searchInFile(file, query, results)
            }
        }
    }

    private fun searchInFile(file: File, query: String, results: MutableList<com.example.core.model.SearchResult>) {
        try {
            val lines = file.readLines()
            lines.forEachIndexed { index, line ->
                if (line.contains(query, ignoreCase = true)) {
                    val start = line.indexOf(query, ignoreCase = true)
                    results.add(com.example.core.model.SearchResult(file, index + 1, line.trim(), start until start + query.length))
                }
            }
        } catch (e: Exception) {
            // Skip binary or unreadable files
        }
    }

    fun replaceInProject(query: String, replacement: String) {
        viewModelScope.launch(Dispatchers.IO) {
            projectRoot?.let { root ->
                replaceInDirectory(root, query, replacement)
            }
            withContext(Dispatchers.Main) {
                refreshFileTree()
                _activeFile.value?.let { openFile(it) }
            }
        }
    }

    private fun replaceInDirectory(directory: File, query: String, replacement: String) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                if (!file.name.startsWith(".") && file.name != "build" && file.name != "node_modules") {
                    replaceInDirectory(file, query, replacement)
                }
            } else {
                try {
                    val content = file.readText()
                    if (content.contains(query, ignoreCase = true)) {
                        val newContent = content.replace(query, replacement, ignoreCase = true)
                        file.writeText(newContent)
                    }
                } catch (e: Exception) {}
            }
        }
    }

    fun saveActiveFile() {
        _activeFile.value?.let { file ->
            val content = _fileContent.value
            viewModelScope.launch(Dispatchers.IO) {
                FileSystemManager.writeFile(file, content)
                withContext(Dispatchers.Main) {
                    _dirtyFiles.value = _dirtyFiles.value - file
                }
            }
        }
    }
}
