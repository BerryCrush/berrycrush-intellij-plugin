package com.berrycrush.intellij.completion

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.berrycrush.intellij.psi.BerryCrushIncludeElement
import com.berrycrush.intellij.psi.BerryCrushParametersBlockElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
import com.berrycrush.intellij.reference.BerryCrushFragmentReference
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext

/**
 * Completion contributor for BerryCrush language.
 *
 * Provides completion for keywords, directives, and assertion conditions.
 */
class BerryCrushCompletionContributor : CompletionContributor() {

    init {
        // Add completions for BerryCrush files
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().withLanguage(BerryCrushLanguage),
            BerryCrushCompletionProvider()
        )
    }
}

/**
 * Provides completion items for BerryCrush keywords and directives.
 */
class BerryCrushCompletionProvider : CompletionProvider<CompletionParameters>() {

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val position = parameters.position
        val text = position.containingFile.text
        val offset = parameters.offset

        // Check if we're typing a variable interpolation ${...}
        if (isInVariableInterpolation(text, offset)) {
            addVariableInterpolationCompletions(position.containingFile, result)
            return
        }

        // Check if we're inside a scenario/feature parameters block
        val parametersBlock = PsiTreeUtil.getParentOfType(position, BerryCrushParametersBlockElement::class.java)
        if (parametersBlock != null) {
            addScenarioParameterCompletions(result)
            return
        }

        // Check if we're in position to add a parameters block
        val scenarioElement = PsiTreeUtil.getParentOfType(position, BerryCrushScenarioElement::class.java)
        if (scenarioElement != null && isInParametersBlockStartPosition(text, offset, scenarioElement)) {
            addParametersBlockKeyword(result)
        }

        // Check if we're inside an include directive's parameter block
        val includeElement = PsiTreeUtil.getParentOfType(position, BerryCrushIncludeElement::class.java)
        if (includeElement != null && isInParameterBlockPosition(text, offset, includeElement)) {
            addParameterCompletions(includeElement, result)
            return
        }

        // Determine context based on position in the file
        val lineStart = findLineStart(text, offset)
        val lineText = text.substring(lineStart, offset).trimStart()
        val indentLevel = countIndent(text.substring(lineStart, offset))

