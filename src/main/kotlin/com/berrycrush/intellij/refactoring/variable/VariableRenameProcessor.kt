package com.berrycrush.intellij.refactoring.variable

import com.berrycrush.intellij.psi.BerryCrushFile
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenamePsiElementProcessor

/**
 * Handles renaming of variable placeholders within scenario scope.
 *
 * Variables in BerryCrush:
 * - Definition: `extract $.id => petId`
 * - Usage: `{{petId}}`
 *
 * Scope: Variables are scoped to the scenario they're defined in.
 * Renaming updates all occurrences (definition and usages) within the same scenario.
 */
class VariableRenameProcessor : RenamePsiElementProcessor() {

    override fun canProcessElement(element: PsiElement): Boolean {
        if (element.containingFile !is BerryCrushFile) return false

        val text = element.text
        val lineText = getLineText(element)

        return isVariableUsage(text) || isVariableDefinition(lineText)
    }

    override fun prepareRenaming(
        element: PsiElement,
        newName: String,
        allRenames: MutableMap<PsiElement, String>,
        scope: SearchScope,
    ) {
        val variableName = extractVariableName(element) ?: return
        val scenarioScope = findScenarioScope(element)
        val file = element.containingFile ?: return

        // Find all variable usages in the scenario scope
        findVariableUsages(file, variableName, scenarioScope)
            .filter { it !== element }
            .forEach { usage -> allRenames[usage] = newName }
    }

    /**
     * Extracts variable name from element.
     * Handles both {{varName}} and "=> varName" syntaxes.
     */
    private fun extractVariableName(element: PsiElement): String? {
        val text = element.text
        val lineText = getLineText(element)

        // From {{varName}} usage
        VARIABLE_USAGE_PATTERN.find(text)?.let { return it.groupValues[1] }

        // From "=> varName" definition
        VARIABLE_DEF_PATTERN.find(lineText)?.let { return it.groupValues[1] }

        return null
    }

    /**
     * Finds the scenario scope (text range) containing this element.
     * Returns null if element is at file scope.
     */
    private fun findScenarioScope(element: PsiElement): TextRange? {
        val file = element.containingFile ?: return null
        val text = file.text
        val elementOffset = element.textOffset

        // Find scenario boundaries by looking for "scenario:" or "Scenario:" keywords
        var scenarioStart = 0
        var scenarioEnd = text.length

        // Find preceding scenario start
        val beforeElement = text.substring(0, elementOffset)
        val lastScenarioMatch = SCENARIO_PATTERN.findAll(beforeElement).lastOrNull()
        if (lastScenarioMatch != null) {
            scenarioStart = lastScenarioMatch.range.first
        }

        // Find following scenario start (which ends current scenario)
        val afterElement = text.substring(elementOffset)
        val nextScenarioMatch = SCENARIO_PATTERN.find(afterElement)
        if (nextScenarioMatch != null) {
            scenarioEnd = elementOffset + nextScenarioMatch.range.first
        }

        return TextRange(scenarioStart, scenarioEnd)
    }

    /**
     * Finds all occurrences of a variable in the file within the given scope.
     */
    private fun findVariableUsages(
        file: PsiFile,
        variableName: String,
        scope: TextRange?,
    ): List<PsiElement> {
        val text = file.text
        val usages = mutableListOf<PsiElement>()

        // Find {{variableName}} usages
        val usagePattern = Regex("""\{\{${Regex.escape(variableName)}}}""")
        usagePattern.findAll(text).forEach { match ->
            if (scope == null || scope.contains(match.range.first)) {
                file.findElementAt(match.range.first)?.let { usages.add(it) }
            }
        }

        // Find "=> variableName" definitions
        val defPattern = Regex("""=>\s*${Regex.escape(variableName)}(?:\s|$)""")
        defPattern.findAll(text).forEach { match ->
            if (scope == null || scope.contains(match.range.first)) {
                // Position at the variable name, not the =>
                val varStart = match.range.first + match.value.indexOf(variableName)
                file.findElementAt(varStart)?.let { usages.add(it) }
            }
        }

        return usages
    }

    private fun isVariableUsage(text: String): Boolean =
        VARIABLE_USAGE_PATTERN.containsMatchIn(text)

    private fun isVariableDefinition(lineText: String): Boolean =
        VARIABLE_DEF_PATTERN.containsMatchIn(lineText)

    private fun getLineText(element: PsiElement): String {
        val document = element.containingFile?.viewProvider?.document ?: return ""
        val offset = element.textOffset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        return document.getText(TextRange(lineStart, lineEnd))
    }

    companion object {
        private val VARIABLE_USAGE_PATTERN = Regex("""\{\{([^}]+)}}""")
        private val VARIABLE_DEF_PATTERN = Regex("""=>\s*([a-zA-Z_][a-zA-Z0-9_]*)""")
        private val SCENARIO_PATTERN = Regex("""(?m)^\s*[Ss]cenario:\s*""")
    }
}
