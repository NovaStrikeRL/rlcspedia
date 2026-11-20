import { buildData, TEAMS, teamById } from './data.js';
import { LiveEngine, need, wins, currentGame, fmtClock } from './live.js';
import { store, applySettings } from './store.js';
import { notify, requestPermission, permission } from './notify.js';

const ICON = {
  live: 'M12 8a4 4 0 100 8 4 4 0 000-8zM4.93 4.93l1.41 1.41A7.95 7.95 0 004 12c0 2.21.9 4.21 2.34 5.66l-1.41 1.41A9.97 9.97 0 012 12c0-2.76 1.12-5.26 2.93-7.07zm14.14 0A9.97 9.97 0 0122 12c0 2.76-1.12 5.26-2.93 7.07l-1.41-1.41A7.95 7.95 0 0020 12c0-2.21-.9-4.21-2.34-5.66l1.41-1.41z',
  trophy: 'M19 5h-2V3H7v2H5c-1.1 0-2 .9-2 2v1c0 2.55 1.92 4.63 4.39 4.94.63 1.5 1.98 2.63 3.61 2.96V19H7v2h10v-2h-4v-3.1c1.63-.33 2.98-1.46 3.61-2.96C19.08 12.63 21 10.55 21 8V7c0-1.1-.9-2-2-2zM5 8V7h2v3.82C5.84 10.4 5 9.3 5 8zm14 0c0 1.3-.84 2.4-2 2.82V7h2v1z',
  teams: 'M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3s1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5C6.34 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5c0-2.33-4.67-3.5-7-3.5zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z',
  tune: 'M3 17v2h6v-2H3zM3 5v2h10V5H3zm10 16v-2h8v-2h-8v-2h-2v6h2zM7 9v2H3v2h4v2h2V9H7zm14 4v-2H11v2h10zm-6-4h2V7h4V5h-4V3h-2v6z',
  star: 'M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z',
  starO: 'M22 9.24l-7.19-.62L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21 12 17.27 18.18 21l-1.63-7.03L22 9.24zM12 15.4l-3.76 2.27 1-4.28-3.32-2.88 4.38-.38L12 6.1l1.71 4.04 4.38.38-3.32 2.88 1 4.28L12 15.4z',
  back: 'M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z',
  bell: 'M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2zm6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 6 7.92 6 11v5l-2 2v1h16v-1l-2-2z',
  more: 'M16.59 8.59L12 13.17 7.41 8.59 6 10l6 6 6-6z',
  less: 'M12 8l-6 6 1.41 1.41L12 10.83l4.59 4.58L18 14z',
  check: 'M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z',
  search: 'M15.5 14h-.79l-.28-.27A6.47 6.47 0 0016 9.5 6.5 6.5 0 109.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5zm-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z',
};
const icon = (n, cls = 'i') => `<svg class="${cls}" viewBox="0 0 24 24" aria-hidden="true"><path d="${ICON[n]}"/></svg>`;
const esc = s => String(s).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const $ = s => document.querySelector(s);

const data = buildData();
const engine = new LiveEngine(data);
const expanded = new Set();
const flashing = new Set();
const view = $('#view');
const tById = Object.fromEntries(data.tournaments.map(t => [t.id, t]));
const dateFmt = new Intl.DateTimeFormat('es-MX', { day: 'numeric', month: 'short' });
const timeFmt = new Intl.DateTimeFormat('es-MX', { weekday: 'short', hour: '2-digit', minute: '2-digit' });

/* ---------------- Helpers ---------------- */
const hasFav = s => s.teams.some(store.isFav);
const byPriority = (a, b) => (hasFav(b) - hasFav(a)) || a.start - b.start;
const tname = id => teamById[id].name;

function countdown(ms) {
  if (ms <= 0) return 'Ahora';
  const m = Math.floor(ms / 60e3), h = Math.floor(m / 60), d = Math.floor(h / 24);
  if (d >= 1) return `En ${d} d`;
  if (h >= 1) return `En ${h} h ${m % 60} min`;
  return `En ${m} min ${Math.floor(ms / 1000) % 60} s`;
}

