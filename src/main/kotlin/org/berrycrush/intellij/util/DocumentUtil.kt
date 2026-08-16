package org.berrycrush.intellij.util

import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement

val PsiElement.lineNumber: Int
    get() = PsiDocumentManager.getInstance(project).getDocument(containingFile)?.getLineNumber(textOffset)?.let { it + 1 } ?: -1
