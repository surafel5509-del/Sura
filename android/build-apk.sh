#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

if [ ! -f local.properties ]; then
  echo "local.properties not found in android/. Please create it with your SDK/NDK paths."
  echo "See local.properties.example for the expected content." >&2
  exit 1
fi

echo "Building debug APK..."
if [ -x ./gradlew ]; then
  ./gradlew assembleDebug
else
  gradle assembleDebug
fi

APK_PATH="$ROOT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
  echo "APK produced: $APK_PATH"
else
  echo "Build finished but APK not found at $APK_PATH" >&2
  exit 1
fi
