package com.berrycrush.intellij.commenter

import com.intellij.lang.Commenter

/**
 * Commenter for BerryCrush language.
 *
 * Supports line comments with '#' prefix.
 */
class BerryCrushCommenter : Commenter {
    override fun getLineCommentPrefix(): String = "# "

    override fun getBlockCommentPrefix(): String? = null
    override fun getBlockCommentSuffix(): String? = null
    override fun getCommentedBlockCommentPrefix(): String? = null
    override fun getCommentedBlockCommentSuffix(): String? = null
}
