package com.berrycrush.intellij.navigation

import com.berrycrush.intellij.BerryCrushIcons
import com.berrycrush.intellij.index.IncludeUsageIndex
import com.berrycrush.intellij.psi.BerryCrushAssertElement
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushIncludeElement
import com.berrycrush.intellij.psi.BerryCrushOperationRefElement
import com.berrycrush.intellij.psi.BerryCrushPsiElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.berrycrush.intellij.reference.BerryCrushAssertionReference
import com.berrycrush.intellij.reference.BerryCrushFragmentReference
import com.berrycrush.intellij.reference.BerryCrushOperationReference
import com.berrycrush.intellij.reference.BerryCrushStepReference
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Provides gutter icons for BerryCrush navigation.
 *
 * Shows icons for:
 * - Fragment definitions (links to usages - reverse navigation)
 * - Include directives (links to fragment file)
 * - Operation references (links to OpenAPI spec)
 * - Step definitions (links to @Step annotated methods)
 * - Assertion definitions (links to @Assertion annotated methods)
 */
private val CLASS_LIST: Array<Class<out BerryCrushPsiElement>> = arrayOf(BerryCrushFragmentElement::class.java, BerryCrushIncludeElement::class.java, BerryCrushStepElement::class.java, BerryCrushAssertElement::class.java, BerryCrushOperationRefElement::class.java)
class BerryCrushLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>,
    ) {
        val marker = when (val e = getOneOfParent(element)) {
            is BerryCrushFragmentElement -> e.markFragment(element)
            is BerryCrushIncludeElement -> e.markInclude(element)
            is BerryCrushStepElement -> e.markStep(element)
            is BerryCrushAssertElement -> e.markAssert(element)
            is BerryCrushOperationRefElement -> e.markOperationReference(element)
            else -> null
        }
        if (marker != null) {
            result += marker
        }
    }

    private fun getOneOfParent(element: PsiElement): PsiElement? = CLASS_LIST.mapNotNull {
        PsiTreeUtil.getParentOfType(element, it, true)
    }.firstOrNull { it.firstChild == element }

    private fun BerryCrushFragmentElement.markFragment(element: PsiElement): RelatedItemLineMarkerInfo<*>? {
        return fragmentName?.let { name ->
            val usages = IncludeUsageIndex.findIncludeUsages(this.project, name)
            return if (usages.isNotEmpty()) {
                NavigationGutterIconBuilder
                    .create(AllIcons.Gutter.ImplementingMethod)
                    .setTargets(usages)
                    .setTooltipText("Fragment '$name' - ${usages.size} usage(s)")
                    .setPopupTitle("Usages of fragment '$name'")
                    .createLineMarkerInfo(element)
            } else {
                NavigationGutterIconBuilder
                    .create(BerryCrushIcons.FRAGMENT_FILE)
                    .setTargets(usages)
                    .setTooltipText("Fragment: $fragmentName (no usages)")
                    .setPopupTitle("Fragment '$name'")
                    .createLineMarkerInfo(element)
            }
        }
    }

    private fun BerryCrushIncludeElement.markInclude(element: PsiElement): RelatedItemLineMarkerInfo<*>? {
        return fragmentName?.let { name ->
            val target = BerryCrushFragmentReference.findFragmentByName(project, name)
            return if (target != null) {
                NavigationGutterIconBuilder
                    .create(AllIcons.Gutter.ImplementedMethod)
                    .setTargets(listOf(target))
                    .setTooltipText("Go to fragment: $name")
                    .createLineMarkerInfo(element)
            } else {
                NavigationGutterIconBuilder
                    .create(BerryCrushIcons.FRAGMENT_FILE)
                    .setTargets(listOf())
                    .setTooltipText("Fragment: $fragmentName (not found)")
                    .createLineMarkerInfo(element)
            }
        }
    }

    private fun BerryCrushStepElement.markStep(element: PsiElement): RelatedItemLineMarkerInfo<*>? = stepText?.let { text ->
        BerryCrushStepReference.findMatchingStepMethods(project, text).let { methods ->
            if (methods.isNotEmpty()) {
                NavigationGutterIconBuilder
                    .create(AllIcons.Gutter.ImplementedMethod)
                    .setTargets(methods)
                    .setTooltipText("Go to @Step definition")
                    .setPopupTitle("Step definitions")
                    .createLineMarkerInfo(element)
            } else {
                null
            }
        }
    }

    private fun BerryCrushAssertElement.markAssert(element: PsiElement): RelatedItemLineMarkerInfo<*>? = assertionText?.let { text ->
        BerryCrushAssertionReference.findMatchingAssertionMethods(project, text).let { methods ->
            if (methods.isNotEmpty()) {
                NavigationGutterIconBuilder
                    .create(AllIcons.Gutter.ImplementedMethod)
                    .setTargets(methods)
                    .setTooltipText("Go to @Assertion definition")
                    .setPopupTitle("Assertion definitions")
                    .createLineMarkerInfo(element)
            } else {
                null
            }
        }
    }

    private fun BerryCrushOperationRefElement.markOperationReference(element: PsiElement): RelatedItemLineMarkerInfo<*>? = BerryCrushOperationReference.findOperationInOpenAPI(project, operationId)?.let { target ->
        NavigationGutterIconBuilder
            .create(AllIcons.Webreferences.Openapi)
            .setTargets(listOf(target))
            .setTooltipText("Go to OpenAPI operation: $operationId")
            .createLineMarkerInfo(element)
    }
}
