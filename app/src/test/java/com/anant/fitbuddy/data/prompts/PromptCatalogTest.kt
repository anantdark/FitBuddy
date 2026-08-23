package com.anant.fitbuddy.data.prompts

import com.anant.fitbuddy.data.region.AppRegion
import com.anant.fitbuddy.data.region.RegionPacks
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PromptCatalogTest {

    private val sharedFiles = listOf(
        "analyze",
        "target",
        "progress",
        "progress_chat",
        "workout_calories",
        "classify_exercise",
        "parse_workout",
        "workout_name",
        "ask_portion"
    )

    private val regionFiles = listOf(
        "analyze_system_intro",
        "target_system_intro",
        "progress_system_intro",
        "analyze_prompt_priors",
        "target_coach_notes",
        "progress_food_guidance",
        "measurement_prompt_notes",
        "ask_portion_prompt_notes"
    )

    private val fragments = listOf(
        "strict_clarification",
        "image_attached_note",
        "force_estimate_note"
    )

    @Test
    fun sharedAndFragmentFiles_load() {
        sharedFiles.forEach { name ->
            val text = PromptCatalog.load("prompts/shared/$name.txt")
            assertTrue(name, text.isNotBlank())
        }
        fragments.forEach { name ->
            val text = PromptCatalog.load("prompts/shared/fragments/$name.txt")
            assertTrue(name, text.isNotBlank())
        }
    }

    @Test
    fun everyRegion_hasAllOverlayFiles() {
        AppRegion.entries.forEach { region ->
            regionFiles.forEach { file ->
                val text = PromptCatalog.regionText(region, file)
                assertTrue("${region.name}/$file", text.isNotBlank())
            }
        }
    }

    @Test
    fun renderedAnalyzeAndTarget_includeRegionMarkers() {
        val india = RegionPacks.pack(AppRegion.INDIA)
        val us = RegionPacks.pack(AppRegion.US)
        val europe = RegionPacks.pack(AppRegion.EUROPE)
        val latAm = RegionPacks.pack(AppRegion.LATIN_AMERICA)

        val indiaAnalyze = PromptCatalog.analyzePrompt(
            userStateContextJson = "{}",
            userText = "dal chawal",
            hasImage = false,
            forceEstimate = false,
            strictClarification = false,
            pack = india
        )
        assertTrue(indiaAnalyze.contains("Indian", ignoreCase = true))
        assertTrue(indiaAnalyze.contains("dal chawal"))
        assertFalse(indiaAnalyze.contains("{{"))

        val usTarget = PromptCatalog.targetPrompt("{}", us)
        assertTrue(usTarget.contains("North American", ignoreCase = true))
        assertFalse(usTarget.contains("{{"))

        val euProgress = PromptCatalog.progressPrompt("metrics", europe)
        assertTrue(euProgress.contains("European", ignoreCase = true))

        val latAmChat = PromptCatalog.progressChatSystemPrompt("{}", latAm)
        assertTrue(latAmChat.contains("Latin American", ignoreCase = true))
    }

    @Test
    fun askPortionUserPrompt_rendersDishAndNotes() {
        val pack = RegionPacks.pack(AppRegion.INDIA)
        val prompt = PromptCatalog.askPortionUserPrompt("Paneer bhurji", pack.askPortionPromptNotes)
        assertTrue(prompt.startsWith("Paneer bhurji."))
        assertTrue(prompt.contains("katori", ignoreCase = true))
        assertTrue(prompt.contains("Break into named ingredients"))
        assertFalse(prompt.contains("{{"))
    }

    @Test
    fun strictClarificationAndImageFragments_injectWhenEnabled() {
        val pack = RegionPacks.pack(AppRegion.US)
        val withFlags = PromptCatalog.analyzePrompt(
            userStateContextJson = "{}",
            userText = "salad",
            hasImage = true,
            forceEstimate = true,
            strictClarification = true,
            pack = pack
        )
        assertTrue(withFlags.contains("STRICT CLARIFICATION MODE"))
        assertTrue(withFlags.contains("An image is attached"))
        assertTrue(withFlags.contains("ALWAYS return status \"SUCCESS\""))
        assertTrue(withFlags.contains("the attached photo makes portion size obvious"))
    }
}
