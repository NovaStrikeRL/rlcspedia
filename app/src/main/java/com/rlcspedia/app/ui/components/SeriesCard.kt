@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.rlcspedia.app.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rlcspedia.app.R
import com.rlcspedia.app.data.LiveRepository
import com.rlcspedia.app.data.Series
import com.rlcspedia.app.data.SeriesStatus
import com.rlcspedia.app.data.GAME_LENGTH
import com.rlcspedia.app.data.formatClock
import com.rlcspedia.app.ui.theme.LocalReduceMotion
import com.rlcspedia.app.ui.theme.LocalTeamColors
import java.text.SimpleDateFormat
import java.util.Locale

private val es = Locale.forLanguageTag("es-MX")

fun countdown(target: Long): String {
    val ms = target - System.currentTimeMillis()
    if (ms <= 0) return "Ahora"
    val m = ms / 60_000; val h = m / 60; val d = h / 24
    return when {
        d >= 1 -> "En $d d"
        h >= 1 -> "En $h h ${m % 60} min"
        else -> "En $m min ${(ms / 1000) % 60} s"
    }
}

fun startTime(t: Long): String = SimpleDateFormat("EEE HH:mm", es).format(t)

@Composable
fun SeriesCard(s: Series, favorites: Set<String>, modifier: Modifier = Modifier) {
    val fav = s.teams.any { it in favorites }
    val colors = MaterialTheme.colorScheme
    val border = if (fav) BorderStroke(2.dp, colors.primary) else null
    val content: @Composable () -> Unit = { SeriesContent(s, favorites) }
    when (s.status) {
        SeriesStatus.LIVE -> ElevatedCard(
            modifier.fillMaxWidth().then(
                if (border != null) Modifier.border(border, MaterialTheme.shapes.extraLarge) else Modifier
            ),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceContainerLow),
        ) { content() }
        SeriesStatus.DONE -> Card(
            modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
            border = border,
        ) { content() }
        SeriesStatus.UPCOMING -> OutlinedCard(
            modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
            border = border ?: CardDefaults.outlinedCardBorder(),
        ) { content() }
    }
}

@Composable
private fun SeriesContent(s: Series, favorites: Set<String>) {
    var expanded by rememberSaveable(s.id) { mutableStateOf(false) }
        val t = LiveRepository.tournament(s.tournamentId)
    val (wa, wb) = s.wins
    val g = s.currentGame
    val live = s.status == SeriesStatus.LIVE
    val done = s.status == SeriesStatus.DONE
    val tc = LocalTeamColors.current

    Column(Modifier.padding(bottom = 4.dp)) {
        // Encabezado
        Row(
            Modifier.padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (live) LiveChip()
            Text(
                "${t?.name ?: ""} ${t?.region ?: ""} · ${s.stage}",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
            )
            Surface(shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), color = Color.Transparent) {
                Text("Bo${s.bestOf}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
            }
        }

        // Filas de equipo
        listOf(0, 1).forEach { side ->
            val id = s.teams[side]
            val wins = if (side == 0) wa else wb
            val lost = done && s.seriesWinner != side
            val accent = if (side == 0) tc.blue else tc.orange
            val score = when {
                live && g != null -> "${if (side == 0) g.scoreA else g.scoreB}"
                live -> "–"
                done -> "$wins"
                else -> ""
            }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${LiveRepository.team(id).name}. $wins de ${s.needed} juegos ganados." +
                            if (live) " Goles en este juego: $score." else ""
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.width(6.dp).height(36.dp).clip(RoundedCornerShape(3.dp)).background(accent))
                Spacer(Modifier.width(12.dp))
                if (id in favorites) {
                    Icon(painterResource(R.drawable.ic_star), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    LiveRepository.team(id).name,
                    style = MaterialTheme.typography.titleMedium.let { if (lost) it.copy(fontWeight = FontWeight.Normal) else it },
                    color = if (lost) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Pips(s.needed, wins, accent)
                Spacer(Modifier.width(12.dp))
                Score(score, if (lost) MaterialTheme.colorScheme.onSurfaceVariant else accent)
            }
        }

        // Último gol (anunciado por TalkBack)
        if (live) {
            val gi = s.games.indexOfLast { it.goals.isNotEmpty() }
            if (gi >= 0) {
                val last = s.games[gi].goals.last()
                val suffix = if (gi != s.games.lastIndex || s.pauseTicks > 0) " en el juego ${gi + 1}" else ""
                Text(
                    "Último gol: ${last.player} (${LiveRepository.team(s.teams[last.side]).tag}) · ${last.scoreA}–${last.scoreB}$suffix",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }

        // Reloj del juego como progreso ondulado (solo cuando la fuente lo publica)
        if (live && g != null && g.clockKnown && !g.overtime) {
            val progress = 1f - g.clock / GAME_LENGTH.toFloat()
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    .semantics { contentDescription = "Tiempo restante ${formatClock(g.clock)}" },
            )
        }

        // Pie
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            val status = when (s.status) {
                SeriesStatus.LIVE -> when {
                    s.pauseTicks > 0 -> "Serie $wa–$wb · siguiente juego"
                    g == null -> "Juego ${s.games.size + 1} · Serie $wa–$wb"
                    !g.clockKnown -> "Juego ${s.games.size}${if (g.overtime) " · Tiempo extra" else ""} · Serie $wa–$wb"
                    else -> "Juego ${s.games.size} · ${if (g.overtime) "Tiempo extra " else ""}${formatClock(g.clock)} · Serie $wa–$wb"
                }
                SeriesStatus.DONE -> "Final · gana ${LiveRepository.team(s.teams[s.seriesWinner]).name}"
                SeriesStatus.UPCOMING -> "${startTime(s.start)} · ${countdown(s.start)}"
            }
            Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            s.url?.let { url ->
                val ctx = LocalContext.current
                TextButton(onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }) { Text("Liquipedia") }
            }
            if (s.games.isNotEmpty()) TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Ocultar juegos" else "Ver juegos")
                Icon(painterResource(if (expanded) R.drawable.ic_expand_less else R.drawable.ic_expand_more), null)
            }
        }

        AnimatedVisibility(expanded) { GamesDetail(s) }
    }
}

