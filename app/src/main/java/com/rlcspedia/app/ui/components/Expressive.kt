@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.rlcspedia.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.rlcspedia.app.R
import com.rlcspedia.app.ui.theme.LocalReduceMotion

/* ---------- Íconos: Material Symbols Rounded (relleno = estado activo) ---------- */
object Symbols {
    val live = R.drawable.ms_sensors to R.drawable.ms_sensors_fill
    val trophy = R.drawable.ms_trophy to R.drawable.ms_trophy_fill
    val star = R.drawable.ms_star to R.drawable.ms_star_fill
    val settings = R.drawable.ms_settings to R.drawable.ms_settings_fill
    const val back = R.drawable.ms_arrow_back
    const val expand = R.drawable.ms_keyboard_arrow_down
    const val check = R.drawable.ms_check
    const val notifications = R.drawable.ms_notifications
    const val search = R.drawable.ms_search
    const val close = R.drawable.ms_close
    const val openInNew = R.drawable.ms_open_in_new
    const val refresh = R.drawable.ms_refresh
    const val filter = R.drawable.ms_filter_list
    const val stadium = R.drawable.ms_stadium
    const val groups = R.drawable.ms_groups
    const val unfoldMore = R.drawable.ms_unfold_more
    const val unfoldLess = R.drawable.ms_unfold_less
    const val calendar = R.drawable.ms_calendar_month
    const val database = R.drawable.ms_database
    const val contrast = R.drawable.ms_contrast
    const val textFields = R.drawable.ms_text_fields
    const val palette = R.drawable.ms_palette
    const val animation = R.drawable.ms_animation
    const val formatSize = R.drawable.ms_format_size
    const val info = R.drawable.ms_info
    const val public = R.drawable.ms_public
    const val watch = R.drawable.ms_watch
    const val soccer = R.drawable.ms_sports_soccer_fill
}

/* ---------- Forma que se transforma entre dos polígonos de MaterialShapes ---------- */
class MorphShape(private val morph: Morph, private val progress: Float, private val rotation: Float = 0f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = morph.toPath(progress).asComposePath()
        val m = Matrix()
        m.scale(size.width, size.height)
        path.transform(m)
        if (rotation != 0f) {
            val r = Matrix()
            r.translate(size.width / 2, size.height / 2); r.rotateZ(rotation); r.translate(-size.width / 2, -size.height / 2)
            path.transform(r)
        }
        return Outline.Generic(path)
    }
}

@Composable
fun rememberMorph(from: RoundedPolygon, to: RoundedPolygon) = remember(from, to) { Morph(from, to) }

/** Forma animada: pasa de [from] a [to] con el resorte espacial del esquema de movimiento. */
@Composable
fun animatedMorphShape(from: RoundedPolygon, to: RoundedPolygon, toTarget: Boolean): Shape {
    val morph = rememberMorph(from, to)
    val reduce = LocalReduceMotion.current
    val p by animateFloatAsState(
        targetValue = if (toTarget) 1f else 0f,
        animationSpec = if (reduce) androidx.compose.animation.core.snap() else MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "morph",
    )
    return MorphShape(morph, p.coerceIn(0f, 1f))
}

/* ---------- Esquinas que se contraen al presionar (tarjetas) ---------- */
@Composable
fun pressShape(interaction: MutableInteractionSource, rest: Dp = 28.dp, pressed: Dp = 16.dp): Shape {
    val isPressed by interaction.collectIsPressedAsState()
    val reduce = LocalReduceMotion.current
    val r by animateDpAsState(
        if (isPressed && !reduce) pressed else rest,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(), label = "press",
    )
    return RoundedCornerShape(r)
}

/* ---------- Listas segmentadas: esquinas grandes en los extremos, chicas en medio ---------- */
fun segmentedShape(index: Int, count: Int, outer: Dp = 24.dp, inner: Dp = 4.dp): Shape = when {
    count == 1 -> RoundedCornerShape(outer)
    index == 0 -> RoundedCornerShape(outer, outer, inner, inner)
    index == count - 1 -> RoundedCornerShape(inner, inner, outer, outer)
    else -> RoundedCornerShape(inner)
}

/**
 * Estallido de gol: cuando [trigger] cambia, una forma "Soft burst" crece y gira detrás
 * del marcador y se desvanece. Es el momento expresivo principal de la app.
 */
@Composable
fun GoalBurst(trigger: Any?, color: Color, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val reduce = LocalReduceMotion.current
    val anim = remember { Animatable(0f) }
    val morph = rememberMorph(MaterialShapes.Circle, MaterialShapes.SoftBurst)
    val first = remember { booleanArrayOf(true) }
    LaunchedEffect(trigger) {
        if (first[0]) { first[0] = false; return@LaunchedEffect }
        if (reduce) return@LaunchedEffect
        anim.snapTo(0f)
        anim.animateTo(1f, androidx.compose.animation.core.tween(900))
    }
    val t = anim.value
    if (t <= 0f || t >= 1f) return
    Box(
        modifier.size(size).graphicsLayer {
            val s = 0.4f + 0.9f * t
            scaleX = s; scaleY = s
            rotationZ = 90f * t
            alpha = (1f - t) * 0.55f
        }.clip(MorphShape(morph, (t * 1.6f).coerceAtMost(1f))).drawBehind { drawRect(color) }
    )
}
