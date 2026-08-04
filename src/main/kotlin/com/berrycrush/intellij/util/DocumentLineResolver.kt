package com.berrycrush.intellij.util

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

fun PsiFile.lineNumber(range: TextRange): Pair<Int, Int>? =
    PsiDocumentManager.getInstance(project).getDocument(this)?.let { document ->
        val start = 0.coerceAtLeast(range.startOffset.coerceAtMost(document.textLength))
        val end = 0.coerceAtLeast(range.endOffset.coerceAtMost(document.textLength))
        document.getLineNumber(start) to document.getLineNumber(end)
    }
