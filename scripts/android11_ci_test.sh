#!/usr/bin/env bash
# Run as one Bash process from android-emulator-runner.
set -Eeuo pipefail

DIAGNOSTICS_FILE="android11-emulator-logcat.txt"

collect_diagnostics() {
  {
    echo "===== ADB devices ====="
    adb devices -l || true
    echo "===== Android boot properties ====="
    adb shell getprop sys.boot_completed || true
    adb shell getprop init.svc.bootanim || true
    echo "===== Package Manager binder ====="
    adb shell service check package || true
    echo "===== Recent Android logcat ====="
    adb logcat -d -b all -t 5000 || true
  } > "$DIAGNOSTICS_FILE" 2>&1
}

on_error() {
  status=$?
  trap - ERR
  echo "Android 11 updater E2E verification failed; collecting diagnostics."
  collect_diagnostics
  exit "$status"
}
trap on_error ERR

wait_for_android_services() {
  timeout_seconds="${1:-180}"
  deadline=$((SECONDS + timeout_seconds))
  while (( SECONDS < deadline )); do
    boot_completed="$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r' || true)"
    package_service="$(adb shell service check package 2>/dev/null || true)"
    if adb get-state >/dev/null 2>&1 &&
       [ "$boot_completed" = "1" ] &&
       echo "$package_service" | grep -q "found" &&
       adb shell cmd package list packages >/dev/null 2>&1; then
      echo "Android boot and Package Manager are ready."
      return 0
    fi
    echo "Waiting for Android/Package Manager readiness..."
    adb reconnect device >/dev/null 2>&1 || true
    sleep 5
  done
  echo "Android Package Manager did not become ready in ${timeout_seconds}s."
  return 1
}

adb wait-for-device
wait_for_android_services 300

apk="app/build/outputs/apk/release/app-release.apk"
test_apk="app/build/outputs/apk/androidTest/release/app-release-androidTest.apk"
test -s "$apk"
test -s "$test_apk"

echo "Installing base release APK (version 1.16.5)."
adb install --no-streaming -r "$apk"
adb shell appops set com.sifat.browser REQUEST_INSTALL_PACKAGES allow
adb install --no-streaming -r "$test_apk"

echo "Running updater check → download → SHA-256 → signing certificate → installer handoff test."
adb shell am instrument -w -r com.sifat.browser.test/androidx.test.runner.AndroidJUnitRunner

echo "Updater E2E instrumentation test passed."
