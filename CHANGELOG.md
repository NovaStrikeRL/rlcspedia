# Changelog

Todos los cambios relevantes de RLCSpedia se documentan aquí.
Formato: [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) · Versionado: [SemVer](https://semver.org/lang/es/).

## [Sin publicar]

### Pendiente
- Proveedor de datos reales (Liquipedia LPDB vía backend propio).
- Servidor Web Push para notificaciones con la app cerrada.
- Widget nativo de Android (Jetpack Glance) y tile de Wear OS vía empaquetado TWA/Capacitor.
- Logo definitivo y nombre final.

## [0.1.0] - 2026-11-20

### Añadido
- PWA instalable con Material Design 3: tokens de color claro/oscuro, tipografía, forma y elevación.
- Navegación M3: barra inferior en móvil y riel lateral en pantallas ≥ 840 px.
- **En vivo**: tarjetas de serie con marcador del juego actual, indicadores de juegos ganados (Bo3/Bo5/Bo7), reloj, tiempo extra, último gol y detalle gol por gol.
- **Torneos**: lista de torneos en curso y próximos con cuenta regresiva; detalle por torneo con sus series.
- **Favoritos**: búsqueda de equipos y marcado con estrella; las series de favoritos se ordenan primero y se resaltan.
- **Notificaciones** (teléfono y reloj por espejo): inicio de serie, goles (jugador, equipo y marcador), fin de juego y fin de serie; opción "solo favoritos"; botón de prueba.
- **Widget** "Serie en vivo" (Adaptive Card, PWA widgets) actualizado desde el Service Worker.
- **Accesibilidad**: tema Sistema/Claro/Oscuro, contraste alto, tamaño de texto 85–150 %, fuente de alta legibilidad (Atkinson Hyperlegible), reducir movimiento, anuncios de goles para lector de pantalla, foco visible y enlace "Saltar al contenido".
- Service Worker con caché offline y manejo de `push` listo para backend.
- Proveedor de datos de demostración con equipos ficticios y simulador de partidos.
- Logo provisional original (marcador de posición).
