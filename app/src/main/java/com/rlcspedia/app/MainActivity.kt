@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.rlcspedia.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.material3.TopAppBar
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
import com.rlcspedia.app.data.LiveEvent
import com.rlcspedia.app.data.LiveRepository
import com.rlcspedia.app.data.SeriesStatus
import com.rlcspedia.app.ui.screens.LiveScreen
import com.rlcspedia.app.ui.screens.SettingsScreen
import com.rlcspedia.app.ui.screens.TeamsScreen
import com.rlcspedia.app.ui.screens.TournamentScreen
import com.rlcspedia.app.ui.screens.TournamentsScreen
import com.rlcspedia.app.ui.theme.RLCSpediaTheme

enum class Tab(val label: String, val icon: Int) {
    LIVE("En vivo", R.drawable.ic_live),
    TOURNAMENTS("Torneos", R.drawable.ic_trophy),
    FAVORITES("Favoritos", R.drawable.ic_star),
    SETTINGS("Ajustes", R.drawable.ic_tune),
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
    val scroll = TopAppBarDefaults.pinnedScrollBehavior()
    val liveCount = series.count { it.status == SeriesStatus.LIVE }

    BackHandler(enabled = openTournament != null) { openTournament = null }

    // Avisos dentro de la app (independientes del permiso de notificaciones)
    LaunchedEffect(Unit) {
        LiveRepository.events.collect { e ->
            val s = e.series
            val t = { id: String -> LiveRepository.teamById.getValue(id) }
            val fav = s.teams.any(store::isFavorite)
            when (e) {
                is LiveEvent.SeriesStarted -> snackbar.showSnackbar("Empieza ${t(s.teamA).name} vs ${t(s.teamB).name} · Bo${s.bestOf}")
                is LiveEvent.GoalScored -> if (fav) snackbar.showSnackbar("Gol de ${e.goal.player} (${t(s.teams[e.goal.side]).tag}) · ${e.goal.scoreA}–${e.goal.scoreB}")
                is LiveEvent.SeriesEnded -> snackbar.showSnackbar("${t(s.teams[s.seriesWinner]).name} gana la serie ${s.wins.first}–${s.wins.second}")
                else -> Unit
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scroll.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painterResource(R.drawable.ic_logo), contentDescription = null,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("RLCSpedia", style = MaterialTheme.typography.headlineSmall)
                    }
                },
                scrollBehavior = scroll,
            )
        },
        bottomBar = {
            ShortNavigationBar {
                Tab.entries.forEach { t ->
                    ShortNavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t; if (t != Tab.TOURNAMENTS) openTournament = null },
                        icon = {
                            if (t == Tab.LIVE && liveCount > 0) {
                                BadgedBox(badge = { Badge { Text("$liveCount") } }) { Icon(painterResource(t.icon), null) }
                            } else Icon(painterResource(t.icon), null)
                        },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        when (tab) {
            Tab.LIVE -> LiveScreen(series, settings, padding)
            Tab.TOURNAMENTS -> openTournament?.let { id ->
                TournamentScreen(id, series, settings, padding, onBack = { openTournament = null })
            } ?: TournamentsScreen(padding, onOpen = { openTournament = it })
            Tab.FAVORITES -> TeamsScreen(settings, padding, onToggle = store::toggleFavorite)
            Tab.SETTINGS -> SettingsScreen(settings, padding, onChange = store::update)
        }
    }
}
