package org.berrycrush.intellij.parser.impl

import com.intellij.lang.PsiBuilder
import org.berrycrush.intellij.psi.BerryCrushElementTypes

internal fun PsiBuilder.parseFragment() {
    val marker = mark()
    advanceLexer()
    skipToEndOfLine(BerryCrushElementTypes.BLOCK_NAME)
    parseScenarioContent(0)

    marker.done(BerryCrushElementTypes.FRAGMENT)
}