/* ---------------- Vistas ---------------- */
function seriesCard(s) {
  const w = wins(s), g = currentGame(s), n = need(s);
  const t = tById[s.tId];
  const isOpen = expanded.has(s.id);
  const winner = s.status === 'done' ? (w[0] > w[1] ? 0 : 1) : -1;
  const rows = [0, 1].map(i => {
    const id = s.teams[i], fav = store.isFav(id);
    const score = s.status === 'live' && g ? g.score[i] : s.status === 'done' ? w[i] : '–';
    return `<div class="team-row ${i ? 'orange' : 'blue'} ${winner === 1 - i ? 'lost' : ''}">
      <span class="stripe"></span>
      <span class="team-name">${fav ? icon('star', 'i star') : ''}<span>${esc(tname(id))}</span></span>
      <span class="pips" role="img" aria-label="${w[i]} de ${n} juegos ganados">${Array.from({ length: n }, (_, k) => `<span class="pip ${k < w[i] ? 'on' : ''}"></span>`).join('')}</span>
      <span class="score" aria-label="${s.status === 'live' ? 'Goles en el juego actual' : 'Juegos ganados'}: ${score}">${score}</span>
    </div>`;
  }).join('');

  let status;
  if (s.status === 'live') status = s.pause ? `Serie ${w[0]}–${w[1]} · siguiente juego` : `<b>Juego ${s.games.length}</b> · ${g?.ot ? 'Tiempo extra ' : ''}${fmtClock(g?.clock ?? 0)} · Serie ${w[0]}–${w[1]}`;
  else if (s.status === 'done') status = `Final · gana <b>${esc(tname(s.teams[winner]))}</b>`;
  else status = `${timeFmt.format(s.start)} · ${countdown(s.start - Date.now())}`;

  const gi = s.games.findLastIndex(x => x.goals.length);
  const last = s.status === 'live' && gi >= 0 ? s.games[gi].goals.at(-1) : null;
  const lastHtml = last
    ? `<p class="last-goal ${flashing.has(s.id) ? 'flash' : ''}">Último gol: <b>${esc(last.player)}</b> (${esc(teamById[s.teams[last.side]].tag)}) · ${last.score.join('–')}${gi !== s.games.length - 1 || s.pause ? ` en el juego ${gi + 1}` : ''}</p>` : '';

  const games = isOpen ? `<div class="games">${s.games.map((gm, i) => `
    <div class="game">
      <div class="game-line"><span>Juego ${i + 1}${gm.ot ? ' (TE)' : ''}${gm.done ? '' : ' · en curso'}</span><span>${gm.score.join(' – ')}</span></div>
      <ul class="goals">${gm.goals.map(ev => `<li><span class="dot ${ev.side ? 'orange' : 'blue'}"></span><span class="t">${fmtClock(ev.clock)}</span><span>${esc(ev.player)} · ${esc(teamById[s.teams[ev.side]].tag)} · ${ev.score.join('–')}</span></li>`).join('') || '<li>Sin goles todavía</li>'}</ul>
    </div>`).join('') || '<p class="view-sub" style="margin:0">La serie aún no empieza.</p>'}</div>` : '';

  return `<article class="series ${s.status} ${hasFav(s) ? 'fav' : ''}" aria-label="${esc(tname(s.teams[0]))} contra ${esc(tname(s.teams[1]))}">
    <div class="series-head">
      ${s.status === 'live' ? '<span class="chip live">En vivo</span>' : ''}
      <span class="where">${esc(t.name)} ${esc(t.region)} · ${esc(s.stage)}</span>
      <span class="chip">Bo${s.bo}</span>
    </div>
    ${rows}
    ${lastHtml}
    <div class="series-foot">
      <span class="status">${status}</span>
      <button class="btn text" data-toggle="${s.id}" data-focus="t-${s.id}" aria-expanded="${isOpen}">
        ${isOpen ? 'Ocultar juegos' : 'Ver juegos'} ${icon(isOpen ? 'less' : 'more')}
      </button>
    </div>
    ${games}
  </article>`;
}

