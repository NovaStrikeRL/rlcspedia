# RLCSpedia

App de Android para seguir la RLCS de Rocket League: próximos torneos, series en vivo (Bo3/Bo5/Bo7), cada gol al instante en el teléfono y el reloj, widget en la pantalla de inicio y tus equipos favoritos primero. Diseño Material 3 Expressive.

> **Estado:** v0.2.0 con datos de demostración (equipos ficticios y partidos simulados). Ver [CHANGELOG](CHANGELOG.md).

## Descargar

El APK está en **Releases**. En el teléfono permite "Instalar apps de origen desconocido" para tu navegador o gestor de archivos. Requiere Android 8.0 o superior.

## Compilar

**GitHub Actions** compila en cada push (el APK queda como artefacto del workflow) y publica una Release al subir un tag:

```bash
git tag v0.2.0 && git push origin v0.2.0
```

**Local** (JDK 17+, Android SDK, Gradle 9.6):

```bash
gradle :app:assembleRelease
# app/build/outputs/apk/release/app-release.apk
```

En Android Studio: *Open* → carpeta del repo.

## Stack

Kotlin · Jetpack Compose (BOM 2026.11.01) · Material 3 Expressive · Jetpack Glance (widget) · AGP 9.5 · minSdk 26 · targetSdk 37

## Estructura

```
app/src/main/java/com/rlcspedia/app/
  RLCSpediaApp.kt           Arranque: simulador, notificaciones y widget
  MainActivity.kt           Scaffold, barra de navegación corta, avisos internos
  data/                     Modelos, datos demo, motor en vivo, ajustes
  notify/Notifier.kt        Canales y notificaciones (se reflejan en el reloj)
  widget/LiveWidget.kt      Widget Glance y su actualizador
  ui/theme/Theme.kt         Color, tipografía, movimiento expresivo, colores de equipo
  ui/components/SeriesCard.kt
  ui/screens/               En vivo, Torneos, Favoritos, Ajustes
```

## Datos reales

`DemoData` y el simulador de `LiveRepository` son de demostración. Para datos reales, sustitúyelos por un proveedor que entregue las mismas estructuras y emita los mismos `LiveEvent`.

Fuente sugerida: **Liquipedia LPDB API** (requiere API key y respetar sus límites). La key no debe ir en la app: un backend pequeño (p. ej. FastAPI) consulta, cachea y envía avisos con Firebase Cloud Messaging, de modo que lleguen aunque la app esté cerrada.

## Firma

El APK de release se firma con la llave de depuración para poder instalarlo directo. Para Google Play, crea una llave propia y configúrala con secretos de GitHub.

## Logo

El ícono es un marcador de posición original. El logo de la RLCS es marca registrada de Psyonix/Epic Games; úsalo solo con permiso.
