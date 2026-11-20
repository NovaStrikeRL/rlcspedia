// Motor en vivo. En modo demo simula partidos; con un proveedor real,
// el proveedor emite los mismos eventos: seriesStart, goal, gameEnd, seriesEnd, update.
import { goal, GAME_LEN } from './data.js';

const TICK_MS = 1000;      // 1 s real
const SIM_SECONDS = 10;    // = 10 s de juego (un juego dura ~30 s reales en demo)
const GOAL_P = 0.035;      // probabilidad de gol por equipo y tick

export const need = s => Math.ceil(s.bo / 2);
export const wins = s => s.games.filter(g => g.done).reduce((w, g) => (w[g.score[0] > g.score[1] ? 0 : 1]++, w), [0, 0]);
export const currentGame = s => s.games.find(g => !g.done);
export const fmtClock = c => c < 0 ? `+${Math.floor(-c / 60)}:${String(-c % 60).padStart(2, '0')}` : `${Math.floor(c / 60)}:${String(c % 60).padStart(2, '0')}`;

export class LiveEngine extends EventTarget {
  constructor(data) { super(); this.data = data; }
  start() { this.timer ??= setInterval(() => this.tick(), TICK_MS); }
  stop() { clearInterval(this.timer); this.timer = null; }
  emit(type, detail) { this.dispatchEvent(new CustomEvent(type, { detail })); }

  tick() {
    const now = Date.now();
    for (const s of this.data.series) {
      if (s.status === 'upcoming' && now >= s.start) {
        s.status = 'live';
        s.games.push(newGame());
        this.emit('seriesStart', { s });
      }
      if (s.status !== 'live') continue;
      if (s.pause > 0) { s.pause--; if (s.pause === 0) { s.games.push(newGame()); this.emit('gameStart', { s }); } continue; }
      const g = currentGame(s); if (!g) continue;

      g.clock -= SIM_SECONDS;
      if (!g.ot) g.clock = Math.max(g.clock, 0);
      for (const side of [0, 1]) {
        if (Math.random() < GOAL_P) {
          g.score[side]++;
          const ev = goal(side, s.teams[side], g.clock, g.score);
          g.goals.push(ev);
          this.emit('goal', { s, g, ev });
          if (g.ot) break; // gol de oro
        }
      }
      let over = false;
      if (g.ot) over = g.score[0] !== g.score[1];
      else if (g.clock === 0) { if (g.score[0] !== g.score[1]) over = true; else g.ot = true; }
      if (over) {
        g.done = true;
        const w = wins(s);
        this.emit('gameEnd', { s, g, index: s.games.length });
        if (w[0] >= need(s) || w[1] >= need(s)) { s.status = 'done'; this.emit('seriesEnd', { s, w }); }
        else s.pause = 3;
      }
    }
    this.emit('update');
  }
}

function newGame() { return { score: [0, 0], goals: [], ot: false, clock: GAME_LEN, done: false }; }
