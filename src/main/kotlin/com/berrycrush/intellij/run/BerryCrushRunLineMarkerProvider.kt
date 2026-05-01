package com.berrycrush.intellij.run

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Provides run icons (▶) in the gutter for BerryCrush scenarios and features.
 *
 * Click the icon to run the specific scenario or all scenarios in a feature.
 * Right-click for run/debug options.
 *
 * Supports:
 * - `scenario:` blocks - run single scenario
 * - `feature:` blocks - run all scenarios in feature
 */
class BerryCrushRunLineMarkerProvider : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        // Only process leaf elements to avoid duplicate markers
        if (element !is LeafPsiElement) return null

        val text = element.text.trim().lowercase()

        return when {
            isScenarioKeyword(text, element) -> createScenarioRunInfo(element)
            isFeatureKeyword(text, element) -> createFeatureRunInfo(element)
            else -> null
        }
    }

    /**
     * Check if this element is the start of a scenario block.
     */
    private fun isScenarioKeyword(text: String, element: PsiElement): Boolean {
        if (!text.startsWith("scenario")) return false

        // Check that it's followed by a colon (scenario block, not just the word)
        val parent = element.parent
        val parentText = parent?.text?.trim()?.lowercase() ?: return false
        return parentText.startsWith("scenario:")
    }

    /**
     * Check if this element is the start of a feature block.
     */
    private fun isFeatureKeyword(text: String, element: PsiElement): Boolean {
        if (!text.startsWith("feature")) return false

        // Check that it's followed by a colon (feature block, not just the word)
        val parent = element.parent
        val parentText = parent?.text?.trim()?.lowercase() ?: return false
        return parentText.startsWith("feature:")
    }

    /**
     * Create run marker info for a scenario block.
     */
    @Suppress("SpreadOperator") // Required by IntelliJ API for ExecutorAction
    private fun createScenarioRunInfo(element: PsiElement): Info {
        val scenarioName = extractName(element, "scenario:")
        return Info(
            AllIcons.RunConfigurations.TestState.Run,
            { "Run scenario '$scenarioName'" },
            *ExecutorAction.getActions(0),
        )
    }

    /**
     * Create run marker info for a feature block.
     */
    @Suppress("SpreadOperator") // Required by IntelliJ API for ExecutorAction
    private fun createFeatureRunInfo(element: PsiElement): Info {
        val featureName = extractName(element, "feature:")
        return Info(
            AllIcons.RunConfigurations.TestState.Run_run,
            { "Run all scenarios in feature '$featureName'" },
            *ExecutorAction.getActions(0),
        )
    }

    /**
     * Extract the name from a block definition line.
     * E.g., "scenario: My Test" -> "My Test"
     */
    private fun extractName(element: PsiElement, prefix: String): String {
        val parent = element.parent
        val parentText = parent?.text?.trim() ?: return "Unknown"

        // Find the line containing this element
        val lines = parentText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.lowercase().startsWith(prefix)) {
                val name = trimmed.substring(prefix.length).trim()
                // Remove quotes if present
                return name.removeSurrounding("\"").removeSurrounding("'")
            }
        }

        return "Unknown"
    }
}
