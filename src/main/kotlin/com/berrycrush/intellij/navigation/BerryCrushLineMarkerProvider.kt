package com.berrycrush.intellij.navigation

import com.berrycrush.intellij.BerryCrushIcons
import com.berrycrush.intellij.index.IncludeUsageIndex
import com.berrycrush.intellij.psi.BerryCrushAssertElement
import com.berrycrush.intellij.psi.BerryCrushFragmentElement
import com.berrycrush.intellij.psi.BerryCrushFragmentRefElement
import com.berrycrush.intellij.psi.BerryCrushIncludeElement
import com.berrycrush.intellij.psi.BerryCrushOperationRefElement
import com.berrycrush.intellij.psi.BerryCrushStepElement
import com.berrycrush.intellij.reference.BerryCrushFragmentReference
import com.berrycrush.intellij.reference.BerryCrushOperationReference
import com.berrycrush.intellij.reference.BerryCrushStepReference
import com.berrycrush.intellij.reference.BerryCrushAssertionReference
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiDocumentManager
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
class BerryCrushLineMarkerProvider : LineMarkerProvider {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // Only create markers for elements that are the FIRST significant element on their line
        // This prevents duplicate markers when multiple elements on the same line match patterns
        if (!isFirstElementOnLine(element)) {
            return null
        }
        
        val text = element.text
        val lowerText = text.lowercase()
        val trimmedLower = lowerText.trim()
        
        // Check for fragment definition FIRST (before step keywords, since "fragment" could match)
        if (trimmedLower.startsWith("fragment:") || trimmedLower == "fragment") {
            val fullLineText = getFullLineText(element)
            if (!fullLineText.trim().lowercase().startsWith("fragment:")) {
                return null
            }
            val fragmentName = extractFragmentName(fullLineText)
            if (fragmentName != null) {
                val usages = IncludeUsageIndex.findIncludeUsages(element.project, fragmentName)
                return if (usages.isNotEmpty()) {
                    NavigationGutterIconBuilder
                        .create(AllIcons.Gutter.ImplementingMethod)
                        .setTargets(usages)
                        .setTooltipText("Fragment '$fragmentName' - ${usages.size} usage(s)")
                        .setPopupTitle("Usages of fragment '$fragmentName'")
                        .createLineMarkerInfo(element)
                } else {
                    LineMarkerInfo(
                        element,
                        element.textRange,
                        BerryCrushIcons.FRAGMENT_FILE,
                        { "Fragment: $fragmentName (no usages)" },
                        null,
                        GutterIconRenderer.Alignment.CENTER,
                        { "Fragment definition" }
                    )
                }
            }
        }
        
        // Check for include directive
        if (trimmedLower.startsWith("include")) {
            val fullLineText = getFullLineText(element)
            val fragmentName = extractIncludeFragmentName(fullLineText)
            if (fragmentName != null) {
                val target = BerryCrushFragmentReference.findFragmentByName(element.project, fragmentName)
                return if (target != null) {
                    NavigationGutterIconBuilder
                        .create(AllIcons.Gutter.ImplementedMethod)
                        .setTargets(listOf(target))
                        .setTooltipText("Go to fragment: $fragmentName")
                        .createLineMarkerInfo(element)
                } else {
                    LineMarkerInfo(
                        element,
                        element.textRange,
                        BerryCrushIcons.FRAGMENT_FILE,
                        { "Fragment: $fragmentName (not found)" },
                        null,
                        GutterIconRenderer.Alignment.CENTER,
                        { "Include directive" }
                    )
                }
            }
        }
        
        // Match step keywords - handle "given " (with space) or just "given"
        val isStepKeyword = trimmedLower.startsWith("given") || 
                            trimmedLower.startsWith("when") || 
                            trimmedLower.startsWith("then") ||
                            trimmedLower.startsWith("and") || 
                            trimmedLower.startsWith("but")
        
        if (isStepKeyword) {
            val fullLineText = getFullLineText(element)
            val stepText = extractStepText(fullLineText)
            
            if (stepText != null) {
                val methods = BerryCrushStepReference.findMatchingStepMethods(element.project, stepText)
                if (methods.isNotEmpty()) {
                    return NavigationGutterIconBuilder
                        .create(AllIcons.Gutter.ImplementedMethod)
                        .setTargets(methods)
                        .setTooltipText("Go to @Step definition")
                        .setPopupTitle("Step definitions")
                        .createLineMarkerInfo(element)
                }
            }
        }
        
