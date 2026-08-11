package com.example.core.filesystem

import com.example.core.model.ProjectType
import java.io.File

object ProjectTemplateManager {

    fun createProjectFromTemplate(root: File, projectName: String, type: ProjectType) {
        val projectDir = File(root, projectName)
        projectDir.mkdirs()

        // Common structure
        val appDir = File(projectDir, "app")
        val srcDir = File(appDir, "src/main")
        val javaDir = File(srcDir, "java/com/example/$projectName")
        val resDir = File(srcDir, "res")
        val layoutDir = File(resDir, "layout")
        val valuesDir = File(resDir, "values")

        javaDir.mkdirs()
        layoutDir.mkdirs()
        valuesDir.mkdirs()

        // build.gradle (root)
        File(projectDir, "build.gradle").writeText("""
            // Top-level build file
            buildscript {
                repositories {
                    google()
                    mavenCentral()
                }
                dependencies {
                    classpath 'com.android.tools.build:gradle:8.2.2'
                }
            }
        """.trimIndent())

        // settings.gradle
        File(projectDir, "settings.gradle").writeText("""
            include ':app'
            rootProject.name = "$projectName"
        """.trimIndent())

        // app/build.gradle
        File(appDir, "build.gradle").writeText("""
            apply plugin: 'com.android.application'

            android {
                namespace "com.example.$projectName"
                compileSdk 34

                defaultConfig {
                    applicationId "com.example.$projectName"
                    minSdk 24
                    targetSdk 34
                    versionCode 1
                    versionName "1.0"
                }
            }
        """.trimIndent())

        // AndroidManifest.xml
        File(srcDir, "AndroidManifest.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android">
                <application
                    android:allowBackup="true"
                    android:icon="@mipmap/ic_launcher"
                    android:label="$projectName"
                    android:theme="@style/Theme.AppCompat.Light">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                    </activity>
                </application>
            </manifest>
        """.trimIndent())

        // MainActivity
        val mainActivityContent = when (type) {
            ProjectType.KOTLIN_APP, ProjectType.EMPTY_ACTIVITY -> {
                """
                package com.example.$projectName

                import android.os.Bundle
                import androidx.appcompat.app.AppCompatActivity

                class MainActivity : AppCompatActivity() {
                    override fun onCreate(savedInstanceState: Bundle?) {
                        super.onCreate(savedInstanceState)
                        setContentView(R.layout.activity_main)
                    }
                }
                """.trimIndent()
            }
            else -> {
                """
                package com.example.$projectName;

                import android.os.Bundle;
                import androidx.appcompat.app.AppCompatActivity;

                public class MainActivity extends AppCompatActivity {
                    @Override
                    protected void onCreate(Bundle savedInstanceState) {
                        super.onCreate(savedInstanceState);
                        setContentView(R.layout.activity_main);
                    }
                }
                """.trimIndent()
            }
        }
        val ext = if (type == ProjectType.KOTLIN_APP || type == ProjectType.EMPTY_ACTIVITY) "kt" else "java"
        File(javaDir, "MainActivity.$ext").writeText(mainActivityContent)

        // activity_main.xml
        File(layoutDir, "activity_main.xml").writeText("""
            <?xml version="1.0" encoding="utf-8"?>
            <RelativeLayout xmlns:android="http://schemas.android.com/apk/res/android"
                android:layout_width="match_parent"
                android:layout_height="match_parent">
                <TextView
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="Hello Codra Studio!"
                    android:layout_centerInParent="true" />
            </RelativeLayout>
        """.trimIndent())

        // strings.xml
        File(valuesDir, "strings.xml").writeText("""
            <resources>
                <string name="app_name">$projectName</string>
            </resources>
        """.trimIndent())

        // Create a dummy gradlew for fallback
        File(projectDir, "gradlew").apply {
            val dollar = "$"
            writeText("""
                #!/system/bin/sh
                # Minimal gradle wrapper fallback for Android
                if command -v gradle >/dev/null 2>&1; then
                    exec gradle "${dollar}@"
                else
                    # Try to find gradle in common paths if not in PATH
                    for g in /usr/bin/gradle /usr/local/bin/gradle /opt/gradle/bin/gradle; do
                        if [ -x "${dollar}g" ]; then
                            exec "${dollar}g" "${dollar}@"
                        fi
                    done
                    echo "Error: 'gradle' command not found. Please install Gradle or provide a valid wrapper."
                    exit 1
                fi
            """.trimIndent())
            setExecutable(true)
        }
    }
}
