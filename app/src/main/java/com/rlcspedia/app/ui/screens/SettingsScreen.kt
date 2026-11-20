@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.rlcspedia.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.rlcspedia.app.R
import com.rlcspedia.app.RLCSpediaApp
import com.rlcspedia.app.data.AppSettings
import com.rlcspedia.app.data.ContrastLevel
import com.rlcspedia.app.data.NotifyPrefs
import com.rlcspedia.app.data.ThemeMode
import com.rlcspedia.app.notify.Notifier
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(settings: AppSettings, padding: PaddingValues, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    val ctx = LocalContext.current
    fun granted() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var permission by remember { mutableStateOf(granted()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
            SectionTitle("Ajustes")

            Group("Apariencia y accesibilidad") {
                Labeled("Tema") {
                    Connected(
                        listOf(ThemeMode.SYSTEM to "Sistema", ThemeMode.LIGHT to "Claro", ThemeMode.DARK to "Oscuro"),
                        settings.theme,
                    ) { v -> onChange { it.copy(theme = v) } }
                }
                Labeled("Contraste") {
                    Connected(listOf(ContrastLevel.STANDARD to "Estándar", ContrastLevel.HIGH to "Alto"), settings.contrast) { v ->
                        onChange { it.copy(contrast = v) }
                    }
                }
                if (Build.VERSION.SDK_INT >= 31) SwitchRow(
                    "Color de tu fondo de pantalla", "Usa Material You. Los colores de equipo azul y naranja no cambian.",
                    settings.dynamicColor && settings.contrast != ContrastLevel.HIGH,
                    enabled = settings.contrast != ContrastLevel.HIGH,
                ) { v -> onChange { it.copy(dynamicColor = v) } }
                Labeled("Tamaño de texto: ${(settings.fontScale * 100).roundToInt()} %") {
                    Slider(
                        value = settings.fontScale, onValueChange = { v -> onChange { it.copy(fontScale = (v * 20).roundToInt() / 20f) } },
                        valueRange = 0.85f..1.5f, steps = 12,
                        modifier = Modifier.semantics { stateDescription = "${(settings.fontScale * 100).roundToInt()} por ciento" },
                    )
                    Text("Se suma al tamaño de fuente del sistema.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SwitchRow("Fuente de alta legibilidad", "Atkinson Hyperlegible también en marcadores y títulos", settings.legibleFont) { v ->
                    onChange { it.copy(legibleFont = v) }
                }
                SwitchRow("Reducir movimiento", "Quita animaciones de marcador e indicadores", settings.reduceMotion) { v ->
                    onChange { it.copy(reduceMotion = v) }
                }
            }

            Group("Notificaciones (teléfono y reloj)") {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Permiso", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            if (permission) "Activadas en este teléfono" else "Aún no activadas",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!permission) Button(onClick = {
                        if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                    }) { Text("Activar") }
                    else FilledTonalButton(onClick = {
                        Notifier(ctx, RLCSpediaApp.instance.settings).postTest()
                    }) { Text("Probar") }
                }
                val n = settings.notify
                fun set(f: (NotifyPrefs) -> NotifyPrefs) = onChange { it.copy(notify = f(it.notify)) }
                SwitchRow("Inicio de serie", null, n.seriesStart) { v -> set { it.copy(seriesStart = v) } }
                SwitchRow("Goles", "Jugador, equipo y marcador del juego", n.goals) { v -> set { it.copy(goals = v) } }
                SwitchRow("Fin de cada juego", "Resultado del juego y cómo va la serie", n.gameEnd) { v -> set { it.copy(gameEnd = v) } }
                SwitchRow("Fin de serie", "Ganador y marcador final", n.seriesEnd) { v -> set { it.copy(seriesEnd = v) } }
                SwitchRow("Solo de mis favoritos", null, n.favoritesOnly) { v -> set { it.copy(favoritesOnly = v) } }
            }
            Text(
                "En el reloj: Wear OS (y Galaxy Watch, Pixel Watch, etc.) muestran estas notificaciones del teléfono automáticamente.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(
        title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp).semantics { heading() },
    )
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = MaterialTheme.shapes.extraLarge) {
        Column { content() }
    }
}

@Composable
private fun Labeled(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        content()
    }
}

@Composable
private fun SwitchRow(title: String, hint: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        Modifier.fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

/** Grupo de botones conectados (M3 Expressive) para elegir una opción. */
@Composable
private fun <T> Connected(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
        options.forEachIndexed { i, (value, label) ->
            val checked = value == selected
            ToggleButton(
                checked = checked,
                onCheckedChange = { onSelect(value) },
                shapes = when (i) {
                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                },
                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
            ) {
                if (checked) {
                    Icon(painterResource(R.drawable.ic_check), null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                }
                Text(label, maxLines = 1)
            }
        }
    }
}
