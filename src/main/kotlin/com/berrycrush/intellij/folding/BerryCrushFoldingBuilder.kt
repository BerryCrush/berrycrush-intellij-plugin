package com.berrycrush.intellij.folding

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType

/**
 * Code folding builder for BerryCrush files.
 *
 * Provides folding for features, scenarios, fragments, backgrounds, and examples.
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

        // Check if this is a foldable block keyword
        if (elementType in FOLDABLE_KEYWORDS) {
            val foldRegion = findFoldRegion(node, document)
            if (foldRegion != null && foldRegion.length > MIN_FOLD_LENGTH) {
                descriptors.add(FoldingDescriptor(node, foldRegion))
            }
        }

        // Recursively process children
        var child = node.firstChildNode
        while (child != null) {
            collectFoldingRegions(child, document, descriptors)
            child = child.treeNext
        }
    }

    private fun findFoldRegion(node: ASTNode, document: Document): TextRange? {
        val startLine = document.getLineNumber(node.startOffset)
        val startLineEnd = document.getLineEndOffset(startLine)

        // Find the end of the block (next block keyword or end of file)
        var endOffset = findBlockEnd(node)
        if (endOffset <= startLineEnd) return null

        // Trim trailing whitespace/newlines
        val text = document.charsSequence
        while (endOffset > startLineEnd && text[endOffset - 1].isWhitespace()) {
            endOffset--
        }

        return TextRange(startLineEnd, endOffset)
    }

    private fun findBlockEnd(startNode: ASTNode): Int {
        var currentNode: ASTNode? = startNode.treeNext
        val startIndent = getIndentLevel(startNode)

        while (currentNode != null) {
            val elementType = currentNode.elementType

            // Check if this is another block at the same or lower indent level
            if (elementType in BLOCK_BOUNDARIES) {
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
            BerryCrushTokenTypes.FEATURE -> "feature: ..."
            BerryCrushTokenTypes.SCENARIO -> "scenario: ..."
            BerryCrushTokenTypes.OUTLINE -> "outline: ..."
            BerryCrushTokenTypes.FRAGMENT -> "fragment: ..."
            BerryCrushTokenTypes.BACKGROUND -> "background: ..."
            BerryCrushTokenTypes.EXAMPLES -> "examples: ..."
            BerryCrushTokenTypes.GIVEN,
            BerryCrushTokenTypes.WHEN,
            BerryCrushTokenTypes.THEN,
            BerryCrushTokenTypes.AND,
            BerryCrushTokenTypes.BUT -> "..."
            else -> "..."
        }
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean = false

    companion object {
        private const val MIN_FOLD_LENGTH = 10

        private val FOLDABLE_KEYWORDS = setOf(
            BerryCrushTokenTypes.FEATURE,
            BerryCrushTokenTypes.SCENARIO,
            BerryCrushTokenTypes.OUTLINE,
            BerryCrushTokenTypes.FRAGMENT,
            BerryCrushTokenTypes.BACKGROUND,
            BerryCrushTokenTypes.EXAMPLES,
        )

        private val BLOCK_BOUNDARIES = setOf(
            BerryCrushTokenTypes.FEATURE,
            BerryCrushTokenTypes.SCENARIO,
            BerryCrushTokenTypes.OUTLINE,
            BerryCrushTokenTypes.FRAGMENT,
            BerryCrushTokenTypes.BACKGROUND,
            BerryCrushTokenTypes.EXAMPLES,
            BerryCrushTokenTypes.GIVEN,
            BerryCrushTokenTypes.WHEN,
            BerryCrushTokenTypes.THEN,
        )
    }
}
