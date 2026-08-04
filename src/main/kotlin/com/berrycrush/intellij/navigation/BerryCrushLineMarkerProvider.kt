package com.berrycrush.intellij.navigation

import com.berrycrush.intellij.BerryCrushIcons
import com.berrycrush.intellij.index.IncludeUsageIndex
import com.berrycrush.intellij.psi.BerryCrushAssertElement
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushIncludeElement
import com.berrycrush.intellij.psi.BerryCrushOperationRefElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.berrycrush.intellij.reference.BerryCrushFragmentReference
import com.berrycrush.intellij.reference.BerryCrushOperationReference
import com.berrycrush.intellij.reference.BerryCrushStepReference
import com.berrycrush.intellij.reference.BerryCrushAssertionReference
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

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
class BerryCrushLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Line markers must be registered for leaf elements only (per IntelliJ guidelines)
        // Skip non-leaf elements to avoid performance warnings
        if (element.firstChild != null) {
            return null
        }

        // Only create markers for elements that are the FIRST significant element on their line
        // This prevents duplicate markers when multiple elements on the same line match patterns
        if (!isFirstElementOnLine(element)) {
            return null
        }
        return when (element) {
            is BerryCrushFragmentElement -> element.markFragment()
            is BerryCrushIncludeElement -> element.markInclude()
            is BerryCrushStepElement -> element.markStep()
            is BerryCrushAssertElement -> element.markAssert()
            is BerryCrushOperationRefElement -> element.markOperationReference()
            else -> null
        }
    }

    private fun BerryCrushFragmentElement.markFragment(): LineMarkerInfo<*>? {
        return fragmentName?.let { name ->
            val usages = IncludeUsageIndex.findIncludeUsages(this.project, name)
            return if (usages.isNotEmpty()) {
                NavigationGutterIconBuilder
                    .create(AllIcons.Gutter.ImplementingMethod)
                    .setTargets(usages)
                    .setTooltipText("Fragment '$name' - ${usages.size} usage(s)")
                    .setPopupTitle("Usages of fragment '$name'")
                    .createLineMarkerInfo(this)
            } else {
                LineMarkerInfo(
                    this,
                    this.textRange,
                    BerryCrushIcons.FRAGMENT_FILE,
                    { "Fragment: $fragmentName (no usages)" },
                    null,
                    GutterIconRenderer.Alignment.CENTER,
                    { "Fragment definition" }
                )
            }
        }
    }

    private fun BerryCrushIncludeElement.markInclude(): LineMarkerInfo<*>? {
        return fragmentName?.let { name ->
            val target = BerryCrushFragmentReference.findFragmentByName(project, name)
            return if (target != null) {
                NavigationGutterIconBuilder
                    .create(AllIcons.Gutter.ImplementedMethod)
                    .setTargets(listOf(target))
                    .setTooltipText("Go to fragment: $name")
                    .createLineMarkerInfo(this)
            } else {
                LineMarkerInfo(
                    this,
                    this.textRange,
                    BerryCrushIcons.FRAGMENT_FILE,
                    { "Fragment: $fragmentName (not found)" },
                    null,
                    GutterIconRenderer.Alignment.CENTER,
                    { "Include directive" }
                )
            }
        }
    }

    private fun BerryCrushStepElement.markStep(): LineMarkerInfo<*>? {
        return stepText?.let { text ->
            BerryCrushStepReference.findMatchingStepMethods(project, text).let { methods ->
                if (methods.isNotEmpty()) {
                    NavigationGutterIconBuilder
                        .create(AllIcons.Gutter.ImplementedMethod)
                        .setTargets(methods)
                        .setTooltipText("Go to @Step definition")
                        .setPopupTitle("Step definitions")
                        .createLineMarkerInfo(this)
                } else {
                    null
                }
            }
        }
    }

    private fun BerryCrushAssertElement.markAssert(): LineMarkerInfo<*>? {
        return assertionText?.let { text ->
            BerryCrushAssertionReference.findMatchingAssertionMethods(project, text).let { methods ->
                if (methods.isNotEmpty()) {
                    NavigationGutterIconBuilder
                        .create(AllIcons.Gutter.ImplementedMethod)
                        .setTargets(methods)
                        .setTooltipText("Go to @Assertion definition")
                        .setPopupTitle("Assertion definitions")
                        .createLineMarkerInfo(this)
                } else {
                    null
                }
            }
        }
    }

    private fun BerryCrushOperationRefElement.markOperationReference(): LineMarkerInfo<*>? {
        return BerryCrushOperationReference.findOperationInOpenAPI(project, operationId)?.let { target ->
            NavigationGutterIconBuilder
                .create(AllIcons.Webreferences.Openapi)
                .setTargets(listOf(target))
                .setTooltipText("Go to OpenAPI operation: $operationId")
                .createLineMarkerInfo(this)
        }
    }

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        // All markers are handled in getLineMarkerInfo() for consistency
        // PSI-based markers (BerryCrushFragmentElement, etc.) are disabled
        // because the parser doesn't reliably create these types and
        // text-based detection in getLineMarkerInfo() handles all cases
    }

    /**
     * Checks if this element is the first significant (non-whitespace) element on its line.
     * Uses document-based line number detection for accuracy.
     */
    private fun isFirstElementOnLine(element: PsiElement): Boolean {
        val containingFile = element.containingFile ?: return true
        val document = PsiDocumentManager.getInstance(element.project).getDocument(containingFile) ?: return true
        
        val elementOffset = element.textOffset
        val lineNumber = document.getLineNumber(elementOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        
        // Get text from line start to element start
        val textBeforeElement = document.getText(TextRange(lineStartOffset, elementOffset))
        
        // If there's non-whitespace content before this element, it's not first on line
        return textBeforeElement.isBlank()
    }
}
