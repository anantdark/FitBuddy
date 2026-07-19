package com.anant.fitbuddy.data.model

/**
 * Mid-range North Indian portion priors shared by the AI prompt and offline simulator.
 * Values are typical home servings — adjust for visible size / stated count.
 */
object NorthIndianStaples {

    data class Staple(
        val name: String,
        val weightG: Int,
        val calories: Int,
        val proteinG: Int,
        val carbsG: Int,
        val fatsG: Int
    )

    val ROTI = Staple("Wheat roti/phulka", 35, 105, 3, 20, 2)
    val PARATHA = Staple("Stuffed paratha", 100, 290, 7, 36, 12)
    val NAAN = Staple("Naan", 100, 300, 9, 50, 7)
    val BHATURA = Staple("Bhatura", 80, 280, 6, 36, 12)
    val DAL_KATORI = Staple("Dal (1 katori)", 160, 150, 9, 22, 4)
    val RICE_BOWL = Staple("Cooked rice bowl", 175, 200, 4, 44, 1)
    val CURD_KATORI = Staple("Curd/raita katori", 120, 75, 4, 6, 4)
    val SABZI_DRY = Staple("Dry sabzi katori", 120, 140, 4, 14, 8)
    val GHEE_TSP = Staple("Ghee/oil (1 tsp)", 5, 45, 0, 0, 5)
    val SAMOSA = Staple("Samosa (1)", 100, 260, 4, 32, 12)
    val CHOLE_KATORI = Staple("Chole (1 katori)", 160, 220, 10, 28, 8)

    /** Compact table injected into the analysis system prompt. */
    fun promptReferenceTable(): String = """
        Staple reference (typical home serving; adjust to size/count — priors, not hard law):
        - 1 roti/chapati/phulka ~${ROTI.weightG} g → ~${ROTI.calories} kcal / P${ROTI.proteinG} C${ROTI.carbsG} F${ROTI.fatsG}
        - 1 stuffed paratha ~${PARATHA.weightG} g → ~${PARATHA.calories} kcal / P${PARATHA.proteinG} C${PARATHA.carbsG} F${PARATHA.fatsG}
        - 1 naan ~${NAAN.weightG} g → ~${NAAN.calories} kcal / P${NAAN.proteinG} C${NAAN.carbsG} F${NAAN.fatsG}
        - 1 bhatura ~${BHATURA.weightG} g → ~${BHATURA.calories} kcal / P${BHATURA.proteinG} C${BHATURA.carbsG} F${BHATURA.fatsG}
        - 1 katori dal ~${DAL_KATORI.weightG} g → ~${DAL_KATORI.calories} kcal / P${DAL_KATORI.proteinG} C${DAL_KATORI.carbsG} F${DAL_KATORI.fatsG}
        - 1 katori chole ~${CHOLE_KATORI.weightG} g → ~${CHOLE_KATORI.calories} kcal / P${CHOLE_KATORI.proteinG} C${CHOLE_KATORI.carbsG} F${CHOLE_KATORI.fatsG}
        - cooked rice bowl ~${RICE_BOWL.weightG} g → ~${RICE_BOWL.calories} kcal / P${RICE_BOWL.proteinG} C${RICE_BOWL.carbsG} F${RICE_BOWL.fatsG}
        - curd/raita katori ~${CURD_KATORI.weightG} g → ~${CURD_KATORI.calories} kcal / P${CURD_KATORI.proteinG} C${CURD_KATORI.carbsG} F${CURD_KATORI.fatsG}
        - dry sabzi katori ~${SABZI_DRY.weightG} g → ~${SABZI_DRY.calories} kcal / P${SABZI_DRY.proteinG} C${SABZI_DRY.carbsG} F${SABZI_DRY.fatsG}
        - 1 samosa ~${SAMOSA.weightG} g → ~${SAMOSA.calories} kcal / P${SAMOSA.proteinG} C${SAMOSA.carbsG} F${SAMOSA.fatsG}
        - cooking fat: 1 tsp ghee/oil ~${GHEE_TSP.weightG} g → ~${GHEE_TSP.calories} kcal (all fat)
    """.trimIndent()
}
