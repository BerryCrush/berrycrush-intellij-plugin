package com.berrycrush.intellij.refactoring.variable

import com.berrycrush.intellij.psi.BerryCrushFile
import com.berrycrush.intellij.psi.BerryCrushParameterEntryElement
import com.berrycrush.intellij.psi.BerryCrushVariableInterpolationElement
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.search.SearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenamePsiElementProcessor

/**
 * Handles renaming of variable placeholders within scenario scope.
 *
 * Variables in BerryCrush:
 * - Definition: `extract $.id => petId`
 * - Usage: `{{petId}}`
 * - Variable interpolation: `${varName}`, `${context.varName}`, `${param.paramName}`
 * - Parameter definition: `paramName: value` in parameters block
 *
 * Scope: Variables are scoped to the scenario they're defined in.
 * Renaming updates all occurrences (definition and usages) within the same scenario.
 */
class VariableRenameProcessor : RenamePsiElementProcessor() {

    override fun canProcessElement(element: PsiElement): Boolean {
        if (element.containingFile !is BerryCrushFile) return false

        // Check if it's a parameter entry element
        if (element is BerryCrushParameterEntryElement) return true

        // Check if it's a variable interpolation element
        if (element is BerryCrushVariableInterpolationElement) return true

        val text = element.text
        val lineText = getLineText(element)

        // Check for variable interpolation syntax ${...}
        if (isVariableInterpolation(text)) return true

        return isVariableUsage(text) || isVariableDefinition(lineText)
    }

    override fun prepareRenaming(
        element: PsiElement,
        newName: String,
        allRenames: MutableMap<PsiElement, String>,
        scope: SearchScope,
    ) {
        val file = element.containingFile ?: return

        // Handle parameter entry rename
        if (element is BerryCrushParameterEntryElement) {
            val paramName = element.parameterName ?: return
            // Find all ${param.paramName} references
            findParameterReferences(file, paramName)
                .filter { it !== element }
                .forEach { ref -> allRenames[ref] = "\${param.$newName}" }
            return
        }

        // Handle variable interpolation rename
        if (element is BerryCrushVariableInterpolationElement) {
            val variableName = element.variableName ?: return
            val refType = element.refType

            when (refType) {
                BerryCrushVariableInterpolationElement.RefType.PARAM -> {
                    // Find parameter definition and other references
                    findParameterDefinition(file, variableName)?.let { def ->
                        allRenames[def] = newName
                    }
                    findParameterReferences(file, variableName)
                        .filter { it !== element }
                        .forEach { ref -> allRenames[ref] = "\${param.$newName}" }
                }
                BerryCrushVariableInterpolationElement.RefType.CONTEXT,
                BerryCrushVariableInterpolationElement.RefType.SHORTHAND -> {
                    // Find extract definition and variable usages
                    val scenarioScope = findScenarioScope(element)
                    findVariableUsages(file, variableName, scenarioScope)
                        .filter { it !== element }
                        .forEach { usage -> allRenames[usage] = newName }
                    // Also update variable interpolations
                    findVariableInterpolations(file, variableName, scenarioScope)
                        .filter { it !== element }
                        .forEach { usage ->
                            val newText = if (usage.text.startsWith("\${context.")) {
                                "\${context.$newName}"
                            } else {
                                "\${$newName}"
                            }
                            allRenames[usage] = newText
                        }
                }
                BerryCrushVariableInterpolationElement.RefType.ENV -> {
                    // Environment variables - no rename support
                }
            }
            return
        }

        // Legacy variable handling
        val variableName = extractVariableName(element) ?: return
        val scenarioScope = findScenarioScope(element)

        // Find all variable usages in the scenario scope
        findVariableUsages(file, variableName, scenarioScope)
            .filter { it !== element }
            .forEach { usage -> allRenames[usage] = newName }

        // Also update variable interpolations
        findVariableInterpolations(file, variableName, scenarioScope)
            .filter { it !== element }
            .forEach { usage ->
                val newText = if (usage.text.startsWith("\${context.")) {
                    "\${context.$newName}"
                } else {
                    "\${$newName}"
                }
                allRenames[usage] = newText
            }
    }

    /**
     * Find parameter definition by name.
     */
    private fun findParameterDefinition(file: PsiFile, paramName: String): PsiElement? {
        return PsiTreeUtil.findChildrenOfType(file, BerryCrushParameterEntryElement::class.java)
            .firstOrNull { it.parameterName == paramName }
    }

    /**
     * Find all ${param.paramName} references in the file.
     */
    private fun findParameterReferences(file: PsiFile, paramName: String): List<PsiElement> {
        val text = file.text
        val usages = mutableListOf<PsiElement>()

        val pattern = Regex("""\$\{param\.${Regex.escape(paramName)}}""")
        pattern.findAll(text).forEach { match ->
            file.findElementAt(match.range.first)?.let { usages.add(it) }
        }

        return usages
    }

    /**
     * Find all variable interpolations (${varName}, ${context.varName}) in scope.
     */
    private fun findVariableInterpolations(
        file: PsiFile,
        variableName: String,
        scope: TextRange?
    ): List<PsiElement> {
        val text = file.text
        val usages = mutableListOf<PsiElement>()

        // Find ${variableName} usages
        val pattern1 = Regex("""\$\{${Regex.escape(variableName)}}""")
        pattern1.findAll(text).forEach { match ->
            if (scope == null || scope.contains(match.range.first)) {
                file.findElementAt(match.range.first)?.let { usages.add(it) }
            }
        }

        // Find ${context.variableName} usages
        val pattern2 = Regex("""\$\{context\.${Regex.escape(variableName)}}""")
        pattern2.findAll(text).forEach { match ->
            if (scope == null || scope.contains(match.range.first)) {
                file.findElementAt(match.range.first)?.let { usages.add(it) }
            }
        }

        return usages
    }

    /**
     * Check if text is a variable interpolation.
     */
    private fun isVariableInterpolation(text: String): Boolean =
        VARIABLE_INTERPOLATION_PATTERN.containsMatchIn(text)

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
        private val VARIABLE_INTERPOLATION_PATTERN = Regex("""\$\{(?:env\.|context\.|param\.)?[^}]+}""")
    }
}
