# Changelog

Todos los cambios relevantes de RLCSpedia se documentan aquí.
Formato: [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) · Versionado: [SemVer](https://semver.org/lang/es/).

## [Sin publicar]

### Pendiente
- Avisos al instante con la app cerrada (hoy cada ~15 min por límite de Android; requiere Firebase Cloud Messaging).
- Goleador y reloj del juego con datos reales (Liquipedia no los publica; requiere otra fuente).
- Live Updates de Android 16+ para la serie en curso.
- Tile y app de Wear OS.
- Logo definitivo y nombre final.

## [0.3.0] - 2026-11-20

### Añadido
- **Datos reales de la RLCS desde Liquipedia** (LiquipediaDB API v3): torneos, equipos con jugadores, series con su formato, marcador de la serie y de cada juego.
  - El workflow "Datos de Liquipedia" consulta la API cada 10 min con la clave guardada como secreto del repo y publica `feed.json` en la rama `data`. La app nunca contiene la clave.
  - La app lee el feed cada minuto mientras está abierta y cada 15 min en segundo plano (WorkManager) para notificar inicio de serie, cambios de marcador, fin de juego y fin de serie.
  - Atribución a Liquipedia (CC-BY-SA 3.0) en pantalla y botón para abrir cada serie en Liquipedia.
- Ajuste **Fuente de datos**: Liquipedia o Demostración. Si el feed falla, un aviso permite cambiar a demostración.
- Deslizar para actualizar con el indicador de carga expresivo.

### Cambiado
- Material 3 Expressive en más componentes: barra superior flexible (`MediumFlexibleTopAppBar`) con subtítulo del estado de los datos, reloj del juego como progreso ondulado (`LinearWavyProgressIndicator`) y carga inicial con `ContainedLoadingIndicator`.
- Con datos reales, los avisos de gol indican el equipo y el marcador (la fuente no indica el jugador).

## [0.2.1] - 2026-11-20

### Corregido
- La app se cerraba al abrirla: R8 eliminaba el constructor de la base de datos de WorkManager (que usa el widget). Se agregaron reglas de conservación y WorkManager 2.10.1.

### Añadido
- Prueba automática en emulador (Android 15) que abre la app, recorre las pestañas y falla si hay un cierre inesperado.

## [0.2.0] - 2026-11-20

### Cambiado
- **RLCSpedia ahora es una app nativa de Android** (Kotlin + Jetpack Compose) en lugar de una PWA. El prototipo web queda en el historial de git (`v0.1.0`).
- Diseño migrado a **Material 3 Expressive**: `MaterialExpressiveTheme` con esquema de movimiento expresivo, barra de navegación corta, grupos de botones conectados, indicador de carga con formas que se transforman y formas expresivas (`MaterialShapes`).

### Añadido
- **En vivo**: tarjetas de serie con marcador animado del juego actual, indicadores de juegos ganados (Bo3/Bo5/Bo7), reloj, tiempo extra, último gol y detalle gol por gol.
- **Torneos**: torneos en curso y próximos con cuenta regresiva; detalle con sus series.
- **Favoritos**: búsqueda por equipo, región o jugador; los favoritos se ordenan primero, se resaltan y cambian de forma de avatar.
- **Notificaciones** con canales separados (Goles, Inicio de series, Resultados): inicio de serie, cada gol con jugador, equipo y marcador, fin de juego y fin de serie; opción "solo de mis favoritos"; botón de prueba.
- **Reloj**: las notificaciones se reflejan en Wear OS y otros relojes conectados al teléfono.
- **Widget de pantalla de inicio** (Jetpack Glance) con la serie más relevante, favoritos primero; usa los colores del sistema.
- **Accesibilidad**: tema Sistema/Claro/Oscuro, contraste alto, color dinámico (Material You) opcional, tamaño de texto 85–150 % sobre el del sistema, fuente Atkinson Hyperlegible en todo, reducir movimiento, anuncios de goles para TalkBack, descripciones completas de marcadores y objetivos táctiles de 48 dp.
- Ícono adaptativo con versión monocromática para íconos temáticos.
- Fuentes incluidas: Chakra Petch (marcadores) y Atkinson Hyperlegible (texto), ambas con licencia OFL.
- GitHub Actions: compila el APK en cada push a `main` y crea la Release `vX.Y.Z` automáticamente cuando cambia `versionName`.

### Eliminado
- PWA (HTML/CSS/JS, Service Worker y widget de Windows).

## [0.1.0] - 2026-11-20

### Añadido
- Prototipo PWA con Material Design 3: en vivo, torneos, favoritos, notificaciones web, widget de Windows 11 y ajustes de accesibilidad.
- Proveedor de datos de demostración con equipos ficticios y simulador de partidos.
