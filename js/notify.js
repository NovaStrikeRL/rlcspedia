// Notificaciones locales vía Service Worker (se reflejan en Wear OS / Apple Watch
// cuando el reloj tiene activado el espejo de notificaciones del teléfono).
import { store } from './store.js';

export const canNotify = () => 'Notification' in window;
export const permission = () => (canNotify() ? Notification.permission : 'unsupported');

export async function requestPermission() {
  if (!canNotify()) return 'unsupported';
  return Notification.requestPermission();
}

/**
 * @param {'seriesStart'|'goals'|'gameEnd'|'seriesEnd'} kind
 * @param {{title:string, body:string, tag:string, teams:string[], url?:string}} n
 */
export async function notify(kind, n) {
  const s = store.get();
  if (!s.notify[kind]) return false;
  if (s.notify.favoritesOnly && !n.teams.some(store.isFav)) return false;
  if (permission() !== 'granted') return false;

  const options = {
    body: n.body,
    tag: n.tag,              // reemplaza la notificación anterior de la misma serie
    renotify: true,
    icon: 'icons/icon-192.png',
    badge: 'icons/badge-96.png',
    vibrate: kind === 'goals' ? [80, 40, 80] : [200],
    data: { url: n.url || './#live' },
    silent: false,
  };
  try {
    const reg = await navigator.serviceWorker?.getRegistration();
    if (reg) await reg.showNotification(n.title, options);
    else new Notification(n.title, options);
    return true;
  } catch { return false; }
}