function viewLive() {
  const live = data.series.filter(s => s.status === 'live').sort(byPriority);
  const soon = data.series.filter(s => s.status === 'upcoming' && s.start - Date.now() < 6 * 3600e3).sort(byPriority);
  const done = data.series.filter(s => s.status === 'done').sort(byPriority);
  return `
    <div class="banner">${icon('bell')}<span>Datos de demostración: los partidos se simulan para probar marcadores y notificaciones.</span></div>
    <h2 class="view-title">En vivo</h2>
    <div class="stack">${live.map(seriesCard).join('') || '<p class="empty">No hay series en vivo ahora mismo.</p>'}</div>
    ${soon.length ? `<h2 class="view-title">Próximas series</h2><div class="stack">${soon.map(seriesCard).join('')}</div>` : ''}
    ${done.length ? `<h2 class="view-title">Terminadas</h2><div class="stack">${done.map(seriesCard).join('')}</div>` : ''}`;
}

function viewTournaments() {
  const now = Date.now();
  const list = data.tournaments.filter(t => t.end > now).sort((a, b) => a.start - b.start);
  return `<h2 class="view-title">Torneos</h2>
    <p class="view-sub">En curso y próximos, por fecha de inicio.</p>
    <div class="stack">${list.map(t => {
      const on = t.start <= now;
      const d = new Date(t.start);
      return `<button class="tourney ${on ? 'now' : ''}" data-go="#torneo-${t.id}" data-focus="go-${t.id}">
        <span class="date-block"><span class="d">${d.getDate()}</span><br><span class="m">${dateFmt.format(d).split(' ')[1]}</span></span>
        <span><h3>${esc(t.name)} · ${esc(t.region)}</h3><p>${esc(t.tier)} · ${t.teams} equipos · ${esc(t.prize)} · ${esc(t.venue)}</p></span>
        <span class="countdown">${on ? '<span class="chip live">En curso</span>' : countdown(t.start - now)}</span>
      </button>`;
    }).join('')}</div>`;
}

function viewTournament(id) {
  const t = tById[id];
  if (!t) return `<p class="empty">No encontramos ese torneo. <a href="#torneos">Volver a torneos</a></p>`;
  const ss = data.series.filter(s => s.tId === id);
  const order = { live: 0, upcoming: 1, done: 2 };
  ss.sort((a, b) => order[a.status] - order[b.status] || byPriority(a, b));
  return `<div style="display:flex;align-items:center;gap:4px;margin-top:8px">
      <a class="icon-btn" href="#torneos" aria-label="Volver a torneos">${icon('back')}</a>
      <h2 class="view-title" style="margin:0">${esc(t.name)} · ${esc(t.region)}</h2></div>
    <p class="view-sub" style="margin-top:4px">${dateFmt.format(t.start)} – ${dateFmt.format(t.end)} · ${esc(t.prize)} · ${esc(t.stage)}</p>
    <div class="stack">${ss.map(seriesCard).join('') || '<p class="empty">Las series se publican cuando se define el bracket.</p>'}</div>`;
}

function viewTeams(q = '') {
  const f = q.trim().toLowerCase();
  const list = TEAMS.filter(t => !f || (t.name + t.tag + t.region + t.players.join()).toLowerCase().includes(f))
    .sort((a, b) => (store.isFav(b.id) - store.isFav(a.id)) || a.name.localeCompare(b.name));
  return `<h2 class="view-title">Equipos favoritos</h2>
    <p class="view-sub">Sus series aparecen primero y puedes recibir avisos solo de ellos.</p>
    <label class="search">${icon('search')}<span class="sr-only">Buscar equipo</span>
      <input id="team-q" type="search" placeholder="Buscar equipo, región o jugador" value="${esc(q)}" autocomplete="off"></label>
    <ul class="list">${list.map(t => {
      const fav = store.isFav(t.id);
      return `<li class="list-item"><span class="avatar" aria-hidden="true">${esc(t.tag)}</span>
        <span class="text"><b>${esc(t.name)}</b><small>${esc(t.region)} · ${t.players.map(esc).join(', ')}</small></span>
        <button class="icon-btn" data-fav="${t.id}" data-focus="f-${t.id}" aria-pressed="${fav}" aria-label="${fav ? 'Quitar de' : 'Agregar a'} favoritos: ${esc(t.name)}">${icon(fav ? 'star' : 'starO')}</button></li>`;
    }).join('') || '<li class="empty">Ningún equipo coincide con la búsqueda.</li>'}</ul>`;
}

