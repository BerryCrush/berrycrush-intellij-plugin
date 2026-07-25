package com.berrycrush.intellij.inspection

import com.berrycrush.intellij.lexer.BerryCrushLexer
import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import com.berrycrush.intellij.reference.BerryCrushAssertionReference
import com.berrycrush.intellij.util.ModuleScopeResolver
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiFile
import com.intellij.psi.tree.IElementType

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

    override fun checkFile(file: PsiFile, holder: ProblemsHolder) {
        val project = file.project
        val scope = ModuleScopeResolver.getModuleDependencyScope(file)
        val lines = file.text.lines()
        val lexer = BerryCrushLexer()
        lines.forEachIndexed { lineIndex, line ->
            lexer.start(line)
            lexer.skipWhiteSpace()
            if (lexer.tokenType == BerryCrushTokenTypes.ASSERT) {
                lexer.advance()
                lexer.skipWhiteSpace()
                if (!lexer.isBuiltInAssertion()) {
                    val assertionText = line.trim().removePrefix("assert").trim()
                    val matchingMethods = BerryCrushAssertionReference.findMatchingAssertionMethodsInScope(
                        project,
                        assertionText,
                        scope
                    )

                    if (matchingMethods.isEmpty()) {
                        findElementAtLine(file, lineIndex, 0)?.let { element ->
                            holder.registerProblem(
                                element,
                                "Assertion '$assertionText' has no matching @Assertion definition",
                                ProblemHighlightType.WEAK_WARNING,
                                CreateAssertionQuickFix(assertionText)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Check if the assertion text matches a built-in assertion pattern.
 */
private fun BerryCrushLexer.isBuiltInAssertion(): Boolean {
    return checkAssertionCondition()
}

private fun BerryCrushLexer.skipWhiteSpace() {
    fun IElementType?.shouldSkip() = tokenType != null && this == BerryCrushTokenTypes.WHITE_SPACE || this == BerryCrushTokenTypes.INDENT
    while (tokenType.shouldSkip()) advance()
}

private fun BerryCrushLexer.checkAssertionCondition(negated: Boolean = false): Boolean {
    skipWhiteSpace()
    // check
    return when (tokenType) {
        BerryCrushTokenTypes.NOT -> if (!negated) {
            advance() // skip not
            checkAssertionCondition(true)
        } else {
            // not not is not allowed
            false
        }
        BerryCrushTokenTypes.STATUS -> checkStatusAssertion()
        BerryCrushTokenTypes.VARIABLE -> checkVariableAssertion()
        BerryCrushTokenTypes.JSON_PATH -> checkJsonPathAssertion()
        BerryCrushTokenTypes.HEADER -> checkHeaderAssertion()
        BerryCrushTokenTypes.CONTAINS -> checkSimpleCondition()
        BerryCrushTokenTypes.RESPONSE_TIME -> checkSimpleCondition()
        BerryCrushTokenTypes.SCHEMA -> checkSchemaAssertion()
        else -> false
    }
}

// status(Code) 2xx thing
private fun BerryCrushLexer.checkStatusAssertion(): Boolean {
    fun String.isStatusCode() = Regex("\\dxx").matches(this)
    advance() // skip status
    skipWhiteSpace()
    val type = tokenType
    val value = tokenText
    skipWhiteSpace()
    return tokenType != null && (type == BerryCrushTokenTypes.NUMBER || (type == BerryCrushTokenTypes.STRING && value.isStatusCode()))
}

// {{variable}} op [value]
private fun BerryCrushLexer.checkVariableAssertion() = checkOperator(true)

// $.json op [value]
private fun BerryCrushLexer.checkJsonPathAssertion() = checkOperator()

// Http-Header [[:=] value]
private fun BerryCrushLexer.checkHeaderAssertion(): Boolean {
    advance() // skip header
    skipWhiteSpace()
    // header name is required
    if (tokenType != BerryCrushTokenTypes.TEXT) return false
    advance() // skip header name
    skipWhiteSpace()
    return when (tokenType) {
        BerryCrushTokenTypes.COLON,
        BerryCrushTokenTypes.EQUALS -> checkValue()
        null -> true
        else -> false
    }
}

private fun BerryCrushLexer.checkSimpleCondition(negated: Boolean = false): Boolean {
    advance()
    return if (!negated && tokenType == BerryCrushTokenTypes.NOT) {
        checkSimpleCondition(true)
    } else {
        checkValue()
    }
}

private fun BerryCrushLexer.checkSchemaAssertion(): Boolean {
    advance()
    return !checkValue()
}

private fun BerryCrushLexer.checkOperator(negated: Boolean = false): Boolean {
    advance()
    skipWhiteSpace()
    return when (tokenType) {
        BerryCrushTokenTypes.NOT -> !negated && checkOperator(true)
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
        BerryCrushTokenTypes.ARRAY_SIZE -> {
            advance()
            checkValue()
        }

        BerryCrushTokenTypes.EXISTS,
        BerryCrushTokenTypes.EMPTY,
        BerryCrushTokenTypes.NOT_EMPTY -> {
            advance()
            !checkValue()
        }
        else -> false
    }
}

fun BerryCrushLexer.checkValue(): Boolean {
    skipWhiteSpace()
    return tokenType != null
}
