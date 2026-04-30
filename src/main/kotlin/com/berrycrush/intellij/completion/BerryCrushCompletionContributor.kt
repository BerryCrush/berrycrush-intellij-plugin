package com.berrycrush.intellij.completion

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
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
