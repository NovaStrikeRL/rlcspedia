# RLCSpedia

Sigue los torneos de la RLCS de Rocket League: próximos torneos, series en vivo (Bo3/Bo5/Bo7), goles al instante y tus equipos favoritos primero. PWA con Material Design 3.

> **Estado:** v0.1.0 con datos de demostración (equipos ficticios y partidos simulados). Ver [CHANGELOG](CHANGELOG.md).

## Ejecutar

Sin dependencias ni build.

```bash
npx serve .          # o: python3 -m http.server 8080
```

Los Service Workers y las notificaciones requieren `https` o `localhost`.

**GitHub Pages:** Settings → Pages → Deploy from branch → `main` / root. El archivo `.nojekyll` ya está incluido.

## Estructura

```
index.html              Shell de la app
manifest.webmanifest    Instalación, accesos directos y widget
sw.js                   Caché offline, notificaciones, push y widgets
css/tokens.css          Tokens M3 (claro, oscuro, alto contraste)
css/app.css             Componentes M3
js/app.js               Vistas, router y eventos
js/data.js              Proveedor de demostración
js/live.js              Motor en vivo (eventos: seriesStart, goal, gameEnd, seriesEnd)
js/store.js             Favoritos y ajustes (localStorage)
js/notify.js            Notificaciones locales
widgets/                Plantilla Adaptive Card + datos iniciales
icons/                  Logo provisional e íconos
```

## Plataformas: qué funciona hoy

| Función | Estado en la PWA | Camino a nativo |
|---|---|---|
| Notificaciones con la app abierta | ✅ | — |
| Notificaciones en el reloj | ✅ por espejo del teléfono (Wear OS, Apple Watch) | Tile de Wear OS |
| Notificaciones con la app cerrada | ⚙️ `sw.js` ya maneja `push`; falta servidor | Backend Web Push (VAPID) |
| Widget de pantalla de inicio | ✅ Windows 11 (PWA widgets) | Android: empaquetar con TWA/Capacitor + Jetpack Glance; iOS: WidgetKit |
| iPhone | ✅ notificaciones si se instala en pantalla de inicio (iOS 16.4+) | — |

## Datos reales

`js/data.js` es un proveedor de demostración. Para datos reales, crea un proveedor que entregue la misma forma (`tournaments`, `series` con `games` y `goals`) y emita los eventos de `LiveEngine`.

Fuente sugerida: **Liquipedia LPDB API** (requiere API key y respetar sus límites). La key no debe vivir en el cliente: usa un backend pequeño (p. ej. FastAPI) que consulte, cachee y envíe Web Push.

## Logo

`icons/logo.svg` es un marcador de posición original. El logo de la RLCS es marca registrada de Psyonix/Epic Games; úsalo solo con permiso.

## Accesibilidad

Tema (sistema/claro/oscuro), contraste alto, tamaño de texto 85–150 %, Atkinson Hyperlegible, reducir movimiento, anuncios `aria-live` de goles, foco visible, objetivos táctiles ≥ 48 px.
