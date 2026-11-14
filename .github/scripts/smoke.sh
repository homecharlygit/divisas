#!/usr/bin/env bash
# Instala y abre la app en el emulador y muestra cualquier crash
set +e
adb logcat -c
adb install -r app/build/outputs/apk/release/app-release.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo "===== Abriendo release ====="
adb shell am start -W -n dev.charly.divisas/.MainActivity
sleep 25
adb shell pidof dev.charly.divisas && echo "RELEASE SIGUE ABIERTA" || echo "RELEASE SE CERRO"

echo "===== Abriendo debug ====="
adb shell am start -W -n dev.charly.divisas.debug/dev.charly.divisas.MainActivity
sleep 25
adb shell pidof dev.charly.divisas.debug && echo "DEBUG SIGUE ABIERTA" || echo "DEBUG SE CERRO"

adb root
sleep 5
adb wait-for-device
for pkg in dev.charly.divisas dev.charly.divisas.debug; do
  echo "===== Widget ($pkg) ====="
  adb shell am start -W -n $pkg/dev.charly.divisas.MainActivity > /dev/null
  sleep 5
  adb logcat -c
  adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE \
    -n $pkg/dev.charly.divisas.widget.RateWidgetReceiver --eia appWidgetIds 1
  sleep 30
  PID=$(adb shell pidof $pkg)
  echo "pid=$PID"
  if [ -n "$PID" ]; then adb logcat -d --pid=$PID | tail -60; fi
  adb logcat -d | grep -E "GlanceAppWidget|Glance|AppWidget|RemoteViews" | grep -v "chatty" | tail -40
done

echo "===== CRASH BUFFER ====="
adb logcat -d -b crash
echo "===== AndroidRuntime ====="
adb logcat -d -s AndroidRuntime:E
echo "===== FIN ====="
exit 0
