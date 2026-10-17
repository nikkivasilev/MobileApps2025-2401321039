package com.macrosnap.app.model

/**
 * A combination of foods eaten together more than once (one meal on one day), ready to log again
 * in one go: each food with its newest nutrition and the amount usually eaten in this meal.
 */
data class PastMeal(
    val key: String,
    val items: List<FoodItem>,
    val timesLogged: Int,
    val lastLoggedAt: Long,
) {
    val total: Macros get() = items.map { it.total }.sum()

    /** A meal is named by its foods, in the order they were logged. */
    val name: String get() = items.joinToString { it.name }
}

object MealHistory {

    /** A single food is already covered by "Your foods". */
    private const val MIN_FOODS = 2

    /** Eaten once is an occasion, not a habit; a meal card's "log again" covers one-offs. */
    private const val MIN_TIMES = 2

    /** One meal on one day: everything logged under it, however many times "add" was pressed. */
    private data class Occasion(val entries: List<FoodEntry>) {
        val foods: Map<String, List<FoodEntry>> = entries.groupBy { foodKey(it.item.name) }
        val key: String = mealKey(foods.keys)
        val loggedAt: Long = entries.maxOf { it.createdAt }
    }

    /**
     * The meals eaten at least twice with the exact same foods, most useful first: often and
     * recently eaten lead. [hidden] maps keys to when the user hid them; eaten again since, a meal
     * comes back.
     */
    fun build(entries: List<FoodEntry>, hidden: Map<String, Long> = emptyMap(), now: Long = System.currentTimeMillis()): List<PastMeal> =
        entries
            .groupBy { it.date to it.mealType }
            .values
            .map(::Occasion)
            .filter { it.foods.size >= MIN_FOODS }
            .groupBy { it.key }
            .mapNotNull { (key, occasions) ->
                if (occasions.size < MIN_TIMES) return@mapNotNull null
                val newestFirst = occasions.sortedByDescending { it.loggedAt }
                val lastLoggedAt = newestFirst.first().loggedAt
                if ((hidden[key] ?: Long.MIN_VALUE) >= lastLoggedAt) return@mapNotNull null
                val score = occasions.sumOf { recencyWeight(it.loggedAt, now) }
                score to PastMeal(key, mealItems(newestFirst), occasions.size, lastLoggedAt)
            }
            .sortedByDescending { it.first }
            .map { it.second }

    /** The foods in the order they were last logged, at the amount usually eaten in this meal. */
    private fun mealItems(newestFirst: List<Occasion>): List<FoodItem> {
        val latest = newestFirst.first()
        return latest.entries
            .sortedWith(compareBy<FoodEntry> { it.createdAt }.thenBy { it.id })
            .map { foodKey(it.item.name) }
            .distinct()
            .map { food ->
                val newestEntry = latest.foods.getValue(food).maxWith(compareBy<FoodEntry> { it.createdAt }.thenBy { it.id })
                // A food logged twice in one meal (a second helping) counts as one larger amount.
                val amounts = newestFirst.map { occasion -> occasion.foods.getValue(food).sumOf { it.item.grams } }
                newestEntry.item.copy(grams = usualGrams(amounts))
            }
    }
}

/** The same foods make the same meal, whatever order they were logged in. */
fun mealKey(foodKeys: Collection<String>): String = foodKeys.sorted().joinToString(MEAL_KEY_SEPARATOR)

private const val MEAL_KEY_SEPARATOR = "\n"