@Composable
private fun LiveChip() {
    val tc = LocalTeamColors.current
    Surface(color = tc.live, contentColor = tc.onLive, shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.padding(start = 2.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (LocalReduceMotion.current) Box(Modifier.padding(6.dp).size(8.dp).clip(RoundedCornerShape(4.dp)).background(tc.onLive))
            else LoadingIndicator(Modifier.size(20.dp), color = tc.onLive)
            Text("En vivo", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Pips(total: Int, on: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.clearAndSetSemantics { }) {
        repeat(total) { i ->
            val filled = i < on
            Box(
                Modifier.size(14.dp).clip(RoundedCornerShape(4.dp))
                    .background(if (filled) color else MaterialTheme.colorScheme.outlineVariant)
            )
        }
    }
}

@Composable
private fun Score(score: String, color: Color) {
    val reduce = LocalReduceMotion.current
    Box(Modifier.widthIn(min = 44.dp).clearAndSetSemantics { }, contentAlignment = Alignment.CenterEnd) {
        if (reduce) ScoreText(score, color)
        else AnimatedContent(
            targetState = score,
            transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
            label = "score",
        ) { ScoreText(it, color) }
    }
}

@Composable
private fun ScoreText(score: String, color: Color) =
    Text(score, style = MaterialTheme.typography.displayMedium, color = color, textAlign = TextAlign.End)

@Composable
private fun GamesDetail(s: Series) {
    val tc = LocalTeamColors.current
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        s.games.forEachIndexed { i, g ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth()) {
                Text(
                    "Juego ${i + 1}${if (g.overtime) " (TE)" else ""}${if (!g.done) " · en curso" else ""}",
                    style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f),
                )
                Text("${g.scoreA} – ${g.scoreB}", style = MaterialTheme.typography.labelLarge)
            }
            if (g.goals.isEmpty() && g.clockKnown) Text("Sin goles todavía", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            g.goals.forEach { ev ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(if (ev.side == 0) tc.blue else tc.orange))
                    Spacer(Modifier.width(8.dp))
                    Text(formatClock(ev.clock), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(56.dp))
                    Text(
                        "${ev.player} · ${LiveRepository.team(s.teams[ev.side]).tag} · ${ev.scoreA}–${ev.scoreB}",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
