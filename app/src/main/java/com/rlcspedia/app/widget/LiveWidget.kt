package com.rlcspedia.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.rlcspedia.app.MainActivity
import com.rlcspedia.app.R
import com.rlcspedia.app.data.LiveRepository
import com.rlcspedia.app.data.Series
import com.rlcspedia.app.data.SeriesStatus
import com.rlcspedia.app.data.SettingsStore
import com.rlcspedia.app.data.formatClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Datos que muestra el widget; se guardan para sobrevivir si el proceso muere. */
data class WidgetSnapshot(
    val tournament: String = "RLCSpedia",
    val status: String = "Abre la app para ver series en vivo",
    val teamA: String = "—", val teamB: String = "—",
    val scoreA: String = "", val scoreB: String = "",
    val series: String = "",
    val live: Boolean = false,
)

private object WidgetStore {
    fun save(ctx: Context, s: WidgetSnapshot) = ctx.getSharedPreferences("widget", Context.MODE_PRIVATE).edit {
        putString("t", s.tournament); putString("st", s.status)
        putString("a", s.teamA); putString("b", s.teamB)
        putString("sa", s.scoreA); putString("sb", s.scoreB)
        putString("se", s.series); putBoolean("l", s.live)
    }
    fun load(ctx: Context): WidgetSnapshot {
        val p = ctx.getSharedPreferences("widget", Context.MODE_PRIVATE)
        val d = WidgetSnapshot()
        return WidgetSnapshot(
            p.getString("t", d.tournament)!!, p.getString("st", d.status)!!,
            p.getString("a", d.teamA)!!, p.getString("b", d.teamB)!!,
            p.getString("sa", d.scoreA)!!, p.getString("sb", d.scoreB)!!,
            p.getString("se", d.series)!!, p.getBoolean("l", false),
        )
    }
}

class LiveWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snap = WidgetStore.load(context)
        provideContent { GlanceTheme { WidgetContent(snap) } }
    }
}

class LiveWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = LiveWidget()
}

@Composable
private fun WidgetContent(s: WidgetSnapshot) {
    val c = GlanceTheme.colors
    Column(
        modifier = GlanceModifier.fillMaxSize().background(c.widgetBackground).cornerRadius(28.dp)
            .padding(16.dp).clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth()) {
            Text(s.tournament, style = TextStyle(color = c.onSurfaceVariant, fontSize = 12.sp), maxLines = 1, modifier = GlanceModifier.defaultWeight())
            if (s.live) Box(
                modifier = GlanceModifier.background(c.error).cornerRadius(8.dp).padding(horizontal = 8.dp, vertical = 2.dp),
            ) { Text("En vivo", style = TextStyle(color = c.onError, fontSize = 11.sp, fontWeight = FontWeight.Bold)) }
        }
        Spacer(GlanceModifier.height(6.dp))
        TeamLine(s.teamA, s.scoreA, ColorProvider(R.color.team_blue))
        TeamLine(s.teamB, s.scoreB, ColorProvider(R.color.team_orange))
        Spacer(GlanceModifier.height(4.dp))
        Text("${s.status}${if (s.series.isNotEmpty()) " · ${s.series}" else ""}", style = TextStyle(color = c.onSurfaceVariant, fontSize = 12.sp), maxLines = 1)
    }
}

@Composable
private fun TeamLine(name: String, score: String, accent: ColorProvider) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Box(GlanceModifier.width(5.dp).height(22.dp).background(accent).cornerRadius(3.dp)) {}
        Spacer(GlanceModifier.width(10.dp))
        Text(name, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold), maxLines = 1, modifier = GlanceModifier.defaultWeight())
        Text(score, style = TextStyle(color = accent, fontSize = 24.sp, fontWeight = FontWeight.Bold))
    }
}

/** Refresca el widget con la serie más relevante (en vivo y de favoritos primero). */
object WidgetUpdater {
    private const val INTERVAL_MS = 15_000L

    fun start(context: Context, scope: CoroutineScope, settings: SettingsStore) {
        scope.launch {
            LiveRepository.series.first()
            while (true) {
                val snap = snapshot(LiveRepository.series.value, settings)
                WidgetStore.save(context, snap)
                runCatching { LiveWidget().updateAll(context) }
                delay(INTERVAL_MS)
            }
        }
    }

    private fun snapshot(all: List<Series>, settings: SettingsStore): WidgetSnapshot {
        val fav = { s: Series -> s.teams.any(settings::isFavorite) }
        val pick = all.filter { it.status == SeriesStatus.LIVE }.sortedWith(compareByDescending(fav).thenBy { it.start }).firstOrNull()
            ?: all.filter { it.status == SeriesStatus.UPCOMING }.sortedWith(compareByDescending(fav).thenBy { it.start }).firstOrNull()
            ?: return WidgetSnapshot()
        val t = LiveRepository.tournament(pick.tournamentId)
        val (wa, wb) = pick.wins
        val g = pick.currentGame
        val live = pick.status == SeriesStatus.LIVE
        return WidgetSnapshot(
            tournament = t?.let { "${it.name} ${it.region}" } ?: "",
            status = if (live) "Juego ${pick.games.size} · ${formatClock(g?.clock ?: 0)}" else countdown(pick.start),
            teamA = LiveRepository.teamById.getValue(pick.teamA).name,
            teamB = LiveRepository.teamById.getValue(pick.teamB).name,
            scoreA = if (live) "${g?.scoreA ?: 0}" else "",
            scoreB = if (live) "${g?.scoreB ?: 0}" else "",
            series = "Serie $wa–$wb · Bo${pick.bestOf}",
            live = live,
        )
    }

    private fun countdown(start: Long): String {
        val m = ((start - System.currentTimeMillis()) / 60_000).coerceAtLeast(0)
        return if (m >= 60) "En ${m / 60} h ${m % 60} min" else "En $m min"
    }
}
