package com.rlcspedia.app.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Contenido ya convertido del feed. */
data class Feed(
    val generatedAt: Long,
    val attribution: String,
    val tournaments: List<Tournament>,
    val teams: List<Team>,
    val series: List<Series>,
)

/**
 * Lee el feed que publica el workflow "Datos de Liquipedia" en la rama `data` del repo.
 * La app nunca llama a Liquipedia directamente (su clave vive en GitHub).
 */
object FeedClient {
    const val FEED_URL = "https://raw.githubusercontent.com/NovaStrikeRL/rlcspedia/data/feed.json"
    private const val CACHE = "feed.json"

    suspend fun download(context: Context): Feed = withContext(Dispatchers.IO) {
        val conn = (URL(FEED_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000; readTimeout = 15_000
            setRequestProperty("User-Agent", "RLCSpedia-Android")
            setRequestProperty("Cache-Control", "no-cache")
        }
        try {
            when (val code = conn.responseCode) {
                200 -> Unit
                404 -> throw FeedException("Aún no hay datos publicados. Revisa que el secreto LIQUIPEDIA_API_KEY esté configurado.")
                else -> throw FeedException("El servidor respondió $code")
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val feed = parse(text)
            File(context.filesDir, CACHE).writeText(text)
            feed
        } finally {
            conn.disconnect()
        }
    }

    fun cached(context: Context): Feed? = runCatching {
        File(context.filesDir, CACHE).takeIf { it.exists() }?.readText()?.let(::parse)
    }.getOrNull()

    fun parse(text: String): Feed {
        val root = JSONObject(text)
        val tournaments = root.optJSONArray("tournaments").objects().map {
            Tournament(
                id = it.getString("id"), name = it.optString("name"), region = it.optString("region"),
                tier = it.optString("tier"), start = it.optLong("start"), end = it.optLong("end"),
                prize = it.optString("prize"), teams = it.optInt("teams"), venue = it.optString("venue"),
                stage = it.optString("stage"), url = it.optString("url").ifEmpty { null },
            )
        }
        val teams = root.optJSONArray("teams").objects().map {
            Team(
                id = it.getString("id"), name = it.optString("name"), tag = it.optString("tag"),
                region = it.optString("region"), players = it.optJSONArray("players").strings(),
            )
        }
        val series = root.optJSONArray("series").objects().mapNotNull { s ->
            val status = runCatching { SeriesStatus.valueOf(s.getString("status")) }.getOrNull() ?: return@mapNotNull null
            val games = s.optJSONArray("games").objects().map { g ->
                Game(
                    scoreA = g.optInt("scoreA"), scoreB = g.optInt("scoreB"),
                    overtime = g.optBoolean("overtime"), done = g.optBoolean("done"),
                    clock = 0, clockKnown = false,
                )
            }
            Series(
                id = s.getString("id"), tournamentId = s.getString("tournamentId"), stage = s.optString("stage"),
                bestOf = s.optInt("bestOf", 5), teamA = s.getString("teamA"), teamB = s.getString("teamB"),
                start = s.optLong("start"), status = status, games = games,
                url = s.optString("url").ifEmpty { null },
                seriesScore = s.optInt("winsA") to s.optInt("winsB"),
            )
        }
        return Feed(root.optLong("generatedAt"), root.optString("attribution", "Datos de Liquipedia, CC-BY-SA 3.0"), tournaments, teams, series)
    }

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else List(length()) { getJSONObject(it) }
    private fun JSONArray?.strings(): List<String> = if (this == null) emptyList() else List(length()) { getString(it) }
}

class FeedException(message: String) : Exception(message)
