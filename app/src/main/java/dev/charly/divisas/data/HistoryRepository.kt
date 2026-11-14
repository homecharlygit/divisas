package dev.charly.divisas.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneOffset

enum class HistoryRange(val days: Long) { WEEK(7), MONTH(30), HALF_YEAR(182), YEAR(365) }

data class HistoryPoint(val date: LocalDate, val rate: Double)

/** Histórico diario del Banco Central Europeo vía Frankfurter v1. */
class HistoryRepository {

    private val cache = mutableMapOf<String, List<HistoryPoint>>()

    fun supports(from: String, to: String): Boolean = from != to && from in SUPPORTED && to in SUPPORTED

    suspend fun load(from: String, to: String, range: HistoryRange): List<HistoryPoint> {
        val end = LocalDate.now(ZoneOffset.UTC)
        val key = "$from-$to-$range-$end"
        cache[key]?.let { return it }
        return withContext(Dispatchers.IO) {
            val start = end.minusDays(range.days)
            val j = JSONObject(Http.get("https://api.frankfurter.dev/v1/$start..$end?base=$from&symbols=$to"))
            val rates = j.getJSONObject("rates")
            val points = rates.keys().asSequence().mapNotNull { day ->
                val v = rates.getJSONObject(day).optDouble(to, Double.NaN)
                if (v.isNaN()) null else HistoryPoint(LocalDate.parse(day), v)
            }.sortedBy { it.date }.toList()
            cache[key] = points
            points
        }
    }

    companion object {
        /** Monedas con tipo de referencia del BCE. */
        val SUPPORTED = setOf(
            "AUD", "BGN", "BRL", "CAD", "CHF", "CNY", "CZK", "DKK", "EUR", "GBP", "HKD", "HUF",
            "IDR", "ILS", "INR", "ISK", "JPY", "KRW", "MXN", "MYR", "NOK", "NZD", "PHP", "PLN",
            "RON", "SEK", "SGD", "THB", "TRY", "USD", "ZAR",
        )
    }
}
