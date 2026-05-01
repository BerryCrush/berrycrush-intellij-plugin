package com.berrycrush.intellij.reference

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

private const val STEP_ANNOTATION_FQN = "org.berrycrush.step.Step"
private const val ASSERTION_ANNOTATION_FQN = "org.berrycrush.step.Assertion"

/**
 * Contributes references for @Step/@Assertion annotation strings in Java files.
 *
 * Enables Cmd+Click navigation from the annotation string to scenario file usages.
 */
class JavaAnnotationStringReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Register for string literals inside annotations
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(PsiLiteralExpression::class.java)
                .inside(PsiAnnotation::class.java),
            JavaAnnotationStringReferenceProvider()
        )
    }
}

/**
 * Provider that creates references for @Step/@Assertion annotation string parameters.
 */
class JavaAnnotationStringReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        val literal = element as? PsiLiteralExpression ?: return PsiReference.EMPTY_ARRAY
        
        // Only process string literals
        val value = literal.value as? String ?: return PsiReference.EMPTY_ARRAY
        if (value.isEmpty()) return PsiReference.EMPTY_ARRAY
        
        // Find the containing annotation
        val annotation = findContainingAnnotation(literal) ?: return PsiReference.EMPTY_ARRAY
        
        // Check if it's @Step or @Assertion
        val qualifiedName = annotation.qualifiedName ?: return PsiReference.EMPTY_ARRAY
        val isStep = qualifiedName == STEP_ANNOTATION_FQN || qualifiedName.endsWith(".Step")
        val isAssertion = qualifiedName == ASSERTION_ANNOTATION_FQN || qualifiedName.endsWith(".Assertion")
        
        if (!isStep && !isAssertion) return PsiReference.EMPTY_ARRAY
        
        // Create reference for the string content (skip the quotes)
        val text = literal.text
        val startOffset = if (text.startsWith("\"") || text.startsWith("'")) 1 else 0
        val endOffset = if (text.endsWith("\"") || text.endsWith("'")) text.length - 1 else text.length
        
        if (startOffset >= endOffset) return PsiReference.EMPTY_ARRAY
        
        return arrayOf(
            AnnotationStringReference(
                literal,
                TextRange(startOffset, endOffset),
                value,
                isAssertion
            )
        )
    }
    
    private fun findContainingAnnotation(element: PsiElement): PsiAnnotation? {
        var current: PsiElement? = element
        while (current != null) {
            if (current is PsiAnnotation) {
                return current
            }
            current = current.parent
        }
        return null
    }
}
