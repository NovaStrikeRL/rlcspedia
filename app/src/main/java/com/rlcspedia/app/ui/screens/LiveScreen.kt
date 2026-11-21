@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.rlcspedia.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.rlcspedia.app.R
import com.rlcspedia.app.RLCSpediaApp
import com.rlcspedia.app.data.AppSettings
import com.rlcspedia.app.data.DataSource
import com.rlcspedia.app.data.FeedState
import com.rlcspedia.app.data.LiveRepository
import com.rlcspedia.app.data.Series
import com.rlcspedia.app.data.SeriesStatus
import com.rlcspedia.app.ui.components.SeriesCard
import kotlinx.coroutines.launch

/** Orden: favoritos primero, luego por hora de inicio. */
fun List<Series>.prioritized(favs: Set<String>) =
    sortedWith(compareByDescending<Series> { s -> s.teams.any { it in favs } }.thenBy { it.start })

@Composable
fun LiveScreen(series: List<Series>, settings: AppSettings, feed: FeedState, padding: PaddingValues) {
    val favs = settings.favorites
    val now = System.currentTimeMillis()
    val live = series.filter { it.status == SeriesStatus.LIVE }.prioritized(favs)
    val soonWindow = if (feed == FeedState.Demo) 6 * 3_600_000L else 48 * 3_600_000L
    val soon = series.filter { it.status == SeriesStatus.UPCOMING && it.start - now < soonWindow }.prioritized(favs)
    val done = series.filter { it.status == SeriesStatus.DONE }.sortedByDescending { it.start }.take(12).prioritized(favs)

    RefreshableList(padding, feed) {
        item { FeedBanner(feed) }
        if (feed == FeedState.Loading && series.isEmpty()) {
            item { Box(Modifier.padding(48.dp)) { ContainedLoadingIndicator(Modifier.size(64.dp)) } }
            return@RefreshableList
        }
        section("En vivo", live, favs, empty = "No hay series en vivo ahora mismo.")
        if (soon.isNotEmpty()) section("Próximas series", soon, favs)
        if (done.isNotEmpty()) section("Terminadas", done, favs)
        item { Attribution(feed) }
    }
}

/** Lista con "desliza para actualizar" e indicador expresivo. Solo actualiza con Liquipedia. */
@Composable
fun RefreshableList(padding: PaddingValues, feed: FeedState, content: LazyListScope.() -> Unit) {
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            if (feed == FeedState.Demo) return@PullToRefreshBox
            scope.launch { refreshing = true; LiveRepository.refresh(); refreshing = false }
        },
        state = state,
        modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(state = state, isRefreshing = refreshing, modifier = Modifier.align(Alignment.TopCenter))
        },
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = padding.calculateBottomPadding() + 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
private fun FeedBanner(feed: FeedState) {
    val store = RLCSpediaApp.instance.settings
    val (text, action) = when (feed) {
        FeedState.Demo -> "Modo demostración: equipos ficticios y partidos simulados." to
            ("Usar datos reales" to { store.update { it.copy(dataSource = DataSource.LIQUIPEDIA) } })
        is FeedState.Error -> "No se pudieron cargar los datos: ${feed.message}" to
            ("Usar demostración" to { store.update { it.copy(dataSource = DataSource.DEMO) } })
        else -> return
    }
    Surface(
        color = if (feed is FeedState.Error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.large, modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_bell), null)
                Text(text, style = MaterialTheme.typography.bodyMedium)
            }
            FilledTonalButton(onClick = action.second, modifier = Modifier.align(Alignment.End)) { Text(action.first) }
        }
    }
}

/** Atribución requerida por la licencia CC-BY-SA de Liquipedia. */
@Composable
fun Attribution(feed: FeedState) {
    if (feed == FeedState.Demo) return
    val ctx = LocalContext.current
    Column(Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Datos de Liquipedia, licencia CC-BY-SA 3.0. El marcador puede llevar minutos de retraso.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
        )
        TextButton(onClick = {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://liquipedia.net/rocketleague/Main_Page")))
        }) { Text("Abrir Liquipedia") }
    }
}

fun LazyListScope.section(title: String, list: List<Series>, favs: Set<String>, empty: String? = null) {
    item(key = "h-$title") { SectionTitle(title) }
    if (list.isEmpty() && empty != null) item(key = "e-$title") {
        Text(empty, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
    }
    items(list, key = { "$title-${it.id}" }) { SeriesCard(it, favs, Modifier.widthIn(max = 720.dp).animateItem()) }
}

@Composable
fun SectionTitle(text: String) = Text(
    text, style = MaterialTheme.typography.titleLarge,
    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(top = 12.dp, start = 4.dp).semantics { heading() },
)
