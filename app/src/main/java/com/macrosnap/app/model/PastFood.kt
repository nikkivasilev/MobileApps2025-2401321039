package com.macrosnap.app.model

import java.util.Locale
import kotlin.math.pow

/**
 * A food the user has logged before, ready to log again without asking the AI: nutrition from
 * the most recent entry of that name, grams set to the amount usually eaten.
 */
data class PastFood(
    val key: String,
    val item: FoodItem,
    val timesLogged: Int,
    val lastLoggedAt: Long,
)

/** Names that differ only in case or spacing are the same food. */
fun foodKey(name: String): String = name.trim().lowercase(Locale.ROOT).replace(WHITESPACE, " ")

object FoodHistory {

    /** How many recent entries decide the usual amount. */
    private const val USUAL_GRAMS_WINDOW = 10

    /** An entry's weight in the ranking halves every this many days. */
    private const val HALF_LIFE_DAYS = 30.0

    private const val DAY_MILLIS = 24 * 60 * 60 * 1000.0

    /**
     * Collapses the log into one [PastFood] per name, most useful first: often and recently logged
     * foods lead. [hidden] maps keys to when the user hid them; a food logged again since comes back.
     */
    fun build(entries: List<FoodEntry>, hidden: Map<String, Long> = emptyMap(), now: Long = System.currentTimeMillis()): List<PastFood> =
        entries
            .groupBy { foodKey(it.item.name) }
            .mapNotNull { (key, group) ->
                val newestFirst = group.sortedWith(compareByDescending<FoodEntry> { it.createdAt }.thenByDescending { it.id })
                val latest = newestFirst.first()
                if ((hidden[key] ?: Long.MIN_VALUE) >= latest.createdAt) return@mapNotNull null
                val score = group.sumOf { 0.5.pow(((now - it.createdAt) / DAY_MILLIS).coerceAtLeast(0.0) / HALF_LIFE_DAYS) }
                score to PastFood(
                    key = key,
                    item = latest.item.copy(grams = usualGrams(newestFirst)),
                    timesLogged = group.size,
                    lastLoggedAt = latest.createdAt,
                )
            }
            .sortedByDescending { it.first }
            .map { it.second }

    /** The most common recent amount, so a one-off double portion doesn't become the default. Ties go to the newest. */
    private fun usualGrams(newestFirst: List<FoodEntry>): Double {
        val recent = newestFirst.take(USUAL_GRAMS_WINDOW).map { Math.round(it.item.grams).toDouble() }
        val counts = recent.groupingBy { it }.eachCount()
        val best = counts.values.max()
        return recent.first { counts.getValue(it) == best }
    }
}

/**
 * Past foods whose name contains every word of [query] (ignoring amounts and units), best first.
 * Names starting with the query come ahead of mid-name matches; otherwise the history order holds.
 */
fun List<PastFood>.matching(query: String): List<PastFood> {
    val words = foodKey(query).split(' ').filter { it.isNotEmpty() && !it.isQuantityWord() }
    if (words.isEmpty()) return emptyList()
    val phrase = words.joinToString(" ")
    return filter { food -> words.all { it in food.key } }
        .sortedBy { if (it.key.startsWith(phrase)) 0 else 1 }
}

/**
 * Grams stated in free text such as "250 ml milk", "0,5 л" or "2 kg" (ml counted as grams, as the
 * rest of the app does for drinks). Null when no weight or volume is given.
 */
fun parseQuantityGrams(text: String): Double? {
    val match = QUANTITY.find(text.lowercase(Locale.ROOT)) ?: return null
    val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    val grams = when (match.groupValues[2]) {
        "kg", "кг", "l", "л" -> amount * 1000
        else -> amount
    }
    return grams.takeIf { it > 0 && it <= 5000 }
}

/** Up to [limit] matches for the food being typed in a describe box; none until it's two letters long. */
fun List<PastFood>.suggestionsFor(describeInput: String, limit: Int = 4): List<PastFood> {
    val segment = currentDescribeSegment(describeInput)
    if (segment.count { it.isLetter() } < 2) return emptyList()
    return matching(segment).take(limit)
}

/**
 * Takes [food] in place of the segment being typed: the item at the typed amount (or the usual
 * one), and the describe text left for the AI.
 */
fun acceptSuggestion(describeInput: String, food: PastFood): Pair<FoodItem, String> {
    val grams = parseQuantityGrams(currentDescribeSegment(describeInput)) ?: food.item.grams
    return food.item.copy(grams = grams) to withoutCurrentSegment(describeInput)
}

/** The part of a describe box still being typed: after the last comma, since earlier parts are other foods. */
fun currentDescribeSegment(input: String): String = input.substringAfterLast(',').trim()

/** [input] without its last segment, for when a suggestion has replaced what was being typed. */
fun withoutCurrentSegment(input: String): String =
    if (',' in input) input.substringBeforeLast(',').trim() else ""

private fun String.isQuantityWord(): Boolean = toDoubleOrNull() != null || QUANTITY.matches(this) || this in UNITS

private val WHITESPACE = Regex("\\s+")
private val UNITS = setOf("g", "gr", "ml", "kg", "l", "г", "гр", "мл", "кг", "л")
private val QUANTITY = Regex("""(\d+(?:[.,]\d+)?)\s*(kg|кг|ml|мл|gr|гр|g|г|l|л)(?![\p{L}])""")
