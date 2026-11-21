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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.rlcspedia.app.RLCSpediaApp
import com.rlcspedia.app.data.AppSettings
import com.rlcspedia.app.data.ContrastLevel
import com.rlcspedia.app.data.DataSource
import com.rlcspedia.app.data.NotifyPrefs
import com.rlcspedia.app.data.ThemeMode
import com.rlcspedia.app.notify.Notifier
import com.rlcspedia.app.ui.components.Symbols
import com.rlcspedia.app.ui.components.segmentedShape
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(settings: AppSettings, padding: PaddingValues, onChange: ((AppSettings) -> AppSettings) -> Unit) {
    val ctx = LocalContext.current
    fun granted() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    var permission by remember { mutableStateOf(granted()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permission = it }
    val n = settings.notify
    fun setN(f: (NotifyPrefs) -> NotifyPrefs) = onChange { it.copy(notify = f(it.notify)) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 24.dp)
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
            SectionTitle("Ajustes")

            Group(
                "Datos",
                {
                    Labeled("Fuente", Symbols.database) {
                        Connected(listOf(DataSource.DEMO to "Demostración", DataSource.LIQUIPEDIA to "Liquipedia"), settings.dataSource) { v ->
                            onChange { it.copy(dataSource = v) }
                        }
                        Hint(
                            if (settings.dataSource == DataSource.LIQUIPEDIA)
                                "Torneos, series y marcadores reales de la RLCS (CC-BY-SA 3.0). Requiere el secreto LIQUIPEDIA_API_KEY en el repo. Liquipedia no publica goleador ni reloj."
                            else "Equipos ficticios y partidos simulados, con goleador y reloj, para probar la app.",
                        )
                    }
                },
            )

            Group(
                "Apariencia y accesibilidad",
                {
                    Labeled("Tema", Symbols.palette) {
                        Connected(
                            listOf(ThemeMode.SYSTEM to "Sistema", ThemeMode.LIGHT to "Claro", ThemeMode.DARK to "Oscuro"), settings.theme,
                        ) { v -> onChange { it.copy(theme = v) } }
                    }
                },
                {
                    Labeled("Contraste", Symbols.contrast) {
                        Connected(listOf(ContrastLevel.STANDARD to "Estándar", ContrastLevel.HIGH to "Alto"), settings.contrast) { v ->
                            onChange { it.copy(contrast = v) }
                        }
                    }
                },
                *(if (Build.VERSION.SDK_INT >= 31) arrayOf<@Composable () -> Unit>({
                    SwitchRow(
                        Symbols.palette, "Color de tu fondo de pantalla", "Material You. Los colores de equipo azul y naranja no cambian.",
                        settings.dynamicColor && settings.contrast != ContrastLevel.HIGH, enabled = settings.contrast != ContrastLevel.HIGH,
                    ) { v -> onChange { it.copy(dynamicColor = v) } }
                }) else emptyArray()),
                {
                    Labeled("Tamaño de texto: ${(settings.fontScale * 100).roundToInt()} %", Symbols.formatSize) {
                        Slider(
                            value = settings.fontScale, onValueChange = { v -> onChange { it.copy(fontScale = (v * 20).roundToInt() / 20f) } },
                            valueRange = 0.85f..1.5f, steps = 12,
                            modifier = Modifier.semantics { stateDescription = "${(settings.fontScale * 100).roundToInt()} por ciento" },
                        )
                        Hint("Se suma al tamaño de fuente del sistema.")
                    }
                },
                {
                    SwitchRow(Symbols.textFields, "Fuente de alta legibilidad", "Atkinson Hyperlegible también en marcadores y títulos", settings.legibleFont) { v ->
                        onChange { it.copy(legibleFont = v) }
                    }
                },
                {
                    SwitchRow(Symbols.animation, "Reducir movimiento", "Quita transformaciones de formas, resortes y estallidos de gol", settings.reduceMotion) { v ->
                        onChange { it.copy(reduceMotion = v) }
                    }
                },
            )

            Group(
                "Notificaciones (teléfono y reloj)",
                {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(Symbols.notifications), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Permiso", style = MaterialTheme.typography.bodyLarge)
                            Hint(if (permission) "Activadas en este teléfono" else "Aún no activadas")
                        }
                        if (!permission) Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                else ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                            },
                            shapes = ButtonDefaults.shapes(),
                        ) { Text("Activar") }
                        else FilledTonalButton(
                            onClick = { Notifier(ctx, RLCSpediaApp.instance.settings).postTest() },
                            shapes = ButtonDefaults.shapes(),
                        ) { Text("Probar") }
                    }
                },
                { SwitchRow(null, "Inicio de serie", null, n.seriesStart) { v -> setN { it.copy(seriesStart = v) } } },
                { SwitchRow(null, "Goles", "Equipo y marcador (y jugador en modo demostración)", n.goals) { v -> setN { it.copy(goals = v) } } },
                { SwitchRow(null, "Fin de cada juego", "Resultado del juego y cómo va la serie", n.gameEnd) { v -> setN { it.copy(gameEnd = v) } } },
                { SwitchRow(null, "Fin de serie", "Ganador y marcador final", n.seriesEnd) { v -> setN { it.copy(seriesEnd = v) } } },
                { SwitchRow(Symbols.star.first, "Solo de mis favoritos", null, n.favoritesOnly) { v -> setN { it.copy(favoritesOnly = v) } } },
            )
            Row(Modifier.padding(horizontal = 4.dp, vertical = 12.dp)) {
                Icon(painterResource(Symbols.watch), null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(8.dp))
                Hint("En el reloj: Wear OS, Pixel Watch y Galaxy Watch muestran estas notificaciones del teléfono automáticamente.")
            }
        }
    }
}

/** Grupo de ajustes como lista segmentada de M3 Expressive. */
@Composable
private fun Group(title: String, vararg rows: @Composable () -> Unit) {
    Text(
        title, style = MaterialTheme.typography.titleMediumEmphasized, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 24.dp, bottom = 8.dp).semantics { heading() },
    )
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        rows.forEachIndexed { i, row ->
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = segmentedShape(i, rows.size)) { row() }
        }
    }
}

@Composable
private fun Hint(text: String) =
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

@Composable
private fun Labeled(label: String, icon: Int, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        content()
    }
}

@Composable
private fun SwitchRow(icon: Int?, title: String, hint: String?, checked: Boolean, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(16.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (hint != null) Hint(hint)
        }
        Spacer(Modifier.width(16.dp))
        Switch(
            checked = checked, onCheckedChange = null, enabled = enabled,
            thumbContent = if (checked) {
                { Icon(painterResource(Symbols.check), null, Modifier.size(SwitchDefaults.IconSize)) }
            } else null,
        )
    }
}

/** Grupo de botones conectados (M3 Expressive): la opción elegida se redondea por completo. */
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
                    Icon(painterResource(Symbols.check), null, Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                }
                Text(label, maxLines = 1)
            }
        }
    }
}
