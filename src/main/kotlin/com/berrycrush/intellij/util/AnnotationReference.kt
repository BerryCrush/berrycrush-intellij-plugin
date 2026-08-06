package com.berrycrush.intellij.util

import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

object AnnotationReference {
    /**
     * Checks if the step text matches the pattern.
     *
     * Patterns support placeholders like {int}, {string}, {word}, etc.
     */
    fun matchesPattern(
        stepText: String,
        pattern: String,
    ): Boolean {
        // Convert pattern placeholders to regex
        val regexPattern =
            pattern
                .replace(Regex("""\{int}"""), """(-?\\d+)""")
                .replace(Regex("""\{string}"""), """("[^"]*"|'[^']*')""")
                .replace(Regex("""\{word}"""), """(\\w+)""")
                .replace(Regex("""\{float}"""), """(-?\\d+\\.?\\d*)""")
                .replace(Regex("""\{any}"""), """(.+?)""")
                .let { "^$it$" }

        return try {
            Regex(regexPattern, RegexOption.IGNORE_CASE).matches(stepText)
        } catch (_: Exception) {
            // If regex compilation fails, fall back to simple contains check
            stepText.contains(pattern, ignoreCase = true)
        }
    }

    fun findAnnotationClass(project: Project, fqn: String): PsiClass? {
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val scope = GlobalSearchScope.allScope(project)
        return javaPsiFacade.findClass(fqn, scope)
    }

    /**
     * Gets the pattern value from a @Step annotation on a method.
     */
    fun getAnnotationPattern(method: PsiMethod, fqn: String): String? {
        val annotation = method.getAnnotation(fqn) ?: return null
        return getAnnotationStringValue(annotation, "pattern")
            ?: getAnnotationStringValue(annotation, "value")
    }

    /**
     * Extracts a string attribute value from an annotation.
     */
    fun getAnnotationStringValue(
        annotation: PsiAnnotation,
        attributeName: String,
    ): String? {
        val attributeValue = annotation.findAttributeValue(attributeName) ?: return null
        val text = attributeValue.text
        // Remove surrounding quotes if present
        return if (text.startsWith("\"") && text.endsWith("\"") && text.length >= 2) {
            text.substring(1, text.length - 1)
        } else {
            text
        }
    }
}
