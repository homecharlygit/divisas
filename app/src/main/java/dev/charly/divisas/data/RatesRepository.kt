package dev.charly.divisas.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneOffset

/** Tipos de cambio relativos a 1 USD. */
data class Rates(
    val rates: Map<String, Double>,
    val updatedMillis: Long,
    val source: String,
)

class RatesRepository(context: Context) {

    private val prefs = context.getSharedPreferences("rates_cache", Context.MODE_PRIVATE)

    /** Último tipo de cambio guardado, para arrancar rápido y funcionar sin conexión. */
    fun cached(): Rates? {
        val raw = prefs.getString(KEY, null) ?: return null
        return runCatching {
            val j = JSONObject(raw)
            Rates(j.getJSONObject("rates").toDoubleMap(), j.getLong("updated"), j.getString("source"))
        }.getOrNull()
    }

    /** Cuándo se bajó por última vez el tipo de cambio (no cuándo lo publicó la fuente). */
    fun lastFetchMillis(): Long = prefs.getLong(KEY_FETCHED, 0L)

    /** Baja el tipo de cambio actual. Intenta ExchangeRate-API y, si falla, Frankfurter (BCE). */
    suspend fun fetch(): Rates = withContext(Dispatchers.IO) {
        val result = runCatching { fetchErApi() }.getOrNull() ?: fetchFrankfurter()
        save(result)
        result
    }

    private fun fetchErApi(): Rates {
        val j = JSONObject(Http.get("https://open.er-api.com/v6/latest/USD"))
        if (j.optString("result") != "success") throw IOException("ExchangeRate-API: ${j.optString("error-type")}")
        return Rates(
            rates = j.getJSONObject("rates").toDoubleMap(),
            updatedMillis = j.getLong("time_last_update_unix") * 1000,
            source = "ExchangeRate-API",
        )
    }

    private fun fetchFrankfurter(): Rates {
        val j = JSONObject(Http.get("https://api.frankfurter.dev/v1/latest?base=USD"))
        val date = LocalDate.parse(j.getString("date"))
        return Rates(
            rates = j.getJSONObject("rates").toDoubleMap() + ("USD" to 1.0),
            updatedMillis = date.atTime(16, 0).toInstant(ZoneOffset.UTC).toEpochMilli(),
            source = "Frankfurter (BCE)",
        )
    }

    private fun save(r: Rates) {
        val j = JSONObject()
            .put("rates", JSONObject(r.rates))
            .put("updated", r.updatedMillis)
            .put("source", r.source)
        prefs.edit()
            .putString(KEY, j.toString())
            .putLong(KEY_FETCHED, System.currentTimeMillis())
            .apply()
    }

    private fun JSONObject.toDoubleMap(): Map<String, Double> =
        keys().asSequence().associateWith { getDouble(it) }

    private companion object {
        const val KEY = "rates_json"
        const val KEY_FETCHED = "fetched_at"
    }
}
