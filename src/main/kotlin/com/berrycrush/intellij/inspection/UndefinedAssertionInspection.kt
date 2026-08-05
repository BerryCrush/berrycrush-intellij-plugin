package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.psi.BerryCrushAssertElement
import com.berrycrush.intellij.psi.BerryCrushAssertOperationElement
import com.berrycrush.intellij.psi.BerryCrushConditionElement
import com.berrycrush.intellij.psi.BerryCrushElementTypes
import com.berrycrush.intellij.psi.BerryCrushJsonPathElement
import com.berrycrush.intellij.psi.BerryCrushNotElement
import com.berrycrush.intellij.psi.BerryCrushOperatorElement
import com.berrycrush.intellij.psi.BerryCrushPsiElement
import com.berrycrush.intellij.psi.BerryCrushVariableRefElement
import com.berrycrush.intellij.reference.BerryCrushAssertionReference
import com.berrycrush.intellij.util.ModuleScopeResolver
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jayway.jsonpath.JsonPath

/**
 * Inspection that detects undefined assertion references.
 *
 * Highlights `assert` directives that don't match any @Assertion
 * annotated method in the project.
 */
class UndefinedAssertionInspection : BerryCrushInspection() {
    override fun getDisplayName(): String = "Undefined assertion"

    override fun getShortName(): String = "BerryCrushUndefinedAssertion"

    override fun getGroupDisplayName(): String = "BerryCrush"

    override fun isEnabledByDefault(): Boolean = true

    override fun checkFile(
        file: PsiFile,
        holder: ProblemsHolder,
    ) {
        val project = file.project
        val scope = ModuleScopeResolver.getModuleDependencyScope(file)
        PsiTreeUtil.findChildrenOfAnyType(file, BerryCrushAssertElement::class.java).forEach { assertElement ->
            if (!assertElement.condition.isBuiltInAssertion(holder)) {
                assertElement.assertionText?.let { assertionText ->
                    val matchingMethods =
                        BerryCrushAssertionReference.findMatchingAssertionMethodsInScope(
                            project,
                            assertionText,
                            scope,
                        )

                    if (matchingMethods.isEmpty()) {
                        holder.registerProblem(
                            assertElement,
                            "Assertion '$assertionText' has no matching @Assertion definition",
                            ProblemHighlightType.WEAK_WARNING,
                            CreateAssertionQuickFix(assertionText),
                        )
                    }
                } ?: holder.registerProblem(assertElement, "No assertion text found", ProblemHighlightType.ERROR)
            }
        }
    }
}

/**
 * Check if the assertion text matches a built-in assertion pattern.
 */
private fun BerryCrushConditionElement?.isBuiltInAssertion(holder: ProblemsHolder): Boolean = this == null || checkAssertionCondition(holder, this.children.toList().filterIsInstance<BerryCrushPsiElement>())

private fun BerryCrushConditionElement.checkAssertionCondition(
    holder: ProblemsHolder,
    elements: List<PsiElement>,
    negate: Boolean = false,
): Boolean = elements.isNotEmpty() &&
    if (elements[0] is BerryCrushNotElement) {
        // not not is not allowed
        !negate && checkAssertionCondition(holder, elements.drop(1), true)
    } else {
        when (val element = elements[0]) {
            is BerryCrushAssertOperationElement -> checkAssertionOperation(element, elements.drop(1))
            is BerryCrushJsonPathElement -> checkJsonPathAssertion(holder, element, elements.drop(1))
            is BerryCrushVariableRefElement -> checkOperatorCondition(elements.drop(1))
            else -> false
        }
    }

private fun checkAssertionOperation(
    element: BerryCrushAssertOperationElement,
    elements: List<PsiElement>,
): Boolean = when (element.operatorType) {
    BerryCrushTokenTypes.CONTAINS,
    BerryCrushTokenTypes.RESPONSE_TIME,
    -> checkSimpleCondition(element, elements)
    BerryCrushTokenTypes.STATUS -> checkStatusCondition(elements)
    BerryCrushTokenTypes.HEADER -> checkHeaderCondition(elements)
    BerryCrushTokenTypes.SCHEMA -> elements.isEmpty()
    else -> false // unknown operation, should never happen
}

