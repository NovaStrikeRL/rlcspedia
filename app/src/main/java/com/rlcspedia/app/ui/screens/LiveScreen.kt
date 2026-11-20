package com.rlcspedia.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rlcspedia.app.R
import com.rlcspedia.app.data.AppSettings
import com.rlcspedia.app.data.Series
import com.rlcspedia.app.data.SeriesStatus
import com.rlcspedia.app.ui.components.SeriesCard

/** Orden: favoritos primero, luego por hora de inicio. */
fun List<Series>.prioritized(favs: Set<String>) =
    sortedWith(compareByDescending<Series> { s -> s.teams.any { it in favs } }.thenBy { it.start })

@Composable
fun LiveScreen(series: List<Series>, settings: AppSettings, padding: PaddingValues) {
    val favs = settings.favorites
    val now = System.currentTimeMillis()
    val live = series.filter { it.status == SeriesStatus.LIVE }.prioritized(favs)
    val soon = series.filter { it.status == SeriesStatus.UPCOMING && it.start - now < 6 * 3_600_000 }.prioritized(favs)
    val done = series.filter { it.status == SeriesStatus.DONE }.prioritized(favs)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp, bottom = padding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large, modifier = Modifier.widthIn(max = 720.dp)) {
                Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(painterResource(R.drawable.ic_bell), null)
                    Text("Datos de demostración: los partidos se simulan para probar marcadores, notificaciones y widget.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        section("En vivo", live, favs, empty = "No hay series en vivo ahora mismo.")
        if (soon.isNotEmpty()) section("Próximas series", soon, favs)
        if (done.isNotEmpty()) section("Terminadas", done, favs)
    }
}

fun LazyListScope.section(title: String, list: List<Series>, favs: Set<String>, empty: String? = null) {
    item(key = "h-$title") { SectionTitle(title) }
    if (list.isEmpty() && empty != null) item { Text(empty, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
    items(list, key = { it.id }) { SeriesCard(it, favs, Modifier.widthIn(max = 720.dp).animateItem()) }
}

@Composable
fun SectionTitle(text: String) = Text(
    text, style = MaterialTheme.typography.titleLarge,
    modifier = Modifier.widthIn(max = 720.dp).padding(top = 12.dp, start = 4.dp).semantics { heading() }
        .then(Modifier.padding(0.dp)),
)
