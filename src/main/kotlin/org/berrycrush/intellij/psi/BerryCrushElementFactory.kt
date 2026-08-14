package org.berrycrush.intellij.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import org.berrycrush.intellij.language.BerryCrushLanguage

object BerryCrushElementFactory {
    fun createBlockNameIdentifier(project: Project, name: String): PsiElement {
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText(BerryCrushLanguage, name)
        return PsiTreeUtil.findChildOfType(file, BerryCrushBlockNameElement::class.java)
            ?: throw InternalError("BerryCrushBlockNameElement not found")
    }

    fun createParameterKeyElement(project: Project, name: String): PsiElement {
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText(
                BerryCrushLanguage,
                """
                    parameters:
                      $name: value
                """.trimIndent(),
            )
        return PsiTreeUtil.findChildOfType(file, BerryCrushParameterKeyElement::class.java)
            ?: throw InternalError("BerryCrushParameterKeyElement not found")
    }

    fun createExtractVariableNameElement(project: Project, name: String): BerryCrushExtractElement {
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText(
                BerryCrushLanguage,
                """
                    scenario: foo
                      given step 
                        extract $.id => $name
                """.trimIndent(),
            )

        return PsiTreeUtil.findChildOfType(file, BerryCrushExtractElement::class.java)
            ?: throw InternalError("Extract variable identifier not found")
    }

    fun createExampleHeaderElement(project: Project, name: String): PsiElement {
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText(
                BerryCrushLanguage,
                """
                    outline: foo
                      examples:
                        | $name |
                """.trimIndent(),
            )
        return PsiTreeUtil.findChildOfType(file, BerryCrushExampleHeaderElement::class.java)
            ?: throw InternalError("BerryCrushExampleHeaderElement not found")
    }

    fun createVariableRefElement(project: Project, name: String): PsiElement {
        val variableRefText = if (name.startsWith("{{") && name.endsWith("}}")) name else "{{$name}}"
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText(
                BerryCrushLanguage,
                """
                    outline: foo
                      when $variableRefText
                """.trimIndent(),
            )
        return PsiTreeUtil.findChildOfType(file, BerryCrushVariableRefElement::class.java)
            ?: throw InternalError("BerryCrushExampleHeaderElement not found")
    }

    fun createStringLiteralElement(project: Project, literalText: String): BerryCrushStringLiteralElement {
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText(
                BerryCrushLanguage,
                """
                    scenario: foo
                      parameters:
                        message: $literalText
                """.trimIndent(),
            )
        return PsiTreeUtil.findChildOfType(file, BerryCrushStringLiteralElement::class.java)
            ?: throw InternalError("BerryCrushStringLiteralElement not found")
    }

    fun createFragmentRefElement(project: Project, name: String): PsiElement {
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText(
                BerryCrushLanguage,
                """
                    scenario: foo
                      include $name
                """.trimIndent(),
            )
        return PsiTreeUtil.findChildOfType(file, BerryCrushFragmentRefElement::class.java)
            ?: throw InternalError("BerryCrushFragmentRefElement not found")
    }
}
