package com.berrycrush.intellij.psi

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil

object BerryCrushElementFactory {
    fun createBlockNameIdentifier(project: Project, name: String): PsiElement {
        val file = PsiFileFactory.getInstance(project)
            .createFileFromText(BerryCrushLanguage, name)
        return PsiTreeUtil.findChildOfType(file, BerryCrushBlockNameElement::class.java)
            ?: throw InternalError("BerryCrushBlockNameElement not found")
    }
}
