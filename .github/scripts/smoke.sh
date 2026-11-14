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

echo "===== Widget (release) ====="
adb logcat -c
adb shell am broadcast -a android.appwidget.action.APPWIDGET_UPDATE \
  -n dev.charly.divisas/.widget.RateWidgetReceiver --eia appWidgetIds 1
sleep 45
adb logcat -d | grep -iE "glance|WM-|work|RateWidget|divisas|AndroidRuntime|FATAL|Exception" | grep -viE "chatty|ActivityTaskManager: Displayed" | tail -150

echo "===== CRASH BUFFER ====="
adb logcat -d -b crash
echo "===== AndroidRuntime ====="
adb logcat -d -s AndroidRuntime:E
echo "===== FIN ====="
exit 0
