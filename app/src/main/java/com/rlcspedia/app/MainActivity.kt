@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.rlcspedia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.toShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import com.rlcspedia.app.ui.components.Symbols
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.MediumFlexibleTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rlcspedia.app.data.FeedState
import com.rlcspedia.app.data.LiveEvent
import com.rlcspedia.app.data.LiveRepository
import com.rlcspedia.app.data.SeriesStatus
import com.rlcspedia.app.ui.screens.LiveScreen
import com.rlcspedia.app.ui.screens.SettingsScreen
import com.rlcspedia.app.ui.screens.TeamsScreen
import com.rlcspedia.app.ui.screens.TournamentScreen
import com.rlcspedia.app.ui.screens.TournamentsScreen
import com.rlcspedia.app.ui.theme.RLCSpediaTheme

/** Ícono Material Symbols: delineado en reposo, relleno cuando la pestaña está activa. */
enum class Tab(val label: String, val icons: Pair<Int, Int>) {
    LIVE("En vivo", Symbols.live),
    TOURNAMENTS("Torneos", Symbols.trophy),
    FAVORITES("Favoritos", Symbols.star),
    SETTINGS("Ajustes", Symbols.settings),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = RLCSpediaApp.instance.settings
        setContent {
            val settings by store.state.collectAsStateWithLifecycle()
            RLCSpediaTheme(settings) { App() }
        }
    }
}

@Composable
private fun App() {
    val store = RLCSpediaApp.instance.settings
    val settings by store.state.collectAsStateWithLifecycle()
    val series by LiveRepository.series.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.LIVE) }
    var openTournament by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val feed by LiveRepository.feedState.collectAsStateWithLifecycle()
    val liveCount = series.count { it.status == SeriesStatus.LIVE }
    val reduceMotion = settings.reduceMotion

    BackHandler(enabled = openTournament != null) { openTournament = null }

    // Avisos dentro de la app (independientes del permiso de notificaciones)
    LaunchedEffect(Unit) {
        LiveRepository.events.collect { e ->
            val s = e.series
            val t = { id: String -> LiveRepository.team(id) }
            val fav = s.teams.any(store::isFavorite)
            when (e) {
                is LiveEvent.SeriesStarted -> snackbar.showSnackbar("Empieza ${t(s.teamA).name} vs ${t(s.teamB).name} · Bo${s.bestOf}")
                is LiveEvent.GoalScored -> if (fav) snackbar.showSnackbar("Gol de ${e.goal.player} (${t(s.teams[e.goal.side]).tag}) · ${e.goal.scoreA}–${e.goal.scoreB}")
                is LiveEvent.ScoreUpdate -> if (fav) snackbar.showSnackbar("Gol de ${t(s.teams[e.side]).name} · ${e.scoreA}–${e.scoreB}")
                is LiveEvent.SeriesEnded -> snackbar.showSnackbar("${t(s.teams[s.seriesWinner]).name} gana la serie ${s.wins.first}–${s.wins.second}")
                else -> Unit
            }
        }
    }

    val wide = LocalConfiguration.current.screenWidthDp >= 600
    val select: (Tab) -> Unit = { t -> tab = t; if (t != Tab.TOURNAMENTS) openTournament = null }

    @Composable
    fun TabIcon(t: Tab) {
        val icon = @Composable { Icon(painterResource(if (tab == t) t.icons.second else t.icons.first), null) }
        if (t == Tab.LIVE && liveCount > 0) BadgedBox(badge = { Badge { Text("$liveCount") } }) { icon() } else icon()
    }

    Row(Modifier.fillMaxSize()) {
        // Pantallas anchas (tabletas, plegables abiertos): riel de navegación ancho
        if (wide) WideNavigationRail(modifier = Modifier.fillMaxHeight()) {
            Tab.entries.forEach { t ->
                WideNavigationRailItem(
                    selected = tab == t, onClick = { select(t) }, railExpanded = false,
                    icon = { TabIcon(t) }, label = { Text(t.label) },
                )
            }
        }
        Scaffold(
            modifier = Modifier.weight(1f).nestedScroll(scroll.nestedScrollConnection),
            topBar = {
                MediumFlexibleTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painterResource(R.drawable.ic_logo), contentDescription = null,
                                modifier = Modifier.size(36.dp).clip(MaterialShapes.Cookie9Sided.toShape()),
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("RLCSpedia")
                        }
                    },
                    subtitle = { Text(feedSubtitle(feed)) },
                    scrollBehavior = scroll,
                )
            },
            bottomBar = {
                if (!wide) ShortNavigationBar {
                    Tab.entries.forEach { t ->
                        ShortNavigationBarItem(
                            selected = tab == t, onClick = { select(t) },
                            icon = { TabIcon(t) }, label = { Text(t.label) },
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbar) },
        ) { padding ->
            // Transición expresiva entre pestañas (resorte del esquema de movimiento)
            val spatial = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
            val effects = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
            AnimatedContent(
                targetState = tab to openTournament,
                transitionSpec = {
                    if (reduceMotion) fadeIn(snap()) togetherWith fadeOut(snap())
                    else {
                        val dir = if (targetState.first.ordinal >= initialState.first.ordinal) 1 else -1
                        (slideInHorizontally(spatial) { it / 6 * dir } + fadeIn(effects)) togetherWith
                            (slideOutHorizontally(spatial) { -it / 6 * dir } + fadeOut(effects))
                    }
                },
                label = "tabs",
            ) { (current, opened) ->
                when (current) {
                    Tab.LIVE -> LiveScreen(series, settings, feed, padding)
                    Tab.TOURNAMENTS -> opened?.let { id ->
                        TournamentScreen(id, series, settings, padding, onBack = { openTournament = null })
                    } ?: TournamentsScreen(padding, onOpen = { openTournament = it })
                    Tab.FAVORITES -> TeamsScreen(settings, padding, onToggle = store::toggleFavorite)
                    Tab.SETTINGS -> SettingsScreen(settings, padding, onChange = store::update)
                }
            }
        }
    }
}

fun minutesAgo(t: Long): String {
    val m = ((System.currentTimeMillis() - t) / 60_000).coerceAtLeast(0)
    return when {
        m < 1 -> "hace un momento"
        m < 60 -> "hace $m min"
        else -> "hace ${m / 60} h"
    }
}

fun feedSubtitle(f: FeedState): String = when (f) {
    FeedState.Demo -> "Modo demostración"
    FeedState.Loading -> "Cargando datos de Liquipedia…"
    is FeedState.Ok -> "Liquipedia · actualizado ${minutesAgo(f.generatedAt)}"
    is FeedState.Error -> "Sin conexión con los datos" + (f.lastOk?.let { " · último ${minutesAgo(it)}" } ?: "")
}
