package com.berrycrush.intellij.parser.impl

import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.intellij.lang.PsiBuilder

internal fun PsiBuilder.parseFragment() {
    val marker = mark()
    advanceLexer()
    skipToEndOfLine(BerryCrushElementTypes.BLOCK_NAME)
    parseScenarioContent(0)

    marker.done(BerryCrushElementTypes.FRAGMENT)
}
