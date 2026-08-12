# Future2D (Sura)

This repository contains a small native Android app engine (`future2d`) with C++ native code and an Android wrapper.

Quick status
- Native library: builds successfully (see `native/build`).
- Android app: project configured; building an APK requires an Android SDK/NDK installed locally.

How to build (native library only)

1. From project root, build the native library via CMake (example for the included `native` folder):

```bash
cd native && mkdir -p build && cd build
cmake ..
make -j
```

This produces `libfuture2d.so` in `native/build`.

How to build the Android APK (requires Android SDK/NDK)

Prerequisites:
- Android SDK and NDK installed
- Java JDK (matching Gradle/AGP requirements)

Steps:

1. Add a `local.properties` file in the `android/` folder that points to your SDK. Create `/workspaces/Sura/android/local.properties` with content such as:

```
sdk.dir=/path/to/Android/Sdk
ndk.dir=/path/to/Android/Sdk/ndk/<version>
```

2. Use the Gradle wrapper if available, or system Gradle. Recommended: run from `android/`:

```bash
# If there is a wrapper
./gradlew assembleDebug
# Or with system gradle
gradle assembleDebug
```

Notes and recent changes made by automation:
- Added `namespace` to `android/app/build.gradle` to satisfy modern AGP.
- Bumped AGP in `android/build.gradle` to `9.0.0` to match the environment Gradle.
- Fixed native warnings and ensured renderer init is checked before creating subsystems.
- The native library builds successfully inside the dev container; however the APK build failed here due to missing Android SDK/NDK.

If you'd like, I can add a Gradle wrapper and/or a `local.properties.example` file, or attempt to build an APK if you provide the SDK path.

---
If you want me to continue: reply with `build-apk` and either provide the SDK/NDK paths or allow me to add a Gradle wrapper and `local.properties.example` for you to fill in.
# Future 2D Game Engine (scaffold)

This repository contains the initial scaffolding for the Future 2D Game Engine native Android project.

Build notes:

- Requires Android NDK r26+ and CMake 3.22+.
- Open this folder in Android Studio and import the `android` Gradle project.
- The native code lives under `native/` and builds a shared library `future2d`.
# 2D-game-engine
New full game engine 
https://github.com/surafel121212/2D-game-engine.git
