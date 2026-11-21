@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.rlcspedia.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.remember
import com.rlcspedia.app.ui.components.MorphShape
import com.rlcspedia.app.ui.components.Symbols
import com.rlcspedia.app.ui.components.pressShape
import com.rlcspedia.app.ui.components.rememberMorph
import com.rlcspedia.app.ui.theme.LocalReduceMotion
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rlcspedia.app.data.AppSettings
import com.rlcspedia.app.data.FeedState
import com.rlcspedia.app.data.LiveRepository
import com.rlcspedia.app.data.Series
import com.rlcspedia.app.data.SeriesStatus
import com.rlcspedia.app.data.Tournament
import com.rlcspedia.app.ui.components.SeriesCard
import com.rlcspedia.app.ui.components.countdown
import java.text.SimpleDateFormat
import java.util.Locale

private val es = Locale.forLanguageTag("es-MX")
private fun day(t: Long) = SimpleDateFormat("d", es).format(t)
private fun month(t: Long) = SimpleDateFormat("MMM", es).format(t).trimEnd('.')
private fun range(a: Long, b: Long) = SimpleDateFormat("d MMM", es).let { "${it.format(a)} – ${it.format(b)}" }

@Composable
fun TournamentsScreen(padding: PaddingValues, onOpen: (String) -> Unit) {
    val all by LiveRepository.tournaments.collectAsStateWithLifecycle()
    val feed by LiveRepository.feedState.collectAsStateWithLifecycle()
    val now = System.currentTimeMillis()
    val list = all.filter { it.end > now }.sortedBy { it.start }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, padding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { SectionTitle("Torneos") }
        if (list.isEmpty()) item {
            Text(
                if (feed == FeedState.Loading) "Cargando torneos…" else "No hay torneos de la RLCS en las próximas semanas.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp),
            )
        }
        items(list, key = { it.id }) { TournamentCard(it, now) { onOpen(it.id) } }
        item { Attribution(feed) }
    }
}

@Composable
private fun TournamentCard(t: Tournament, now: Long, onClick: () -> Unit) {
    val ongoing = t.start <= now
    val body: @Composable () -> Unit = {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Bloque de fecha con forma expresiva (cookie para en curso, cuadrado suave para próximos)
            // En curso: "Sunny" que gira lento; próximos: "Cookie de 4 lados" estática
            val spin = if (ongoing && !LocalReduceMotion.current) {
                val tr = rememberInfiniteTransition(label = "spin")
                tr.animateFloat(0f, 360f, infiniteRepeatable(tween(24_000, easing = LinearEasing)), label = "deg").value
            } else 0f
            Surface(
                shape = MorphShape(rememberMorph(MaterialShapes.Cookie4Sided, MaterialShapes.Sunny), if (ongoing) 1f else 0f, spin),
                color = if (ongoing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = if (ongoing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(64.dp),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(day(t.start), style = MaterialTheme.typography.headlineSmallEmphasized, textAlign = TextAlign.Center)
                    Text(month(t.start), style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(if (t.region.isBlank()) t.name else "${t.name} · ${t.region}", style = MaterialTheme.typography.titleMediumEmphasized)
                Text(
                    listOf(t.tier, if (t.teams > 0) "${t.teams} equipos" else "", t.prize, t.venue).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(if (ongoing) "En curso" else countdown(t.start), style = MaterialTheme.typography.labelLarge)
        }
    }
    val mod = Modifier.widthIn(max = 720.dp).fillMaxWidth()
    val interaction = remember { MutableInteractionSource() }
    val shape = pressShape(interaction, rest = if (ongoing) 32.dp else 24.dp, pressed = 14.dp)
    if (ongoing) Card(onClick, mod, shape = shape, interactionSource = interaction, colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    )) { body() }
    else OutlinedCard(onClick, mod, shape = shape, interactionSource = interaction) { body() }
}

@Composable
fun TournamentScreen(id: String, series: List<Series>, settings: AppSettings, padding: PaddingValues, onBack: () -> Unit) {
    val t = LiveRepository.tournament(id)
    if (t == null) { LaunchedEffect(id) { onBack() }; return }
    val order = mapOf(SeriesStatus.LIVE to 0, SeriesStatus.UPCOMING to 1, SeriesStatus.DONE to 2)
    val list = series.filter { it.tournamentId == id }.prioritized(settings.favorites).sortedBy { order.getValue(it.status) }
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 4.dp, 16.dp, padding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Row(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, shapes = IconButtonDefaults.shapes()) { Icon(painterResource(Symbols.back), "Volver a torneos") }
                Column {
                    Text("${t.name} · ${t.region}", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    Text(listOf(range(t.start, t.end), t.prize, t.stage).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (list.isEmpty()) item {
            Box(Modifier.padding(32.dp)) {
                Text("Las series se publican en Liquipedia cuando se define el bracket.", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
        }
        items(list, key = { it.id }) { SeriesCard(it, settings.favorites, Modifier.widthIn(max = 720.dp).animateItem()) }
    }
}
