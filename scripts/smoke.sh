#!/usr/bin/env bash
# Instala el APK, abre la app y verifica que siga viva. Imprime el crash si lo hay.
set -u
mkdir -p smoke
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat -c
adb shell am start -W -n com.rlcspedia.app/.MainActivity
sleep 20
adb shell pm grant com.rlcspedia.app android.permission.POST_NOTIFICATIONS || true
# Recorre las 4 pestañas (pantalla del emulador: 320x640 px) y vuelve a En vivo
for x in 120 200 280 40; do adb shell input tap $x 615 || true; sleep 4; adb exec-out screencap -p > "smoke/tab-$x.png" || true; done
sleep 30  # deja correr el simulador (goles, fin de juego, notificaciones)
adb logcat -d > smoke/logcat.txt
adb exec-out screencap -p > smoke/screen.png || true
echo "===== CRASH ====="
grep -A60 -E "FATAL EXCEPTION|AndroidRuntime: Process" smoke/logcat.txt | head -120 || true
echo "===== FIN CRASH ====="
if grep -q "Process: com.rlcspedia.app" smoke/logcat.txt; then
  echo "FALLO: la app se cerró (ver CRASH)"; exit 1
elif adb shell pidof com.rlcspedia.app > /dev/null; then
  echo "OK: la app sigue abierta"
else
  echo "FALLO: la app se cerró"; exit 1
fi
