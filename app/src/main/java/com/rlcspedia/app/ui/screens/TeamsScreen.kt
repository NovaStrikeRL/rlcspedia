@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.rlcspedia.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rlcspedia.app.data.AppSettings
import com.rlcspedia.app.data.LiveRepository
import com.rlcspedia.app.data.Team
import com.rlcspedia.app.ui.components.Symbols
import com.rlcspedia.app.ui.components.animatedMorphShape
import com.rlcspedia.app.ui.components.segmentedShape
import com.rlcspedia.app.ui.theme.LocalReduceMotion

@Composable
fun TeamsScreen(settings: AppSettings, padding: PaddingValues, onToggle: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val favs = settings.favorites
    val teams by LiveRepository.teams.collectAsStateWithLifecycle()
    val q = query.trim().lowercase()
    val list = teams
        .filter { q.isEmpty() || (it.name + it.tag + it.region + it.players.joinToString()).lowercase().contains(q) }
        .sortedWith(compareByDescending<Team> { it.id in favs }.thenBy { it.name })

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, padding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { SectionTitle("Equipos favoritos") }
        item {
            Text(
                "Sus series aparecen primero y puedes recibir avisos solo de ellos.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(start = 4.dp, bottom = 8.dp),
            )
        }
        item {
            OutlinedTextField(
                value = query, onValueChange = { query = it }, singleLine = true,
                leadingIcon = { Icon(painterResource(Symbols.search), null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }, shapes = IconButtonDefaults.shapes()) {
                        Icon(painterResource(Symbols.close), "Borrar búsqueda")
                    }
                },
                label = { Text("Buscar equipo, región o jugador") },
                shape = MaterialShapes.Pill.toShape(),
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(bottom = 12.dp),
            )
        }
        if (list.isEmpty()) item {
            Text(
                if (teams.isEmpty()) "Los equipos aparecen cuando se publican las series de la RLCS." else "Ningún equipo coincide con la búsqueda.",
                modifier = Modifier.padding(24.dp),
            )
        }
        // Lista segmentada de M3 Expressive: esquinas grandes en los extremos del grupo
        itemsIndexed(list, key = { _, t -> t.id }) { i, t ->
            TeamRow(t, t.id in favs, i, list.size, onToggle, Modifier.widthIn(max = 720.dp).fillMaxWidth().animateItem())
        }
    }
}

@Composable
private fun TeamRow(t: Team, fav: Boolean, index: Int, count: Int, onToggle: (String) -> Unit, modifier: Modifier) {
    val reduce = LocalReduceMotion.current
    // Al marcar favorito, el avatar pasa de círculo a "galleta" de 9 lados y gira un poco
    val avatarShape = animatedMorphShape(MaterialShapes.Circle, MaterialShapes.Cookie9Sided, toTarget = fav)
    val spin by animateFloatAsState(if (fav && !reduce) 40f else 0f, MaterialTheme.motionScheme.slowSpatialSpec(), label = "spin")
    ListItem(
        headlineContent = { Text(t.name, style = MaterialTheme.typography.titleMediumEmphasized) },
        supportingContent = {
            val info = listOf(t.region, t.players.joinToString()).filter { it.isNotBlank() }.joinToString(" · ")
            if (info.isNotEmpty()) Text(info)
        },
        leadingContent = {
            Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                Surface(
                    color = if (fav) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.size(48.dp).graphicsLayer { rotationZ = spin }.clip(avatarShape),
                ) {}
                Text(
                    t.tag, style = MaterialTheme.typography.labelMediumEmphasized,
                    color = if (fav) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        },
        trailingContent = {
            IconToggleButton(
                checked = fav, onCheckedChange = { onToggle(t.id) },
                shapes = IconButtonDefaults.toggleableShapes(),
                modifier = Modifier.semantics { stateDescription = if (fav) "Favorito" else "No favorito" },
            ) {
                Icon(painterResource(if (fav) Symbols.star.second else Symbols.star.first), contentDescription = "Favorito: ${t.name}")
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = modifier.clip(segmentedShape(index, count)),
    )
}
