#!/usr/bin/env bash
# Run as one Bash process from android-emulator-runner. Keeping this in a
# separate file prevents the action from evaluating function declarations
# as separate shell commands.
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

installed=false
for attempt in 1 2 3; do
  echo "Base APK installation attempt ${attempt}/3 (non-streaming)"
  if adb install --no-streaming -r "$apk"; then
    installed=true
    break
  fi
  echo "Install attempt ${attempt} failed; checking Android services before retry."
  adb reconnect device >/dev/null 2>&1 || true
  adb wait-for-device
  wait_for_android_services 180
done
if [ "$installed" != "true" ]; then
  echo "Base APK installation failed after 3 attempts."
  exit 1
fi

adb shell am start -W -n com.sifat.browser/com.adda.browser.MainActivity
app_started=false
for attempt in $(seq 1 45); do
  if adb shell pidof com.sifat.browser 2>/dev/null | grep -q '[0-9]'; then
    app_started=true
    break
  fi
  sleep 2
done
if [ "$app_started" != "true" ]; then
  echo "SIFAT Browser process did not start."
  exit 1
fi

sleep 10
python scripts/android11_smoke.py
echo "Android 11 install, launch, and UI smoke test passed."

adb shell appops set com.sifat.browser REQUEST_INSTALL_PACKAGES allow
adb install --no-streaming -r "$test_apk"

echo "Running updater check → download → SHA-256 → signing certificate → installer handoff test."
instrumentation_log="$(mktemp)"
echo "Running AndroidJUnitRunner and capturing its actual test result."
adb shell am instrument -w -r com.sifat.browser.test/androidx.test.runner.AndroidJUnitRunner 2>&1 | tee "$instrumentation_log"

# adb may exit 0 even when AndroidJUnitRunner reports assertion failures.
# AndroidJUnitRunner's -r mode emits per-test status codes, not the plain JUnit
# "OK (N tests)" summary. Require the named test, one discovered test, a
# successful per-test status, and the final instrumentation success code.
if ! grep -Fqx 'INSTRUMENTATION_STATUS: test=checksDownloadsVerifiesAndOpensAndroidInstaller' "$instrumentation_log" ||
   ! grep -Fqx 'INSTRUMENTATION_STATUS: numtests=1' "$instrumentation_log" ||
   ! grep -Fqx 'INSTRUMENTATION_STATUS_CODE: 0' "$instrumentation_log" ||
   ! grep -Fqx 'INSTRUMENTATION_CODE: 0' "$instrumentation_log" ||
   grep -Fq 'FAILURES!!!' "$instrumentation_log" ||
   grep -Eq '^INSTRUMENTATION_STATUS_CODE: -[0-9]+' "$instrumentation_log"; then
  echo "Updater E2E instrumentation reported a test failure; CI must fail."
  collect_diagnostics
  cat "$instrumentation_log"
  exit 1
fi
rm -f "$instrumentation_log"
echo "Updater E2E instrumentation test passed (runner and JUnit results verified)."
