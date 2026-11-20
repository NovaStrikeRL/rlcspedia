package com.rlcspedia.app.data

data class Team(
    val id: String,
    val name: String,
    val tag: String,
    val region: String,
    val players: List<String>,
)

data class Tournament(
    val id: String,
    val name: String,
    val region: String,
    val tier: String,
    val start: Long,
    val end: Long,
    val prize: String,
    val teams: Int,
    val venue: String,
    val stage: String,
)

/** side = 0 (equipo azul / A) o 1 (equipo naranja / B). clock en segundos; negativo = tiempo extra. */
data class Goal(val side: Int, val player: String, val clock: Int, val scoreA: Int, val scoreB: Int)

data class Game(
    val scoreA: Int = 0,
    val scoreB: Int = 0,
    val goals: List<Goal> = emptyList(),
    val overtime: Boolean = false,
    val clock: Int = GAME_LENGTH,
    val done: Boolean = false,
) {
    val winner: Int get() = if (scoreA > scoreB) 0 else 1
}

enum class SeriesStatus { UPCOMING, LIVE, DONE }

data class Series(
    val id: String,
    val tournamentId: String,
    val stage: String,
    val bestOf: Int,
    val teamA: String,
    val teamB: String,
    val start: Long,
    val status: SeriesStatus,
    val games: List<Game>,
    val pauseTicks: Int = 0,
) {
    val needed: Int get() = (bestOf + 1) / 2
    val teams: List<String> get() = listOf(teamA, teamB)
    val wins: Pair<Int, Int>
        get() = games.filter { it.done }.fold(0 to 0) { (a, b), g -> if (g.winner == 0) (a + 1) to b else a to (b + 1) }
    val currentGame: Game? get() = games.lastOrNull { !it.done }
    val seriesWinner: Int get() = if (wins.first > wins.second) 0 else 1
}

sealed interface LiveEvent {
    val series: Series
    data class SeriesStarted(override val series: Series) : LiveEvent
    data class GoalScored(override val series: Series, val goal: Goal, val gameNumber: Int, val overtime: Boolean) : LiveEvent
    data class GameEnded(override val series: Series, val game: Game, val gameNumber: Int) : LiveEvent
    data class SeriesEnded(override val series: Series) : LiveEvent
}

const val GAME_LENGTH = 300

fun formatClock(clock: Int): String =
    if (clock < 0) "+%d:%02d".format(-clock / 60, -clock % 60) else "%d:%02d".format(clock / 60, clock % 60)