function seg(name, value, options) {
  return `<div class="segmented" role="radiogroup" aria-label="${name}">${options.map(([v, label]) =>
    `<button role="radio" aria-checked="${v === value}" data-set="${name}" data-val="${v}">${icon('check')}${label}</button>`).join('')}</div>`;
}
function sw(path, checked, label, hint = '') {
  return `<label class="setting"><span class="text">${label}${hint ? `<small>${hint}</small>` : ''}</span>
    <span class="switch"><input type="checkbox" role="switch" data-sw="${path}" ${checked ? 'checked' : ''}><span class="track"></span></span></label>`;
}

function viewSettings() {
  const s = store.get(), p = permission();
  const permText = { granted: 'Activadas en este dispositivo', denied: 'Bloqueadas: actívalas desde los ajustes del navegador', default: 'Aún no activadas', unsupported: 'Este navegador no admite notificaciones' }[p];
  return `<h2 class="view-title">Ajustes</h2>
    <h3 class="group-title">Apariencia y accesibilidad</h3>
    <div class="card-flat">
      <div class="setting col"><span>Tema</span>${seg('theme', s.theme, [['system', 'Sistema'], ['light', 'Claro'], ['dark', 'Oscuro']])}</div>
      <div class="setting col"><span>Contraste</span>${seg('contrast', s.contrast, [['standard', 'Estándar'], ['high', 'Alto']])}</div>
      <div class="setting col"><label for="fs">Tamaño de texto: <b id="fs-val">${Math.round(s.fontScale * 100)}%</b></label>
        <input id="fs" type="range" min="0.85" max="1.5" step="0.05" value="${s.fontScale}" aria-valuetext="${Math.round(s.fontScale * 100)} por ciento"></div>
      ${sw('legible', s.legible, 'Fuente de alta legibilidad', 'Usa Atkinson Hyperlegible también en marcadores y títulos')}
      ${sw('motion', s.motion === 'reduce', 'Reducir movimiento', 'Quita animaciones y parpadeos de goles')}
    </div>
    <h3 class="group-title">Notificaciones (teléfono y reloj)</h3>
    <div class="card-flat">
      <div class="setting"><span class="text">Permiso<small>${permText}</small></span>
        ${p === 'default' ? '<button class="btn filled" id="perm">Activar</button>' : p === 'granted' ? '<button class="btn tonal" id="test">Probar</button>' : ''}</div>
      ${sw('notify.seriesStart', s.notify.seriesStart, 'Inicio de serie')}
      ${sw('notify.goals', s.notify.goals, 'Goles', 'Jugador, equipo y marcador del juego')}
      ${sw('notify.gameEnd', s.notify.gameEnd, 'Fin de cada juego', 'Resultado del juego y cómo va la serie')}
      ${sw('notify.seriesEnd', s.notify.seriesEnd, 'Fin de serie', 'Ganador y marcador final')}
      ${sw('notify.favoritesOnly', s.notify.favoritesOnly, 'Solo de mis favoritos')}
    </div>
    <p class="view-sub" style="margin-top:12px">En el reloj: las notificaciones del teléfono llegan a Wear OS y Apple Watch si tienes activado el espejo de notificaciones del navegador o de la app instalada.</p>`;
}

/* ---------------- Router y render ---------------- */
let teamQuery = '';
function route() { return location.hash.slice(1) || (data.series.some(s => s.status === 'live') ? 'live' : 'torneos'); }

