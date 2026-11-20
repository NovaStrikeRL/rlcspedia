// RLCSpedia Service Worker: caché offline, notificaciones y widgets.
const CACHE = 'rlcspedia-v0.1.0';
const ASSETS = [
  './', 'index.html', 'manifest.webmanifest',
  'css/tokens.css', 'css/app.css',
  'js/app.js', 'js/data.js', 'js/live.js', 'js/store.js', 'js/notify.js',
  'icons/logo.svg', 'icons/icon-192.png', 'icons/icon-512.png', 'icons/badge-96.png',
  'widgets/live-template.json', 'widgets/live-data.json',
];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(ASSETS)).then(() => self.skipWaiting()));
});
self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim()));
});
self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  e.respondWith(caches.match(e.request).then(r => r || fetch(e.request)));
});

// Tocar una notificación abre/enfoca la app
self.addEventListener('notificationclick', e => {
  e.notification.close();
  const url = e.notification.data?.url || './#live';
  e.waitUntil(self.clients.matchAll({ type: 'window', includeUncontrolled: true }).then(cs => {
    const c = cs.find(x => 'focus' in x);
    return c ? c.focus().then(w => w.navigate?.(url)) : self.clients.openWindow(url);
  }));
});

// Web Push (requiere servidor; ver README → "Notificaciones push reales")
self.addEventListener('push', e => {
  const d = e.data?.json() ?? {};
  e.waitUntil(self.registration.showNotification(d.title || 'RLCSpedia', {
    body: d.body, tag: d.tag, renotify: true, icon: 'icons/icon-192.png', badge: 'icons/badge-96.png',
    vibrate: d.kind === 'goals' ? [80, 40, 80] : [200], data: { url: d.url || './#live' },
  }));
  if (d.widget) e.waitUntil(updateWidget(d.widget));
});

/* ---------- Widgets (PWA widgets: Windows 11 hoy; ver README) ---------- */
let snapshot = null;
self.addEventListener('message', e => {
  if (e.data?.type === 'widget') { snapshot = e.data.payload; updateWidget(snapshot); }
});
async function updateWidget(payload) {
  if (!self.widgets) return;
  const w = await self.widgets.getByTag('live');
  if (!w) return;
  const template = await (await fetch(w.definition.msAcTemplate)).text();
  await self.widgets.updateByTag('live', { template, data: JSON.stringify(payload) });
}
self.addEventListener('widgetinstall', e => e.waitUntil(updateWidget(snapshot || {
  tournament: 'RLCSpedia', status: 'Abre la app para ver series en vivo', teamA: '—', teamB: '—', scoreA: '0', scoreB: '0', series: '',
})));
self.addEventListener('widgetresume', e => e.waitUntil(snapshot ? updateWidget(snapshot) : Promise.resolve()));
self.addEventListener('widgetclick', e => {
  if (e.action === 'open') e.waitUntil(self.clients.openWindow('./#live'));
});
