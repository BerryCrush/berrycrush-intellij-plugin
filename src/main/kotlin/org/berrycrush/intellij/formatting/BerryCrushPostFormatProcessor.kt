package org.berrycrush.intellij.formatting

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import org.berrycrush.intellij.language.BerryCrushLanguage
import kotlin.math.max

/**
 * Post-format processor for BerryCrush files.
 *
 * Because BerryCrush uses a flat AST structure (all elements as siblings),
 * the standard Block-based formatting cannot properly handle nested indentation.
 * This processor performs full reformatting including:
 * - Context-aware indentation
 * - Table column alignment
 * - Spacing normalization
 */
class BerryCrushPostFormatProcessor : PostFormatProcessor {
    companion object {
        private const val INDENT_SIZE = 2

        // Step keywords
        private val STEP_KEYWORDS = setOf("given", "when", "then", "and", "but")

        // Directives
        private val DIRECTIVES =
            setOf(
                "call",
                "assert",
                "extract",
                "include",
                "if",
                "else",
                "webhook",
                "bodyfile",
            )

        private val ROOT_BLOCK_PREFIXES = setOf("feature:", "fragment:", "scenario:", "outline:")
    }

    override fun processElement(
        source: PsiElement,
        settings: CodeStyleSettings,
    ): PsiElement = source

    override fun processText(
        source: PsiFile,
        rangeToReformat: TextRange,
        settings: CodeStyleSettings,
    ): TextRange {
        // Only process BerryCrush files
        if (source.language != BerryCrushLanguage) {
            return rangeToReformat
        }

        val document = source.viewProvider.document ?: return rangeToReformat
        val text = document.text

        // Perform full reformatting
        val formattedText = reformatDocument(text)

        if (formattedText != text) {
            document.setText(formattedText)
            return TextRange(0, formattedText.length)
        }

        return rangeToReformat
    }

    /**
     * Reformat the entire document with proper indentation and alignment.
     * Uses continue statements to efficiently handle different line types
     * (empty lines, table rows, normal lines) without deep nesting.
     */
    @Suppress("LoopWithTooManyJumpStatements")
    private fun reformatDocument(text: String): String {
        val lines = text.lines()
        val result = mutableListOf<String>()

        var context = FormattingContext()
        val tableLines = mutableListOf<String>()
        var tableIndent = 0
        var inTable = false
        var inDetachedRootCommentBlock = false

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            val leadingSpaces =
                line.indexOfFirst { !it.isWhitespace() }.let {
                    if (it == -1) 0 else it
                }

            // Handle empty lines
            if (trimmed.isEmpty()) {
                if (inTable) {
                    // End table and align it
                    result.addAll(alignTableColumns(tableLines, tableIndent))
                    tableLines.clear()
                    inTable = false
                }
                result.add("")
                inDetachedRootCommentBlock = false
                context = context.copy(previousLineBlank = true)
                continue
            }

            // Handle table rows
            if (trimmed.startsWith("|")) {
                if (!inTable) {
                    inTable = true
                    tableIndent = context.currentIndent + INDENT_SIZE
                }
                tableLines.add(trimmed)
                continue
            }

            // End table if we were in one
            if (inTable) {
                result.addAll(alignTableColumns(tableLines, tableIndent))
                tableLines.clear()
                inTable = false
            }

            // Detached comments before root blocks should stay at root indentation.
            if (trimmed.startsWith("#")) {
                val nextStructural =
                    lines
                        .drop(index + 1)
                        .firstOrNull {
                            val nextTrimmed = it.trim()
                            nextTrimmed.isNotEmpty() && !nextTrimmed.startsWith("#")
                        }
                val nextTrimmed = nextStructural?.trim().orEmpty()
                val nextIsRootBlock = ROOT_BLOCK_PREFIXES.any { nextTrimmed.lowercase().startsWith(it) }
                val shouldRootIndent =
                    nextIsRootBlock &&
                        (inDetachedRootCommentBlock || context.previousLineBlank || !context.inDirective)

                if (shouldRootIndent) {
                    result.add(formatLine(trimmed, 0))
                    inDetachedRootCommentBlock = true
                    context = context.copy(currentIndent = 0, previousLineBlank = false)
                    continue
                }
            } else {
                inDetachedRootCommentBlock = false
            }

            // Calculate indent and update context
            val (indent, newContext) = calculateIndentAndContext(trimmed, leadingSpaces, context)
            context = newContext

            // Format the line with proper indent and spacing
            val formattedLine = formatLine(trimmed, indent)
            result.add(formattedLine)
        }

