package com.example.ui.screens

import kotlinx.serialization.Serializable

@Serializable
object HomeDestination

@Serializable
data class EditorDestination(val projectPath: String, val projectName: String)

@Serializable
object SettingsDestination