function render() {
  const r = route();
  const focusKey = document.activeElement?.dataset?.focus;
  const hadQ = document.activeElement?.id === 'team-q';
  const sel = hadQ ? document.activeElement.selectionStart : 0;
  if (r === 'live') view.innerHTML = viewLive();
  else if (r === 'torneos') view.innerHTML = viewTournaments();
  else if (r.startsWith('torneo-')) view.innerHTML = viewTournament(r.slice(7));
  else if (r === 'favoritos') view.innerHTML = viewTeams(teamQuery);
  else if (r === 'ajustes') view.innerHTML = viewSettings();
  else view.innerHTML = viewLive();
  if (focusKey) view.querySelector(`[data-focus="${focusKey}"]`)?.focus();
  if (hadQ) { const q = $('#team-q'); q.focus(); q.setSelectionRange(sel, sel); }

  const tab = r.startsWith('torneo') ? 'torneos' : r;
  document.querySelectorAll('.nav a').forEach(a => a.dataset.tab === tab ? a.setAttribute('aria-current', 'page') : a.removeAttribute('aria-current'));
  const n = data.series.filter(s => s.status === 'live').length;
  $('#live-badge').textContent = n; $('#live-badge').hidden = !n;
}
const LIVE_VIEWS = r => r === 'live' || r === 'torneos' || r.startsWith('torneo-');

/* ---------------- Eventos de UI ---------------- */
view.addEventListener('click', async e => {
  const el = e.target.closest('[data-toggle],[data-go],[data-fav],[data-set],#perm,#test');
  if (!el) return;
  if (el.dataset.toggle) { const id = el.dataset.toggle; expanded.has(id) ? expanded.delete(id) : expanded.add(id); render(); }
  else if (el.dataset.go) location.hash = el.dataset.go;
  else if (el.dataset.fav) {
    store.toggleFavorite(el.dataset.fav);
    toast(store.isFav(el.dataset.fav) ? `${tname(el.dataset.fav)} agregado a favoritos` : `${tname(el.dataset.fav)} quitado de favoritos`);
    render();
  }
  else if (el.dataset.set) { store.set({ [el.dataset.set]: el.dataset.val }); render(); }
  else if (el.id === 'perm') { await requestPermission(); render(); }
  else if (el.id === 'test') {
    const ok = await notify('goals', { title: '⚽ Gol de Kestrel (AUR)', body: 'Aurora Esports 2 – 1 Flip Reset Club · Juego 3 · 1:12', tag: 'test', teams: ['aur'] });
    toast(ok ? 'Notificación de prueba enviada' : 'Activa "Goles" para probar');
  }
});
view.addEventListener('change', e => {
  const k = e.target.dataset.sw; if (!k) return;
  const v = e.target.checked;
  if (k === 'motion') store.set({ motion: v ? 'reduce' : 'system' });
  else if (k.startsWith('notify.')) store.set({ notify: { [k.split('.')[1]]: v } });
  else store.set({ [k]: v });
});
view.addEventListener('input', e => {
  if (e.target.id === 'fs') {
    const v = Number(e.target.value); store.set({ fontScale: v });
    $('#fs-val').textContent = `${Math.round(v * 100)}%`;
    e.target.setAttribute('aria-valuetext', `${Math.round(v * 100)} por ciento`);
  }
  if (e.target.id === 'team-q') { teamQuery = e.target.value; render(); }
});
store.subscribe(s => applySettings(s));
addEventListener('hashchange', () => { render(); scrollTo(0, 0); });
addEventListener('scroll', () => $('.top-bar').classList.toggle('scrolled', scrollY > 4), { passive: true });

/* ---------------- Snackbar y anuncios accesibles ---------------- */
let toastTimer;
function toast(msg) {
  const el = $('#snackbar'); el.textContent = msg; el.classList.add('show');
  clearTimeout(toastTimer); toastTimer = setTimeout(() => el.classList.remove('show'), 4000);
}
const announce = msg => { $('#announcer').textContent = msg; };

