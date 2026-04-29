package com.berrycrush.intellij.language

import com.intellij.lang.Language

/**
 * BerryCrush language definition.
 *
 * Represents the BerryCrush DSL for OpenAPI-driven BDD testing.
 */
object BerryCrushLanguage : Language("BerryCrush") {
    override fun getDisplayName(): String = "BerryCrush"
    override fun isCaseSensitive(): Boolean = true
}
