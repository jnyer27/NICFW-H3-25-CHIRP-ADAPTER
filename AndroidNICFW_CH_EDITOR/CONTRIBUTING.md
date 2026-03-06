# Contributing to AndroidNICFW_CH_EDITOR

## Build environment

- **Use the checked-in Gradle wrapper** for builds: run `./gradlew` (Unix/macOS) or `gradlew.bat` (Windows). Do not rely on a globally installed Gradle.
- **Version alignment:** Dependency and plugin versions are managed in `gradle/libs.versions.toml`. When updating AGP, Kotlin, or libraries, change them there so all modules stay in sync.

## Windows: avoiding file lock issues

On Windows, real-time anti-virus (e.g. Windows Defender) or lingering Gradle/Java processes can lock files under `build/` or `app/build/`, which leads to errors like "Unable to delete directory" during clean or build.

- **Exclude the project directory** (or at least the `build/` and `app/build/` folders) from **real-time anti-virus scanning**. This is a one-time environment step and prevents the lock from recurring.
- If a build or clean fails with a delete error, close Android Studio and any other Gradle/Java processes, then retry.

See the [README](README.md) Troubleshooting section for more detail.
