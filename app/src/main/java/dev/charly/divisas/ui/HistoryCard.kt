package dev.charly.divisas.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.charly.divisas.data.HistoryRange
import dev.charly.divisas.ui.i18n.Strings
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HistoryCard(
    history: HistoryUi,
    from: String,
    to: String,
    str: Strings,
    locale: Locale,
    onRange: (HistoryRange) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHaptics.current
    val points = history.points
    var selected by remember(points) { mutableStateOf<Int?>(null) }

    val first = points.firstOrNull()?.rate
    val last = points.lastOrNull()?.rate
    val change = if (first != null && last != null && first != 0.0) (last - first) / first * 100 else null
    val up = (change ?: 0.0) >= 0
    val trendColor = if (up) colors.tertiary else colors.error

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = colors.surfaceContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp)) {
            // Encabezado: título + valor seleccionado o variación del periodo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(str.history, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "$from → $to",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
                val sel = selected?.let { points.getOrNull(it) }
                AnimatedContent(
                    targetState = sel?.let { "${fmtRate(it.rate, locale)} · ${it.date.format(DateTimeFormatter.ofPattern("d MMM yy", locale))}" }
                        ?: change?.let { (if (up) "▲ " else "▼ ") + String.format(locale, "%.2f%%", abs(it)) }
                        ?: "",
                    transitionSpec = { (fadeIn() + scaleIn(Motion.bouncy(), initialScale = 0.85f)) togetherWith fadeOut() },
                    label = "historyHeader",
                ) { text ->
                    if (text.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = if (sel != null) colors.primaryContainer else trendColor.copy(alpha = 0.16f),
                            contentColor = if (sel != null) colors.onPrimaryContainer else trendColor,
                        ) {
                            Text(
                                text,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().height(170.dp),
            ) {
                when {
                    history.unsupported -> Text(
                        str.historyUnsupported,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    history.error -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(str.historyError, color = colors.error, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { haptics.click(); onRetry() }) { Text(str.retry) }
                    }
                    points.size < 2 && history.loading -> LoadingIndicator(modifier = Modifier.size(56.dp))
                    points.size >= 2 -> HistoryChart(
                        rates = points.map { it.rate },
                        selected = selected,
                        lineColor = colors.primary,
                        fillColor = colors.primary,
                        guideColor = colors.outline,
                        dotColor = colors.primary,
                        dotRing = colors.surfaceContainer,
                        dimmed = history.loading,
                        onSelect = { idx ->
                            if (idx != selected) {
                                selected = idx
                                if (idx != null) {
                                    // Más fuerte en los extremos del periodo (mínimo y máximo)
                                    val rates = points.map { it.rate }
                                    val v = rates[idx]
                                    if (v == rates.max() || v == rates.min()) haptics.click(0.7f) else haptics.tick(0.3f)
                                }
                            }
                        },
                    )
                }
            }

            if (points.size >= 2 && !history.unsupported) {
                val rates = points.map { it.rate }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text(
                        "${str.minLabel} ${fmtRate(rates.min(), locale)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${str.maxLabel} ${fmtRate(rates.max(), locale)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                HistoryRange.entries.forEachIndexed { i, range ->
                    SegmentedButton(
                        selected = history.range == range,
                        onClick = {
                            if (history.range != range) {
                                haptics.select()
                                onRange(range)
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = i, count = HistoryRange.entries.size),
                        label = { Text(str.rangeLabels[i]) },
                    )
                }
            }
        }
    }
}

/** Línea con curvas suaves que se dibuja de izquierda a derecha con resorte. Arrastra para ver cada día. */
@Composable
private fun HistoryChart(
    rates: List<Double>,
    selected: Int?,
    lineColor: androidx.compose.ui.graphics.Color,
    fillColor: androidx.compose.ui.graphics.Color,
    guideColor: androidx.compose.ui.graphics.Color,
    dotColor: androidx.compose.ui.graphics.Color,
    dotRing: androidx.compose.ui.graphics.Color,
    dimmed: Boolean,
    onSelect: (Int?) -> Unit,
) {
    val draw = remember { Animatable(0f) }
    LaunchedEffect(rates) {
        draw.snapTo(0f)
        draw.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessVeryLow))
    }
    val dot = remember { Animatable(0f) }
    LaunchedEffect(selected) {
        if (selected != null) {
            dot.snapTo(0.4f)
            dot.animateTo(1f, Motion.wobbly())
        } else dot.animateTo(0f, Motion.snappy())
    }

    fun indexAt(x: Float, width: Float): Int =
        ((x / width) * (rates.size - 1)).roundToInt().coerceIn(0, rates.size - 1)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .pointerInput(rates) {
                detectHorizontalDragGestures(
                    onDragStart = { onSelect(indexAt(it.x, size.width.toFloat())) },
                    onDragEnd = { onSelect(null) },
                    onDragCancel = { onSelect(null) },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onSelect(indexAt(change.position.x, size.width.toFloat()))
                    },
                )
            }
            .pointerInput(rates) {
                detectTapGestures(onTap = { onSelect(indexAt(it.x, size.width.toFloat())) })
            },
    ) {
        val min = rates.min()
        val max = rates.max()
        val span = (max - min).takeIf { it > 0 } ?: 1.0
        val padY = 14.dp.toPx()
        val h = size.height - padY * 2
        fun pt(i: Int) = Offset(
            x = size.width * i / (rates.size - 1),
            y = padY + (h * (1 - (rates[i] - min) / span)).toFloat(),
        )

        val line = Path().apply {
            val p0 = pt(0)
            moveTo(p0.x, p0.y)
            for (i in 1 until rates.size) {
                val a = pt(i - 1)
                val b = pt(i)
                val cx = (a.x + b.x) / 2
                cubicTo(cx, a.y, cx, b.y, b.x, b.y)
            }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }

        val p = draw.value.coerceIn(0f, 1f)
        val alpha = if (dimmed) 0.4f else 1f

        // Relleno con degradado, revelado junto con la línea
        clipRect(right = size.width * p) {
            drawPath(
                fill,
                Brush.verticalGradient(
                    listOf(fillColor.copy(alpha = 0.32f * alpha), fillColor.copy(alpha = 0f)),
                ),
            )
        }
        // Línea: solo el tramo ya "dibujado"
        val measure = PathMeasure().apply { setPath(line, false) }
        val partial = Path()
        measure.getSegment(0f, measure.length * p, partial, true)
        drawPath(
            partial,
            lineColor.copy(alpha = alpha),
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Punto seleccionado con guía punteada
        if (selected != null && selected in rates.indices) {
            val s = pt(selected)
            drawLine(
                guideColor.copy(alpha = 0.6f),
                Offset(s.x, 0f),
                Offset(s.x, size.height),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
            )
            val r = 7.dp.toPx() * dot.value
            drawCircle(dotRing, radius = r + 3.dp.toPx() * dot.value, center = s)
            drawCircle(dotColor, radius = r, center = s)
        }
    }
}

private fun fmtRate(v: Double, locale: Locale): String =
    NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = if (abs(v) >= 1) 2 else 4
    }.format(v)
