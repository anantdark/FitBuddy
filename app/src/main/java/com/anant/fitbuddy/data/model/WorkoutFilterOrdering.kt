package com.anant.fitbuddy.data.model

/**
 * Default chip order and body-part generalization for the workout exercise picker.
 *
 * Long-pressed chips are prepended by the caller; everything else follows [DEFAULT_BODY_PARTS]
 * / [DEFAULT_EQUIPMENTS], then any leftover labels A–Z.
 */
object WorkoutFilterOrdering {

    /** Preferred body-part chips (Arms/Legs are generalized umbrellas). */
    val DEFAULT_BODY_PARTS: List<String> = listOf(
        "Chest",
        "Back",
        "Legs",
        "Cardio",
        "Arms",
        "Shoulders",
        "Waist",
        "Neck",
    )

    /**
     * Preferred equipment chips. Names match ExerciseDB title-case labels
     * (“Body weight”, not FitBuddy’s [Equipment.BODYWEIGHT] tag).
     */
    val DEFAULT_EQUIPMENTS: List<String> = listOf(
        "Body weight",
        "Dumbbell",
        "Band",
        "Barbell",
        "Assisted",
        "Cable",
        "Kettlebell",
        "Resistance band",
        "EZ barbell",
        "Olympic barbell",
        "Trap bar",
        "Leverage machine",
        "Smith machine",
        "Sled machine",
        "Medicine ball",
        "Stability ball",
        "Bosu ball",
        "Rope",
        "Weighted",
        "Wheel roller",
        "Roller",
        "Tire",
        "Hammer",
        "Stationary bike",
        "Elliptical machine",
        "Stepmill machine",
        "Skierg machine",
        "Upper body ergometer",
    )

    /**
     * Collapses catalog body-part names into picker chips (Upper/Lower arms → Arms, etc.).
     */
    fun displayBodyParts(catalogLabels: List<String>): List<String> {
        if (catalogLabels.isEmpty()) return emptyList()
        var hasArms = false
        var hasLegs = false
        val others = LinkedHashSet<String>()
        for (raw in catalogLabels) {
            when (raw.trim().lowercase()) {
                "", "upper arms", "lower arms", "arms" -> hasArms = true
                "upper legs", "lower legs", "legs" -> hasLegs = true
                else -> others.add(titleCaseLabel(raw))
            }
        }
        val out = ArrayList<String>(others.size + 2)
        out.addAll(others)
        if (hasArms) out.add("Arms")
        if (hasLegs) out.add("Legs")
        return out
    }

    /** True when [exerciseBodyParts] matches the selected (possibly generalized) chip. */
    fun matchesBodyPart(exerciseBodyParts: List<String>, selected: String): Boolean {
        val aliases = bodyPartAliases(selected)
        return exerciseBodyParts.any { part ->
            aliases.any { it.equals(part.trim(), ignoreCase = true) }
        }
    }

    fun bodyPartAliases(selected: String): Set<String> = when (selected.trim().lowercase()) {
        "arms" -> setOf("arms", "upper arms", "lower arms")
        "legs" -> setOf("legs", "upper legs", "lower legs")
        else -> setOf(selected.trim().lowercase())
    }

    /**
     * [pinned] first (most recently promoted at index 0), then [defaultOrder], then leftovers A–Z.
     * Only labels present in [available] are kept; casing from [available] wins.
     */
    fun orderLabels(
        available: List<String>,
        defaultOrder: List<String>,
        pinned: List<String> = emptyList(),
    ): List<String> {
        if (available.isEmpty()) return emptyList()
        val byLower = LinkedHashMap<String, String>()
        for (label in available) {
            val key = label.lowercase()
            if (key.isNotBlank()) byLower.putIfAbsent(key, label)
        }
        val seen = HashSet<String>()
        val out = ArrayList<String>(byLower.size)
        fun take(key: String) {
            val label = byLower[key] ?: return
            if (seen.add(key)) out.add(label)
        }
        for (p in pinned) take(p.trim().lowercase())
        for (d in defaultOrder) take(d.trim().lowercase())
        byLower.keys.sorted().forEach(::take)
        return out
    }

    /** Prepend [label] to a pin list (deduped). */
    fun promotePin(current: List<String>, label: String): List<String> {
        val trimmed = label.trim()
        if (trimmed.isBlank()) return current
        return listOf(trimmed) + current.filterNot { it.equals(trimmed, ignoreCase = true) }
    }
}
