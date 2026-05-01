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
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon

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
        // Check for step keywords (case-insensitive)
        val text = element.text
        val lowerText = text.lowercase()
        val trimmedLower = lowerText.trim()
        
        // Match step keywords - handle "given " (with space) or just "given"
        val isStepKeyword = trimmedLower.startsWith("given") || 
                            trimmedLower.startsWith("when") || 
                            trimmedLower.startsWith("then") ||
                            trimmedLower.startsWith("and") || 
                            trimmedLower.startsWith("but")
        
        if (isStepKeyword) {
            // The element might be just the keyword token (e.g., "given ")
            // We need to get the full line text from parent or by combining siblings
            val fullLineText = getFullLineText(element)
            val stepText = extractStepText(fullLineText)
            
            if (stepText != null) {
                val methods = BerryCrushStepReference.findMatchingStepMethods(element.project, stepText)
                if (methods.isNotEmpty()) {
                    // Navigate to @Step methods
                    return NavigationGutterIconBuilder
                        .create(AllIcons.Gutter.ImplementedMethod)
                        .setTargets(methods)
                        .setTooltipText("Go to @Step definition")
                        .setPopupTitle("Step definitions")
                        .createLineMarkerInfo(element)
                }
            }
            // Don't show placeholder icons for unmatched steps (cleaner UI)
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
        
        // Check for fragment definition
        if (lowerText.startsWith("fragment:")) {
            val fullLineText = getFullLineText(element)
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
        if (lowerText.startsWith("include")) {
            val fragmentName = text.removePrefix("include").removePrefix("Include").trim().removePrefix("^").trim()
            if (fragmentName.isNotEmpty()) {
                val target = BerryCrushFragmentReference.findFragmentByName(element.project, fragmentName)
                if (target != null) {
                    return NavigationGutterIconBuilder
                        .create(AllIcons.Gutter.ImplementedMethod)
                        .setTargets(listOf(target))
                        .setTooltipText("Go to fragment: $fragmentName")
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
        // Use slow markers for more complex lookups
        for (element in elements) {
            collectNavigationMarkers(element, result)
        }
    }

    private fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in LineMarkerInfo<*>>
    ) {
        // Line markers must be placed on leaf elements (no children)
        if (element.firstChild != null) return

        val parent = element.parent ?: return

        when (parent) {
            // Fragment definition - reverse navigation to usages
            is BerryCrushFragmentElement -> {
                // Only add marker on the first leaf element of the fragment
                if (PsiTreeUtil.firstChild(parent) == element) {
                    val fragmentName = parent.fragmentName
                    if (fragmentName != null) {
                        createFragmentDefinitionMarker(element, fragmentName, result)
                    }
                }
            }

            // Include directive - navigate to fragment file
            is BerryCrushIncludeElement -> {
                if (PsiTreeUtil.firstChild(parent) == element) {
                    val fragmentName = parent.fragmentName
                    if (fragmentName != null) {
                        createIncludeDirectiveMarker(element, fragmentName, result)
                    }
                }
            }

            // Fragment reference in include - navigate to fragment file
            is BerryCrushFragmentRefElement -> {
                if (PsiTreeUtil.firstChild(parent) == element) {
                    createIncludeDirectiveMarker(element, parent.name, result)
                }
            }

            // Operation reference - navigate to OpenAPI spec
            is BerryCrushOperationRefElement -> {
                if (PsiTreeUtil.firstChild(parent) == element) {
                    createOperationReferenceMarkerDirect(element, parent.operationId, result)
                }
            }

            // Step element - navigate to @Step annotated methods
            is BerryCrushStepElement -> {
                if (PsiTreeUtil.firstChild(parent) == element) {
                    val stepText = parent.stepText
                    if (stepText != null) {
                        createStepDefinitionMarker(element, stepText, result)
                    }
                }
            }

            // Assert directive - navigate to @Assertion annotated methods
            is BerryCrushAssertElement -> {
                if (PsiTreeUtil.firstChild(parent) == element) {
                    val assertionText = parent.assertionText
                    if (assertionText != null) {
                        createAssertionDefinitionMarker(element, assertionText, result)
                    }
                }
            }

            // For other elements, check text-based patterns as fallback
            else -> {
                collectTextBasedMarkers(element, result)
            }
        }
    }

    /**
     * Fallback for elements that aren't proper PSI types.
     * Only triggers on leaf elements to avoid duplicate markers.
     */
    private fun collectTextBasedMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        // Only process leaf elements to avoid duplicates
        if (element.firstChild != null) return

        val text = element.text
        val trimmedText = text.trim()

        // Check for fragment definition (fragment: name) - leaf token "Fragment"
        if (trimmedText.startsWith("Fragment", ignoreCase = true)) {
            val fullText = element.text
            val fragmentName = extractFragmentName(fullText)
            if (fragmentName != null) {
                createFragmentDefinitionMarker(element, fragmentName, result)
            }
        }

        // Check for include keyword
        if (trimmedText.startsWith("include", ignoreCase = true)) {
            val fragmentName = trimmedText.removePrefix("include").trim().removePrefix("^").trim()
            if (fragmentName.isNotEmpty()) {
                createIncludeDirectiveMarker(element, fragmentName, result)
            }
        }

        // Check for operation reference (^operationId)
        if (text.startsWith("^") && text.length > 1) {
            val operationId = text.removePrefix("^").trim()
            if (operationId.matches(Regex("[a-zA-Z_][a-zA-Z0-9_]*"))) {
                createOperationReferenceMarkerDirect(element, operationId, result)
            }
        }

        // Check for step keywords (Given, When, Then, And, But)
        // The lexer includes trailing space in the token, e.g., "Given "
        val stepKeyword = trimmedText.removeSuffix(" ").removeSuffix(":")
        if (isStepKeyword(stepKeyword)) {
            val parent = element.parent
            val fullText = parent?.text ?: element.text
            val stepText = extractStepText(fullText)
            if (stepText != null) {
                createStepDefinitionMarker(element, stepText, result)
            }
        }

        // Check for assert keyword
        if (trimmedText.startsWith("assert", ignoreCase = true)) {
            val parent = element.parent
            val fullText = parent?.text ?: element.text
            val assertionText = extractAssertionText(fullText)
            if (assertionText != null) {
                createAssertionDefinitionMarker(element, assertionText, result)
            }
        }
    }

    private fun isStepKeyword(text: String): Boolean {
        return text.equals("Given", ignoreCase = true) ||
               text.equals("When", ignoreCase = true) ||
               text.equals("Then", ignoreCase = true) ||
               text.equals("And", ignoreCase = true) ||
               text.equals("But", ignoreCase = true)
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

    private fun createStepDefinitionMarker(
        element: PsiElement,
        stepText: String,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val methods = BerryCrushStepReference.findMatchingStepMethods(element.project, stepText)
        if (methods.isNotEmpty()) {
            val builder = NavigationGutterIconBuilder
                .create(AllIcons.Gutter.ImplementedMethod)
                .setTargets(methods)
                .setTooltipText("Go to @Step definition")
                .setPopupTitle("Step definitions")

            result.add(builder.createLineMarkerInfo(element))
        } else {
            // Show a placeholder icon even if no @Step methods found
            // This helps debug if the LineMarkerProvider is working
            val builder = NavigationGutterIconBuilder
                .create(AllIcons.Actions.QuickfixBulb)
                .setTargets(listOf(element))
                .setTooltipText("Step: $stepText (no @Step definition found)")

            result.add(builder.createLineMarkerInfo(element))
        }
    }

    private fun createAssertionDefinitionMarker(
        element: PsiElement,
        assertionText: String,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val methods = BerryCrushAssertionReference.findMatchingAssertionMethods(element.project, assertionText)
        if (methods.isNotEmpty()) {
            val builder = NavigationGutterIconBuilder
                .create(AllIcons.Gutter.ImplementedMethod)
                .setTargets(methods)
                .setTooltipText("Go to @Assertion definition")
                .setPopupTitle("Assertion definitions")

            result.add(builder.createLineMarkerInfo(element))
        }
    }

    private fun extractFragmentName(text: String): String? {
        val match = Regex("""fragment:\s*(\S+)""", RegexOption.IGNORE_CASE).find(text)
        return match?.groupValues?.get(1)
    }

    private fun createFragmentDefinitionMarker(
        element: PsiElement,
        fragmentName: String,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        // Find all usages of this fragment (reverse navigation)
        val usages = IncludeUsageIndex.findIncludeUsages(element.project, fragmentName)

        if (usages.isNotEmpty()) {
            val builder = NavigationGutterIconBuilder
                .create(AllIcons.Gutter.ImplementingMethod)
                .setTargets(usages)
                .setTooltipText("Fragment '$fragmentName' - ${usages.size} usage(s)")
                .setPopupTitle("Usages of fragment '$fragmentName'")

            result.add(builder.createLineMarkerInfo(element))
        } else {
            // No usages found, just show the definition marker
            val builder = NavigationGutterIconBuilder
                .create(BerryCrushIcons.FRAGMENT_FILE)
                .setTargets(listOf(element))
                .setTooltipText("Fragment: $fragmentName (no usages)")

            result.add(builder.createLineMarkerInfo(element))
        }
    }

    private fun createIncludeDirectiveMarker(
        element: PsiElement,
        fragmentName: String,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val target = BerryCrushFragmentReference.findFragmentByName(element.project, fragmentName)

        if (target != null) {
            val builder = NavigationGutterIconBuilder
                .create(AllIcons.Gutter.ImplementedMethod)
                .setTargets(listOf(target))
                .setTooltipText("Go to fragment: $fragmentName")

            result.add(builder.createLineMarkerInfo(element))
        }
    }

    private fun createOperationReferenceMarkerDirect(
        element: PsiElement,
        operationId: String,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val target = BerryCrushOperationReference.findOperationInOpenAPI(element.project, operationId)

        if (target != null) {
            val builder = NavigationGutterIconBuilder
                .create(AllIcons.Webreferences.Openapi)
                .setTargets(listOf(target))
                .setTooltipText("Go to OpenAPI operation: $operationId")

            result.add(builder.createLineMarkerInfo(element))
        }
    }

    companion object {
        /**
         * Extract step text by removing the Given/When/Then/And/But prefix.
         */
        internal fun extractStepText(text: String): String? {
            val trimmedText = text.trim()
            val prefixPattern = Regex("""^(Given|When|Then|And|But)\s+""", RegexOption.IGNORE_CASE)
            val match = prefixPattern.find(trimmedText) ?: return null
            return trimmedText.substring(match.range.last + 1).trim()
        }

        /**
         * Extract assertion text by removing the Assert prefix.
         */
        internal fun extractAssertionText(text: String): String? {
            val trimmedText = text.trim()
            val prefixPattern = Regex("""^assert\s+""", RegexOption.IGNORE_CASE)
            val match = prefixPattern.find(trimmedText) ?: return null
            return trimmedText.substring(match.range.last + 1).trim()
        }

        /**
         * Extract fragment name from a Fragment: declaration line.
         */
        internal fun extractFragmentName(text: String): String? {
            val match = Regex("""fragment:\s*(\S+)""", RegexOption.IGNORE_CASE).find(text)
            return match?.groupValues?.get(1)
        }

        /**
         * Check if the given text is a step keyword.
         */
        internal fun isStepKeyword(text: String): Boolean {
            return text.equals("Given", ignoreCase = true) ||
                   text.equals("When", ignoreCase = true) ||
                   text.equals("Then", ignoreCase = true) ||
                   text.equals("And", ignoreCase = true) ||
                   text.equals("But", ignoreCase = true)
        }
    }
}
