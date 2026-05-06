package com.berrycrush.intellij.formatting

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
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
        private val DIRECTIVES = setOf(
            "call", "assert", "extract", "include", "if", "else",
            "body:", "bodyfile", "bodyfile:", "parameters:"
        )
    }

    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {
        return source
    }

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
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
        var tableLines = mutableListOf<String>()
        var tableIndent = 0
        var inTable = false
        
        for (line in lines) {
            val trimmed = line.trim()
            
            // Handle empty lines
            if (trimmed.isEmpty()) {
                if (inTable) {
                    // End table and align it
                    result.addAll(alignTableColumns(tableLines, tableIndent))
                    tableLines.clear()
                    inTable = false
                }
                result.add("")
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
            
            // Calculate indent and update context
            val (indent, newContext) = calculateIndentAndContext(trimmed, context)
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
        context: FormattingContext
    ): Pair<Int, FormattingContext> {
        val lower = trimmed.lowercase()
        val firstWord = lower.split(Regex("\\s+|:")).firstOrNull() ?: ""
        
        return when {
            // Feature/fragment at root level
            lower.startsWith("feature:") || lower.startsWith("fragment:") -> {
                val newContext = context.copy(
                    inFeature = lower.startsWith("feature:"),
                    inFragment = lower.startsWith("fragment:"),
                    inScenario = false,
                    inBackground = false,
                    inStep = false,
                    inExamples = false,
                    currentIndent = 0
                )
                0 to newContext
            }
            
            // Scenario/outline
            lower.startsWith("scenario:") || lower.startsWith("outline:") -> {
                val indent = if (context.inFeature) INDENT_SIZE else 0
                val newContext = context.copy(
                    inScenario = true,
                    inBackground = false,
                    inStep = false,
                    inExamples = false,
                    currentIndent = indent
                )
                indent to newContext
            }
            
            // Background
            lower.startsWith("background:") -> {
                val indent = if (context.inFeature) INDENT_SIZE else 0
                val newContext = context.copy(
                    inBackground = true,
                    inScenario = false,
                    inStep = false,
                    inExamples = false,
                    currentIndent = indent
                )
                indent to newContext
            }
            
            // Examples - at same level as steps in scenario
            lower.startsWith("examples:") -> {
                val indent = when {
                    context.inFeature && context.inScenario -> INDENT_SIZE * 2
                    context.inScenario -> INDENT_SIZE
                    else -> 0  // Standalone examples at root level
                }
                val newContext = context.copy(
                    inExamples = true,
                    inStep = false,
                    currentIndent = indent
                )
                indent to newContext
            }
            
            // Tags (@ at start)
            lower.startsWith("@") -> {
                val indent = if (context.inFeature && !context.inScenario && !context.inBackground) {
                    INDENT_SIZE
                } else {
                    context.currentIndent
                }
                indent to context
            }
            
            // Comments
            lower.startsWith("#") -> {
                context.currentIndent to context
            }
            
            // Step keywords
            firstWord in STEP_KEYWORDS -> {
                val baseIndent = when {
                    context.inFeature && (context.inScenario || context.inBackground) -> INDENT_SIZE * 2
                    context.inFragment -> INDENT_SIZE
                    context.inScenario || context.inBackground -> INDENT_SIZE
                    else -> 0
                }
                val newContext = context.copy(
                    inStep = true,
                    inDirective = false,
                    inExamples = false,
                    currentIndent = baseIndent
                )
                baseIndent to newContext
            }
            
            // Directives (call, assert, include, etc.)
            // Multiple directives at the same level should have the same indentation
            isDirective(lower) -> {
                // If already at directive level, stay there
                val indent = if (context.inDirective) {
                    context.directiveIndent
                } else {
                    // Calculate directive level based on hierarchy
                    when {
                        // Under a step - directive level (one deeper than step)
                        context.inStep -> context.currentIndent + INDENT_SIZE
                        // Directly under fragment - step level
                        context.inFragment -> INDENT_SIZE
                        // Directly under feature+scenario/background - step level
                        context.inFeature && (context.inScenario || context.inBackground) -> INDENT_SIZE * 2
                        // Directly under standalone scenario/background - step level
                        context.inScenario || context.inBackground -> INDENT_SIZE
                        else -> INDENT_SIZE
                    }
                }
                val newContext = context.copy(
                    inDirective = true,
                    currentIndent = indent,
                    directiveIndent = indent
                )
                indent to newContext
            }
            
            // Parameters (key: value lines after a directive)
            isParameter(trimmed) -> {
                val indent = if (context.inDirective) {
                    context.directiveIndent + INDENT_SIZE
                } else {
                    context.currentIndent + INDENT_SIZE
                }
                indent to context.copy(currentIndent = indent)
            }
            
            // Body content (triple quotes or JSON)
            lower.startsWith("'''") || lower.startsWith("\"\"\"") || 
            lower.startsWith("{") || lower.startsWith("}") -> {
                (context.currentIndent + INDENT_SIZE) to context
            }
            
            // Default - maintain context indent
            else -> {
                context.currentIndent to context
            }
        }
    }
    
    /**
     * Check if the line starts with a directive keyword.
     */
    private fun isDirective(lower: String): Boolean {
        return DIRECTIVES.any { lower.startsWith(it) }
    }
    
    /**
     * Check if the line is a parameter (word followed by colon and value).
     */
    private fun isParameter(trimmed: String): Boolean {
        // Parameter pattern: identifier: value
        return trimmed.matches(Regex("^[a-zA-Z_][a-zA-Z0-9_]*:\\s*.+"))
    }
    
    /**
     * Format a line with proper spacing.
     */
    private fun formatLine(trimmed: String, indent: Int): String {
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
            } else if (c == quoteChar && inQuote) {
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
    private fun alignTableColumns(tableLines: List<String>, indent: Int): List<String> {
        if (tableLines.isEmpty()) return emptyList()
        
        val indentStr = " ".repeat(indent)
        
        // Parse each row into cells
        val rows = tableLines.map { line ->
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
    private fun formatTableRow(cells: List<String>, columnWidths: List<Int>, indent: String): String {
        if (cells.isEmpty()) return "$indent|"
        
        val formattedCells = cells.mapIndexed { index, cell ->
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
        val inBackground: Boolean = false,
        val inStep: Boolean = false,
        val inDirective: Boolean = false,
        val inExamples: Boolean = false,
        val currentIndent: Int = 0,
        val directiveIndent: Int = 0
    )
}
