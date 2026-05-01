package com.berrycrush.intellij.run

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil

/**
 * Produces BerryCrush run configurations from context elements.
 *
 * This producer is invoked when:
 * - User clicks on gutter run icons
 * - User right-clicks and selects "Run" from context menu
 * - User presses Ctrl+Shift+F10 (Run from cursor)
 *
 * It creates a BerryCrushRunConfiguration populated with the scenario file path
 * and optionally the scenario/feature name based on the context.
 */
class BerryCrushRunConfigurationProducer : LazyRunConfigurationProducer<BerryCrushRunConfiguration>() {

    override fun getConfigurationFactory(): ConfigurationFactory {
        return BerryCrushConfigurationType.getInstance().configurationFactories[0]
    }

    override fun setupConfigurationFromContext(
        configuration: BerryCrushRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val element = context.psiLocation ?: return false
        val file = element.containingFile ?: return false

        // Only handle BerryCrush files (.scenario)
        val fileName = file.name
        if (!fileName.endsWith(".scenario", ignoreCase = true)) {
            return false
        }

        val virtualFile = file.virtualFile ?: return false
        configuration.scenarioFilePath = virtualFile.path

        // Try to find the scenario or feature at the cursor position
        val scenarioInfo = findScenarioAtElement(element)
        val featureInfo = findFeatureAtElement(element)

        when {
            scenarioInfo != null -> {
                configuration.scenarioName = scenarioInfo.name
                configuration.name = "Scenario: ${scenarioInfo.name}"
            }
            featureInfo != null -> {
                configuration.featureName = featureInfo.name
                configuration.name = "Feature: ${featureInfo.name}"
            }
            else -> {
                configuration.name = "All scenarios in ${file.name}"
            }
        }

        sourceElement.set(element)
        return true
    }

    override fun isConfigurationFromContext(
        configuration: BerryCrushRunConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val element = context.psiLocation ?: return false
        val file = element.containingFile ?: return false
        val virtualFile = file.virtualFile ?: return false

        // Check if same file
        if (configuration.scenarioFilePath != virtualFile.path) {
            return false
        }

        // Check scenario/feature match
        val scenarioInfo = findScenarioAtElement(element)
        val featureInfo = findFeatureAtElement(element)

        return when {
            scenarioInfo != null -> configuration.scenarioName == scenarioInfo.name
            featureInfo != null -> configuration.featureName == featureInfo.name
            else -> configuration.scenarioName == null && configuration.featureName == null
        }
    }

    /**
     * Find the scenario block containing the given element.
     */
    private fun findScenarioAtElement(element: PsiElement): BlockInfo? {
        return findBlockAtElement(element, "scenario:")
    }

    /**
     * Find the feature block containing the given element.
     */
    private fun findFeatureAtElement(element: PsiElement): BlockInfo? {
        return findBlockAtElement(element, "feature:")
    }

    /**
     * Find a block of the given type containing the element.
     *
     * For plain text files (.scenario), we search by line position since
     * there's no semantic PSI structure.
     */
    private fun findBlockAtElement(element: PsiElement, prefix: String): BlockInfo? {
        val file = element.containingFile ?: return null
        val fileText = file.text
        val elementOffset = element.textOffset

        // Find the end of the current line to include it in our search
        val endOfLine = fileText.indexOf('\n', elementOffset).let {
            if (it == -1) fileText.length else it
        }

        // Get all lines up to and including the current line
        val textUpToCurrentLine = fileText.substring(0, endOfLine)
        val lines = textUpToCurrentLine.lines()

        // Search backwards from current line to find the nearest block definition
        for (i in lines.lastIndex downTo 0) {
            val line = lines[i].trim()
            val lineLower = line.lowercase()
            
            if (lineLower.startsWith(prefix)) {
                val name = extractBlockNameFromLine(line, prefix)
                if (name != null) {
                    return BlockInfo(name)
                }
            }
            
            // Stop if we hit another block type (we're inside a different block)
            val isOtherBlockType = (lineLower.startsWith("scenario:") && prefix != "scenario:") ||
                                   (lineLower.startsWith("feature:") && prefix != "feature:")
            if (isOtherBlockType) return null
        }
        return null
    }

    /**
     * Extract the name from a block definition line.
     */
    private fun extractBlockNameFromLine(line: String, prefix: String): String? {
        val trimmed = line.trim()
        if (!trimmed.lowercase().startsWith(prefix)) return null

        val name = trimmed.substring(prefix.length).trim()
            .removeSurrounding("\"")
            .removeSurrounding("'")
        return name.takeIf { it.isNotEmpty() }
    }

    /**
     * Information about a scenario or feature block.
     */
    private data class BlockInfo(val name: String)
}
