package com.berrycrush.intellij.structure

import com.berrycrush.intellij.BerryCrushIcons
import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushFile
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementType
import javax.swing.Icon

/**
 * Structure view factory for BerryCrush files.
 */
class BerryCrushStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is BerryCrushFile) return null

        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel {
                return BerryCrushStructureViewModel(psiFile, editor)
            }
        }
    }
}

/**
 * Structure view model for BerryCrush files.
 */
class BerryCrushStructureViewModel(
    psiFile: BerryCrushFile,
    editor: Editor?
) : StructureViewModelBase(psiFile, editor, BerryCrushStructureViewElement(psiFile)) {
    override fun getSuitableClasses(): Array<Class<*>> = emptyArray()
}

/**
 * Structure view tree element for BerryCrush PSI elements.
 */
class BerryCrushStructureViewElement(private val element: PsiElement) : StructureViewTreeElement {

    override fun getValue(): Any = element

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String? = getElementText()
        override fun getLocationString(): String? = null
        override fun getIcon(unused: Boolean): Icon? = getElementIcon()
    }

    override fun getChildren(): Array<TreeElement> {
        val children = mutableListOf<TreeElement>()

        // Scan for structure elements in the file
        if (element is BerryCrushFile) {
            scanForStructureElements(element, children)
        }

        return children.toTypedArray()
    }

    private fun scanForStructureElements(parent: PsiElement, result: MutableList<TreeElement>) {
        var child = parent.firstChild
        while (child != null) {
            val elementType = child.node?.elementType

            when (elementType) {
                BerryCrushTokenTypes.FEATURE,
                BerryCrushTokenTypes.SCENARIO,
                BerryCrushTokenTypes.OUTLINE,
                BerryCrushTokenTypes.FRAGMENT -> {
                    result.add(BerryCrushStructureViewElement(child))
                }
            }

            // Recursively scan children (but not for already added elements)
            if (elementType !in STRUCTURE_TYPES) {
                scanForStructureElements(child, result)
            }

            child = child.nextSibling
        }
    }

    override fun navigate(requestFocus: Boolean) {
        if (element is com.intellij.pom.Navigatable) {
            element.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean = element is com.intellij.pom.Navigatable && element.canNavigate()

    override fun canNavigateToSource(): Boolean = element is com.intellij.pom.Navigatable && element.canNavigateToSource()

    private fun getElementText(): String {
        if (element is BerryCrushFile) {
            return element.name
        }

        val elementType = element.node?.elementType
        val prefix = when (elementType) {
            BerryCrushTokenTypes.FEATURE -> "feature"
            BerryCrushTokenTypes.SCENARIO -> "scenario"
            BerryCrushTokenTypes.OUTLINE -> "outline"
            BerryCrushTokenTypes.FRAGMENT -> "fragment"
            else -> return element.text.take(50)
        }

        // Try to get the name from the rest of the line
        val text = element.text
        val colonIndex = text.indexOf(':')
        return if (colonIndex >= 0) {
            val name = text.substring(colonIndex + 1).trim().takeWhile { it != '\n' && it != '\r' }
            "$prefix: $name"
        } else {
            prefix
        }
    }

    private fun getElementIcon(): Icon? {
        if (element is BerryCrushFile) {
            return BerryCrushIcons.SCENARIO_FILE
        }

        return when (element.node?.elementType) {
            BerryCrushTokenTypes.FEATURE -> BerryCrushIcons.FEATURE
            BerryCrushTokenTypes.SCENARIO,
            BerryCrushTokenTypes.OUTLINE -> BerryCrushIcons.SCENARIO
            BerryCrushTokenTypes.FRAGMENT -> BerryCrushIcons.FRAGMENT_FILE
            else -> null
        }
    }

    companion object {
        private val STRUCTURE_TYPES = setOf(
            BerryCrushTokenTypes.FEATURE,
            BerryCrushTokenTypes.SCENARIO,
            BerryCrushTokenTypes.OUTLINE,
            BerryCrushTokenTypes.FRAGMENT,
        )
    }
}
