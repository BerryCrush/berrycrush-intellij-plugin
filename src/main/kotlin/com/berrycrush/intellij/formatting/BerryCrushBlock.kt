package com.berrycrush.intellij.formatting

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.intellij.formatting.Alignment
import com.intellij.formatting.Block
import com.intellij.formatting.ChildAttributes
import com.intellij.formatting.Indent
import com.intellij.formatting.Spacing
import com.intellij.formatting.SpacingBuilder
import com.intellij.formatting.Wrap
import com.intellij.lang.ASTNode
import com.intellij.psi.TokenType
import com.intellij.psi.formatter.common.AbstractBlock
import com.intellij.psi.tree.IElementType

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
    private val context: FormattingContext = FormattingContext()
) : AbstractBlock(node, wrap, alignment) {

    override fun getIndent(): Indent = myIndent

    override fun buildChildren(): List<Block> {
        val blocks = mutableListOf<Block>()
        var child = myNode.firstChildNode
        var currentContext = context
        
        while (child != null) {
            if (!isWhitespaceOrNewline(child.elementType)) {
                // Calculate this child's indent based on current context
                val childIndent = calcContextAwareIndent(child, currentContext)
                
                // Create child block with updated context
                val childContext = updateContext(currentContext, child)
                
                blocks.add(
                    BerryCrushBlock(
                        child,
                        null,
                        null,
                        childIndent,
                        spacingBuilder,
                        indentSize,
                        childContext
                    )
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
    private fun updateContext(ctx: FormattingContext, node: ASTNode): FormattingContext {
        val type = node.elementType
        val text = node.text.trim().lowercase()
        
        return when {
            isFeatureElement(type) || text.startsWith("feature:") -> 
                FormattingContext(inFeature = true)
                
            isFragmentElement(type) || text.startsWith("fragment:") -> 
                FormattingContext(inFragment = true)
                
            isScenarioElement(type) || text.startsWith("scenario:") || text.startsWith("outline:") ->
                ctx.copy(inScenario = true, inBackground = false, inExamples = false, inStep = false)
                
            type == BerryCrushTokenTypes.BACKGROUND || text.startsWith("background:") ->
                ctx.copy(inBackground = true, inScenario = false, inExamples = false, inStep = false)
                
            type == BerryCrushTokenTypes.EXAMPLES || text.startsWith("examples:") ->
                ctx.copy(inExamples = true, inStep = false)
                
            isStepElement(type) || isStepKeyword(text) ->
                ctx.copy(inStep = true, inExamples = false)
                
            else -> ctx
        }
    }

    /**
     * Calculate indent for a child based on accumulated context.
     * Extracted complexity into helper methods for better maintainability.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun calcContextAwareIndent(child: ASTNode, ctx: FormattingContext): Indent {
        val type = child.elementType
        val text = child.text.trim().lowercase()
        
        return when {
            isTopLevelElement(type, text) -> Indent.getNoneIndent()
            isScenarioLevelElement(type, text) -> calcScenarioIndent(ctx)
            isBackgroundElement(type, text) -> calcBackgroundIndent(ctx)
            isTagElement(type, text) -> calcTagIndent(ctx)
            isExamplesElement(type, text) -> calcExamplesIndent(ctx)
            isStepElement(type) || isStepKeyword(text) -> calcStepIndent(ctx)
            isDirectiveElement(type) || isDirectiveKeyword(text) -> calcDirectiveIndent(ctx)
            isTableRow(type, text) -> calcTableIndent(ctx)
            else -> Indent.getNoneIndent()
        }
    }
    
    // Helper methods for element type detection
    
    private fun isTopLevelElement(type: IElementType, text: String): Boolean =
        isFeatureElement(type) || isFragmentElement(type) || 
        text.startsWith("feature:") || text.startsWith("fragment:")
    
    private fun isScenarioLevelElement(type: IElementType, text: String): Boolean =
        isScenarioElement(type) || text.startsWith("scenario:") || text.startsWith("outline:")
    
    private fun isBackgroundElement(type: IElementType, text: String): Boolean =
        type == BerryCrushTokenTypes.BACKGROUND || text.startsWith("background:")
    
    private fun isTagElement(type: IElementType, text: String): Boolean =
        type == BerryCrushTokenTypes.TAG || text.startsWith("@")
    
    private fun isExamplesElement(type: IElementType, text: String): Boolean =
        type == BerryCrushTokenTypes.EXAMPLES || text.startsWith("examples:")
    
    private fun isTableRow(type: IElementType, text: String): Boolean =
        type == BerryCrushTokenTypes.PIPE || text.startsWith("|")
    
    // Helper methods for indent calculation
    
    private fun calcScenarioIndent(ctx: FormattingContext): Indent =
        if (ctx.inFeature) Indent.getSpaceIndent(indentSize) else Indent.getNoneIndent()
    
    private fun calcBackgroundIndent(ctx: FormattingContext): Indent =
        if (ctx.inFeature) Indent.getSpaceIndent(indentSize) else Indent.getNoneIndent()
    
    private fun calcTagIndent(ctx: FormattingContext): Indent =
        if (ctx.inFeature) Indent.getSpaceIndent(indentSize) else Indent.getNoneIndent()
    
    private fun calcExamplesIndent(ctx: FormattingContext): Indent {
        val depth = if (ctx.inFeature) 2 else 1
        return Indent.getSpaceIndent(depth * indentSize)
    }
    
    private fun calcStepIndent(ctx: FormattingContext): Indent {
        val depth = when {
            ctx.inFragment -> 1
            ctx.inFeature && (ctx.inScenario || ctx.inBackground) -> 2
            ctx.inScenario || ctx.inBackground -> 1
            else -> 0
        }
        return Indent.getSpaceIndent(depth * indentSize)
    }
    
    private fun calcDirectiveIndent(ctx: FormattingContext): Indent {
        val depth = when {
            ctx.inFragment -> 2
            ctx.inFeature && (ctx.inScenario || ctx.inBackground) -> 3
            ctx.inScenario || ctx.inBackground -> 2
            else -> 1
        }
        return Indent.getSpaceIndent(depth * indentSize)
    }
    
    private fun calcTableIndent(ctx: FormattingContext): Indent {
        val depth = when {
            ctx.inExamples && ctx.inFeature -> 3
            ctx.inExamples -> 2
            else -> 1
        }
        return Indent.getSpaceIndent(depth * indentSize)
    }

    override fun getSpacing(child1: Block?, child2: Block): Spacing? {
        return spacingBuilder.getSpacing(this, child1, child2)
    }

    override fun isLeaf(): Boolean = myNode.firstChildNode == null

    override fun getChildAttributes(newChildIndex: Int): ChildAttributes {
        // When pressing Enter after a line, suggest appropriate indent
        val nextIndent = when {
            isFeatureElement(myNode.elementType) || 
            context.inFeature && (myNode.text.trim().startsWith("feature:")) -> 
                Indent.getSpaceIndent(indentSize)
                
            isScenarioElement(myNode.elementType) ||
            myNode.text.trim().lowercase().let { it.startsWith("scenario:") || it.startsWith("outline:") } ->
                Indent.getSpaceIndent(if (context.inFeature) 2 * indentSize else indentSize)
                
            isStepElement(myNode.elementType) || isStepKeyword(myNode.text.trim().lowercase()) ->
                Indent.getSpaceIndent(if (context.inFeature) 3 * indentSize else 2 * indentSize)
                
            else -> Indent.getNoneIndent()
        }
        return ChildAttributes(nextIndent, null)
    }

    // Helper methods
    
    private fun isWhitespaceOrNewline(type: IElementType): Boolean {
        return type == TokenType.WHITE_SPACE || 
               type == BerryCrushTokenTypes.WHITE_SPACE ||
               type == BerryCrushTokenTypes.NEWLINE
    }

    private fun isFeatureElement(type: IElementType): Boolean {
        return type == BerryCrushTokenTypes.FEATURE ||
               type == BerryCrushElementTypes.FEATURE
    }

    private fun isFragmentElement(type: IElementType): Boolean {
        return type == BerryCrushTokenTypes.FRAGMENT ||
               type == BerryCrushElementTypes.FRAGMENT
    }

    private fun isScenarioElement(type: IElementType): Boolean {
        return type == BerryCrushTokenTypes.SCENARIO ||
               type == BerryCrushTokenTypes.OUTLINE ||
               type == BerryCrushElementTypes.SCENARIO
    }

    private fun isStepElement(type: IElementType): Boolean {
        return type == BerryCrushTokenTypes.GIVEN ||
               type == BerryCrushTokenTypes.WHEN ||
               type == BerryCrushTokenTypes.THEN ||
               type == BerryCrushTokenTypes.AND ||
               type == BerryCrushTokenTypes.BUT ||
               type == BerryCrushElementTypes.STEP
    }
    
    private fun isStepKeyword(text: String): Boolean {
        return text.startsWith("given ") || text.startsWith("when ") ||
               text.startsWith("then ") || text.startsWith("and ") ||
               text.startsWith("but ")
    }

    private fun isDirectiveElement(type: IElementType): Boolean {
        return type == BerryCrushTokenTypes.CALL ||
               type == BerryCrushTokenTypes.ASSERT ||
               type == BerryCrushTokenTypes.EXTRACT ||
               type == BerryCrushTokenTypes.INCLUDE ||
               type == BerryCrushElementTypes.CALL_DIRECTIVE ||
               type == BerryCrushElementTypes.ASSERT_DIRECTIVE ||
               type == BerryCrushElementTypes.INCLUDE_DIRECTIVE
    }
    
    private fun isDirectiveKeyword(text: String): Boolean {
        return text.startsWith("call ") || text.startsWith("assert ") ||
               text.startsWith("include ") || text.startsWith("extract ") ||
               text.startsWith("body:")
    }
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
    val inStep: Boolean = false
)
