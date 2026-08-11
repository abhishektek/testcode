package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.OffsetMapping
import com.example.core.build.LogLevel
import com.example.core.build.BuildResult
import com.example.core.build.EnvironmentInfo
import com.example.core.build.BuildLog
import com.example.core.model.FileNode
import com.example.core.installer.ApkInstaller
import com.example.editor.SyntaxHighlighter
import com.example.ui.EditorViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectName: String,
    projectPath: String,
    viewModel: EditorViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(projectPath) {
        viewModel.loadProject(projectPath)
    }

    val fileTree by viewModel.fileTree.collectAsState()
    val openFiles by viewModel.openFiles.collectAsState()
    val activeFile by viewModel.activeFile.collectAsState()
    val content by viewModel.fileContent.collectAsState()

    val buildLogs by viewModel.buildLogs.collectAsState()
    val explorerMode by viewModel.explorerMode.collectAsState()
    val isBuilding by viewModel.isBuilding.collectAsState()
    val aiResponse by viewModel.aiResponse.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    var showFileExplorer by remember { mutableStateOf(true) }
    var expandedNodes by remember { mutableStateOf(setOf<String>()) }
    var showBottomPanel by remember { mutableStateOf(false) }
    var isBottomPanelFullScreen by remember { mutableStateOf(false) }
    var selectedBottomTab by remember { mutableIntStateOf(0) }
    var showAiAssistant by remember { mutableStateOf(false) }
    
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearchingProject by viewModel.isSearchingProject.collectAsState()
    val buildResult by viewModel.buildResult.collectAsState()
    
    val envInfo by viewModel.envInfo.collectAsState()
    
    val bottomTabs = listOf("Build", "Environment", "Search", "Terminal", "Problems")

    val dirtyFiles by viewModel.dirtyFiles.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var isSearching by remember { mutableStateOf(false) }

    val context = LocalContext.current

    BackHandler(enabled = showFileExplorer) {
        showFileExplorer = false
    }

    LaunchedEffect(viewModel.buildSuccessEvent) {
        viewModel.buildSuccessEvent.collect {
            viewModel.run()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        if (isSearching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Search in project...") },
                                modifier = Modifier.fillMaxWidth().heightIn(max = 48.dp),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { isSearching = false }) {
                                        Icon(Icons.Default.Close, contentDescription = "Close Search")
                                    }
                                }
                            )
                        } else {
                            Text(projectName)
                        }
                    },
                    navigationIcon = {
                        Row {
                            IconButton(onClick = {
                                if (showFileExplorer) {
                                    showFileExplorer = false
                                } else {
                                    onBack()
                                }
                            }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                            IconButton(onClick = { showFileExplorer = !showFileExplorer }) {
                                Icon(if (showFileExplorer) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu, contentDescription = "Toggle Sidebar")
                            }
                        }
                    },
                    actions = {
                        if (!isSearching) {
                            IconButton(onClick = { isSearching = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                        IconButton(onClick = { showAiAssistant = true }) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI Assistant", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.saveActiveFile() }) {
                            Icon(Icons.Default.Save, contentDescription = "Save")
                        }
                        IconButton(onClick = { 
                            viewModel.build()
                            showBottomPanel = true
                            isBottomPanelFullScreen = true
                        }) {
                            Icon(Icons.Default.Build, contentDescription = "Build")
                        }
                        IconButton(onClick = { viewModel.run() }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run")
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    if (activeFile != null) {
                        EditorToolbar(
                            onSymbolClick = { symbol ->
                                // This would ideally interact with the TextFieldValue to insert text
                                // For simplicity, we'll just append it for now in a more robust implementation
                            }
                        )
                    }
                    if (showBottomPanel && !isBottomPanelFullScreen) {
                        BottomPanel(
                            tabs = bottomTabs,
                            selectedTab = selectedBottomTab,
                            onTabSelect = { selectedBottomTab = it },
                            onClose = { showBottomPanel = false },
                            onExpand = { isBottomPanelFullScreen = true },
                            isFullScreen = false,
                            content = {
                                BottomPanelContent(
                                    selectedTab = selectedBottomTab,
                                    viewModel = viewModel,
                                    buildLogs = buildLogs,
                                    buildResult = buildResult,
                                    searchResults = searchResults,
                                    envInfo = envInfo,
                                    context = context
                                )
                            }
                        )
                    }
                }
            }
        ) { padding ->
            Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                if (showFileExplorer) {
                    FileExplorer(
                        files = fileTree,
                        expandedNodes = expandedNodes,
                        onToggleExpand = { path ->
                            expandedNodes = if (expandedNodes.contains(path)) {
                                expandedNodes - path
                            } else {
                                expandedNodes + path
                            }
                        },
                        onFileClick = { 
                            viewModel.openFile(File(it.path))
                            showFileExplorer = false
                        },
                        currentMode = explorerMode,
                        onModeChange = { viewModel.setExplorerMode(it) },
                        modifier = Modifier.width(250.dp).fillMaxHeight()
                    )
                    VerticalDivider()
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Tab bar
                        LazyRow(modifier = Modifier.fillMaxWidth().height(40.dp)) {
                            items(openFiles) { file ->
                                EditorTab(
                                    file = file,
                                    isActive = file == activeFile,
                                    isDirty = dirtyFiles.contains(file),
                                    onClick = { viewModel.openFile(file) },
                                    onClose = { viewModel.closeFile(file) }
                                )
                            }
                        }
                        HorizontalDivider()

                        // Editor
                        activeFile?.let { file ->
                            CodeEditor(
                                content = content,
                                extension = file.extension,
                                onContentChange = { viewModel.onContentChange(it) },
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )
                        } ?: run {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No file open")
                            }
                        }
                    }

                    // Transparent overlay to catch taps when sidebar is open
                    if (showFileExplorer) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    showFileExplorer = false
                                }
                        )
                    }
                }
            }
        }

        if (showBottomPanel && isBottomPanelFullScreen) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                BottomPanel(
                    tabs = bottomTabs,
                    selectedTab = selectedBottomTab,
                    onTabSelect = { selectedBottomTab = it },
                    onClose = { 
                        showBottomPanel = false
                        isBottomPanelFullScreen = false
                    },
                    onExpand = { isBottomPanelFullScreen = false },
                    isFullScreen = true,
                    content = {
                        BottomPanelContent(
                            selectedTab = selectedBottomTab,
                            viewModel = viewModel,
                            buildLogs = buildLogs,
                            buildResult = buildResult,
                            searchResults = searchResults,
                            envInfo = envInfo,
                            context = context
                        )
                    }
                )
            }
        }
    }

    if (showAiAssistant) {
        AiAssistantDialog(
            response = aiResponse,
            isLoading = isAiLoading,
            onDismiss = { showAiAssistant = false },
            onAsk = { viewModel.askAi(it) }
        )
    }

    if (buildResult != null && !showBottomPanel) {
        showBottomPanel = true
        selectedBottomTab = 0
    }
}

