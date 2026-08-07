package com.berrycrush.intellij.structure

import com.berrycrush.intellij.BerryCrushIcons
import com.berrycrush.intellij.psi.BerryCrushAssertElement
import com.berrycrush.intellij.psi.BerryCrushBlockElement
import com.berrycrush.intellij.psi.BerryCrushCallElement
import com.berrycrush.intellij.psi.BerryCrushDirectiveElement
import com.berrycrush.intellij.psi.BerryCrushElseElement
import com.berrycrush.intellij.psi.BerryCrushExtractElement
import com.berrycrush.intellij.psi.BerryCrushFeatureElement
import com.berrycrush.intellij.psi.BerryCrushFile
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushIfElement
import com.berrycrush.intellij.psi.BerryCrushIncludeElement
import com.berrycrush.intellij.psi.BerryCrushOutlineElement
import com.berrycrush.intellij.psi.BerryCrushParametersElement
import com.berrycrush.intellij.psi.BerryCrushPsiElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
import com.berrycrush.intellij.psi.BerryCrushScenarioLikeElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.ide.structureView.StructureViewModelBase
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.util.treeView.smartTree.TreeElement
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.Icon

/**
 * Structure view factory for BerryCrush files.
 */
class BerryCrushStructureViewFactory : PsiStructureViewFactory {
    override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
        if (psiFile !is BerryCrushFile) return null

        return object : TreeBasedStructureViewBuilder() {
            override fun createStructureViewModel(editor: Editor?): StructureViewModel = BerryCrushStructureViewModel(psiFile, editor)
        }
    }
}

/**
 * Structure view model for BerryCrush files.
 */
class BerryCrushStructureViewModel(
    psiFile: BerryCrushFile,
    editor: Editor?,
) : StructureViewModelBase(psiFile, editor, BerryCrushStructureViewElement(psiFile)) {
    override fun getSuitableClasses(): Array<Class<*>> = arrayOf(
        BerryCrushFile::class.java,
        BerryCrushScenarioLikeElement::class.java,
        BerryCrushFeatureElement::class.java,
        BerryCrushStepElement::class.java,
        BerryCrushDirectiveElement::class.java,
    )
}

/**
 * Structure view tree element for BerryCrush PSI elements.
 *
 * Handles two PSI structures:
 * - Fragments: Content is nested inside (use PsiTreeUtil.findChildrenOfType)
 * - Scenarios: Only header, content is siblings (scan siblings)
 *
 * Hierarchy:
 * - File
 *   - Scenario/Fragment/Feature
 *     - Step (given/when/then/and/but)
 *       - Directive (call/assert/include)
 */
