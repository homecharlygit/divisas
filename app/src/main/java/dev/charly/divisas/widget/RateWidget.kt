package dev.charly.divisas.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.charly.divisas.MainActivity
import dev.charly.divisas.R
import dev.charly.divisas.data.RatesRepository
import dev.charly.divisas.ui.i18n.AppLanguage
import dev.charly.divisas.ui.i18n.stringsFor
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Widget con el par de monedas elegido en la app (Material You en Android 12+). */
class RateWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = RatesRepository(context)
        var rates = repo.cached()
        // Si el dato tiene más de una hora, intenta bajar uno nuevo
        if (rates == null || System.currentTimeMillis() - repo.lastFetchMillis() > 60 * 60 * 1000L) {
            runCatching { repo.fetch() }.getOrNull()?.let { rates = it }
        }

        val prefs = context.getSharedPreferences("prefs", Context.MODE_PRIVATE)
        val from = prefs.getString("from", null) ?: "USD"
        val to = prefs.getString("to", null) ?: "MXN"
        val lang = AppLanguage.from(prefs.getString("lang", null))
        val str = stringsFor(lang)
        val locale = lang.locale

        val r = rates
        val value = r?.rates?.let { m -> m[from]?.let { f -> m[to]?.let { t -> t / f } } }
        val rateText = value?.let {
            NumberFormat.getNumberInstance(locale).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = if (it >= 1) 2 else 4
            }.format(it)
        } ?: "—"
        val time = repo.lastFetchMillis().takeIf { it > 0 }?.let {
            DateTimeFormatter.ofPattern("HH:mm", locale).format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
        } ?: ""

        provideContent {
            GlanceTheme {
                WidgetContent(
                    pair = "${flag(from)} $from → ${flag(to)} $to",
                    rate = rateText,
                    subtitle = if (time.isEmpty()) "1 $from" else "1 $from · $time",
                    refreshLabel = str.refresh,
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(pair: String, rate: String, subtitle: String, refreshLabel: String) {
    val colors = GlanceTheme.colors
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .cornerRadius(28.dp)
            .background(colors.primaryContainer)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = pair,
                style = TextStyle(color = colors.onPrimaryContainer, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight(),
            )
            Image(
                provider = ImageProvider(R.drawable.ic_refresh),
                contentDescription = refreshLabel,
                colorFilter = ColorFilter.tint(colors.onPrimaryContainer),
                modifier = GlanceModifier.size(22.dp).clickable(actionRunCallback<RefreshRatesAction>()),
            )
        }
        Spacer(GlanceModifier.defaultWeight())
        Text(
            text = rate,
            style = TextStyle(color = colors.onPrimaryContainer, fontSize = 36.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
        )
        Text(
            text = subtitle,
            style = TextStyle(color = colors.onPrimaryContainer, fontSize = 12.sp),
            maxLines = 1,
        )
    }
}

/** Botón de refrescar del widget. */
class RefreshRatesAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        runCatching { RatesRepository(context).fetch() }
        RateWidget().update(context, glanceId)
    }
}

class RateWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RateWidget()
}

private fun flag(code: String): String {
    if (code == "EUR") return "🇪🇺"
    if (code.length < 2 || !code[0].isLetter() || !code[1].isLetter()) return ""
    return code.take(2).uppercase().map { ch -> String(Character.toChars(0x1F1E6 + (ch - 'A'))) }.joinToString("")
}
