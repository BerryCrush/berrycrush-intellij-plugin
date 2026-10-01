package org.berrycrush.intellij.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import org.berrycrush.intellij.lexer.BerryCrushLexer

private val IDENTIFIER_SYMBOLS = listOf('-', '_', '.')

class BerryCrushNamesValidator: NamesValidator {
    override fun isKeyword(name: String, project: Project): Boolean = BerryCrushLexer.KEYWORDS.contains(name)

    override fun isIdentifier(name: String, project: Project): Boolean = name.isNotEmpty() && name.all { it.isLetterOrDigit() || it in IDENTIFIER_SYMBOLS }
}