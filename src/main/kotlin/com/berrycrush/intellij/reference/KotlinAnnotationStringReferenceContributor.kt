package com.berrycrush.intellij.reference

import com.intellij.openapi.util.TextRange
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext

private const val STEP_ANNOTATION_SHORT = "Step"
private const val ASSERTION_ANNOTATION_SHORT = "Assertion"
private const val STEP_ANNOTATION_FQN = "org.berrycrush.step.Step"
private const val ASSERTION_ANNOTATION_FQN = "org.berrycrush.step.Assertion"

/**
 * Contributes references for @Step/@Assertion annotation strings in Kotlin files.
 *
 * Uses reflection to access Kotlin PSI since kotlin-compiler is an optional dependency.
 * Enables Cmd+Click navigation from the annotation string to scenario file usages.
 */
class KotlinAnnotationStringReferenceContributor : PsiReferenceContributor() {

    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        // Register for all elements and filter in the provider
        // This is necessary because Kotlin PSI classes aren't directly available
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(),
            KotlinAnnotationStringReferenceProvider()
        )
    }
}

/**
 * Provider that creates references for @Step/@Assertion annotation string parameters in Kotlin.
 */
class KotlinAnnotationStringReferenceProvider : PsiReferenceProvider() {

    override fun getReferencesByElement(
        element: PsiElement,
        context: ProcessingContext
    ): Array<PsiReference> {
        // Only process Kotlin files
        val file = element.containingFile ?: return PsiReference.EMPTY_ARRAY
        if (!file.name.endsWith(".kt") && !file.name.endsWith(".kts")) {
            return PsiReference.EMPTY_ARRAY
        }
        
        // Check if this is a string template expression (KtStringTemplateExpression)
        val className = element.javaClass.name
        if (!className.contains("KtStringTemplateExpression") && 
            !className.contains("KtLiteralStringTemplateEntry")) {
            return PsiReference.EMPTY_ARRAY
        }
        
        // Get the string value
        val text = element.text
        if (text.isEmpty()) return PsiReference.EMPTY_ARRAY
        
        // Check if it's inside an annotation
        val annotationEntry = findContainingAnnotationEntry(element) ?: return PsiReference.EMPTY_ARRAY
        
        // Get the annotation name
        val annotationName = getAnnotationName(annotationEntry) ?: return PsiReference.EMPTY_ARRAY
        
        // Check if it's @Step or @Assertion
        val isStep = annotationName == STEP_ANNOTATION_SHORT || 
                     annotationName == STEP_ANNOTATION_FQN ||
                     annotationName.endsWith(".Step")
        val isAssertion = annotationName == ASSERTION_ANNOTATION_SHORT || 
                          annotationName == ASSERTION_ANNOTATION_FQN ||
                          annotationName.endsWith(".Assertion")
        
        if (!isStep && !isAssertion) return PsiReference.EMPTY_ARRAY
        
        // Extract string value and create reference
        val stringValue = extractStringValue(element) ?: return PsiReference.EMPTY_ARRAY
        if (stringValue.isEmpty()) return PsiReference.EMPTY_ARRAY
        
        // Calculate range (skip quotes)
        val startOffset = if (text.startsWith("\"") || text.startsWith("'")) 1 else 0
        val endOffset = if (text.endsWith("\"") || text.endsWith("'")) text.length - 1 else text.length
        
        if (startOffset >= endOffset) return PsiReference.EMPTY_ARRAY
        
        return arrayOf(
            AnnotationStringReference(
                element,
                TextRange(startOffset, endOffset),
                stringValue,
                isAssertion
            )
        )
    }
    
    /**
     * Find containing KtAnnotationEntry using reflection.
     */
    private fun findContainingAnnotationEntry(element: PsiElement): PsiElement? {
        var current: PsiElement? = element
        while (current != null) {
            val className = current.javaClass.name
            if (className.contains("KtAnnotationEntry")) {
                return current
            }
            current = current.parent
        }
        return null
    }
    
    /**
     * Get annotation name from KtAnnotationEntry using reflection.
     */
    private fun getAnnotationName(annotationEntry: PsiElement): String? {
        try {
            // Try getShortName() first
            val shortNameMethod = annotationEntry.javaClass.methods.find { it.name == "getShortName" }
            if (shortNameMethod != null) {
                val shortName = shortNameMethod.invoke(annotationEntry)
                if (shortName != null) {
                    val asStringMethod = shortName.javaClass.methods.find { it.name == "asString" }
                    if (asStringMethod != null) {
                        return asStringMethod.invoke(shortName)?.toString()
                    }
                    return shortName.toString()
                }
            }
            
            // Fallback: try to get typeReference and extract name
            val typeRefMethod = annotationEntry.javaClass.methods.find { it.name == "getTypeReference" }
            if (typeRefMethod != null) {
                val typeRef = typeRefMethod.invoke(annotationEntry)
                if (typeRef != null) {
                    return typeRef.toString()
                }
            }
            
            // Last resort: extract from text
            val text = annotationEntry.text
            val match = Regex("""@(\w+(?:\.\w+)*)""").find(text)
            return match?.groupValues?.get(1)
            
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Extract string value from a Kotlin string template expression.
     */
    private fun extractStringValue(element: PsiElement): String? {
        try {
            // For KtStringTemplateExpression, get children (entries)
            val entries = element.children
            if (entries.isNotEmpty()) {
                // For simple strings, the first entry contains the value
                val entry = entries.firstOrNull()
                if (entry != null) {
                    val entryClassName = entry.javaClass.name
                    if (entryClassName.contains("KtLiteralStringTemplateEntry")) {
                        return entry.text
                    }
                }
            }
            
            // Fallback: strip quotes from text
            val text = element.text
            return when {
                text.startsWith("\"\"\"") && text.endsWith("\"\"\"") -> 
                    text.removePrefix("\"\"\"").removeSuffix("\"\"\"")
                text.startsWith("\"") && text.endsWith("\"") -> 
                    text.removePrefix("\"").removeSuffix("\"")
                text.startsWith("'") && text.endsWith("'") -> 
                    text.removePrefix("'").removeSuffix("'")
                else -> text
            }
        } catch (e: Exception) {
            return null
        }
    }
}
