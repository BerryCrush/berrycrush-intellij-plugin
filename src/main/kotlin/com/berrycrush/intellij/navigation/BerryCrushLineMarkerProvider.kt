package com.berrycrush.intellij.navigation

import com.berrycrush.intellij.BerryCrushIcons
import com.berrycrush.intellij.reference.BerryCrushFragmentReference
import com.berrycrush.intellij.reference.BerryCrushOperationReference
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement

/**
 * Provides gutter icons for BerryCrush navigation.
 *
 * Shows icons for:
 * - Fragment definitions (links to usages)
 * - Include directives (links to fragment file)
 * - Operation references (links to OpenAPI spec)
 */
class BerryCrushLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val text = element.text
        val trimmedText = text.trim()

        // Check for fragment definition (fragment: name)
        if (trimmedText.lowercase().startsWith("fragment:")) {
            val fragmentName = extractFragmentName(text)
            if (fragmentName != null) {
                createFragmentDefinitionMarker(element, fragmentName, result)
            }
        }

        // Check for include directive keyword - gutter icon on "include"
        if (trimmedText.lowercase() == "include") {
            // Find the fragment name from next sibling
            val siblingText = getNextSiblingText(element)
            if (siblingText != null) {
                createIncludeDirectiveMarker(element, siblingText, result)
            }
        }

        // Check for include directive (include fragmentName) - whole line match
        if (trimmedText.lowercase().startsWith("include ")) {
            createIncludeDirectiveMarker(element, text, result)
        }

        // Check for operation reference (^operationId)
        if (text.startsWith("^") && text.length > 1) {
            val operationId = text.removePrefix("^")
            if (operationId.matches(Regex("[a-zA-Z_]\\w*"))) {
                createOperationReferenceMarkerDirect(element, operationId, result)
            }
        }
    }

    private fun getNextSiblingText(element: PsiElement): String? {
        var sibling = element.nextSibling
        while (sibling != null) {
            val text = sibling.text.trim()
            if (text.isNotEmpty() && !text.isBlank()) {
                return text
            }
            sibling = sibling.nextSibling
        }
        return null
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
        val builder = NavigationGutterIconBuilder
            .create(BerryCrushIcons.FRAGMENT_FILE)
            .setTargets(listOf(element))
            .setTooltipText("Fragment: $fragmentName")

        result.add(builder.createLineMarkerInfo(element))
    }

    private fun createIncludeDirectiveMarker(
        element: PsiElement,
        text: String,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val fragmentName = extractIncludeTarget(text) ?: return
        val target = BerryCrushFragmentReference.findFragmentByName(element.project, fragmentName)

        if (target != null) {
            val builder = NavigationGutterIconBuilder
                .create(AllIcons.Gutter.ImplementedMethod)
                .setTargets(listOf(target))
                .setTooltipText("Go to fragment: $fragmentName")

            result.add(builder.createLineMarkerInfo(element))
        }
    }

    private fun extractIncludeTarget(text: String): String? {
        // Handle both "include fragmentName" and just "fragmentName"
        val match = Regex("""(?:include\s+)?\^?([a-zA-Z_][a-zA-Z0-9_.\-]*)""", RegexOption.IGNORE_CASE).find(text.trim())
        return match?.groupValues?.get(1)
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
}