        when {
            // Top level - block keywords
            indentLevel == 0 && lineText.isEmpty() -> {
                addBlockKeywords(result)
            }
            // After step keyword - directives
            lineText.matches(Regex("^(given|when|then|and|but)\\s.*")) -> {
                // No additional completions needed for step descriptions
            }
            // Inside step - directives
            indentLevel >= 2 && lineText.isEmpty() -> {
                addDirectives(result)
            }
            // After 'assert' - conditions
            lineText.startsWith("assert ") || lineText.startsWith("assert") -> {
                addConditions(result)
            }
            // General - all keywords
            else -> {
                addAllKeywords(result)
            }
        }
    }

    /**
     * Check if cursor is inside a variable interpolation: ${...}
     */
    private fun isInVariableInterpolation(text: String, offset: Int): Boolean {
        // Look backwards for ${
        var pos = offset - 1
        while (pos >= 1) {
            if (text[pos] == '}') return false // Already closed
            if (text[pos - 1] == '$' && text[pos] == '{') return true
            if (text[pos] == '\n') return false // Don't cross lines
            pos--
        }
        return false
    }

    /**
     * Add completions for variable interpolation prefixes and known variables.
     */
    private fun addVariableInterpolationCompletions(file: PsiFile, result: CompletionResultSet) {
        // Add prefix suggestions
        VARIABLE_PREFIXES.forEach { (prefix, description) ->
            result.addElement(
                LookupElementBuilder.create(prefix)
                    .withIcon(AllIcons.Nodes.Variable)
                    .withTypeText(description)
            )
        }

        // Add extracted variables from the file (context variables)
        val extractedVars = extractExtractedVariables(file)
        extractedVars.forEach { varName ->
            result.addElement(
                LookupElementBuilder.create(varName)
                    .withIcon(AllIcons.Nodes.Variable)
                    .withTypeText("context variable")
            )
            result.addElement(
                LookupElementBuilder.create("context.$varName")
                    .withIcon(AllIcons.Nodes.Variable)
                    .withTypeText("context variable")
            )
        }
    }

    /**
     * Extract variable names from extract directives in the file.
     */
    private fun extractExtractedVariables(file: PsiFile): Set<String> {
        val text = file.text
        // Pattern: extract varName = ...
        val pattern = Regex("""extract\s+(\w+)\s*=""")
        return pattern.findAll(text)
            .map { it.groupValues[1] }
            .toSet()
    }

    /**
     * Check if the cursor is in position to add a parameters: block.
     */
    private fun isInParametersBlockStartPosition(
        text: String,
        offset: Int,
        scenarioElement: BerryCrushScenarioElement
    ): Boolean {
        // Check if we're on the line after scenario:
        val scenarioTextOffset = scenarioElement.textRange.startOffset
        val scenarioFirstLine = text.indexOf('\n', scenarioTextOffset)
        if (scenarioFirstLine == -1 || offset <= scenarioFirstLine) return false

        // Check indentation
        val lineStart = findLineStart(text, offset)
        val indent = countIndent(text.substring(lineStart, offset))
        return indent >= 1 // Should be indented under scenario
    }

    /**
     * Add parameters: keyword completion.
     */
    private fun addParametersBlockKeyword(result: CompletionResultSet) {
        result.addElement(
            LookupElementBuilder.create("parameters:")
                .withIcon(AllIcons.Nodes.Property)
                .withTypeText("parameters block")
                .withBoldness(true)
        )
    }

    /**
     * Add known parameter name completions for scenario/feature parameters blocks.
     */
    private fun addScenarioParameterCompletions(result: CompletionResultSet) {
        KNOWN_PARAMETERS.forEach { (paramName, description) ->
            result.addElement(
                LookupElementBuilder.create("$paramName: ")
                    .withIcon(AllIcons.Nodes.Property)
                    .withTypeText(description)
                    .withPresentableText(paramName)
            )
        }
    }

    /**
     * Check if the cursor is in a position suitable for parameter completion
     * (after the include line, in an indented position).
     */
    private fun isInParameterBlockPosition(text: String, offset: Int, includeElement: BerryCrushIncludeElement): Boolean {
        // Check if we're after the include line (not on the same line)
        val includeEndOffset = includeElement.textRange.startOffset + includeElement.text.indexOf('\n')
        if (offset <= includeEndOffset) return false

        // Check indentation - parameters should be indented
        val lineStart = findLineStart(text, offset)
        val indent = countIndent(text.substring(lineStart, offset))
        return indent >= 3 // Parameters are typically indented 3+ levels
    }

    /**
     * Add parameter name completions based on fragment's expected variables.
     */
    private fun addParameterCompletions(includeElement: BerryCrushIncludeElement, result: CompletionResultSet) {
        val fragmentName = includeElement.fragmentName ?: return
        val project = includeElement.project

        // Find the fragment definition
        val fragmentFile = BerryCrushFragmentReference.findFragmentByName(project, fragmentName)
            as? PsiFile ?: return

        // Extract expected parameters from fragment
        val expectedParams = extractExpectedParameters(fragmentFile)
        val providedParams = includeElement.parameterNames

        // Suggest parameters that haven't been provided yet
        expectedParams.subtract(providedParams).forEach { paramName ->
            result.addElement(
                LookupElementBuilder.create("$paramName: ")
                    .withIcon(AllIcons.Nodes.Parameter)
                    .withTypeText("parameter")
                    .withPresentableText(paramName)
            )
        }
    }

    /**
     * Extract expected parameter names from a fragment file.
     * Looks for {{variableName}} patterns in the fragment content.
     */
    private fun extractExpectedParameters(fragmentFile: PsiFile): Set<String> {
        val text = fragmentFile.text
        val pattern = Regex("""\{\{(\w+)\}\}""")
        return pattern.findAll(text)
            .map { it.groupValues[1] }
            .toSet()
    }

    private fun findLineStart(text: String, offset: Int): Int {
        var pos = offset - 1
        while (pos >= 0 && text[pos] != '\n') {
            pos--
        }
        return pos + 1
    }

    private fun countIndent(linePrefix: String): Int {
        var count = 0
        for (c in linePrefix) {
            if (c == ' ') count++
            else break
        }
        return count / 2
    }

    private fun addBlockKeywords(result: CompletionResultSet) {
        BLOCK_KEYWORDS.forEach { keyword ->
            result.addElement(
                LookupElementBuilder.create(keyword)
                    .withTypeText("block")
                    .withBoldness(true)
            )
        }
    }

    private fun addDirectives(result: CompletionResultSet) {
        DIRECTIVES.forEach { (keyword, description) ->
            result.addElement(
                LookupElementBuilder.create(keyword)
                    .withTypeText(description)
            )
        }
    }

    private fun addConditions(result: CompletionResultSet) {
        CONDITIONS.forEach { (keyword, description) ->
            result.addElement(
                LookupElementBuilder.create(keyword)
                    .withTypeText(description)
            )
        }
    }

    private fun addAllKeywords(result: CompletionResultSet) {
        addBlockKeywords(result)
        STEP_KEYWORDS.forEach { keyword ->
            result.addElement(
                LookupElementBuilder.create(keyword)
                    .withTypeText("step")
            )
        }
        addDirectives(result)
    }

    companion object {
        private val BLOCK_KEYWORDS = listOf(
            "feature:",
            "scenario:",
            "outline:",
            "fragment:",
            "parameters:",
            "background:",
            "examples:",
        )

        private val STEP_KEYWORDS = listOf(
            "given ",
            "when ",
            "then ",
            "and ",
            "but ",
        )

        private val DIRECTIVES = listOf(
            "call " to "API call",
            "assert " to "assertion",
            "extract " to "variable extraction",
            "include " to "fragment include",
            "body:" to "request body",
            "if " to "conditional",
            "else if " to "conditional",
            "else" to "conditional",
            "fail " to "fail with message",
        )

        private val CONDITIONS = listOf(
            "status " to "HTTP status code",
            "header " to "response header",
            "contains " to "body contains text",
            "schema" to "JSON schema validation",
            "responseTime " to "response time limit",
            "exists" to "value exists",
            "not " to "negation",
        )

        /**
         * Known parameter names for scenario/feature parameters blocks.
         */
        private val KNOWN_PARAMETERS = listOf(
            "timeout" to "request timeout in milliseconds",
            "baseUrl" to "base URL for API calls",
            "environment" to "execution environment",
            "shareVariablesAcrossScenarios" to "share extracted variables",
            "logRequests" to "log HTTP requests",
            "logResponses" to "log HTTP responses",
            "strictSchemaValidation" to "strict JSON schema validation",
            "followRedirects" to "follow HTTP redirects",
            "multiTestSequentialCount" to "sequential test count",
            "multiTestConcurrentCount" to "concurrent test count",
            "autoAssertions.enabled" to "enable auto assertions",
            "autoAssertions.statusCode" to "auto assert status code",
            "autoAssertions.contentType" to "auto assert content type",
            "autoAssertions.schema" to "auto assert schema",
        )

        /**
         * Variable interpolation prefixes.
         */
        private val VARIABLE_PREFIXES = listOf(
            "env." to "environment variable",
            "context." to "context variable",
            "param." to "parameter reference",
        )
    }
}
