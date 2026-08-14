package org.berrycrush.intellij.navigation

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import org.berrycrush.intellij.lexer.BerryCrushLexer
import org.berrycrush.intellij.lexer.BerryCrushTokenTypes
import org.berrycrush.intellij.psi.BerryCrushFile

/**
 * Find Usages provider for BerryCrush elements.
 *
 * Enables "Find Usages" (Alt+F7) for fragments and operations.
 */
class BerryCrushFindUsagesProvider : FindUsagesProvider {
    override fun canFindUsagesFor(element: PsiElement): Boolean = element is PsiNamedElement

    override fun getHelpId(element: PsiElement): String? = null

    override fun getType(element: PsiElement): String = when (element) {
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

    override fun getDescriptiveName(element: PsiElement): String = when (element) {
        is PsiNamedElement -> element.name ?: "<unnamed>"
        else -> element.text.take(30)
    }

    override fun getNodeText(
        element: PsiElement,
        useFullName: Boolean,
    ): String = getDescriptiveName(element)

    override fun getWordsScanner(): WordsScanner = DefaultWordsScanner(
        BerryCrushLexer(),
        BerryCrushTokenTypes.IDENTIFIERS,
        BerryCrushTokenTypes.COMMENTS,
        BerryCrushTokenTypes.STRINGS,
    )
}
