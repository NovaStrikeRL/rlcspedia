@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.rlcspedia.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rlcspedia.app.R
import com.rlcspedia.app.data.AppSettings
import com.rlcspedia.app.data.LiveRepository

@Composable
fun TeamsScreen(settings: AppSettings, padding: PaddingValues, onToggle: (String) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val favs = settings.favorites
    val q = query.trim().lowercase()
    val teams by LiveRepository.teams.collectAsStateWithLifecycle()
    val list = teams
        .filter { q.isEmpty() || (it.name + it.tag + it.region + it.players.joinToString()).lowercase().contains(q) }
        .sortedWith(compareByDescending<com.rlcspedia.app.data.Team> { it.id in favs }.thenBy { it.name })
    // Formas expresivas para avatares: la forma distingue favoritos sin depender del color
    val favShape = MaterialShapes.Cookie9Sided.toShape()
    val shape = CircleShape

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
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), null) },
                label = { Text("Buscar equipo, región o jugador") },
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().padding(bottom = 12.dp),
            )
        }
        if (list.isEmpty()) item {
            Text(
                if (teams.isEmpty()) "Los equipos aparecen cuando se publican las series de la RLCS." else "Ningún equipo coincide con la búsqueda.",
                modifier = Modifier.padding(24.dp),
            )
        }
        items(list, key = { it.id }) { t ->
            val fav = t.id in favs
            ListItem(
                headlineContent = { Text(t.name) },
                supportingContent = {
                    val info = listOf(t.region, t.players.joinToString()).filter { it.isNotBlank() }.joinToString(" · ")
                    if (info.isNotEmpty()) Text(info)
                },
                leadingContent = {
                    Surface(
                        shape = if (fav) favShape else shape,
                        color = if (fav) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(44.dp),
                    ) { Box(contentAlignment = Alignment.Center) { Text(t.tag, style = MaterialTheme.typography.labelMedium) } }
                },
                trailingContent = {
                    IconToggleButton(
                        checked = fav, onCheckedChange = { onToggle(t.id) },
                        modifier = Modifier.semantics { stateDescription = if (fav) "Favorito" else "No favorito" },
                    ) {
                        Icon(
                            painterResource(if (fav) R.drawable.ic_star else R.drawable.ic_star_outline),
                            contentDescription = "Favorito: ${t.name}",
                            tint = if (fav) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().animateItem()
                    .padding(vertical = 1.dp),
            )
        }
    }
}