@Composable
fun BottomPanel(
    tabs: List<String>,
    selectedTab: Int,
    onTabSelect: (Int) -> Unit,
    onClose: () -> Unit,
    onExpand: () -> Unit,
    isFullScreen: Boolean,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = if (isFullScreen) Modifier.fillMaxSize() else Modifier.fillMaxWidth().height(250.dp),
        tonalElevation = 8.dp,
        shadowElevation = 8.dp
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    edgePadding = 16.dp,
                    modifier = Modifier.weight(1f),
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { onTabSelect(index) },
                            text = { Text(title, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                Row {
                    IconButton(onClick = onExpand) {
                        Icon(
                            if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            contentDescription = if (isFullScreen) "Minimize" else "Expand",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                    }
                }
            }
            HorizontalDivider()
            Box(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                content()
            }
        }
    }
}

@Composable
fun BottomPanelContent(
    selectedTab: Int,
    viewModel: EditorViewModel,
    buildLogs: List<com.example.core.build.BuildLog>,
    buildResult: com.example.core.build.BuildResult?,
    searchResults: List<com.example.core.model.SearchResult>,
    envInfo: com.example.core.build.EnvironmentInfo?,
    context: android.content.Context
) {
    when (selectedTab) {
        0 -> BuildOutput(
            logs = buildLogs,
            result = buildResult,
            isBuilding = viewModel.isBuilding.value,
            envInfo = envInfo,
            onBuild = { viewModel.build() },
            onRefreshEnv = { viewModel.refreshEnvInfo() },
            onInstall = { result -> ApkInstaller.installApk(context, result.apkFile!!) },
            onShare = { result -> 
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", result.apkFile!!)
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Share APK"))
            },
            onExport = { result ->
                val downloads = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val target = File(downloads, result.apkFile!!.name)
                try {
                    result.apkFile!!.inputStream().use { input ->
                        target.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    android.widget.Toast.makeText(context, "Exported to Downloads", android.widget.Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        )
        1 -> EnvironmentView(envInfo = envInfo, onRefresh = { viewModel.refreshEnvInfo() })
        2 -> SearchResultsView(
            results = searchResults,
            onResultClick = { viewModel.openFile(it) },
            onReplace = { find, replace -> viewModel.replaceInProject(find, replace) }
        )
        3 -> TerminalOutput()
        4 -> ProblemsOutput()
    }
}

@Composable
fun EnvironmentView(
    envInfo: com.example.core.build.EnvironmentInfo?,
    onRefresh: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Build Environment", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh Environment")
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        if (envInfo == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    EnvironmentItem(
                        label = "JDK",
                        status = envInfo.javaExecutablePath != null,
                        detail = envInfo.javaExecutablePath ?: "Not detected"
                    )
                }
                item {
                    EnvironmentItem(
                        label = "Android SDK",
                        status = envInfo.androidHome != null,
                        detail = envInfo.androidHome ?: "Not detected"
                    )
                }
                item {
                    EnvironmentItem(
                        label = "Android Platforms",
                        status = envInfo.platforms.isNotEmpty(),
                        detail = if (envInfo.platforms.isEmpty()) "No platforms found" else envInfo.platforms.joinToString()
                    )
                }
                item {
                    EnvironmentItem(
                        label = "Build Tools",
                        status = envInfo.buildTools.isNotEmpty(),
                        detail = if (envInfo.buildTools.isEmpty()) "No build tools found" else envInfo.buildTools.joinToString()
                    )
                }
                item {
                    EnvironmentItem(
                        label = "Gradle Wrapper",
                        status = envInfo.gradlewExists,
                        detail = buildString {
                            append(if (envInfo.gradlewExists) "gradlew found" else "gradlew missing")
                            if (envInfo.wrapperJarExists) append(", jar found")
                            if (envInfo.wrapperPropsExists) append(", props found")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EnvironmentItem(
    label: String,
    status: Boolean,
    detail: String
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        onClick = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (status) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (status) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun FileExplorer(
    files: List<FileNode>,
    expandedNodes: Set<String>,
    onToggleExpand: (String) -> Unit,
    onFileClick: (FileNode) -> Unit,
    modifier: Modifier = Modifier,
    level: Int = 0,
    currentMode: EditorViewModel.ExplorerMode = EditorViewModel.ExplorerMode.PROJECT,
    onModeChange: (EditorViewModel.ExplorerMode) -> Unit = {}
) {
    Column(modifier = if (level == 0) modifier.background(MaterialTheme.colorScheme.surface) else Modifier) {
        if (level == 0) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AssistChip(
                    onClick = { onModeChange(EditorViewModel.ExplorerMode.PROJECT) },
                    label = { Text("Project", style = MaterialTheme.typography.labelSmall) },
                    border = if (currentMode == EditorViewModel.ExplorerMode.PROJECT) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = if (currentMode == EditorViewModel.ExplorerMode.PROJECT) AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.primary) else AssistChipDefaults.assistChipColors()
                )
                AssistChip(
                    onClick = { onModeChange(EditorViewModel.ExplorerMode.DEVICE) },
                    label = { Text("Storage", style = MaterialTheme.typography.labelSmall) },
                    border = if (currentMode == EditorViewModel.ExplorerMode.DEVICE) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
                    colors = if (currentMode == EditorViewModel.ExplorerMode.DEVICE) AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.primary) else AssistChipDefaults.assistChipColors()
                )
                IconButton(onClick = { 
                    if (level == 0) {
                        // Refresh logic could go here
                    }
                }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(16.dp))
                }
            }
            HorizontalDivider()
        }
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(files) { node ->
                FileExplorerItem(
                    node = node,
                    level = level,
                    isExpanded = expandedNodes.contains(node.path),
                    expandedNodes = expandedNodes,
                    onToggleExpand = onToggleExpand,
                    onFileClick = onFileClick
                )
            }
        }
    }
}

@Composable
fun FileExplorerItem(
    node: FileNode,
    level: Int,
    isExpanded: Boolean,
    expandedNodes: Set<String>,
    onToggleExpand: (String) -> Unit,
    onFileClick: (FileNode) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (node.isDirectory) {
                        onToggleExpand(node.path)
                    } else {
                        onFileClick(node)
                    }
                }
                .padding(start = (level * 16 + 8).dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (node.isDirectory) {
                    if (isExpanded) Icons.Default.FolderOpen else Icons.Default.Folder
                } else {
                    Icons.Default.Description
                },
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (node.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            if (node.isDirectory) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        if (node.isDirectory && isExpanded) {
            var children by remember(node.path) { mutableStateOf<List<FileNode>>(emptyList()) }
            
            LaunchedEffect(isExpanded) {
                if (isExpanded) {
                    val result = withContext(Dispatchers.IO) {
                        com.example.core.filesystem.FileSystemManager.listFiles(File(node.path))
                    }
                    children = result
                }
            }

            children.forEach { child ->
                FileExplorerItem(
                    node = child,
                    level = level + 1,
                    isExpanded = expandedNodes.contains(child.path),
                    expandedNodes = expandedNodes,
                    onToggleExpand = onToggleExpand,
                    onFileClick = onFileClick
                )
            }
        }
    }
}

@Composable
fun EditorTab(
    file: File,
    isActive: Boolean,
    isDirty: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .background(if (isActive) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isDirty) {
            Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
            Spacer(Modifier.width(4.dp))
        }
        Text(file.name, style = MaterialTheme.typography.labelSmall, color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = { onClose() }, modifier = Modifier.size(16.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(12.dp))
        }
    }
}

@Composable
fun CodeEditor(
    content: String,
    extension: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Use a key to only reset the state when the actual file changes, not on every content sync
    var textFieldValue by remember(extension) {
        mutableStateOf(TextFieldValue(content))
    }

    // Sync content if it changed externally (e.g. from Save or Undo)
    LaunchedEffect(content) {
        if (content != textFieldValue.text) {
            textFieldValue = textFieldValue.copy(text = content)
        }
    }

    val syntaxTransformation = remember(extension) {
        SyntaxHighlightTransformation(extension)
    }

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp)
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                onContentChange(it.text)
            },
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            visualTransformation = syntaxTransformation
        )
    }
}

