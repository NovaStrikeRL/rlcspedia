#!/usr/bin/env bash
# Instala el APK, abre la app y verifica que siga viva. Imprime el crash si lo hay.
set -u
mkdir -p smoke
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat -c
adb shell am start -W -n com.rlcspedia.app/.MainActivity
sleep 20
adb shell pm grant com.rlcspedia.app android.permission.POST_NOTIFICATIONS || true
adb shell input tap 540 2280 || true
sleep 5
adb logcat -d > smoke/logcat.txt
adb exec-out screencap -p > smoke/screen.png || true
echo "===== CRASH ====="
grep -A60 -E "FATAL EXCEPTION|AndroidRuntime: Process" smoke/logcat.txt | head -120 || true
echo "===== FIN CRASH ====="
if adb shell pidof com.rlcspedia.app > /dev/null; then
  echo "OK: la app sigue abierta"
else
  echo "FALLO: la app se cerró"; exit 1
fi
