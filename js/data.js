// Proveedor de datos de DEMOSTRACIÓN (equipos y torneos ficticios).
// Para datos reales, implementa la misma interfaz en js/providers/ (ver README).

export const TEAMS = [
  { id: 'aur', name: 'Aurora Esports', tag: 'AUR', region: 'NA', players: ['Kestrel', 'Vanta', 'Mojo'] },
  { id: 'nvt', name: 'Nova Turbo', tag: 'NVT', region: 'NA', players: ['Zeal', 'Pip', 'Corvo'] },
  { id: 'bst', name: 'Boost Syndicate', tag: 'BST', region: 'NA', players: ['Rook', 'Juno', 'Saber'] },
  { id: 'flp', name: 'Flip Reset Club', tag: 'FLP', region: 'NA', players: ['Echo', 'Tidal', 'Brisk'] },
  { id: 'ceil', name: 'Ceiling Shot GG', tag: 'CSG', region: 'EU', players: ['Lumen', 'Fjord', 'Kato'] },
  { id: 'wvd', name: 'Wavedash Union', tag: 'WVD', region: 'EU', players: ['Orla', 'Sten', 'Mirko'] },
  { id: 'aer', name: 'Aerial Ascent', tag: 'AER', region: 'EU', players: ['Halo', 'Dusk', 'Ivo'] },
  { id: 'pnz', name: 'Pinch Zone', tag: 'PNZ', region: 'EU', players: ['Vex', 'Nils', 'Arno'] },
  { id: 'trq', name: 'Torque MX', tag: 'TRQ', region: 'SAM', players: ['Cacto', 'Ramal', 'Tizón'] },
  { id: 'hlx', name: 'Halcones X', tag: 'HLX', region: 'SAM', players: ['Brasa', 'Nubo', 'Quetz'] },
  { id: 'dmo', name: 'Demo Kings', tag: 'DMO', region: 'OCE', players: ['Koa', 'Reef', 'Dingo'] },
  { id: 'spd', name: 'Supersonic Div', tag: 'SPD', region: 'MENA', players: ['Najm', 'Sahm', 'Ward'] },
];
export const teamById = Object.fromEntries(TEAMS.map(t => [t.id, t]));

const MIN = 60e3, HOUR = 60 * MIN, DAY = 24 * HOUR;
const GAME_LEN = 300; // segundos de juego (5:00)

// Juego terminado generado al azar (para series ya jugadas)
export function randomFinishedGame(a, b) {
  const goals = []; const sc = [0, 0];
  const n = 1 + Math.floor(Math.random() * 6);
  const times = Array.from({ length: n }, () => Math.floor(Math.random() * GAME_LEN)).sort((x, y) => y - x);
  for (const t of times) {
    const side = Math.random() < .5 ? 0 : 1;
    sc[side]++;
    goals.push(goal(side, side ? b : a, t, sc));
  }
  let ot = false;
  if (sc[0] === sc[1]) { // tiempo extra
    ot = true; const side = Math.random() < .5 ? 0 : 1; sc[side]++;
    goals.push(goal(side, side ? b : a, -Math.floor(Math.random() * 90), sc));
  }
  return { score: [...sc], goals, ot, clock: 0, done: true };
}
export function goal(side, teamId, clock, sc) {
  const p = teamById[teamId].players;
  return { side, player: p[Math.floor(Math.random() * p.length)], clock, score: [...sc] };
}

function series(id, tId, stage, bo, a, b, start, playedGames = 0, liveGame = false) {
  const games = [];
  const need = Math.ceil(bo / 2); const wins = [0, 0];
  for (let i = 0; i < playedGames && wins[0] < need && wins[1] < need; i++) {
    const g = randomFinishedGame(a, b); games.push(g); wins[g.score[0] > g.score[1] ? 0 : 1]++;
  }
  const finished = wins[0] >= need || wins[1] >= need;
  let status = start > Date.now() ? 'upcoming' : finished ? 'done' : 'live';
  if (status === 'live' && (liveGame || games.length === 0)) {
    games.push({ score: [0, 0], goals: [], ot: false, clock: GAME_LEN - Math.floor(Math.random() * 120), done: false });
  }
  return { id, tId, stage, bo, teams: [a, b], start, status, games, pause: 0 };
}

export function buildData() {
  const now = Date.now();
  const tournaments = [
    { id: 't1', name: 'Open de Invierno', region: 'NA', tier: 'Open', start: now - 2 * HOUR, end: now + 6 * HOUR, prize: '$100,000', teams: 16, venue: 'En línea', stage: 'Playoffs' },
    { id: 't2', name: 'Open de Invierno', region: 'EU', tier: 'Open', start: now + 2 * DAY + 3 * HOUR, end: now + 4 * DAY, prize: '$136,000', teams: 16, venue: 'En línea', stage: 'Fase suiza' },
    { id: 't3', name: 'Major de Invierno', region: 'Mundial', tier: 'Major', start: now + 19 * DAY, end: now + 23 * DAY, prize: '$354,000', teams: 16, venue: 'LAN', stage: 'Por anunciar' },
    { id: 't4', name: 'Open de Primavera', region: 'SAM', tier: 'Open', start: now + 34 * DAY, end: now + 36 * DAY, prize: '$40,000', teams: 16, venue: 'En línea', stage: 'Por anunciar' },
  ];
  const s = [
    series('s1', 't1', 'Cuartos de final', 5, 'aur', 'flp', now - 90 * MIN, 5),
    series('s2', 't1', 'Cuartos de final', 7, 'nvt', 'bst', now - 40 * MIN, 2, true),
    series('s3', 't1', 'Cuartos de final', 5, 'trq', 'hlx', now - 5 * MIN, 0, true),
    series('s4', 't1', 'Cuartos de final', 5, 'dmo', 'spd', now + 2 * MIN),
    series('s5', 't1', 'Semifinal', 7, 'aur', 'nvt', now + 45 * MIN),
    series('s6', 't2', 'Ronda 1', 5, 'ceil', 'wvd', now + 2 * DAY + 3 * HOUR),
    series('s7', 't2', 'Ronda 1', 5, 'aer', 'pnz', now + 2 * DAY + 4 * HOUR),
  ];
  // s1 debe estar terminada
  while (s[0].status !== 'done') s[0] = series('s1', 't1', 'Cuartos de final', 5, 'aur', 'flp', now - 90 * MIN, 7);
  return { tournaments, series: s };
}

export { GAME_LEN };
