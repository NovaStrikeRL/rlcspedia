package com.rlcspedia.app.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Fuente única de verdad para torneos y series.
 * En modo demo simula los partidos: 1 s real = 10 s de juego (un juego dura ~30 s).
 * Un proveedor real emitiría los mismos [LiveEvent].
 */
object LiveRepository {
    private const val TICK_MS = 1_000L
    private const val SIM_SECONDS = 10
    private const val GOAL_P = 0.035

    val teams = DemoData.teams
    val teamById = DemoData.teamById

    private val initial = DemoData.build()
    private val _tournaments = MutableStateFlow(initial.first)
    val tournaments: StateFlow<List<Tournament>> = _tournaments.asStateFlow()

    private val _series = MutableStateFlow(initial.second)
    val series: StateFlow<List<Series>> = _series.asStateFlow()

    private val _events = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<LiveEvent> = _events.asSharedFlow()

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                delay(TICK_MS)
                _series.value = _series.value.map { tick(it) }
            }
        }
    }

    fun tournament(id: String) = _tournaments.value.firstOrNull { it.id == id }

    private fun emit(e: LiveEvent) { _events.tryEmit(e) }

    private fun tick(input: Series): Series {
        var s = input
        val now = System.currentTimeMillis()

        if (s.status == SeriesStatus.UPCOMING && now >= s.start) {
            s = s.copy(status = SeriesStatus.LIVE, games = s.games + Game())
            emit(LiveEvent.SeriesStarted(s))
            return s
        }
        if (s.status != SeriesStatus.LIVE) return s

        if (s.pauseTicks > 0) {
            val left = s.pauseTicks - 1
            return if (left == 0) s.copy(pauseTicks = 0, games = s.games + Game()) else s.copy(pauseTicks = left)
        }

        val idx = s.games.indexOfLast { !it.done }
        if (idx < 0) return s
        var g = s.games[idx]

        var clock = g.clock - SIM_SECONDS
        if (!g.overtime) clock = clock.coerceAtLeast(0)
        g = g.copy(clock = clock)

        for (side in 0..1) {
            if (Random.nextDouble() < GOAL_P) {
                val a = g.scoreA + if (side == 0) 1 else 0
                val b = g.scoreB + if (side == 1) 1 else 0
                val goal = DemoData.goal(side, s.teams[side], g.clock, a, b)
                g = g.copy(scoreA = a, scoreB = b, goals = g.goals + goal)
                s = s.withGame(idx, g)
                emit(LiveEvent.GoalScored(s, goal, idx + 1, g.overtime))
                if (g.overtime) break // gol de oro
            }
        }

        val over = when {
            g.overtime -> g.scoreA != g.scoreB
            g.clock == 0 && g.scoreA != g.scoreB -> true
            g.clock == 0 -> { g = g.copy(overtime = true); false }
            else -> false
        }
        if (over) g = g.copy(done = true)
        s = s.withGame(idx, g)

        if (over) {
            emit(LiveEvent.GameEnded(s, g, idx + 1))
            val (wa, wb) = s.wins
            s = if (wa >= s.needed || wb >= s.needed) {
                s.copy(status = SeriesStatus.DONE).also { emit(LiveEvent.SeriesEnded(it)) }
            } else s.copy(pauseTicks = 3)
        }
        return s
    }

    private fun Series.withGame(i: Int, g: Game) = copy(games = games.toMutableList().also { it[i] = g })
}
