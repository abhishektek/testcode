package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.ProjectRepository
import com.example.ui.EditorViewModel
import com.example.ui.MainViewModel
import com.example.ui.screens.EditorDestination
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.HomeDestination
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.CodraTheme

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var projectRepository: ProjectRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "codra_studio_db"
        ).build()
        projectRepository = ProjectRepository(database.projectDao())

        setContent {
            CodraTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = HomeDestination) {
                    composable<HomeDestination> {
                        val viewModel: MainViewModel = viewModel(
                            factory = object : ViewModelProvider.Factory {
                                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                                    if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                                        @Suppress("UNCHECKED_CAST")
                                        return MainViewModel(projectRepository, filesDir) as T
                                    }
                                    throw IllegalArgumentException("Unknown ViewModel class")
                                }
                            }
                        )
                        HomeScreen(
                            viewModel = viewModel,
                            onNavigateToEditor = { project ->
                                navController.navigate(EditorDestination(project.path, project.name))
                            }
                        )
                    }

                    composable<EditorDestination> { backStackEntry ->
                        val dest: EditorDestination = backStackEntry.toRoute()
                        val viewModel: EditorViewModel = viewModel()
                        EditorScreen(
                            projectName = dest.projectName,
                            projectPath = dest.projectPath,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                // System is running low on memory while app is in foreground
            }
            TRIM_MEMORY_UI_HIDDEN -> {
                // App's UI is no longer visible
            }
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                // App is in background and memory is low, help the system
                System.gc()
            }
        }
    }
}
