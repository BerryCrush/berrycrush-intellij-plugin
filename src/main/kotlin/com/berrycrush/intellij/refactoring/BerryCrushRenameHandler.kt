package com.berrycrush.intellij.refactoring

import com.berrycrush.intellij.index.FragmentIndex
import com.berrycrush.intellij.index.IncludeUsageIndex
import com.berrycrush.intellij.psi.BerryCrushFile
import com.intellij.codeInsight.highlighting.HighlightManager
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.RenameHandler

/**
 * Rename handler for BerryCrush elements (fragments, variables).
 *
 * Provides rename functionality via Shift+F6 or Refactor menu.
 * Handles:
 * - Fragment definitions and include directives
 * - Variable placeholders within scenarios
 */
class BerryCrushRenameHandler : RenameHandler {

    override fun isAvailableOnDataContext(dataContext: DataContext): Boolean {
        val file = CommonDataKeys.PSI_FILE.getData(dataContext) ?: return false
        if (file !is BerryCrushFile) return false

        val editor = CommonDataKeys.EDITOR.getData(dataContext) ?: return false
        val offset = editor.caretModel.offset
        val lineText = getLineTextAtOffset(editor, offset)

        return isFragmentDefinition(lineText) ||
            isIncludeDirective(lineText) ||
            isVariableAtOffset(editor, offset)
    }

