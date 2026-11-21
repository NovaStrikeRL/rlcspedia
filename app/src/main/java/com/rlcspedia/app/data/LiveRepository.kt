package com.rlcspedia.app.data

import android.content.Context
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

/**
 * Fuente única de verdad para torneos, equipos y series.
 * - LIQUIPEDIA: lee el feed publicado en GitHub cada minuto y compara con el estado
 *   anterior para emitir eventos (inicio de serie, cambio de marcador, fin de juego y de serie).
 * - DEMO: simula partidos (1 s real = 10 s de juego).
 */
object LiveRepository {
    private const val POLL_MS = 60_000L
    private const val TICK_MS = 1_000L
    private const val SIM_SECONDS = 10
    private const val GOAL_P = 0.035

    private lateinit var appContext: Context
    private lateinit var scope: CoroutineScope
    private var job: Job? = null
    private val mutex = Mutex()
    private var baseline = false

    var source: DataSource = DataSource.DEMO
        private set

    private val _teams = MutableStateFlow(DemoData.teams)
    val teams: StateFlow<List<Team>> = _teams.asStateFlow()
    private var teamIndex: Map<String, Team> = DemoData.teamById

    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    val tournaments: StateFlow<List<Tournament>> = _tournaments.asStateFlow()

    private val _series = MutableStateFlow<List<Series>>(emptyList())
    val series: StateFlow<List<Series>> = _series.asStateFlow()

    private val _feedState = MutableStateFlow<FeedState>(FeedState.Demo)
    val feedState: StateFlow<FeedState> = _feedState.asStateFlow()

    private val _events = MutableSharedFlow<LiveEvent>(extraBufferCapacity = 128)
    val events: SharedFlow<LiveEvent> = _events.asSharedFlow()

    fun team(id: String): Team = teamIndex[id] ?: Team(id, id.replace('-', ' '), id.take(3).uppercase(), "", emptyList())
    fun tournament(id: String) = _tournaments.value.firstOrNull { it.id == id }

    fun init(context: Context, appScope: CoroutineScope, initial: DataSource) {
        appContext = context.applicationContext
        scope = appScope
        setSource(initial)
    }

    fun setSource(next: DataSource) {
        if (::scope.isInitialized.not()) return
        job?.cancel()
        source = next
        when (next) {
            DataSource.DEMO -> {
                val (t, s) = DemoData.build()
                setTeams(DemoData.teams); _tournaments.value = t; _series.value = s
                _feedState.value = FeedState.Demo
                job = scope.launch { while (isActive) { delay(TICK_MS); _series.value = _series.value.map { tick(it) } } }
            }
            DataSource.LIQUIPEDIA -> {
                val cached = FeedClient.cached(appContext)
                baseline = cached != null
                if (cached != null) {
                    applyFeed(cached, emitEvents = false)
                    _feedState.value = FeedState.Ok(cached.generatedAt, cached.attribution)
                } else {
                    setTeams(emptyList()); _tournaments.value = emptyList(); _series.value = emptyList()
                    _feedState.value = FeedState.Loading
                }
                job = scope.launch { while (isActive) { refresh(); delay(POLL_MS) } }
            }
        }
    }

    /** Descarga el feed y emite eventos por diferencias. Devuelve true si funcionó. */
    suspend fun refresh(): Boolean {
        if (source != DataSource.LIQUIPEDIA) return false
        return mutex.withLock {
            try {
                val feed = FeedClient.download(appContext)
                applyFeed(feed, emitEvents = baseline)
                baseline = true
                _feedState.value = FeedState.Ok(feed.generatedAt, feed.attribution)
                true
            } catch (e: Exception) {
                val last = (_feedState.value as? FeedState.Ok)?.generatedAt ?: (_feedState.value as? FeedState.Error)?.lastOk
                _feedState.value = FeedState.Error(e.message ?: "Sin conexión", last)
                false
            }
        }
    }

    private fun setTeams(list: List<Team>) {
        _teams.value = list; teamIndex = list.associateBy { it.id }
    }

    private fun applyFeed(feed: Feed, emitEvents: Boolean) {
        val old = _series.value.associateBy { it.id }
        setTeams(feed.teams)
        _tournaments.value = feed.tournaments
        _series.value = feed.series
        if (!emitEvents) return
        for (n in feed.series) {
            val o = old[n.id]
            if (n.status == SeriesStatus.UPCOMING) continue
            if (o == null || o.status == SeriesStatus.UPCOMING) emit(LiveEvent.SeriesStarted(n))
            n.games.forEachIndexed { i, g ->
                val og = o?.games?.getOrNull(i)
                val pa = og?.scoreA ?: 0; val pb = og?.scoreB ?: 0
                if (!g.done) {
                    if (g.scoreA > pa) emit(LiveEvent.ScoreUpdate(n, 0, i + 1, g.scoreA, g.scoreB))
                    if (g.scoreB > pb) emit(LiveEvent.ScoreUpdate(n, 1, i + 1, g.scoreA, g.scoreB))
                } else if (og?.done != true) {
                    emit(LiveEvent.GameEnded(n, g, i + 1))
                }
            }
            if (n.status == SeriesStatus.DONE && o?.status != SeriesStatus.DONE) emit(LiveEvent.SeriesEnded(n))
        }
    }

    private fun emit(e: LiveEvent) { _events.tryEmit(e) }

    /* ---------------- Simulador (modo demostración) ---------------- */
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
