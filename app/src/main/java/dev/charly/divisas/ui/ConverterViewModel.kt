package dev.charly.divisas.ui

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.charly.divisas.data.HistoryPoint
import dev.charly.divisas.data.HistoryRange
import dev.charly.divisas.data.HistoryRepository
import dev.charly.divisas.data.RatesRepository
import dev.charly.divisas.data.UpdateInfo
import dev.charly.divisas.data.UpdateRepository
import dev.charly.divisas.widget.RateWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import java.io.File
import dev.charly.divisas.ui.i18n.AppLanguage
import dev.charly.divisas.ui.theme.DEFAULT_PALETTE
import kotlinx.coroutines.launch

data class UiState(
    val from: String,
    val to: String,
    val amount: String,
    val rates: Map<String, Double>? = null,
    val updatedMillis: Long? = null,
    val source: String? = null,
    val loading: Boolean = false,
    val offline: Boolean = false,
) {
    val amountValue: Double
        get() = amount.replace(",", "").toDoubleOrNull() ?: 0.0

    fun convert(value: Double, from: String, to: String): Double? {
        val r = rates ?: return null
        val f = r[from] ?: return null
        val t = r[to] ?: return null
        return value / f * t
    }

    /** Monedas disponibles: primero las favoritas, luego el resto en orden alfabético. */
    val currencies: List<String>
        get() {
            val all = rates?.keys ?: FAVORITES.toSet()
            return FAVORITES.filter { it in all } + all.filterNot { it in FAVORITES }.sorted()
        }
}

data class AppSettings(
    val palette: Int,
    val dark: Boolean,
    val language: AppLanguage,
    val blur: BlurType,
    val haptics: Boolean,
    val hapticStrength: Float,
)

data class HistoryUi(
    val range: HistoryRange = HistoryRange.MONTH,
    val pair: String = "",
    val points: List<HistoryPoint> = emptyList(),
    val loading: Boolean = false,
    val unsupported: Boolean = false,
    val error: Boolean = false,
)

data class UpdateUi(
    val info: UpdateInfo? = null,
    val checking: Boolean = false,
    /** null = sin descarga, 0..1 progreso, -1 tamaño desconocido */
    val progress: Float? = null,
    val file: File? = null,
    val error: Boolean = false,
    val upToDate: Boolean = false,
    val dismissed: Boolean = false,
    val needsPermission: Boolean = false,
)

val FAVORITES = listOf("MXN", "USD", "EUR", "CAD", "GBP", "JPY", "CNY", "BRL", "COP", "ARS")

class ConverterViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = RatesRepository(app)
    private val historyRepo = HistoryRepository()
    val updates = UpdateRepository(app)
    private var historyJob: Job? = null
    private val prefs = app.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    private var lastFetchMillis = 0L

    var state by mutableStateOf(
        UiState(
            from = prefs.getString("from", null) ?: "USD",
            to = prefs.getString("to", null) ?: "MXN",
            amount = prefs.getString("amount", null) ?: "100",
        )
    )
        private set

    var settings by mutableStateOf(
        AppSettings(
            palette = prefs.getInt("palette", DEFAULT_PALETTE),
            dark = prefs.getBoolean("dark", true), // oscuro por defecto
            language = AppLanguage.from(prefs.getString("lang", null)),
            blur = BlurType.from(prefs.getString("blur", null)), // Kawase por defecto
            haptics = prefs.getBoolean("haptics", true),
            hapticStrength = prefs.getFloat("hapticStrength", 0.7f),
        )
    )
        private set

    fun setPalette(index: Int) {
        settings = settings.copy(palette = index)
        prefs.edit().putInt("palette", index).apply()
    }

    fun setDark(dark: Boolean) {
        settings = settings.copy(dark = dark)
        prefs.edit().putBoolean("dark", dark).apply()
    }

    fun setLanguage(lang: AppLanguage) {
        settings = settings.copy(language = lang)
        prefs.edit().putString("lang", lang.code).apply()
    }

    fun setHaptics(on: Boolean) {
        settings = settings.copy(haptics = on)
        prefs.edit().putBoolean("haptics", on).apply()
    }

    fun setHapticStrength(value: Float) {
        settings = settings.copy(hapticStrength = value)
        prefs.edit().putFloat("hapticStrength", value).apply()
    }

    fun setBlur(type: BlurType) {
        settings = settings.copy(blur = type)
        prefs.edit().putString("blur", type.code).apply()
    }

    var history by mutableStateOf(HistoryUi())
        private set

    var updateUi by mutableStateOf(UpdateUi())
        private set

    init {
        repo.cached()?.let {
            state = state.copy(rates = it.rates, updatedMillis = it.updatedMillis, source = it.source)
        }
        refresh()
        loadHistory()
        // Busca actualizaciones solo si pasaron más de 6 horas desde la última vez
        if (System.currentTimeMillis() - updates.lastCheckMillis() > 6 * 60 * 60 * 1000L) checkForUpdates()
    }

    // ---------------- Historial ----------------

    fun setHistoryRange(range: HistoryRange) {
        if (range == history.range) return
        history = history.copy(range = range)
        loadHistory()
    }

    fun loadHistory() {
        val from = state.from
        val to = state.to
        historyJob?.cancel()
        if (!historyRepo.supports(from, to)) {
            history = history.copy(pair = "$from/$to", points = emptyList(), loading = false, unsupported = true, error = false)
            return
        }
        history = history.copy(loading = true, unsupported = false, error = false)
        val range = history.range
        historyJob = viewModelScope.launch {
            history = try {
                history.copy(pair = "$from/$to", points = historyRepo.load(from, to, range), loading = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                history.copy(loading = false, error = true)
            }
        }
    }

    // ---------------- Actualizaciones ----------------

    fun checkForUpdates() {
        if (updateUi.checking) return
        updateUi = updateUi.copy(checking = true, error = false, upToDate = false)
        viewModelScope.launch {
            updateUi = try {
                val info = updates.check()
                updateUi.copy(info = info, checking = false, upToDate = info == null, dismissed = false)
            } catch (e: Exception) {
                updateUi.copy(checking = false, error = true)
            }
        }
    }

    fun dismissUpdate() {
        updateUi = updateUi.copy(dismissed = true)
    }

    /** Descarga el APK y al terminar abre el instalador. */
    fun downloadAndInstall() {
        val info = updateUi.info ?: return
        updateUi.file?.let { if (it.exists()) { install(it); return } }
        if (updateUi.progress != null) return
        updateUi = updateUi.copy(progress = 0f, error = false)
        viewModelScope.launch {
            try {
                val file = updates.download(info) { p -> updateUi = updateUi.copy(progress = p) }
                updateUi = updateUi.copy(progress = null, file = file)
                install(file)
            } catch (e: Exception) {
                updateUi = updateUi.copy(progress = null, error = true)
            }
        }
    }

    private fun install(file: File) {
        if (!updates.canInstall()) {
            updateUi = updateUi.copy(needsPermission = true)
            updates.openInstallPermissionSettings()
        } else {
            updateUi = updateUi.copy(needsPermission = false)
            updates.install(file)
        }
    }

    private fun refreshWidget() {
        viewModelScope.launch { runCatching { RateWidget().updateAll(getApplication()) } }
    }

    fun refresh() {
        if (state.loading) return
        state = state.copy(loading = true)
        viewModelScope.launch {
            state = try {
                val r = repo.fetch()
                lastFetchMillis = System.currentTimeMillis()
                refreshWidget()
                state.copy(
                    rates = r.rates, updatedMillis = r.updatedMillis, source = r.source,
                    loading = false, offline = false,
                )
            } catch (e: Exception) {
                state.copy(loading = false, offline = true)
            }
        }
    }

    /** Se llama al volver a la app: refresca si pasó más de una hora. */
    fun refreshIfStale() {
        if (System.currentTimeMillis() - lastFetchMillis > 60 * 60 * 1000L) refresh()
    }

    fun setAmount(input: String) {
        // Solo dígitos y un punto decimal
        var dot = false
        val clean = buildString {
            for (c in input) {
                if (c.isDigit()) append(c)
                else if ((c == '.' || c == ',') && !dot) { append('.'); dot = true }
            }
        }.take(15)
        update(state.copy(amount = clean))
    }

    fun setFrom(code: String) {
        update(if (code == state.to) state.copy(from = code, to = state.from) else state.copy(from = code))
    }

    fun setTo(code: String) {
        update(if (code == state.from) state.copy(to = code, from = state.to) else state.copy(to = code))
    }

    fun swap() = update(state.copy(from = state.to, to = state.from))

    private fun update(s: UiState) {
        val pairChanged = s.from != state.from || s.to != state.to
        state = s
        if (pairChanged) {
            loadHistory()
            refreshWidget()
        }
        prefs.edit()
            .putString("from", s.from)
            .putString("to", s.to)
            .putString("amount", s.amount)
            .apply()
    }
}
