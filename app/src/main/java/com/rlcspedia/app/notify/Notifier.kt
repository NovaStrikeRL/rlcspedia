package com.rlcspedia.app.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rlcspedia.app.MainActivity
import com.rlcspedia.app.R
import com.rlcspedia.app.data.LiveEvent
import com.rlcspedia.app.data.LiveRepository
import com.rlcspedia.app.data.Series
import com.rlcspedia.app.data.SettingsStore
import com.rlcspedia.app.data.formatClock

/**
 * Notificaciones del sistema. Wear OS (y relojes con la app del fabricante)
 * reflejan automáticamente estas notificaciones del teléfono.
 */
class Notifier(private val context: Context, private val settings: SettingsStore) {

    fun createChannels() {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_GOALS, "Goles", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Jugador, equipo y marcador al momento de cada gol"
                    enableVibration(true); vibrationPattern = longArrayOf(0, 80, 40, 80)
                },
                NotificationChannel(CH_SERIES, "Inicio de series", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Aviso cuando empieza una serie"
                },
                NotificationChannel(CH_RESULTS, "Resultados", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Fin de cada juego y de cada serie"
                },
            )
        )
    }

    fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun onEvent(e: LiveEvent) {
        val prefs = settings.value.notify
        val s = e.series
        if (prefs.favoritesOnly && s.teams.none(settings::isFavorite)) return
        val a = team(s.teamA); val b = team(s.teamB)
        when (e) {
            is LiveEvent.SeriesStarted -> if (prefs.seriesStart) post(
                s, CH_SERIES, "Serie en vivo: ${a.tag} vs ${b.tag}",
                "${a.name} vs ${b.name} · Bo${s.bestOf} · ${tournamentName(s)}",
            )
            is LiveEvent.GoalScored -> if (prefs.goals) {
                val scorer = team(s.teams[e.goal.side])
                post(
                    s, CH_GOALS, "Gol de ${e.goal.player} (${scorer.tag})",
                    "${a.tag} ${e.goal.scoreA} – ${e.goal.scoreB} ${b.tag} · Juego ${e.gameNumber}" +
                        (if (e.overtime) " · Tiempo extra" else "") + " · ${formatClock(e.goal.clock)}",
                )
            }
            is LiveEvent.GameEnded -> if (prefs.gameEnd) {
                val (wa, wb) = s.wins
                val w = team(s.teams[e.game.winner])
                post(
                    s, CH_RESULTS, "Juego ${e.gameNumber}: gana ${w.tag}",
                    "${e.game.scoreA}–${e.game.scoreB}" + (if (e.game.overtime) " (TE)" else "") +
                        " · Serie ${a.tag} $wa–$wb ${b.tag}",
                )
            }
            is LiveEvent.SeriesEnded -> if (prefs.seriesEnd) {
                val (wa, wb) = s.wins
                val w = team(s.teams[s.seriesWinner])
                post(s, CH_RESULTS, "${w.name} gana la serie", "${a.tag} $wa–$wb ${b.tag} · ${tournamentName(s)} · ${s.stage}")
            }
        }
    }

    fun postTest() = post(
        null, CH_GOALS, "Gol de Kestrel (AUR)",
        "AUR 2 – 1 FLP · Juego 3 · 1:12",
    )

    private fun post(s: Series?, channel: String, title: String, text: String) {
        if (!canNotify()) return
        val intent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_ball)
            .setColor(ContextCompat.getColor(context, R.color.brand_blue))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(if (channel == CH_GOALS) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setLocalOnly(false) // permite el puente al reloj
            .build()
        try {
            // Mismo tag por serie: cada aviso reemplaza al anterior de esa serie.
            NotificationManagerCompat.from(context).notify(s?.id ?: "test", channel.hashCode(), n)
        } catch (_: SecurityException) { }
    }

    private fun team(id: String) = LiveRepository.teamById.getValue(id)
    private fun tournamentName(s: Series) = LiveRepository.tournament(s.tournamentId)?.let { "${it.name} ${it.region}" } ?: ""

    companion object {
        const val CH_GOALS = "goals"
        const val CH_SERIES = "series"
        const val CH_RESULTS = "results"
    }
}
