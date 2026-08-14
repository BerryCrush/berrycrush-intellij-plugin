package org.berrycrush.intellij.formatting

import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType
import org.berrycrush.intellij.lexer.BerryCrushTokenTypes
import org.berrycrush.intellij.psi.BerryCrushBackgroundElement
import org.berrycrush.intellij.psi.BerryCrushDirectiveElement
import org.berrycrush.intellij.psi.BerryCrushExampleRowElement
import org.berrycrush.intellij.psi.BerryCrushExamplesElement
import org.berrycrush.intellij.psi.BerryCrushFeatureElement
import org.berrycrush.intellij.psi.BerryCrushFragmentElement
import org.berrycrush.intellij.psi.BerryCrushOutlineElement
import org.berrycrush.intellij.psi.BerryCrushScenarioElement
import org.berrycrush.intellij.psi.BerryCrushStepElement
import org.berrycrush.intellij.psi.BerryCrushTagElement

/**
 * Formatting block for BerryCrush language elements.
 *
 * This block tracks formatting context (inFeature, inScenario, etc.) and passes
 * it to child blocks to correctly calculate indentation levels.
 *
 * @param context The formatting context from the parent block
 */
class BerryCrushBlock(
    node: ASTNode,
    wrap: Wrap?,
    alignment: Alignment?,
    private val myIndent: Indent,
    private val spacingBuilder: SpacingBuilder,
    private val indentSize: Int = 2,
    private val context: FormattingContext = FormattingContext(),
) : AbstractBlock(node, wrap, alignment) {
    override fun getIndent(): Indent = myIndent

    override fun buildChildren(): List<Block> {
        val blocks = mutableListOf<Block>()
        var child = myNode.firstChildNode
        var currentContext = context

        while (child != null) {
            if (!isWhitespaceOrNewline(child.elementType)) {
                // Calculate this child's indent based on current context
                val childIndent = calcContextAwareIndent(child.psi, currentContext)

                // Create child block with updated context
                val childContext = updateContext(currentContext, child.psi)

                blocks.add(
                    BerryCrushBlock(
                        child,
                        null,
                        null,
                        childIndent,
                        spacingBuilder,
                        indentSize,
                        childContext,
                    ),
                )

                // Update context for next sibling
                currentContext = childContext
            }
            child = child.treeNext
        }
        return blocks
    }

    /**
     * Update formatting context based on the current node.
     */
    private fun updateContext(
        ctx: FormattingContext,
        node: PsiElement,
    ): FormattingContext = when (node) {
        is BerryCrushFeatureElement -> FormattingContext(inFeature = true)
        is BerryCrushScenarioElement, is BerryCrushOutlineElement ->
            ctx.copy(inScenario = true, inBackground = false, inExamples = false, inStep = false)
        is BerryCrushBackgroundElement -> ctx.copy(inBackground = true, inScenario = false, inExamples = false, inStep = false)
        is BerryCrushFragmentElement -> FormattingContext(inFragment = true)
        is BerryCrushStepElement -> ctx.copy(inStep = true, inExamples = false)
        is BerryCrushExamplesElement -> ctx.copy(inExamples = true, inStep = false)
        else -> ctx
    }

    /**
     * Calculate indent for a child based on accumulated context.
     * Extracted complexity into helper methods for better maintainability.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun calcContextAwareIndent(
        child: PsiElement,
        ctx: FormattingContext,
    ): Indent = when (child) {
        is BerryCrushFragmentElement, is BerryCrushFeatureElement -> Indent.getNoneIndent()
        is BerryCrushScenarioElement, is BerryCrushOutlineElement -> calcScenarioIndent(ctx)
        is BerryCrushBackgroundElement -> calcBackgroundIndent(ctx)
        is BerryCrushExamplesElement -> calcExamplesIndent(ctx)
        is BerryCrushStepElement -> calcStepIndent(ctx)
        is BerryCrushDirectiveElement -> calcDirectiveIndent(ctx)
        is BerryCrushExampleRowElement -> calcTableIndent(ctx)
        is BerryCrushTagElement -> calcTagIndent(ctx)
        else -> Indent.getNoneIndent()
    }

    // Helper methods for indent calculation

    private fun calcScenarioIndent(ctx: FormattingContext): Indent = if (ctx.inFeature) Indent.getSpaceIndent(indentSize) else Indent.getNoneIndent()

    private fun calcBackgroundIndent(ctx: FormattingContext): Indent = if (ctx.inFeature) Indent.getSpaceIndent(indentSize) else Indent.getNoneIndent()

    private fun calcTagIndent(ctx: FormattingContext): Indent = if (ctx.inFeature) Indent.getSpaceIndent(indentSize) else Indent.getNoneIndent()

    private fun calcExamplesIndent(ctx: FormattingContext): Indent {
        val depth = if (ctx.inFeature) 2 else 1
        return Indent.getSpaceIndent(depth * indentSize)
    }

    private fun calcStepIndent(ctx: FormattingContext): Indent {
        val depth =
            when {
                ctx.inFragment -> 1
                ctx.inFeature && (ctx.inScenario || ctx.inBackground) -> 2
                ctx.inScenario || ctx.inBackground -> 1
                else -> 0
            }
        return Indent.getSpaceIndent(depth * indentSize)
    }

    private fun calcDirectiveIndent(ctx: FormattingContext): Indent {
        val depth =
            when {
                ctx.inFragment -> 2
                ctx.inFeature && (ctx.inScenario || ctx.inBackground) -> 3
                ctx.inScenario || ctx.inBackground -> 2
                else -> 1
            }
        return Indent.getSpaceIndent(depth * indentSize)
    }

    private fun calcTableIndent(ctx: FormattingContext): Indent {
        val depth =
            when {
                ctx.inExamples && ctx.inFeature -> 3
                ctx.inExamples -> 2
                else -> 1
            }
        return Indent.getSpaceIndent(depth * indentSize)
    }

    override fun getSpacing(
        child1: Block?,
        child2: Block,
    ): Spacing? = spacingBuilder.getSpacing(this, child1, child2)

    override fun isLeaf(): Boolean = myNode.firstChildNode == null

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        // When pressing Enter after a line, suggest appropriate indent
        val nextIndent =
            when (myNode.psi) {
                is BerryCrushFeatureElement -> Indent.getSpaceIndent(indentSize)
                is BerryCrushScenarioElement, is BerryCrushOutlineElement -> Indent.getSpaceIndent(if (context.inFeature) 2 * indentSize else indentSize)
                is BerryCrushStepElement -> Indent.getSpaceIndent(if (context.inFeature) 3 * indentSize else 2 * indentSize)
                else -> Indent.getNoneIndent()
            }
        return ChildAttributes(nextIndent, null)
    }

    // Helper methods

    private fun isWhitespaceOrNewline(type: IElementType): Boolean = type == TokenType.WHITE_SPACE ||
        type == BerryCrushTokenTypes.WHITE_SPACE ||
        type == BerryCrushTokenTypes.NEWLINE
}

/**
 * Context for tracking what block we're inside during formatting.
 */
data class FormattingContext(
    val inFeature: Boolean = false,
    val inFragment: Boolean = false,
    val inScenario: Boolean = false,
    val inBackground: Boolean = false,
    val inExamples: Boolean = false,
    val inStep: Boolean = false,
)
