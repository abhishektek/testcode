package com.example.core.installer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object ApkInstaller {

    fun installApk(context: Context, apkFile: File) {
        try {
            if (!apkFile.exists()) {
                Toast.makeText(context, "APK file not found", Toast.LENGTH_SHORT).show()
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    Toast.makeText(context, "Please allow unknown app installs for this demo", Toast.LENGTH_LONG).show()
                    return
                }
            }

            // Use FileProvider for the intent
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                } else {
                    Uri.fromFile(apkFile)
                }
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Installation failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
