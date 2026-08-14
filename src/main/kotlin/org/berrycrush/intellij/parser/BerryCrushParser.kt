package org.berrycrush.intellij.parser

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiParser
import com.intellij.psi.tree.IElementType
import org.berrycrush.intellij.lexer.BerryCrushTokenTypes
import org.berrycrush.intellij.parser.impl.consumeLineIndent
import org.berrycrush.intellij.parser.impl.parseAssertDirective
import org.berrycrush.intellij.parser.impl.parseBackground
import org.berrycrush.intellij.parser.impl.parseCallDirective
import org.berrycrush.intellij.parser.impl.parseExtractDirective
import org.berrycrush.intellij.parser.impl.parseFeature
import org.berrycrush.intellij.parser.impl.parseFragment
import org.berrycrush.intellij.parser.impl.parseIncludeDirective
import org.berrycrush.intellij.parser.impl.parseOperationRef
import org.berrycrush.intellij.parser.impl.parseOutline
import org.berrycrush.intellij.parser.impl.parseParameters
import org.berrycrush.intellij.parser.impl.parseScenario
import org.berrycrush.intellij.parser.impl.parseStep
import org.berrycrush.intellij.parser.impl.parseTag
import org.berrycrush.intellij.parser.impl.parseWebhookDirective
import org.berrycrush.intellij.parser.impl.skipToEndOfLine

/**
 * Parser for BerryCrush language.
 *
 * Creates PSI elements for navigation support (Cmd+Click).
 */
class BerryCrushParser : PsiParser {
    override fun parse(
        root: IElementType,
        builder: PsiBuilder,
    ): ASTNode {
        val rootMarker = builder.mark()

        while (!builder.eof()) {
            parseTopLevel(builder)
        }

        rootMarker.done(root)
        return builder.treeBuilt
    }

    private fun parseTopLevel(builder: PsiBuilder) {
        when (builder.tokenType) {
            BerryCrushTokenTypes.FEATURE -> builder.parseFeature(0)
            BerryCrushTokenTypes.SCENARIO -> builder.parseScenario(0)
            BerryCrushTokenTypes.OUTLINE -> builder.parseOutline(0)
            BerryCrushTokenTypes.FRAGMENT -> builder.parseFragment()
            BerryCrushTokenTypes.BACKGROUND -> builder.parseBackground(0)
            BerryCrushTokenTypes.PARAMETERS -> builder.parseParameters(0)
            BerryCrushTokenTypes.CALL -> builder.parseCallDirective(0)
            BerryCrushTokenTypes.WEBHOOK -> builder.parseWebhookDirective(0)
            BerryCrushTokenTypes.INCLUDE -> builder.parseIncludeDirective(0)
            BerryCrushTokenTypes.OPERATION_REF -> builder.parseOperationRef()
            BerryCrushTokenTypes.EXTRACT -> builder.parseExtractDirective()
            // Step keywords
            BerryCrushTokenTypes.GIVEN,
            BerryCrushTokenTypes.WHEN,
            BerryCrushTokenTypes.THEN,
            BerryCrushTokenTypes.AND,
            BerryCrushTokenTypes.BUT,
            -> builder.parseStep(0)
            // Assert directive
            BerryCrushTokenTypes.ASSERT -> builder.parseAssertDirective()
            BerryCrushTokenTypes.INDENT -> builder.parseTopLevelIndentedLine()
            BerryCrushTokenTypes.TAG -> builder.parseTag()
            else -> builder.advanceLexer()
        }
    }
}

private fun PsiBuilder.parseTopLevelIndentedLine() {
    consumeLineIndent()
    skipToEndOfLine()
}
