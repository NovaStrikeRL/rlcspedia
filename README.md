# RLCSpedia

App de Android para seguir la RLCS de Rocket League: próximos torneos, series en vivo (Bo3/Bo5/Bo7), cada gol al instante en el teléfono y el reloj, widget en la pantalla de inicio y tus equipos favoritos primero. Diseño Material 3 Expressive.

> **Estado:** v0.4.0 · modo demostración por defecto; datos reales de Liquipedia al configurar la clave. Ver [CHANGELOG](CHANGELOG.md).

## Descargar

El APK está en **Releases**. En el teléfono permite "Instalar apps de origen desconocido" para tu navegador o gestor de archivos. Requiere Android 8.0 o superior.

## Compilar

**GitHub Actions** compila en cada push a `main`. Si `versionName` en `app/build.gradle.kts` es nuevo, crea la Release `vX.Y.Z` con el APK y las notas de esa versión del CHANGELOG. Para publicar una versión: sube `versionCode`/`versionName`, agrega su sección al CHANGELOG y haz push.

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

## Datos reales (Liquipedia)

```
LiquipediaDB API ──(cada 10 min, GitHub Actions + secreto)──▶ rama data/feed.json ──▶ app (cada 1 min abierta, 15 min en segundo plano)
```

1. Pide una clave de API en Liquipedia (gratis para proyectos comunitarios y de código abierto no comerciales).
2. En GitHub: **Settings → Secrets and variables → Actions → New repository secret** → nombre `LIQUIPEDIA_API_KEY`.
3. Ejecuta el workflow **Datos de Liquipedia** (pestaña Actions → Run workflow). Después corre solo cada 10 min.

`scripts/feed.py` hace 3 consultas por ejecución (torneos, series y equipos), espera 2 s entre ellas y se identifica con un User-Agent propio. Se incluyen torneos con "RLCS" en el nombre, desde 3 días atrás hasta 60 días adelante.

Limitaciones: Liquipedia no publica goleadores ni el reloj del juego, y su marcador lo actualizan editores, con algunos minutos de retraso.

Datos de [Liquipedia](https://liquipedia.net/rocketleague) bajo licencia [CC-BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0/).

## Firma

El APK de release se firma con la llave de depuración para poder instalarlo directo. Para Google Play, crea una llave propia y configúrala con secretos de GitHub.

## Créditos

Íconos: [Material Symbols](https://fonts.google.com/icons) (Apache 2.0). Fuentes: Chakra Petch y Atkinson Hyperlegible (OFL).

## Logo

El ícono es un marcador de posición original. El logo de la RLCS es marca registrada de Psyonix/Epic Games; úsalo solo con permiso.
