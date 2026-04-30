package com.berrycrush.intellij.navigation

import com.berrycrush.intellij.psi.BerryCrushFile
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.berrycrush.intellij.lexer.BerryCrushLexer
import com.berrycrush.intellij.lexer.BerryCrushTokenTypes

/**
 * Find Usages provider for BerryCrush elements.
 *
 * Enables "Find Usages" (Alt+F7) for fragments and operations.
 */
class BerryCrushFindUsagesProvider : FindUsagesProvider {

    override fun canFindUsagesFor(element: PsiElement): Boolean {
        return element is PsiNamedElement || element is BerryCrushFile
    }

    override fun getHelpId(element: PsiElement): String? = null

    override fun getType(element: PsiElement): String {
        return when (element) {
            is BerryCrushFile -> {
                val fileName = element.name
                when {
                    fileName.endsWith(".fragment") -> "fragment"
                    fileName.endsWith(".scenario") -> "scenario"
                    else -> "file"
                }
            }
            else -> "element"
        }
    }

    override fun getDescriptiveName(element: PsiElement): String {
        return when (element) {
            is PsiNamedElement -> element.name ?: "<unnamed>"
            is BerryCrushFile -> element.name
            else -> element.text.take(30)
        }
    }

    override fun getNodeText(element: PsiElement, useFullName: Boolean): String {
        return getDescriptiveName(element)
    }

    override fun getWordsScanner(): WordsScanner {
        return DefaultWordsScanner(
            BerryCrushLexer(),
            BerryCrushTokenTypes.IDENTIFIERS,
            BerryCrushTokenTypes.COMMENTS,
            BerryCrushTokenTypes.STRINGS
        )
    }
}