        // Handle any remaining table
        if (inTable) {
            result.addAll(alignTableColumns(tableLines, tableIndent))
        }

        return result.joinToString("\n")
    }

    /**
     * Calculate the proper indent level and update context based on line content.
     * This method is inherently complex due to the number of BerryCrush language constructs
     * that need different indentation rules.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun calculateIndentAndContext(
        trimmed: String,
        leadingSpaces: Int,
        context: FormattingContext,
    ): Pair<Int, FormattingContext> {
        val lower = trimmed.lowercase()
        val firstWord = lower.split(Regex("\\s+|:")).firstOrNull() ?: ""
        val isTopLevelByInput = leadingSpaces == 0

        val resetForRoot = getRootContext(isTopLevelByInput, lower, context)

        return when {
            // Feature/fragment at root level
            lower.startsWith("feature:") || lower.startsWith("fragment:") -> featureContext(resetForRoot, lower)
            // Scenario/outline
            lower.startsWith("scenario:") || lower.startsWith("outline:") -> scenarioContext(lower, resetForRoot, isTopLevelByInput)
            // Background
            lower.startsWith("background:") -> backgroundContext(resetForRoot)
            // Examples - valid under outline. If found outside, keep deterministic indentation.
            lower.startsWith("examples:") -> examplesContext(resetForRoot)
            // Parameters block opener
            lower.startsWith("parameters:") -> parametersContext(resetForRoot)
            // Tags (@ at start)
            lower.startsWith("@") -> {
                val indent =
                    if (resetForRoot.inFeature && !resetForRoot.inScenario && !resetForRoot.inBackground) {
                        INDENT_SIZE
                    } else {
                        resetForRoot.currentIndent
                    }
                indent to resetForRoot
            }

            // Comments
            lower.startsWith("#") -> resetForRoot.currentIndent to resetForRoot

            // Step keywords
            firstWord in STEP_KEYWORDS -> stepContext(resetForRoot)
            // Directives (call, assert, include, etc.)
            // Multiple directives at the same level should have the same indentation
            isDirective(lower) -> directiveContext(resetForRoot, lower)

            // Map entries (key: value and key:)
            isMapEntry(trimmed) -> mapEntryContext(resetForRoot, trimmed)

            // Body content (triple quotes or JSON)
            lower.startsWith("'''") ||
                lower.startsWith("\"\"\"") ||
                lower.startsWith("{") ||
                lower.startsWith("}") -> {
                (resetForRoot.currentIndent + INDENT_SIZE) to resetForRoot.copy(previousLineBlank = false)
            }

            // Default - maintain context indent
            else -> {
                resetForRoot.currentIndent to resetForRoot.copy(previousLineBlank = false)
            }
        }
    }

    private fun mapEntryContext(
        resetForRoot: FormattingContext,
        trimmed: String,
    ): Pair<Int, FormattingContext> {
        val parentIndent =
            when {
                resetForRoot.mapIndentStack.isNotEmpty() -> resetForRoot.mapIndentStack.last()
                resetForRoot.inDirective -> resetForRoot.directiveIndent
                resetForRoot.inParametersBlock -> resetForRoot.currentIndent
                else -> resetForRoot.currentIndent
            }
        val indent = parentIndent + INDENT_SIZE
        val hasValue = hasInlineValue(trimmed)
        val nextStack =
            if (hasValue) {
                resetForRoot.mapIndentStack
            } else {
                resetForRoot.mapIndentStack + indent
            }
        return indent to
            resetForRoot.copy(
                currentIndent = indent,
                mapIndentStack = nextStack,
                previousLineBlank = false,
            )
    }

    private fun directiveContext(
        resetForRoot: FormattingContext,
        lower: String,
    ): Pair<Int, FormattingContext> {
        val isConditional = isConditionalDirective(lower)
        val isAssert = isAssertDirective(lower)
        val isElse = lower == "else" || lower.startsWith("else ") || lower == "else:"

        // If already at directive level, stay there
        val baseIndent =
            if (resetForRoot.inDirective) {
                resetForRoot.directiveIndent
            } else {
                // Calculate directive level based on hierarchy
                when {
                    // Under a step - directive level (one deeper than step)
                    resetForRoot.inStep -> resetForRoot.currentIndent + INDENT_SIZE
                    // Directly under fragment - step level
                    resetForRoot.inFragment -> INDENT_SIZE
                    // Directly under standalone scenario/background - step level
                    resetForRoot.inScenario || resetForRoot.inBackground -> resetForRoot.containerIndent + INDENT_SIZE
                    else -> INDENT_SIZE
                }
            }

        val conditionalBaseIndent =
            when {
                isConditional && resetForRoot.inConditionalBranch -> resetForRoot.conditionalBaseIndent
                isConditional -> baseIndent
                else -> resetForRoot.conditionalBaseIndent
            }

        val indent =
            when {
                isConditional && isElse && resetForRoot.inConditionalBranch -> conditionalBaseIndent
                isConditional -> baseIndent
                resetForRoot.inConditionalBranch && isAssert -> resetForRoot.directiveIndent
                else -> baseIndent
            }

        val inConditionalBranch =
            (isConditional || (resetForRoot.inConditionalBranch && isAssert)) &&
                !(resetForRoot.inConditionalBranch && !isConditional && !isAssert)

        val nextDirectiveIndent = if (inConditionalBranch) indent + INDENT_SIZE else indent

        val newContext =
            resetForRoot.copy(
                inDirective = true,
                inParametersBlock = false,
                mapIndentStack = emptyList(),
                currentIndent = indent,
                directiveIndent = nextDirectiveIndent,
                inConditionalBranch = inConditionalBranch,
                conditionalBaseIndent = if (inConditionalBranch) conditionalBaseIndent else 0,
                previousLineBlank = false,
            )
        return indent to newContext
    }

    private fun stepContext(resetForRoot: FormattingContext): Pair<Int, FormattingContext> {
        val baseIndent =
            when {
                resetForRoot.inScenario || resetForRoot.inBackground -> resetForRoot.containerIndent + INDENT_SIZE
                resetForRoot.inFragment -> INDENT_SIZE
                else -> 0
            }
        val newContext =
            resetForRoot.copy(
                inStep = true,
                inDirective = false,
                inExamples = false,
                inParametersBlock = false,
                mapIndentStack = emptyList(),
                currentIndent = baseIndent,
                directiveIndent = 0,
                inConditionalBranch = false,
                conditionalBaseIndent = 0,
                previousLineBlank = false,
            )
        return baseIndent to newContext
    }

    private fun parametersContext(resetForRoot: FormattingContext): Pair<Int, FormattingContext> {
        val indent =
            when {
                resetForRoot.inScenario || resetForRoot.inBackground -> resetForRoot.containerIndent + INDENT_SIZE
                resetForRoot.inFeature -> INDENT_SIZE
                else -> resetForRoot.currentIndent
            }
        val newContext =
            resetForRoot.copy(
                inParametersBlock = true,
                inDirective = false,
                inExamples = false,
                mapIndentStack = emptyList(),
                currentIndent = indent,
                inConditionalBranch = false,
                conditionalBaseIndent = 0,
                previousLineBlank = false,
            )
        return indent to newContext
    }

    private fun examplesContext(resetForRoot: FormattingContext): Pair<Int, FormattingContext> {
        val indent =
            when {
                resetForRoot.inScenario && resetForRoot.inOutline -> resetForRoot.containerIndent + INDENT_SIZE
                resetForRoot.inScenario -> resetForRoot.containerIndent + INDENT_SIZE
                else -> 0
            }
        val newContext =
            resetForRoot.copy(
                inExamples = true,
                inStep = false,
                inDirective = false,
                inParametersBlock = false,
                mapIndentStack = emptyList(),
                currentIndent = indent,
                inConditionalBranch = false,
                conditionalBaseIndent = 0,
                previousLineBlank = false,
            )
        return indent to newContext
    }

    private fun backgroundContext(resetForRoot: FormattingContext): Pair<Int, FormattingContext> {
        val indent = if (resetForRoot.inFeature) INDENT_SIZE else 0
        val newContext =
            resetForRoot.copy(
                inBackground = true,
                inScenario = false,
                inOutline = false,
                inStep = false,
                inDirective = false,
                inExamples = false,
                inParametersBlock = false,
                mapIndentStack = emptyList(),
                currentIndent = indent,
                containerIndent = indent,
                directiveIndent = 0,
                inConditionalBranch = false,
                conditionalBaseIndent = 0,
                previousLineBlank = false,
            )
        return indent to newContext
    }

    private fun scenarioContext(
        lower: String,
        resetForRoot: FormattingContext,
        isTopLevelByInput: Boolean,
    ): Pair<Int, FormattingContext> {
        val isOutline = lower.startsWith("outline:")
        val nestedUnderFeature =
            resetForRoot.inFeature && !isTopLevelByInput && !resetForRoot.previousLineBlank
        val indent = if (nestedUnderFeature) INDENT_SIZE else 0
        val newContext =
            resetForRoot.copy(
                inFeature = nestedUnderFeature,
                inScenario = true,
                inOutline = isOutline,
                inBackground = false,
                inStep = false,
                inDirective = false,
                inExamples = false,
                inParametersBlock = false,
                mapIndentStack = emptyList(),
                currentIndent = indent,
                containerIndent = indent,
                directiveIndent = 0,
                inConditionalBranch = false,
                conditionalBaseIndent = 0,
                previousLineBlank = false,
            )
        return indent to newContext
    }

    private fun featureContext(
        resetForRoot: FormattingContext,
        lower: String,
    ) = 0 to
        resetForRoot.copy(
            inFeature = lower.startsWith("feature:"),
            inFragment = lower.startsWith("fragment:"),
            inScenario = false,
            inOutline = false,
            inBackground = false,
            inStep = false,
            inDirective = false,
            inExamples = false,
            inParametersBlock = false,
            mapIndentStack = emptyList(),
            currentIndent = 0,
            containerIndent = 0,
            directiveIndent = 0,
            inConditionalBranch = false,
            conditionalBaseIndent = 0,
        )

    private fun getRootContext(
        isTopLevelByInput: Boolean,
        lower: String,
        context: FormattingContext,
    ): FormattingContext = if (isTopLevelByInput && ROOT_BLOCK_PREFIXES.any { lower.startsWith(it) }) {
        context.copy(
            inFeature = false,
            inFragment = false,
            inScenario = false,
            inOutline = false,
            inBackground = false,
            inStep = false,
            inDirective = false,
            inExamples = false,
            inParametersBlock = false,
            mapIndentStack = emptyList(),
            currentIndent = 0,
            containerIndent = 0,
            directiveIndent = 0,
            inConditionalBranch = false,
            conditionalBaseIndent = 0,
        )
    } else {
        context
    }

    /**
     * Check if the line starts with a directive keyword.
     */
    private fun isDirective(lower: String): Boolean = DIRECTIVES.any { directive ->
        lower == directive || lower == "$directive:" || lower.startsWith("$directive ") || lower.startsWith("$directive:")
    }

    private fun isConditionalDirective(lower: String): Boolean = lower == "if" ||
        lower.startsWith("if ") ||
        lower == "if:" ||
        lower == "else" ||
        lower.startsWith("else ") ||
        lower == "else:"

    private fun isAssertDirective(lower: String): Boolean = lower == "assert" ||
        lower.startsWith("assert ")

    /**
     * Check if the line is a map entry (`key:` or `key: value`).
     */
    private fun isMapEntry(trimmed: String): Boolean = trimmed.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*:\\s*.*$"))

    /**
     * Check whether a map entry contains a value on the same line.
     */
    private fun hasInlineValue(trimmed: String): Boolean {
        val colonIndex = trimmed.indexOf(':')
        if (colonIndex < 0 || colonIndex == trimmed.lastIndex) return false
        return trimmed.substring(colonIndex + 1).trim().isNotEmpty()
    }

    /**
     * Format a line with proper spacing.
     */
    private fun formatLine(
        trimmed: String,
        indent: Int,
    ): String {
        val indentStr = " ".repeat(indent)

        // Normalize multiple spaces to single space (except in strings)
        val normalized = normalizeSpacing(trimmed)

        return indentStr + normalized
    }

    /**
     * Normalize spacing: reduce multiple spaces to single space,
     * but preserve spacing in quoted strings.
     */
    private fun normalizeSpacing(text: String): String {
        val result = StringBuilder()
        var inQuote = false
        var quoteChar: Char? = null
        var prevWasSpace = false
        var i = 0

        while (i < text.length) {
            val c = text[i]

            // Handle triple quotes
            if (i + 2 < text.length) {
                val triple = text.substring(i, i + 3)
                if (triple == "'''" || triple == "\"\"\"") {
                    result.append(triple)
                    inQuote = !inQuote
                    i += 3
                    prevWasSpace = false
                    continue
                }
            }

            // Handle single/double quotes
            if ((c == '"' || c == '\'') && !inQuote) {
                inQuote = true
                quoteChar = c
                result.append(c)
                prevWasSpace = false
            } else if (c == quoteChar) {
                inQuote = false
                quoteChar = null
                result.append(c)
                prevWasSpace = false
            } else if (inQuote) {
                result.append(c)
                prevWasSpace = false
            } else if (c.isWhitespace()) {
                if (!prevWasSpace) {
                    result.append(' ')
                    prevWasSpace = true
                }
            } else {
                result.append(c)
                prevWasSpace = false
            }

            i++
        }

        return result.toString()
    }

    /**
     * Align columns in table rows.
     */
    private fun alignTableColumns(
        tableLines: List<String>,
        indent: Int,
    ): List<String> {
        if (tableLines.isEmpty()) return emptyList()

        val indentStr = " ".repeat(indent)

        // Parse each row into cells
        val rows =
            tableLines.map { line ->
                parseTableRow(line)
            }

        // Calculate max width for each column
        val columnWidths = calculateColumnWidths(rows)

        // Rebuild each row with aligned columns
        return rows.map { cells ->
            formatTableRow(cells, columnWidths, indentStr)
        }
    }

    /**
     * Parse a table row into cells.
     */
    private fun parseTableRow(row: String): List<String> {
        val trimmed = row.trim()
        if (!trimmed.startsWith("|")) return emptyList()

        // Remove leading/trailing pipes and split by pipe
        val content = trimmed.trim('|').trim()
        return content.split('|').map { it.trim() }
    }

    /**
     * Calculate the maximum width needed for each column.
     */
    private fun calculateColumnWidths(rows: List<List<String>>): List<Int> {
        if (rows.isEmpty()) return emptyList()

        val maxColumns = rows.maxOfOrNull { it.size } ?: 0
        val widths = MutableList(maxColumns) { 0 }

        for (row in rows) {
            for (i in row.indices) {
                widths[i] = max(widths[i], row[i].length)
            }
        }

        return widths
    }

    /**
     * Format a table row with aligned columns.
     */
    private fun formatTableRow(
        cells: List<String>,
        columnWidths: List<Int>,
        indent: String,
    ): String {
        if (cells.isEmpty()) return "$indent|"

        val formattedCells =
            cells.mapIndexed { index, cell ->
                val width = columnWidths.getOrElse(index) { cell.length }
                cell.padEnd(width)
            }

        return "$indent| ${formattedCells.joinToString(" | ")} |"
    }

    /**
     * Context for tracking nesting level during formatting.
     */
    private data class FormattingContext(
        val inFeature: Boolean = false,
        val inFragment: Boolean = false,
        val inScenario: Boolean = false,
        val inOutline: Boolean = false,
        val inBackground: Boolean = false,
        val inStep: Boolean = false,
        val inDirective: Boolean = false,
        val inExamples: Boolean = false,
        val inParametersBlock: Boolean = false,
        val currentIndent: Int = 0,
        val containerIndent: Int = 0,
        val directiveIndent: Int = 0,
        val inConditionalBranch: Boolean = false,
        val conditionalBaseIndent: Int = 0,
        val mapIndentStack: List<Int> = emptyList(),
        val previousLineBlank: Boolean = false,
    )
}
