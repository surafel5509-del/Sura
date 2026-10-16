# Building and shipping

## Requirements

| Tool | Version |
|------|---------|
| JDK | 17 |
| Gradle | 8.7 (the CI installs it; the repo also accepts a system Gradle) |
| Android SDK | platform 33, build-tools 33.0.2 |
| Kotlin | 1.9.10 (declared in `android/build.gradle`) |

## Local build

```bash
cd android
gradle :app:testDebugUnitTest     # JVM unit tests
gradle :app:assembleDebug         # debug APK
gradle :app:assembleRelease       # release APK (unsigned)
```

Outputs:

```
android/app/build/outputs/apk/debug/app-debug.apk
android/app/build/outputs/apk/release/app-release-unsigned.apk
```

## GitHub Actions

`.github/workflows/build-apk.yml` runs on every push to `main`, `arena/**`, feature branches and
pull requests:

1. Sets up JDK 17, Gradle 8.7 and the Android SDK.
2. Runs the unit test suite (`:app:testDebugUnitTest`).
3. Builds debug **and** release APKs.
4. Uploads both APKs plus the test report as artifacts.
5. On failure, posts a **build diagnostics** comment on the commit with the exact compiler errors.
6. On tags (`v*`), publishes the APKs to a GitHub Release.

Download the APK from the workflow run page → *Artifacts* → `sura-engine-debug-apk`.

## Signing a release build

Add a keystore to `android/app/` and reference it in `android/app/build.gradle`:

```groovy
android {
    signingConfigs {
        release {
            storeFile file("sura-release.jks")
            storePassword System.getenv("SURA_STORE_PASSWORD")
            keyAlias System.getenv("SURA_KEY_ALIAS")
            keyPassword System.getenv("SURA_KEY_PASSWORD")
        }
    }
    buildTypes {
        release { signingConfig signingConfigs.release }
    }
}
```

Keep keystores and passwords out of git (see `.gitignore`) and inject them as CI secrets.

## In-app build spec

`Build ▸ Build settings` collects the app name, package id, version and orientation and writes
`Build/build-spec.json` inside the project folder, together with the asset manifest
(scenes, scripts, prefabs). This is what an export pipeline (or your own CI step) consumes to
produce a standalone game APK.
