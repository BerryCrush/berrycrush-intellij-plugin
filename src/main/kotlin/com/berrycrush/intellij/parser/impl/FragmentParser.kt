package com.berrycrush.intellij.parser.impl

import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.intellij.lang.PsiBuilder

internal fun PsiBuilder.parseFragment() {
    val marker = mark()
    advanceLexer()
    skipToEndOfLine()
    parseScenarioContent(0)

    marker.done(BerryCrushElementTypes.FRAGMENT)
}