class SyntaxHighlightTransformation(val extension: String) : VisualTransformation {
    private var lastText: String? = null
    private var lastResult: AnnotatedString? = null

    override fun filter(text: AnnotatedString): TransformedText {
        val currentText = text.text
        if (currentText == lastText && lastResult != null) {
            return TransformedText(lastResult!!, OffsetMapping.Identity)
        }
        
        val highlighted = SyntaxHighlighter.highlight(currentText, extension)
        lastText = currentText
        lastResult = highlighted
        
        return TransformedText(highlighted, OffsetMapping.Identity)
    }
}

@Composable
fun EditorToolbar(onSymbolClick: (String) -> Unit) {
    val symbols = listOf("{", "}", "(", ")", "[", "]", "<", ">", "/", "=", ";", ":", "\"", "'", "_", "-")
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(symbols) { symbol ->
            Surface(
                modifier = Modifier
                    .clickable { onSymbolClick(symbol) },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Text(
                    text = symbol,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun BuildOutput(
    logs: List<com.example.core.build.BuildLog>,
    result: com.example.core.build.BuildResult?,
    isBuilding: Boolean,
    envInfo: com.example.core.build.EnvironmentInfo?,
    onBuild: () -> Unit,
    onRefreshEnv: () -> Unit,
    onInstall: (com.example.core.build.BuildResult) -> Unit,
    onShare: (com.example.core.build.BuildResult) -> Unit,
    onExport: (com.example.core.build.BuildResult) -> Unit
) {
    var showDiagnosticDialog by remember { mutableStateOf(false) }

    if (showDiagnosticDialog && envInfo != null) {
        ToolchainDiagnosticDialog(
            envInfo = envInfo,
            onDismiss = { showDiagnosticDialog = false },
            onRefresh = onRefreshEnv
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F0F))) {
        // Header / Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A1A))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isBuilding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(12.dp))
                }
                Text(
                    "BUILD CONSOLE",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
            
            Row {
                OutlinedButton(
                    onClick = { showDiagnosticDialog = true },
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Icon(Icons.Default.SettingsSuggest, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    Spacer(Modifier.width(4.dp))
                    Text("DIAGNOSTICS", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                
                Spacer(Modifier.width(8.dp))

                if (!isBuilding && result == null) {
                    Button(
                        onClick = onBuild,
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("BUILD APK", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        
        HorizontalDivider(color = Color.DarkGray, thickness = 1.dp)

        // Terminal Output
        Box(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            val listState = rememberLazyListState()
            
            // Auto-scroll to bottom - throttled scroll to avoid UI lag
            LaunchedEffect(logs.size) {
                if (logs.isNotEmpty()) {
                    // Only scroll if we are near the bottom or it's a small list
                    val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                    if (lastVisibleIndex >= logs.size - 5 || logs.size < 50) {
                        listState.scrollToItem(logs.size - 1)
                    }
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log.message,
                        color = when (log.level) {
                            LogLevel.ERROR -> Color(0xFFF44336)
                            LogLevel.SUCCESS -> Color(0xFF4CAF50)
                            LogLevel.WARNING -> Color(0xFFFFC107)
                            else -> Color(0xFFDDDDDD)
                        },
                        style = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    )
                }
            }
        }

        // Result Banner and Actions
        if (result != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF1E1E1E),
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = null,
                            tint = if (result.isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (result.isSuccess) "BUILD SUCCESSFUL" else "BUILD FAILED",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (result.isSuccess) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )
                    }
                    
                    if (result.isSuccess && result.apkFile != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "File: ${result.apkFile.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray
                        )
                        Text(
                            "Size: ${String.format("%.2f", result.apkSize / (1024.0 * 1024.0))} MB | Time: ${result.buildTimeMs / 1000}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val buttonModifier = Modifier.weight(1f).height(44.dp)
                            
                            Button(
                                onClick = { onInstall(result) },
                                modifier = buttonModifier,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Install", style = MaterialTheme.typography.labelLarge)
                            }
                            
                            OutlinedButton(
                                onClick = { onShare(result) },
                                modifier = buttonModifier,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color.DarkGray)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Share", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    } else if (!result.isSuccess) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            result.error ?: "An unknown error occurred during the build process. Check the logs above for more details.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336)
                        )
                        Button(
                            onClick = onBuild,
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(44.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("RETRY BUILD", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchResultsView(
    results: List<com.example.core.model.SearchResult>,
    onResultClick: (File) -> Unit,
    onReplace: (String, String) -> Unit
) {
    var replacementText by remember { mutableStateOf("") }
    var searchText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                label = { Text("Find", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f).height(48.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = replacementText,
                onValueChange = { replacementText = it },
                label = { Text("Replace", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.weight(1f).height(48.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { onReplace(searchText, replacementText) },
                modifier = Modifier.height(40.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Text("Replace All", style = MaterialTheme.typography.labelSmall)
            }
        }
        
        Spacer(Modifier.height(8.dp))
        
        if (results.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results found", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(results) { result ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResultClick(result.file) }
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = "${result.file.name}:${result.line}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = result.text,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
fun TerminalOutput() {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("> _", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp))
    }
}

@Composable
fun ProblemsOutput() {
    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("No problems found", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun AiAssistantDialog(
    response: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onAsk: (String) -> Unit
) {
    var prompt by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Assistant") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("How can I help?") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                if (response.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        LazyColumn(modifier = Modifier.padding(8.dp)) {
                            item {
                                Text(
                                    text = response,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                } else if (isLoading) {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onAsk(prompt) },
                enabled = prompt.isNotBlank() && !isLoading
            ) {
                Text("Ask AI")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ToolchainDiagnosticDialog(
    envInfo: com.example.core.build.EnvironmentInfo,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Toolchain Diagnostics") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { 
                    DiagnosticSection("JDK", envInfo.javaExecutablePath != null) {
                        DiagnosticItem("Path", envInfo.javaExecutablePath ?: "Missing")
                        DiagnosticItem("Version", envInfo.javaVersion ?: "Unknown")
                        DiagnosticItem("Major Version", envInfo.javaMajorVersion?.toString() ?: "Unknown")
                    }
                }
                
                item { 
                    DiagnosticSection("Android SDK", envInfo.androidHome != null) {
                        DiagnosticItem("Path", envInfo.androidHome ?: "Not Found")
                        DiagnosticItem("Compile SDK", envInfo.compileSdk ?: "Unable to detect")
                    }
                }
                
                item { 
                    DiagnosticSection("Platform", envInfo.androidJarExists) {
                        DiagnosticItem("Path", "${envInfo.androidHome}/platforms/android-${envInfo.compileSdk}")
                        DiagnosticItem("android.jar", if (envInfo.androidJarExists) "FOUND" else "MISSING")
                    }
                }
                
                item { 
                    DiagnosticSection("Build Tools", envInfo.aapt2Exists && envInfo.d8Exists) {
                        DiagnosticItem("Version", envInfo.buildTools.lastOrNull() ?: "Missing")
                        DiagnosticItem("Path", "${envInfo.androidHome}/build-tools/${envInfo.buildTools.lastOrNull()}")
                        DiagnosticItem("aapt2", if (envInfo.aapt2Exists) "FOUND" else "MISSING")
                        DiagnosticItem("d8", if (envInfo.d8Exists) "FOUND" else "MISSING")
                        DiagnosticItem("zipalign", if (envInfo.zipalignExists) "FOUND" else "MISSING")
                    }
                }
                
                item { 
                    DiagnosticSection("Gradle Wrapper / Launcher", envInfo.gradlewExists && envInfo.wrapperPropsExists) {
                        DiagnosticItem("gradlew", if (envInfo.gradlewExists) "FOUND" else "MISSING")
                        DiagnosticItem("Wrapper JAR", if (envInfo.wrapperJarExists) "FOUND" else "NOT REQUIRED (Codra launcher)" )
                        DiagnosticItem("Wrapper Properties", if (envInfo.wrapperPropsExists) "FOUND" else "MISSING")
                        DiagnosticItem("Distribution URL", envInfo.distributionUrl ?: "Derived from AGP")
                    }
                }
                
                item { 
                    DiagnosticSection("Gradle Distribution", envInfo.gradleDistributionFound) {
                        DiagnosticItem("Version", envInfo.gradleVersion ?: "Unknown")
                        DiagnosticItem("Local Path", envInfo.gradleDistributionPath ?: "Not Cached")
                    }
                }
                
                item { 
                    DiagnosticSection("Android Gradle Plugin", envInfo.agpVersion != null) {
                        DiagnosticItem("Version", envInfo.agpVersion ?: "Unknown")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        },
        dismissButton = {
            TextButton(onClick = onRefresh) { Text("REFRESH") }
        }
    )
}

@Composable
fun DiagnosticSection(title: String, isOk: Boolean, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (isOk) Icons.Default.CheckCircle else Icons.Default.Error,
                contentDescription = null,
                tint = if (isOk) Color(0xFF4CAF50) else Color(0xFFF44336),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Column(modifier = Modifier.padding(start = 24.dp)) {
            content()
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color.DarkGray.copy(alpha = 0.5f))
    }
}

@Composable
fun DiagnosticItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.width(100.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, color = Color.LightGray, modifier = Modifier.weight(1f))
    }
}
