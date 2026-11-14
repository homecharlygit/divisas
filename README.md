# Divisas

Conversor de divisas para Android con tipo de cambio actualizado, hecho en Kotlin + Jetpack Compose con **Material 3 Expressive** oficial (`androidx.compose.material3:material3:1.5.0-rc01`).

## Qué usa de M3 Expressive
- `MaterialExpressiveTheme` + `MotionScheme.expressive()` (animaciones con resorte)
- `LoadingIndicator` mientras actualiza
- `MaterialShapes.Cookie9Sided` para el botón de intercambiar
- Material You (colores del wallpaper) en Android 12+

## Tipo de cambio
- Fuente principal: [ExchangeRate-API](https://www.exchangerate-api.com/docs/free) (gratis, sin API key, ~160 monedas, actualización diaria).
- Respaldo: [Frankfurter](https://frankfurter.dev) (tipos de referencia del BCE).
- Se guarda en caché para usarse sin conexión y se refresca al abrir la app si pasó más de 1 hora.

Son tipos de referencia, no el precio de compra/venta de un banco o casa de cambio.

## Compilar
- **GitHub Actions:** sube el repo y el workflow `Compilar APK` genera el APK debug y release (firmado con llave debug). Lo descargas en la pestaña *Actions → artifact `divisas-apk`*.
- **Local:** `./gradlew assembleDebug` (JDK 17+). Requiere compileSdk 37 / AGP 9.2.1 / Gradle 9.4.1.

Versiones: AGP 9.2.1 · Kotlin 2.3.10 · Compose BOM 2026.08.00 · material3 1.5.0-rc01 · minSdk 26 · targetSdk 37.

## Licencia
GPL-3.0. Ver [LICENSE](LICENSE).
