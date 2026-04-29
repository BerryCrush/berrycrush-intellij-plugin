package com.berrycrush.intellij.parser

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

/**
 * Base PSI element for BerryCrush language elements.
 */
class BerryCrushPsiElement(node: ASTNode) : ASTWrapperPsiElement(node)
