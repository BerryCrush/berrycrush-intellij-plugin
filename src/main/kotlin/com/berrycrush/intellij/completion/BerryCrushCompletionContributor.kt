package com.berrycrush.intellij.completion

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.berrycrush.intellij.psi.BerryCrushIncludeElement
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
    }
}
