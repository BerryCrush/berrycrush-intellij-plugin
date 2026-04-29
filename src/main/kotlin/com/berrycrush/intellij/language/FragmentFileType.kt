package com.berrycrush.intellij.language

import com.berrycrush.intellij.BerryCrushIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

/**
 * File type for BerryCrush fragment files (.fragment).
 */
object FragmentFileType : LanguageFileType(BerryCrushLanguage) {
    const val EXTENSION = "fragment"

    override fun getName(): String = "BerryCrush Fragment"
    override fun getDisplayName(): String = "BerryCrush Fragment"
    override fun getDescription(): String = "BerryCrush reusable step fragment file"
    override fun getDefaultExtension(): String = EXTENSION
    override fun getIcon(): Icon = BerryCrushIcons.FRAGMENT_FILE
}
