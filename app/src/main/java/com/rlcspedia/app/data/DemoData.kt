package com.rlcspedia.app.data

import kotlin.random.Random

/**
 * Proveedor de DEMOSTRACIÓN: equipos y torneos ficticios.
 * Para datos reales, reemplaza este objeto por un proveedor que entregue
 * las mismas estructuras (ver README → "Datos reales").
 */
object DemoData {
    val teams = listOf(
        Team("aur", "Aurora Esports", "AUR", "NA", listOf("Kestrel", "Vanta", "Mojo")),
        Team("nvt", "Nova Turbo", "NVT", "NA", listOf("Zeal", "Pip", "Corvo")),
        Team("bst", "Boost Syndicate", "BST", "NA", listOf("Rook", "Juno", "Saber")),
        Team("flp", "Flip Reset Club", "FLP", "NA", listOf("Echo", "Tidal", "Brisk")),
        Team("csg", "Ceiling Shot GG", "CSG", "EU", listOf("Lumen", "Fjord", "Kato")),
        Team("wvd", "Wavedash Union", "WVD", "EU", listOf("Orla", "Sten", "Mirko")),
        Team("aer", "Aerial Ascent", "AER", "EU", listOf("Halo", "Dusk", "Ivo")),
        Team("pnz", "Pinch Zone", "PNZ", "EU", listOf("Vex", "Nils", "Arno")),
        Team("trq", "Torque MX", "TRQ", "SAM", listOf("Cacto", "Ramal", "Tizón")),
        Team("hlx", "Halcones X", "HLX", "SAM", listOf("Brasa", "Nubo", "Quetz")),
        Team("dmo", "Demo Kings", "DMO", "OCE", listOf("Koa", "Reef", "Dingo")),
        Team("spd", "Supersonic Div", "SPD", "MENA", listOf("Najm", "Sahm", "Ward")),
    )
    val teamById = teams.associateBy { it.id }

    fun goal(side: Int, teamId: String, clock: Int, a: Int, b: Int): Goal =
        Goal(side, teamById.getValue(teamId).players.random(), clock, a, b)

    private fun finishedGame(teamA: String, teamB: String): Game {
        var a = 0; var b = 0
        val goals = mutableListOf<Goal>()
        val times = List(Random.nextInt(1, 7)) { Random.nextInt(GAME_LENGTH) }.sortedDescending()
        for (t in times) {
            val side = Random.nextInt(2)
            if (side == 0) a++ else b++
            goals += goal(side, if (side == 0) teamA else teamB, t, a, b)
        }
        var ot = false
        if (a == b) {
            ot = true
            val side = Random.nextInt(2)
            if (side == 0) a++ else b++
            goals += goal(side, if (side == 0) teamA else teamB, -Random.nextInt(5, 120), a, b)
        }
        return Game(a, b, goals, ot, 0, true)
    }

    private fun series(
        id: String, tId: String, stage: String, bo: Int, a: String, b: String,
        start: Long, played: Int = 0, liveGame: Boolean = false,
    ): Series {
        val need = (bo + 1) / 2
        val games = mutableListOf<Game>()
        var wa = 0; var wb = 0
        while (games.size < played && wa < need && wb < need) {
            val g = finishedGame(a, b); games += g
            if (g.winner == 0) wa++ else wb++
        }
        val finished = wa >= need || wb >= need
        val status = when {
            start > System.currentTimeMillis() -> SeriesStatus.UPCOMING
            finished -> SeriesStatus.DONE
            else -> SeriesStatus.LIVE
        }
        if (status == SeriesStatus.LIVE && (liveGame || games.isEmpty())) {
            games += Game(clock = GAME_LENGTH - Random.nextInt(120))
        }
        return Series(id, tId, stage, bo, a, b, start, status, games)
    }

    fun build(): Pair<List<Tournament>, List<Series>> {
        val now = System.currentTimeMillis()
        val min = 60_000L; val hour = 60 * min; val day = 24 * hour
        val tournaments = listOf(
            Tournament("t1", "Open de Invierno", "NA", "Open", now - 2 * hour, now + 6 * hour, "$100,000", 16, "En línea", "Playoffs"),
            Tournament("t2", "Open de Invierno", "EU", "Open", now + 2 * day + 3 * hour, now + 4 * day, "$136,000", 16, "En línea", "Fase suiza"),
            Tournament("t3", "Major de Invierno", "Mundial", "Major", now + 19 * day, now + 23 * day, "$354,000", 16, "LAN", "Por anunciar"),
            Tournament("t4", "Open de Primavera", "SAM", "Open", now + 34 * day, now + 36 * day, "$40,000", 16, "En línea", "Por anunciar"),
        )
        var s1 = series("s1", "t1", "Cuartos de final", 5, "aur", "flp", now - 90 * min, 7)
        while (s1.status != SeriesStatus.DONE) s1 = series("s1", "t1", "Cuartos de final", 5, "aur", "flp", now - 90 * min, 7)
        val series = listOf(
            s1,
            series("s2", "t1", "Cuartos de final", 7, "nvt", "bst", now - 40 * min, 2, liveGame = true),
            series("s3", "t1", "Cuartos de final", 5, "trq", "hlx", now - 5 * min, 0, liveGame = true),
            series("s4", "t1", "Cuartos de final", 5, "dmo", "spd", now + 2 * min),
            series("s5", "t1", "Semifinal", 7, "aur", "nvt", now + 45 * min),
            series("s6", "t2", "Ronda 1", 5, "csg", "wvd", now + 2 * day + 3 * hour),
            series("s7", "t2", "Ronda 1", 5, "aer", "pnz", now + 2 * day + 4 * hour),
        )
        return tournaments to series
    }
}
