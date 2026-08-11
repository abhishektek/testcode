# Codra Studio local build fixes

This source package was updated to fix the local build-engine issues found in the uploaded project.

## Fixed

- Project parser now understands Kotlin DSL `compileSdk = 34` and AGP 9.x `compileSdk { version = release(36) { ... } }` forms.
- `minSdk`, `targetSdk`, and explicit `buildToolsVersion` detection was improved.
- Version-catalog AGP aliases such as `libs.plugins.android.application` + `version.ref` are resolved.
- Gradle version is read from `gradle-wrapper.properties`, with an AGP compatibility fallback when a project has no wrapper.
- AGP/Gradle compatibility validation now fails closed instead of treating unknown versions as compatible.
- The build executor never calls a global `gradle` command.
- If an official Gradle wrapper is absent, Codra creates a local launcher that executes the provisioned Gradle distribution through `CODRA_GRADLE_HOME`.
- Gradle distributions are cached under Codra's private `.codra/gradle` directory and can be downloaded on first use.
- `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`, `CODRA_GRADLE_HOME`, and `GRADLE_USER_HOME` are explicitly supplied to the build process.
- SDK provisioning uses `sdkmanager` when it is actually present; missing SDK components are never faked as installed.
- JDK diagnostics now require an executable Java runtime instead of treating Android Runtime metadata as a valid JDK.
- The UI diagnostics no longer marks the JDK as ready merely because `javaHome` is non-null.

## Important runtime requirement

A real Android-compatible JDK and Android SDK/build-tool binaries must exist or be provisioned on the device. The code intentionally does not manufacture fake `android.jar`, `aapt2`, `d8`, or Java binaries.

The uploaded source did not contain an official Gradle wrapper JAR. The runtime build engine therefore uses its local Gradle launcher fallback when the official wrapper is absent.