class BerryCrushStructureViewElement(
    private val element: PsiElement,
) : StructureViewTreeElement {
    override fun getValue(): Any = element

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String = getElementText()

        override fun getLocationString(): String? = null

        override fun getIcon(unused: Boolean): Icon? = getElementIcon()
    }

    override fun getChildren(): Array<TreeElement> {
        val children = mutableListOf<TreeElement>()

        when (element) {
            // File level: collect blocks from file children
            is BerryCrushFile -> {
                collectBlocks(element, children)
            }
            // Fragment:, Scenario: Outline: Background: Content is NESTED inside (parser includes content in marker)
            is BerryCrushScenarioLikeElement -> {
                collectNestedSteps(element, children)
            }
            // Feature: Contains scenarios (and possibly steps)
            is BerryCrushFeatureElement -> {
                collectChildrenForFeature(element, children)
            }
            // Step level: collect nested or sibling directives
            is BerryCrushStepElement -> {
                collectDirectivesForStep(element, children)
            }
            // if-else
            is BerryCrushIfElement, is BerryCrushElseElement -> {
                collectConditionalDirectives(element, children)
            }
        }

        return children.toTypedArray()
    }

    /**
     * Collect block elements (scenarios, fragments, features) from file children.
     */
    private fun collectBlocks(
        file: BerryCrushFile,
        result: MutableList<TreeElement>,
    ) {
        // Use PsiTreeUtil to find all block elements in the file
        file.children.filter {
            it is BerryCrushParametersElement || it is BerryCrushBlockElement
        }.sortedBy {
            it.textOffset
        }.forEach { result.add(BerryCrushStructureViewElement(it)) }
    }

    /**
     * Collect steps that are NESTED inside a fragment (parser nests content).
     */
    private fun collectNestedSteps(
        scenarioLike: BerryCrushScenarioLikeElement,
        result: MutableList<TreeElement>,
    ) {
        scenarioLike.parameter?.let { result.add(BerryCrushStructureViewElement(it)) }

        val steps = scenarioLike.steps
        steps.sortedBy { it.textOffset }.forEach { result.add(BerryCrushStructureViewElement(it)) }
    }

    /**
     * Collect children for a feature (scenarios and/or steps).
     * Features can contain scenarios or steps directly.
     */
    private fun collectChildrenForFeature(
        feature: BerryCrushFeatureElement,
        result: MutableList<TreeElement>,
    ) {
        feature.parameter?.let { result.add(BerryCrushStructureViewElement(it)) }
        // Find all scenarios after this feature (but before the next feature)
        val scenariosInFeature = feature.scenarios

        if (scenariosInFeature.isNotEmpty()) {
            // Feature has scenarios - show scenarios as children
            scenariosInFeature.sortedBy { it.textOffset }.forEach {
                result.add(BerryCrushStructureViewElement(it))
            }
        }
    }

    private fun collectConditionalDirectives(element: BerryCrushPsiElement, result: MutableList<TreeElement>) {
        element.children.filterIsInstance<BerryCrushDirectiveElement>()
            .sortedBy { it.textOffset }
            .forEach { result.add(BerryCrushStructureViewElement(it)) }
    }

    /**
     * Collect directives for a step.
     * Check both nested children and siblings (parser behavior varies).
     */
    private fun collectDirectivesForStep(
        step: BerryCrushStepElement,
        result: MutableList<TreeElement>,
    ) {
        // First, check for nested directives (inside step element)
        val nestedDirectives = step.directives.sortedBy { it.textOffset }
        nestedDirectives.forEach { result.add(BerryCrushStructureViewElement(it)) }
    }

    override fun navigate(requestFocus: Boolean) {
        if (element is Navigatable) {
            element.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean = element is Navigatable && element.canNavigate()

    override fun canNavigateToSource(): Boolean = element is Navigatable && element.canNavigateToSource()

    private fun getElementText(): String = when (element) {
        is BerryCrushFile -> element.name
        is BerryCrushBlockElement -> "${element.keyword}: ${element.description ?: ""}"
        is BerryCrushStepElement -> "${element.keyword?.lowercase() ?: ""} ${element.stepText ?: ""}".trim().take(60)
        is BerryCrushCallElement -> "call ^${element.operationId ?: ""}"
        is BerryCrushAssertElement ->
            element.text
                .trim()
                .takeWhile { it != '\n' }
                .take(60)
        is BerryCrushIncludeElement -> "include ${element.fragmentName ?: ""}"
        else -> {
            // Fallback for other elements
            val text = element.text.trim().takeWhile { it != '\n' && it != '\r' }
            text.take(60)
        }
    }

    private fun getElementIcon(): Icon? = when (element) {
        is BerryCrushFile -> element.name.let { fileName ->
            when {
                fileName.endsWith(".scenario") -> BerryCrushIcons.SCENARIO_FILE
                fileName.endsWith(".fragment") -> BerryCrushIcons.FRAGMENT_FILE
                else -> null // should never happen
            }
        }
        is BerryCrushScenarioElement -> BerryCrushIcons.SCENARIO
        is BerryCrushOutlineElement -> BerryCrushIcons.SCENARIO
        is BerryCrushFragmentElement -> BerryCrushIcons.FRAGMENT
        is BerryCrushFeatureElement -> BerryCrushIcons.FEATURE
        is BerryCrushStepElement -> BerryCrushIcons.STEP
        is BerryCrushCallElement -> BerryCrushIcons.OPERATION
        is BerryCrushAssertElement -> BerryCrushIcons.ASSERTION
        is BerryCrushIncludeElement -> BerryCrushIcons.INCLUDE
        is BerryCrushIfElement, is BerryCrushElseElement -> BerryCrushIcons.CONDITIONAL
        is BerryCrushExtractElement -> BerryCrushIcons.EXTRACT
        is BerryCrushParametersElement -> BerryCrushIcons.PARAMETER
        else -> null
    }
}
