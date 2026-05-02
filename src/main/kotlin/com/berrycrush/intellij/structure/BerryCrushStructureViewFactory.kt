package com.berrycrush.intellij.structure

import com.berrycrush.intellij.BerryCrushIcons
import com.berrycrush.intellij.psi.BerryCrushAssertElement
import com.berrycrush.intellij.psi.BerryCrushCallElement
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.berrycrush.intellij.psi.BerryCrushFeatureElement
import com.berrycrush.intellij.psi.BerryCrushFile
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushIncludeElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
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
    override fun getSuitableClasses(): Array<Class<*>> = arrayOf(
        BerryCrushFile::class.java,
        BerryCrushScenarioElement::class.java,
        BerryCrushFragmentElement::class.java,
        BerryCrushFeatureElement::class.java,
        BerryCrushStepElement::class.java,
        BerryCrushCallElement::class.java,
        BerryCrushAssertElement::class.java,
        BerryCrushIncludeElement::class.java,
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
class BerryCrushStructureViewElement(private val element: PsiElement) : StructureViewTreeElement {

    override fun getValue(): Any = element

    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String? = getElementText()
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
            // Fragment: Content is NESTED inside (parser includes content in marker)
            is BerryCrushFragmentElement -> {
                collectNestedSteps(element, children)
            }
            // Feature: Contains scenarios (and possibly steps)
            is BerryCrushFeatureElement -> {
                collectChildrenForFeature(element, children)
            }
            // Scenario: Content is SIBLINGS after (parser only marks header)
            is BerryCrushScenarioElement -> {
                collectSiblingStepsForBlock(element, children)
            }
            // Step level: collect nested or sibling directives
            is BerryCrushStepElement -> {
                collectDirectivesForStep(element, children)
            }
        }

        return children.toTypedArray()
    }

    /**
     * Collect block elements (scenarios, fragments, features) from file children.
     */
    private fun collectBlocks(file: BerryCrushFile, result: MutableList<TreeElement>) {
        // Use PsiTreeUtil to find all block elements in the file
        val scenarios = PsiTreeUtil.findChildrenOfType(file, BerryCrushScenarioElement::class.java)
        val fragments = PsiTreeUtil.findChildrenOfType(file, BerryCrushFragmentElement::class.java)
        val features = PsiTreeUtil.findChildrenOfType(file, BerryCrushFeatureElement::class.java)

        // Combine and sort by offset to maintain file order
        val allBlocks = (scenarios + fragments + features).sortedBy { it.textOffset }
        allBlocks.forEach { result.add(BerryCrushStructureViewElement(it)) }
    }

    /**
     * Collect steps that are NESTED inside a fragment (parser nests content).
     */
    private fun collectNestedSteps(fragment: BerryCrushFragmentElement, result: MutableList<TreeElement>) {
        val steps = PsiTreeUtil.findChildrenOfType(fragment, BerryCrushStepElement::class.java)
        steps.sortedBy { it.textOffset }.forEach { result.add(BerryCrushStructureViewElement(it)) }
    }

    /**
     * Collect children for a feature (scenarios and/or steps).
     * Features can contain scenarios or steps directly.
     */
    private fun collectChildrenForFeature(feature: BerryCrushFeatureElement, result: MutableList<TreeElement>) {
        val file = feature.containingFile
        if (file == null) return

        val featureEndOffset = findNextFeatureOffset(feature) ?: Int.MAX_VALUE
        val featureStartOffset = feature.textOffset

        // Find all scenarios after this feature (but before the next feature)
        val allScenarios = PsiTreeUtil.findChildrenOfType(file, BerryCrushScenarioElement::class.java)
        val scenariosInFeature = allScenarios.filter {
            it.textOffset > featureStartOffset && it.textOffset < featureEndOffset
        }

        // Find all steps after this feature that aren't part of a scenario
        val allSteps = PsiTreeUtil.findChildrenOfType(file, BerryCrushStepElement::class.java)

        if (scenariosInFeature.isNotEmpty()) {
            // Feature has scenarios - show scenarios as children
            scenariosInFeature.sortedBy { it.textOffset }.forEach {
                result.add(BerryCrushStructureViewElement(it))
            }
        } else {
            // Feature has no scenarios - show steps directly
            val stepsInFeature = allSteps.filter {
                it.textOffset > featureStartOffset && it.textOffset < featureEndOffset
            }
            stepsInFeature.sortedBy { it.textOffset }.forEach {
                result.add(BerryCrushStructureViewElement(it))
            }
        }
    }

    /**
     * Find the offset of the next feature element.
     */
    private fun findNextFeatureOffset(feature: BerryCrushFeatureElement): Int? {
        val file = feature.containingFile ?: return null
        val allFeatures = PsiTreeUtil.findChildrenOfType(file, BerryCrushFeatureElement::class.java)
        val sortedFeatures = allFeatures.sortedBy { it.textOffset }
        val currentIndex = sortedFeatures.indexOfFirst { it.textOffset == feature.textOffset }
        if (currentIndex < 0 || currentIndex >= sortedFeatures.size - 1) {
            return null
        }
        return sortedFeatures[currentIndex + 1].textOffset
    }

    /**
     * Collect steps that are SIBLINGS after a scenario/feature (parser only marks header).
     * Also tries PsiTreeUtil as fallback if direct sibling traversal fails.
     */
    private fun collectSiblingStepsForBlock(block: PsiElement, result: MutableList<TreeElement>) {
        // First, try direct sibling traversal
        var sibling = block.nextSibling
        while (sibling != null) {
            // Stop at next block
            if (isBlockElement(sibling)) {
                break
            }
            // Collect steps
            if (sibling is BerryCrushStepElement) {
                result.add(BerryCrushStructureViewElement(sibling))
            }
            sibling = sibling.nextSibling
        }

        // If no steps found via siblings, try finding steps in the file that come after this block
        if (result.isEmpty()) {
            val file = block.containingFile
            if (file != null) {
                val allSteps = PsiTreeUtil.findChildrenOfType(file, BerryCrushStepElement::class.java)
                val blockEndOffset = block.textRange.endOffset
                val nextBlockOffset = findNextBlockOffset(block)

                for (step in allSteps) {
                    val stepOffset = step.textOffset
                    if (stepOffset > blockEndOffset && (nextBlockOffset == null || stepOffset < nextBlockOffset)) {
                        result.add(BerryCrushStructureViewElement(step))
                    }
                }
            }
        }
    }

    /**
     * Find the offset of the next block element after the given block.
     */
    private fun findNextBlockOffset(block: PsiElement): Int? {
        var sibling = block.nextSibling
        while (sibling != null) {
            if (isBlockElement(sibling)) {
                return sibling.textOffset
            }
            sibling = sibling.nextSibling
        }
        return null
    }

    /**
     * Collect directives for a step.
     * Check both nested children and siblings (parser behavior varies).
     */
    private fun collectDirectivesForStep(step: BerryCrushStepElement, result: MutableList<TreeElement>) {
        // First, check for nested directives (inside step element)
        val nestedCalls = PsiTreeUtil.findChildrenOfType(step, BerryCrushCallElement::class.java)
        val nestedAsserts = PsiTreeUtil.findChildrenOfType(step, BerryCrushAssertElement::class.java)
        val nestedIncludes = PsiTreeUtil.findChildrenOfType(step, BerryCrushIncludeElement::class.java)

        val nestedDirectives = (nestedCalls + nestedAsserts + nestedIncludes).sortedBy { it.textOffset }
        nestedDirectives.forEach { result.add(BerryCrushStructureViewElement(it)) }

        // If no nested directives, check siblings
        if (result.isEmpty()) {
            var sibling = step.nextSibling
            while (sibling != null) {
                // Stop at next step or block
                if (sibling is BerryCrushStepElement || isBlockElement(sibling)) {
                    break
                }
                // Collect directives
                if (isDirectiveElement(sibling)) {
                    result.add(BerryCrushStructureViewElement(sibling))
                }
                sibling = sibling.nextSibling
            }
        }
    }

    private fun isBlockElement(element: PsiElement): Boolean =
        element is BerryCrushScenarioElement ||
            element is BerryCrushFragmentElement ||
            element is BerryCrushFeatureElement ||
            element.node?.elementType in BLOCK_ELEMENT_TYPES

    private fun isDirectiveElement(element: PsiElement): Boolean =
        element is BerryCrushCallElement ||
            element is BerryCrushAssertElement ||
            element is BerryCrushIncludeElement ||
            element.node?.elementType in DIRECTIVE_ELEMENT_TYPES

    override fun navigate(requestFocus: Boolean) {
        if (element is com.intellij.pom.Navigatable) {
            element.navigate(requestFocus)
        }
    }

    override fun canNavigate(): Boolean = element is com.intellij.pom.Navigatable && element.canNavigate()

    override fun canNavigateToSource(): Boolean =
        element is com.intellij.pom.Navigatable && element.canNavigateToSource()

    private fun getElementText(): String = when (element) {
        is BerryCrushFile -> element.name
        is BerryCrushScenarioElement -> "scenario: ${element.scenarioName ?: ""}"
        is BerryCrushFragmentElement -> "fragment: ${element.fragmentName ?: ""}"
        is BerryCrushFeatureElement -> "feature: ${element.featureName ?: ""}"
        is BerryCrushStepElement -> "${element.keyword?.lowercase() ?: ""} ${element.stepText ?: ""}".trim().take(60)
        is BerryCrushCallElement -> "call ^${element.operationId ?: ""}"
        is BerryCrushAssertElement -> element.text.trim().takeWhile { it != '\n' }.take(60)
        is BerryCrushIncludeElement -> "include ${element.fragmentName ?: ""}"
        else -> {
            // Fallback for other elements
            val text = element.text.trim().takeWhile { it != '\n' && it != '\r' }
            text.take(60)
        }
    }

    private fun getElementIcon(): Icon? = when (element) {
        is BerryCrushFile -> BerryCrushIcons.SCENARIO_FILE
        is BerryCrushScenarioElement -> BerryCrushIcons.SCENARIO
        is BerryCrushFragmentElement -> BerryCrushIcons.FRAGMENT_FILE
        is BerryCrushFeatureElement -> BerryCrushIcons.FEATURE
        is BerryCrushStepElement -> BerryCrushIcons.STEP
        is BerryCrushCallElement -> BerryCrushIcons.OPERATION
        is BerryCrushAssertElement -> BerryCrushIcons.ASSERTION
        is BerryCrushIncludeElement -> BerryCrushIcons.FRAGMENT_FILE
        else -> null
    }

    companion object {
        // PSI element types for blocks (used as fallback when class check fails)
        private val BLOCK_ELEMENT_TYPES = setOf(
            BerryCrushElementTypes.FEATURE,
            BerryCrushElementTypes.SCENARIO,
            BerryCrushElementTypes.OUTLINE,
            BerryCrushElementTypes.FRAGMENT,
            BerryCrushElementTypes.BACKGROUND,
            BerryCrushElementTypes.EXAMPLES,
        )

        // PSI element types for directives
        private val DIRECTIVE_ELEMENT_TYPES = setOf(
            BerryCrushElementTypes.CALL_DIRECTIVE,
            BerryCrushElementTypes.ASSERT_DIRECTIVE,
            BerryCrushElementTypes.EXTRACT_DIRECTIVE,
            BerryCrushElementTypes.INCLUDE_DIRECTIVE,
        )
    }
}