/* ---------------- Eventos del motor → UI + notificaciones ---------------- */
const label = s => `${tname(s.teams[0])} vs ${tname(s.teams[1])}`;
engine.addEventListener('seriesStart', ({ detail: { s } }) => {
  const msg = `Empieza ${label(s)} · Bo${s.bo}`;
  toast(msg); announce(msg);
  notify('seriesStart', { title: '🎮 Serie en vivo', body: `${msg} · ${tById[s.tId].name}`, tag: s.id, teams: s.teams });
});
engine.addEventListener('goal', ({ detail: { s, g, ev } }) => {
  const team = teamById[s.teams[ev.side]];
  const sc = `${teamById[s.teams[0]].tag} ${ev.score[0]} – ${ev.score[1]} ${teamById[s.teams[1]].tag}`;
  if (hasFav(s)) toast(`Gol de ${ev.player} (${team.tag}) · ${sc}`);
  announce(`Gol de ${ev.player}, ${team.name}. ${sc}`);
  flashing.add(s.id); setTimeout(() => flashing.delete(s.id), 1300);
  notify('goals', { title: `⚽ Gol de ${ev.player} (${team.tag})`, body: `${sc} · Juego ${s.games.length}${g.ot ? ' · Tiempo extra' : ''} · ${fmtClock(ev.clock)}`, tag: s.id, teams: s.teams });
});
engine.addEventListener('gameEnd', ({ detail: { s, g, index } }) => {
  const w = wins(s), wi = g.score[0] > g.score[1] ? 0 : 1;
  notify('gameEnd', { title: `Juego ${index}: gana ${teamById[s.teams[wi]].tag}`, body: `${g.score.join('–')}${g.ot ? ' (TE)' : ''} · Serie ${teamById[s.teams[0]].tag} ${w[0]}–${w[1]} ${teamById[s.teams[1]].tag}`, tag: s.id, teams: s.teams });
});
engine.addEventListener('seriesEnd', ({ detail: { s, w } }) => {
  const wi = w[0] > w[1] ? 0 : 1;
  const msg = `${tname(s.teams[wi])} gana la serie ${Math.max(...w)}–${Math.min(...w)}`;
  toast(msg); announce(msg);
  notify('seriesEnd', { title: '🏆 Serie terminada', body: `${msg} · ${label(s)}`, tag: s.id, teams: s.teams });
});
engine.addEventListener('update', () => {
  if (LIVE_VIEWS(route())) render();
  else { const n = data.series.filter(s => s.status === 'live').length; $('#live-badge').textContent = n; $('#live-badge').hidden = !n; }
  pushWidgetSnapshot();
});

/* ---------------- Widget: snapshot para el Service Worker ---------------- */
let lastWidget = 0;
function pushWidgetSnapshot() {
  if (Date.now() - lastWidget < 15000 || !navigator.serviceWorker?.controller) return;
  lastWidget = Date.now();
  const s = data.series.filter(x => x.status === 'live').sort(byPriority)[0]
    || data.series.filter(x => x.status === 'upcoming').sort(byPriority)[0];
  if (!s) return;
  const w = wins(s), g = currentGame(s);
  navigator.serviceWorker.controller.postMessage({ type: 'widget', payload: {
    tournament: `${tById[s.tId].name} ${tById[s.tId].region}`,
    status: s.status === 'live' ? `En vivo · Juego ${s.games.length} · ${fmtClock(g?.clock ?? 0)}` : countdown(s.start - Date.now()),
    teamA: tname(s.teams[0]), teamB: tname(s.teams[1]),
    scoreA: String(g?.score[0] ?? 0), scoreB: String(g?.score[1] ?? 0),
    series: `Serie ${w[0]}–${w[1]} · Bo${s.bo}`,
  } });
}

/* ---------------- Arranque ---------------- */
applySettings();
render();
engine.start();
if ('serviceWorker' in navigator) navigator.serviceWorker.register('sw.js').catch(() => {});