        // Check for assert keyword
        if (lowerText.startsWith("assert")) {
            val fullLineText = getFullLineText(element)
            val assertionText = extractAssertionText(fullLineText)
            if (assertionText != null) {
                val methods = BerryCrushAssertionReference.findMatchingAssertionMethods(element.project, assertionText)
                if (methods.isNotEmpty()) {
                    return NavigationGutterIconBuilder
                        .create(AllIcons.Gutter.ImplementedMethod)
                        .setTargets(methods)
                        .setTooltipText("Go to @Assertion definition")
                        .setPopupTitle("Assertion definitions")
                        .createLineMarkerInfo(element)
                }
            }
        }
        
        // Check for operation reference
        if (text.startsWith("^") && text.length > 1) {
            val operationId = text.removePrefix("^").trim()
            if (operationId.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*"))) {
                val target = BerryCrushOperationReference.findOperationInOpenAPI(element.project, operationId)
                if (target != null) {
                    return NavigationGutterIconBuilder
                        .create(AllIcons.Webreferences.Openapi)
                        .setTargets(listOf(target))
                        .setTooltipText("Go to OpenAPI operation: $operationId")
                        .createLineMarkerInfo(element)
                }
            }
        }
        
        return null
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

    @Suppress("UnusedPrivateMember")
    private fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        // DISABLED: PSI-based markers are handled in getLineMarkerInfo()
        // This method is kept for reference but not called
    }

    /**
     * Fallback for elements that aren't proper PSI types.
     * Only triggers on leaf elements to avoid duplicate markers.
     */
    @Suppress("UnusedParameter", "UnusedPrivateMember")
    private fun collectTextBasedMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        // Text-based markers are now handled in getLineMarkerInfo() for better performance
        // This method is kept for PSI-based markers only (which are handled in collectNavigationMarkers)
        // No action needed here - avoids duplicate markers
    }

    private fun isStepKeyword(text: String): Boolean {
        return text == "given" ||
               text == "when" ||
               text == "then" ||
               text == "and" ||
               text == "but"
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
        val textBeforeElement = document.getText(com.intellij.openapi.util.TextRange(lineStartOffset, elementOffset))
        
        // If there's non-whitespace content before this element, it's not first on line
        return textBeforeElement.isBlank()
    }

    /**
     * Gets the full text of the line containing the element.
     * Combines the element text with all following siblings until end of line.
     */
    private fun getFullLineText(element: PsiElement): String {
        val builder = StringBuilder()
        
        // Start with current element's text
        builder.append(element.text)
        
        // Add text from all following siblings on the same line
        var sibling = element.nextSibling
        while (sibling != null) {
            val siblingText = sibling.text
            // Stop at newline
            if (siblingText.contains('\n') || siblingText.contains('\r')) {
                // Add text up to newline
                val newlineIndex = siblingText.indexOfFirst { it == '\n' || it == '\r' }
                if (newlineIndex > 0) {
                    builder.append(siblingText.substring(0, newlineIndex))
                }
                break
            }
            builder.append(siblingText)
            sibling = sibling.nextSibling
        }
        
        return builder.toString()
    }

    private fun extractStepText(text: String): String? = Companion.extractStepText(text)

    private fun extractAssertionText(text: String): String? = Companion.extractAssertionText(text)

    companion object {
        /**
         * Extract step text by removing the given/when/then/and/but prefix (strict lowercase).
         */
        internal fun extractStepText(text: String): String? {
            val trimmedText = text.trim()
            val prefixPattern = Regex("""^(given|when|then|and|but)\s+""")
            val match = prefixPattern.find(trimmedText) ?: return null
            return trimmedText.substring(match.range.last + 1).trim()
        }

        /**
         * Extract assertion text by removing the assert prefix (strict lowercase).
         */
        internal fun extractAssertionText(text: String): String? {
            val trimmedText = text.trim()
            val prefixPattern = Regex("""^assert\s+""")
            val match = prefixPattern.find(trimmedText) ?: return null
            return trimmedText.substring(match.range.last + 1).trim()
        }

        /**
         * Extract fragment name from a fragment: declaration line (strict lowercase).
         */
        internal fun extractFragmentName(text: String): String? {
            val match = Regex("""fragment:\s*(\S+)""").find(text)
            return match?.groupValues?.get(1)
        }

        /**
         * Extract fragment name from an include directive (strict lowercase).
         */
        internal fun extractIncludeFragmentName(text: String): String? {
            val match = Regex("""include\s+\^?([a-zA-Z_][a-zA-Z0-9_.\-]*)""").find(text)
            return match?.groupValues?.get(1)
        }

        /**
         * Check if the given text is a step keyword (strict lowercase).
         */
        internal fun isStepKeyword(text: String): Boolean {
            return text == "given" ||
                   text == "when" ||
                   text == "then" ||
                   text == "and" ||
                   text == "but"
        }
    }
}
