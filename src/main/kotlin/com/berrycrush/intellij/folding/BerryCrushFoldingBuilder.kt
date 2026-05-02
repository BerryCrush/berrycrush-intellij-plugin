package com.berrycrush.intellij.folding

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.tree.IElementType

/**
 * Code folding builder for BerryCrush files.
 *
 * Provides folding for:
 * - Features, scenarios, fragments, backgrounds, and examples (block keywords)
 * - Steps (given, when, then, and, but) with nested content
 * - Doc strings (triple-quoted content)
 */
class BerryCrushFoldingBuilder : FoldingBuilder {

    override fun buildFoldRegions(node: ASTNode, document: Document): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        collectFoldingRegions(node, document, descriptors)
        return descriptors.toTypedArray()
    }

    private fun collectFoldingRegions(
        node: ASTNode,
        document: Document,
        descriptors: MutableList<FoldingDescriptor>
    ) {
        val elementType = node.elementType

        // Check if this is a foldable element
        when {
            elementType in FOLDABLE_BLOCKS -> {
                val foldRegion = findBlockFoldRegion(node, document)
                if (foldRegion != null && foldRegion.length > MIN_FOLD_LENGTH) {
                    descriptors.add(FoldingDescriptor(node, foldRegion))
                }
            }
            elementType in FOLDABLE_STEPS -> {
                val foldRegion = findStepFoldRegion(node, document)
                if (foldRegion != null && foldRegion.length > MIN_FOLD_LENGTH) {
                    descriptors.add(FoldingDescriptor(node, foldRegion))
                }
            }
            elementType == BerryCrushTokenTypes.STRING -> {
                // Check for doc strings (multi-line strings)
                val text = node.text
                if (text.startsWith("\"\"\"") || text.contains('\n')) {
                    val foldRegion = findDocStringFoldRegion(node, document)
                    if (foldRegion != null && foldRegion.length > MIN_FOLD_LENGTH) {
                        descriptors.add(FoldingDescriptor(node, foldRegion))
                    }
                }
            }
        }

        // Recursively process children
        var child = node.firstChildNode
        while (child != null) {
            collectFoldingRegions(child, document, descriptors)
            child = child.treeNext
        }
    }

    /**
     * Find fold region for block keywords (scenario, fragment, feature, etc.)
     */
    private fun findBlockFoldRegion(node: ASTNode, document: Document): TextRange? {
        val startLine = document.getLineNumber(node.startOffset)
        val startLineEnd = document.getLineEndOffset(startLine)

        // Find the end of the block (next block keyword at same or lower indent, or end of file)
        var endOffset = findBlockEnd(node, BLOCK_BOUNDARIES)
        if (endOffset <= startLineEnd) return null

        // Trim trailing whitespace/newlines
        val text = document.charsSequence
        while (endOffset > startLineEnd && text[endOffset - 1].isWhitespace()) {
            endOffset--
        }

        return TextRange(startLineEnd, endOffset)
    }

    /**
     * Find fold region for step keywords (given, when, then, etc.)
     */
    private fun findStepFoldRegion(node: ASTNode, document: Document): TextRange? {
        val startLine = document.getLineNumber(node.startOffset)
        val startLineEnd = document.getLineEndOffset(startLine)

        // Find the end of the step (next step or block keyword, or end of file)
        var endOffset = findBlockEnd(node, STEP_BOUNDARIES)
        if (endOffset <= startLineEnd) return null

        // Trim trailing whitespace/newlines
        val text = document.charsSequence
        while (endOffset > startLineEnd && text[endOffset - 1].isWhitespace()) {
            endOffset--
        }

        // Only fold if there's meaningful content (more than just the step line)
        if (endOffset <= startLineEnd + 1) return null

        return TextRange(startLineEnd, endOffset)
    }

    /**
     * Find fold region for doc strings (triple-quoted content).
     */
    @Suppress("UnusedParameter")  // document may be needed for future enhancements
    private fun findDocStringFoldRegion(node: ASTNode, document: Document): TextRange? {
        val text = node.text
        if (!text.contains('\n')) return null

        val startOffset = node.startOffset
        val endOffset = node.startOffset + node.textLength

        // Find the first newline to start folding after
        val firstNewline = text.indexOf('\n')
        if (firstNewline < 0) return null

        val foldStart = startOffset + firstNewline
        val foldEnd = endOffset

        if (foldEnd <= foldStart + MIN_FOLD_LENGTH) return null

        return TextRange(foldStart, foldEnd)
    }

    private fun findBlockEnd(startNode: ASTNode, boundaries: Set<IElementType>): Int {
        var currentNode: ASTNode? = startNode.treeNext
        val startIndent = getIndentLevel(startNode)

        while (currentNode != null) {
            val elementType = currentNode.elementType

            // Check if this is a boundary at the same or lower indent level
            if (elementType in boundaries) {
                val nodeIndent = getIndentLevel(currentNode)
                if (nodeIndent <= startIndent) {
                    return currentNode.startOffset
                }
            }

            currentNode = currentNode.treeNext
        }

        // Return the end of the file
        return startNode.treeParent?.textRange?.endOffset ?: startNode.textRange.endOffset
    }

    private fun getIndentLevel(node: ASTNode): Int {
        // Count leading whitespace before this node on its line
        var prevNode = node.treePrev
        var spaces = 0
        while (prevNode != null && prevNode.elementType == BerryCrushTokenTypes.INDENT) {
            spaces += prevNode.textLength
            prevNode = prevNode.treePrev
        }
        return spaces / 2 // 2 spaces per indent level
    }

    override fun getPlaceholderText(node: ASTNode): String {
        return when (node.elementType) {
            // Block keywords
            BerryCrushTokenTypes.FEATURE -> "feature: ..."
            BerryCrushTokenTypes.SCENARIO -> "scenario: ..."
            BerryCrushTokenTypes.OUTLINE -> "outline: ..."
            BerryCrushTokenTypes.FRAGMENT -> "fragment: ..."
            BerryCrushTokenTypes.BACKGROUND -> "background: ..."
            BerryCrushTokenTypes.EXAMPLES -> "examples: ..."
            // Step keywords
            BerryCrushTokenTypes.GIVEN -> "given ..."
            BerryCrushTokenTypes.WHEN -> "when ..."
            BerryCrushTokenTypes.THEN -> "then ..."
            BerryCrushTokenTypes.AND -> "and ..."
            BerryCrushTokenTypes.BUT -> "but ..."
            // Doc strings
            BerryCrushTokenTypes.STRING -> "\"\"\""
            else -> "..."
        }
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    companion object {
        private const val MIN_FOLD_LENGTH = 5

        // Block-level foldable elements
        private val FOLDABLE_BLOCKS = setOf(
            BerryCrushTokenTypes.FEATURE,
            BerryCrushTokenTypes.SCENARIO,
            BerryCrushTokenTypes.OUTLINE,
            BerryCrushTokenTypes.FRAGMENT,
            BerryCrushTokenTypes.BACKGROUND,
            BerryCrushTokenTypes.EXAMPLES,
        )

        // Step-level foldable elements
        private val FOLDABLE_STEPS = setOf(
            BerryCrushTokenTypes.GIVEN,
            BerryCrushTokenTypes.WHEN,
            BerryCrushTokenTypes.THEN,
            BerryCrushTokenTypes.AND,
            BerryCrushTokenTypes.BUT,
        )

        // Boundaries for block folding (same or lower indent stops the fold)
        private val BLOCK_BOUNDARIES = setOf(
            BerryCrushTokenTypes.FEATURE,
            BerryCrushTokenTypes.SCENARIO,
            BerryCrushTokenTypes.OUTLINE,
            BerryCrushTokenTypes.FRAGMENT,
            BerryCrushTokenTypes.BACKGROUND,
            BerryCrushTokenTypes.EXAMPLES,
        )

        // Boundaries for step folding
        private val STEP_BOUNDARIES = BLOCK_BOUNDARIES + setOf(
            BerryCrushTokenTypes.GIVEN,
            BerryCrushTokenTypes.WHEN,
            BerryCrushTokenTypes.THEN,
            BerryCrushTokenTypes.AND,
            BerryCrushTokenTypes.BUT,
        )
    }
}