private fun BerryCrushConditionElement.checkJsonPathAssertion(
    holder: ProblemsHolder,
    element: BerryCrushJsonPathElement,
    elements: List<PsiElement>,
): Boolean {
    val jsonPath = element.jsonPathText
    return try {
        JsonPath.compile(jsonPath)
        checkOperatorCondition(elements)
    } catch (e: Exception) {
        holder.registerProblem(
            this,
            e.message ?: "JSON Path parse error",
            ProblemHighlightType.ERROR,
        )
        false
    }
}

private fun checkOperatorCondition(elements: List<PsiElement>): Boolean {
    fun check(
        element: PsiElement,
        elements: List<PsiElement>,
        negate: Boolean = false,
    ): Boolean = when (element) {
        is BerryCrushNotElement -> !negate && elements.isNotEmpty() && check(elements[0], elements.drop(1), true)
        is BerryCrushOperatorElement ->
            when (element.operatorType) {
                BerryCrushTokenTypes.EQUALS,
                BerryCrushTokenTypes.NOT_EQUALS,
                BerryCrushTokenTypes.GREATER_OR_EQUAL,
                BerryCrushTokenTypes.GREATER_THAN,
                BerryCrushTokenTypes.LESS_OR_EQUAL,
                BerryCrushTokenTypes.LESS_THAN,
                BerryCrushTokenTypes.MATCHES,
                BerryCrushTokenTypes.STARTS_WITH,
                BerryCrushTokenTypes.IN,
                BerryCrushTokenTypes.CONTAINS,
                BerryCrushTokenTypes.SIZE,
                BerryCrushTokenTypes.HAS_SIZE,
                BerryCrushTokenTypes.ARRAY_SIZE,
                -> checkValue(elements)
                BerryCrushTokenTypes.EXISTS,
                BerryCrushTokenTypes.EMPTY,
                BerryCrushTokenTypes.NOT_EMPTY,
                -> elements.isEmpty()
                else -> false
            }
        else -> false
    }
    return elements.isNotEmpty() && check(elements[0], elements.drop(1))
}

private fun checkSimpleCondition(
    element: BerryCrushAssertOperationElement,
    elements: List<PsiElement>,
    negate: Boolean = false,
): Boolean = elements.isNotEmpty() &&
    if (elements[0] is BerryCrushNotElement) {
        !negate && checkSimpleCondition(element, elements.drop(1), true)
    } else {
        checkValue(elements)
    }

// status(Code) 2xx thing
private fun checkStatusCondition(elements: List<PsiElement>): Boolean {
    fun checkStatus(element: BerryCrushPsiElement): Boolean = Regex("\\dxx|\\d{3}").matches(element.text)
    return elements.size == 1 && elements[0] is BerryCrushPsiElement && checkStatus(elements[0] as BerryCrushPsiElement)
}

private fun checkHeaderCondition(elements: List<PsiElement>): Boolean = if (elements.isEmpty()) {
    false
} else {
    val header = elements.first()
    if (header.node.elementType != BerryCrushElementTypes.TEXT) {
        false
    } else {
        val rest = elements.drop(1)
        rest.isEmpty() ||
            if (rest[0] is BerryCrushOperatorElement) {
                val op = rest[0] as BerryCrushOperatorElement
                when (op.operatorType) {
                    BerryCrushTokenTypes.COLON,
                    BerryCrushTokenTypes.EQUALS,
                    -> checkValue(rest.drop(1))

                    else -> false
                }
            } else {
                false
            }
    }
}

private fun checkValue(elements: List<PsiElement>): Boolean = elements.size == 1 && elements[0] is BerryCrushPsiElement
