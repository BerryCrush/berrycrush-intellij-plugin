package com.berrycrush.intellij.refactoring.fragment

import com.intellij.openapi.util.TextRange
import com.intellij.psi.AbstractElementManipulator
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Manipulator for fragment name elements in fragment definitions and include directives.
 *
 * Handles text range extraction and replacement for rename refactoring.
 * Works with LeafPsiElement tokens that represent the fragment name.
 */
class FragmentNameManipulator : AbstractElementManipulator<LeafPsiElement>() {

    override fun handleContentChange(
        element: LeafPsiElement,
        range: TextRange,
        newContent: String,
    ): LeafPsiElement? {
        val oldText = element.text
        val newText = buildString {
            append(oldText.substring(0, range.startOffset))
            append(newContent)
            append(oldText.substring(range.endOffset))
        }

        return element.replaceWithText(newText) as? LeafPsiElement
    }

    override fun getRangeInElement(element: LeafPsiElement): TextRange {
        val text = element.text

        // For fragment definition: "fragment: name" -> extract "name" range
        FRAGMENT_DEF_PATTERN.find(text)?.let { match ->
            val nameGroup = match.groups[1] ?: return TextRange.EMPTY_RANGE
            return TextRange(nameGroup.range.first, nameGroup.range.last + 1)
        }

        // For include directive: "include name" -> extract "name" range
        INCLUDE_PATTERN.find(text)?.let { match ->
            val nameGroup = match.groups[1] ?: return TextRange.EMPTY_RANGE
            return TextRange(nameGroup.range.first, nameGroup.range.last + 1)
        }

        // For plain fragment name token (already isolated)
        return TextRange.allOf(text)
    }

    companion object {
        private val FRAGMENT_DEF_PATTERN = Regex("""[Ff]ragment:\s*(\S+)""")
        private val INCLUDE_PATTERN = Regex("""include\s+(\^?[a-zA-Z_][a-zA-Z0-9_.\-]*)""")
    }
}
