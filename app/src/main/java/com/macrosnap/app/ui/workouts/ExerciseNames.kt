package com.macrosnap.app.ui.workouts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.macrosnap.app.data.ExerciseLibrary
import com.macrosnap.app.model.Exercise

// Seeded exercises are stored under their English names (existing installs already have them), so
// they're translated when shown. A user who renames one gets their own name back unchanged.

private val SEEDED_NAMES: Map<String, Int> = ExerciseLibrary.SEEDS.associate { it.name to it.nameRes }

/** The name to show: a default exercise in the UI language, anything the user typed as typed. */
val Exercise.displayName: String
    @Composable @ReadOnlyComposable
    get() {
        val res = if (isCustom) null else SEEDED_NAMES[name]
        return if (res != null) stringResource(res) else name
    }

/** [displayName] of every exercise by id, for filtering outside composition. */
@Composable
@ReadOnlyComposable
fun displayNames(exercises: List<Exercise>): Map<Long, String> = exercises.associate { it.id to it.displayName }

/** Search matches the stored name as well as the shown one, so English names keep working in Bulgarian. */
fun Exercise.matches(query: String, displayName: String): Boolean =
    query.isEmpty() || name.contains(query, ignoreCase = true) || displayName.contains(query, ignoreCase = true)

/** Alphabetical by the shown name; the database orders by the stored (English) names. */
fun List<Exercise>.sortedByDisplayName(displayNames: Map<Long, String>): List<Exercise> =
    sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { displayNames[it.id].orEmpty() })
