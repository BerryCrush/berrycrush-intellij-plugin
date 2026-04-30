package com.berrycrush.intellij.reference

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

/**
 * Contributes references for BerryCrush elements.
 *
 * Handles navigation from leaf tokens by checking text patterns.
 */
class BerryCrushReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Register for ALL elements and filter by file extension in the provider
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(),
            BerryCrushLeafReferenceProvider()
        )
    }
}

/**
 * Provider that creates references for elements in BerryCrush files.
 */
class BerryCrushLeafReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        // Only process BerryCrush files
        val file = element.containingFile ?: return PsiReference.EMPTY_ARRAY
        val fileName = file.name
        if (!fileName.endsWith(".scenario") && !fileName.endsWith(".fragment")) {
            return PsiReference.EMPTY_ARRAY
        }

        val text = element.text

        // Check if this is an operation reference (^operationId)
        if (text.startsWith("^") && text.length > 1) {
            val operationId = text.removePrefix("^")
            if (operationId.matches(Regex("[a-zA-Z_]\\w*"))) {
                return arrayOf(
                    BerryCrushOperationReference(
                        element,
                        TextRange(1, text.length), // Skip the ^
                        operationId
                    )
                )
            }
        }

        // Check if parent is an INCLUDE_DIRECTIVE and this is the fragment name
        val parent = element.parent
        if (parent != null && parent.node?.elementType == BerryCrushElementTypes.FRAGMENT_REF) {
            val fragmentName = text.removePrefix("^")
            return arrayOf(
                BerryCrushFragmentReference(
                    element,
                    TextRange(0, text.length),
                    fragmentName
                )
            )
        }

        // Check if this element is inside an include directive
        if (parent != null && isInsideIncludeDirective(parent)) {
            // Check if this is the fragment name (identifier after "include")
            val parentText = parent.text
            if (parentText.startsWith("include ")) {
                val fragmentName = text.removePrefix("^")
                if (fragmentName.matches(Regex("[a-zA-Z_][a-zA-Z0-9_.\\-]*"))) {
                    return arrayOf(
                        BerryCrushFragmentReference(
                            element,
                            TextRange(0, text.length),
                            fragmentName
                        )
                    )
                }
            }
        }

        return PsiReference.EMPTY_ARRAY
    }

    private fun isInsideIncludeDirective(element: PsiElement): Boolean {
        var current: PsiElement? = element
        while (current != null) {
            if (current.node?.elementType == BerryCrushElementTypes.INCLUDE_DIRECTIVE) {
                return true
            }
            current = current.parent
        }
        return false
    }
}