    override fun isRenaming(dataContext: DataContext): Boolean = isAvailableOnDataContext(dataContext)

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?, dataContext: DataContext) {
        if (editor == null || file == null) return
        val offset = editor.caretModel.offset
        val lineText = getLineTextAtOffset(editor, offset)

        when {
            isFragmentDefinition(lineText) || isIncludeDirective(lineText) ->
                renameFragment(project, editor, file, lineText)
            isVariableAtOffset(editor, offset) ->
                renameVariable(project, editor, file, offset)
        }
    }

    override fun invoke(project: Project, elements: Array<out PsiElement>, dataContext: DataContext) {
        // Not used - we use editor-based rename
    }

    @Suppress("UnusedParameter")
    private fun renameFragment(project: Project, editor: Editor, file: PsiFile, lineText: String) {
        val currentName = extractFragmentName(lineText) ?: return

        val newName = Messages.showInputDialog(
            project,
            "Rename fragment '$currentName' to:",
            "Rename Fragment",
            null,
            currentName,
            null,
        ) ?: return

        if (newName == currentName || newName.isBlank()) return

        // Highlight all usages before rename
        highlightUsages(project, editor, currentName)

        WriteCommandAction.runWriteCommandAction(project, "Rename Fragment", null, {
            // Update all includes
            IncludeUsageIndex.findIncludeUsages(project, currentName).forEach { usage ->
                replaceInElement(usage, currentName, newName)
            }

            // Update the definition
            FragmentIndex.findFragmentElement(project, currentName)?.let { definition ->
                replaceInElement(definition, currentName, newName)
            }
        })
    }

    @Suppress("UnusedParameter")
    private fun renameVariable(project: Project, editor: Editor, file: PsiFile, offset: Int) {
        val document = editor.document
        val text = document.text
        val currentName = extractVariableNameAtOffset(text, offset) ?: return

        val newName = Messages.showInputDialog(
            project,
            "Rename variable '{{$currentName}}' to:",
            "Rename Variable",
            null,
            currentName,
            null,
        ) ?: return

        if (newName == currentName || newName.isBlank()) return

        // Find scenario scope
        val scenarioRange = findScenarioScope(text, offset)

        WriteCommandAction.runWriteCommandAction(project, "Rename Variable", null, {
            // Replace all occurrences in reverse order to preserve offsets
            val usagePattern = Regex("""\{\{${Regex.escape(currentName)}}}""")
            val defPattern = Regex("""=>\s*${Regex.escape(currentName)}(?=\s|$)""")
            // Example table header pattern: | name |
            val examplePattern = Regex("""\|\s*${Regex.escape(currentName)}\s*(?=\|)""")

            val allMatches = mutableListOf<Pair<IntRange, String>>()

            // Find {{variable}} usages
            usagePattern.findAll(text).forEach { match ->
                if (scenarioRange?.contains(match.range.first) != false) {
                    allMatches.add(match.range to "{{$newName}}")
                }
            }

            // Find "=> variable" definitions
            defPattern.findAll(text).forEach { match ->
                if (scenarioRange?.contains(match.range.first) != false) {
                    val newValue = match.value.replace(currentName, newName)
                    allMatches.add(match.range to newValue)
                }
            }

            // Find example table headers | variable |
            examplePattern.findAll(text).forEach { match ->
                if (scenarioRange?.contains(match.range.first) != false) {
                    val newValue = match.value.replace(currentName, newName)
                    allMatches.add(match.range to newValue)
                }
            }

            // Replace in reverse order
            allMatches.sortedByDescending { it.first.first }.forEach { (range, replacement) ->
                document.replaceString(range.first, range.last + 1, replacement)
            }

            PsiDocumentManager.getInstance(project).commitDocument(document)
        })
    }

    private fun highlightUsages(project: Project, editor: Editor, fragmentName: String) {
        val highlightManager = HighlightManager.getInstance(project)
        val usages = IncludeUsageIndex.findIncludeUsages(project, fragmentName)

        usages.forEach { usage ->
            val range = usage.textRange
            highlightManager.addOccurrenceHighlight(
                editor,
                range.startOffset,
                range.endOffset,
                EditorColors.SEARCH_RESULT_ATTRIBUTES,
                0,
                null,
            )
        }
    }

    private fun replaceInElement(element: PsiElement, oldName: String, newName: String) {
        val file = element.containingFile ?: return
        val document = PsiDocumentManager.getInstance(element.project).getDocument(file) ?: return
        val elementOffset = element.textOffset

        // Find the name within this element's line
        val lineNumber = document.getLineNumber(elementOffset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStart, lineEnd))

        val nameIndex = lineText.indexOf(oldName)
        if (nameIndex >= 0) {
            document.replaceString(
                lineStart + nameIndex,
                lineStart + nameIndex + oldName.length,
                newName,
            )
            PsiDocumentManager.getInstance(element.project).commitDocument(document)
        }
    }

    private fun getLineTextAtOffset(editor: Editor, offset: Int): String {
        val document = editor.document
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        return document.getText(TextRange(lineStart, lineEnd))
    }

    private fun extractFragmentName(lineText: String): String? {
        FRAGMENT_DEF_PATTERN.find(lineText)?.let { return it.groupValues[1] }
        INCLUDE_PATTERN.find(lineText)?.let { return it.groupValues[1].removePrefix("^") }
        return null
    }

    private fun isFragmentDefinition(lineText: String): Boolean =
        FRAGMENT_DEF_PATTERN.containsMatchIn(lineText)

    private fun isIncludeDirective(lineText: String): Boolean =
        INCLUDE_PATTERN.containsMatchIn(lineText)

    private fun isVariableAtOffset(editor: Editor, offset: Int): Boolean {
        val text = editor.document.text
        return extractVariableNameAtOffset(text, offset) != null
    }

    private fun extractVariableNameAtOffset(text: String, offset: Int): String? {
        // Try {{...}} pattern first
        extractVariableFromBraces(text, offset)?.let { return it }

        // Try example table header pattern | name |
        extractVariableFromExampleHeader(text, offset)?.let { return it }

        return null
    }

    private fun extractVariableFromBraces(text: String, offset: Int): String? {
        // Look for {{...}} pattern containing the offset
        var searchPos = offset.coerceAtMost(text.length - 1)
        while (searchPos >= 0) {
            val start = text.lastIndexOf("{{", searchPos)
            if (start == -1) return null

            val end = text.indexOf("}}", start + 2)
            if (end == -1) {
                searchPos = start - 1
                continue
            }

            // Check if offset is within this {{...}} range
            if (offset >= start && offset <= end + 2) {
                return text.substring(start + 2, end).takeIf { it.isNotBlank() }
            }

            searchPos = start - 1
        }
        return null
    }

    private fun extractVariableFromExampleHeader(text: String, offset: Int): String? {
        // Get the line containing the offset
        val lineStart = text.lastIndexOf('\n', offset - 1) + 1
        val lineEnd = text.indexOf('\n', offset).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)
        val offsetInLine = offset - lineStart

        // Check if this is an example header line (first row after "example:")
        // Look for pattern: | name1 | name2 | name3 |
        if (!line.trim().startsWith("|")) return null

        // Find which cell the cursor is in
        var cellStart = 0
        var cellEnd = 0
        var inCell = false

        for (i in line.indices) {
            if (line[i] == '|') {
                if (inCell && offsetInLine in cellStart until i) {
                    cellEnd = i
                    break
                }
                cellStart = i + 1
                inCell = true
            }
        }

        if (cellEnd == 0) return null

        val cellContent = line.substring(cellStart, cellEnd).trim()
        // Only return if it looks like a variable name (alphanumeric + underscore)
        return if (cellContent.matches(Regex("""[a-zA-Z_][a-zA-Z0-9_]*"""))) cellContent else null
    }

    private fun findScenarioScope(text: String, offset: Int): IntRange? {
        val beforeText = text.substring(0, offset)
        val lastScenario = SCENARIO_PATTERN.findAll(beforeText).lastOrNull() ?: return null
        val scenarioStart = lastScenario.range.first

        val afterText = text.substring(offset)
        val nextScenario = SCENARIO_PATTERN.find(afterText)
        val scenarioEnd = if (nextScenario != null) offset + nextScenario.range.first else text.length

        return IntRange(scenarioStart, scenarioEnd)
    }

    companion object {
        private val FRAGMENT_DEF_PATTERN = Regex("""^\s*[Ff]ragment:\s*(\S+)""")
        private val INCLUDE_PATTERN = Regex("""^\s*include\s+(\^?[a-zA-Z_][a-zA-Z0-9_.\-]*)""")
        private val SCENARIO_PATTERN = Regex("""(?m)^\s*[Ss]cenario:\s*""")
    }
}
