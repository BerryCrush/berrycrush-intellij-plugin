package org.berrycrush.intellij.reference

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import org.berrycrush.intellij.psi.BerryCrushFragmentRefElement
import org.berrycrush.intellij.psi.BerryCrushOperationRefElement

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
            BerryCrushLeafReferenceProvider(),
        )
    }
}

/**
 * Provider that creates references for elements in BerryCrush files.
 */
class BerryCrushLeafReferenceProvider : PsiReferenceProvider() {
    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext,
    ): Array<PsiReference> {
        // Only process BerryCrush files
        val file = element.containingFile ?: return PsiReference.EMPTY_ARRAY
        val fileName = file.name
        if (!fileName.endsWith(".scenario") && !fileName.endsWith(".fragment")) {
            return PsiReference.EMPTY_ARRAY
        }

        return when (element) {
            is BerryCrushOperationRefElement -> arrayOf(BerryCrushOperationReference(element, element.textRange, element.operationId))
            is BerryCrushFragmentRefElement -> arrayOf(BerryCrushFragmentReference(element, element.textRange, element.name))
            else -> PsiReference.EMPTY_ARRAY
        }
    }
}
