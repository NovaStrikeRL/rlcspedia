// Persistencia local (favoritos y ajustes). Todo envuelto en try/catch.
const KEY = 'rlcspedia:v1';
const DEFAULTS = {
  favorites: [],
  theme: 'system',        // system | light | dark
  contrast: 'standard',   // standard | high
  fontScale: 1,           // 0.85 – 1.5
  legible: false,         // Atkinson Hyperlegible también en marcadores
  motion: 'system',       // system | reduce
  notify: { seriesStart: true, goals: true, gameEnd: true, seriesEnd: true, favoritesOnly: false },
};

let state = load();
const listeners = new Set();

function load() {
  try { return deepMerge(structuredClone(DEFAULTS), JSON.parse(localStorage.getItem(KEY)) || {}); }
  catch { return structuredClone(DEFAULTS); }
}
function deepMerge(a, b) {
  for (const k in b) a[k] = b[k] && typeof b[k] === 'object' && !Array.isArray(b[k]) ? deepMerge(a[k] || {}, b[k]) : b[k];
  return a;
}

export const store = {
  get: () => state,
  set(patch) {
    state = deepMerge(state, patch);
    try { localStorage.setItem(KEY, JSON.stringify(state)); } catch {}
    listeners.forEach(fn => fn(state));
  },
  toggleFavorite(id) {
    const favs = new Set(state.favorites);
    favs.has(id) ? favs.delete(id) : favs.add(id);
    this.set({ favorites: [...favs] });
  },
  isFav: id => state.favorites.includes(id),
  subscribe: fn => (listeners.add(fn), () => listeners.delete(fn)),
};

// Aplica ajustes de accesibilidad al <html>
const mq = matchMedia('(prefers-color-scheme: dark)');
export function applySettings(s = state) {
  const root = document.documentElement;
  const dark = s.theme === 'dark' || (s.theme === 'system' && mq.matches);
  root.dataset.scheme = dark ? 'dark' : 'light';
  root.dataset.contrast = s.contrast;
  root.dataset.legible = String(s.legible);
  root.dataset.motion = s.motion;
  root.style.setProperty('--font-scale', s.fontScale);
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', dark ? '#1e1f25' : '#eeedf4');
}
mq.addEventListener('change', () => applySettings());
