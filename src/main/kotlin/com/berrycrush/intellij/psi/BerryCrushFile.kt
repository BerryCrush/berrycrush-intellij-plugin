package com.berrycrush.intellij.psi

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.berrycrush.intellij.language.ScenarioFileType
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

/**
 * PSI file for BerryCrush scenario and fragment files.
 */
class BerryCrushFile(viewProvider: FileViewProvider) : PsiFileBase(viewProvider, BerryCrushLanguage) {
    override fun getFileType(): FileType = ScenarioFileType
    override fun toString(): String = "BerryCrush File"
}
