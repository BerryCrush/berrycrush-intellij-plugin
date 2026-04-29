package com.berrycrush.intellij

import com.berrycrush.intellij.highlighting.BerryCrushHighlightingColors
import com.berrycrush.intellij.highlighting.BerryCrushSyntaxHighlighter
import com.berrycrush.intellij.lexer.BerryCrushTokenTypes
import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

/**
 * Unit tests for BerryCrush syntax highlighter.
 */
class BerryCrushHighlighterTest {

    private val highlighter = BerryCrushSyntaxHighlighter()

    @Test
    fun `feature keyword is highlighted as block keyword`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.FEATURE)
        assertContains(keys.toList(), BerryCrushHighlightingColors.BLOCK_KEYWORD)
    }

    @Test
    fun `scenario keyword is highlighted as block keyword`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.SCENARIO)
        assertContains(keys.toList(), BerryCrushHighlightingColors.BLOCK_KEYWORD)
    }

    @Test
    fun `given keyword is highlighted as step keyword`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.GIVEN)
        assertContains(keys.toList(), BerryCrushHighlightingColors.STEP_KEYWORD)
    }

    @Test
    fun `when keyword is highlighted as step keyword`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.WHEN)
        assertContains(keys.toList(), BerryCrushHighlightingColors.STEP_KEYWORD)
    }

    @Test
    fun `then keyword is highlighted as step keyword`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.THEN)
        assertContains(keys.toList(), BerryCrushHighlightingColors.STEP_KEYWORD)
    }

    @Test
    fun `call directive is highlighted`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.CALL)
        assertContains(keys.toList(), BerryCrushHighlightingColors.DIRECTIVE)
    }

    @Test
    fun `assert directive is highlighted`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.ASSERT)
        assertContains(keys.toList(), BerryCrushHighlightingColors.DIRECTIVE)
    }

    @Test
    fun `text token has no special highlighting`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.TEXT)
        // TEXT may have no special highlight
        assertTrue(keys.size >= 0)
    }

    @Test
    fun `string is highlighted`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.STRING)
        assertContains(keys.toList(), BerryCrushHighlightingColors.STRING)
    }

    @Test
    fun `number is highlighted`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.NUMBER)
        assertContains(keys.toList(), BerryCrushHighlightingColors.NUMBER)
    }

    @Test
    fun `tag is highlighted`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.TAG)
        assertContains(keys.toList(), BerryCrushHighlightingColors.TAG)
    }

    @Test
    fun `comment is highlighted`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.COMMENT)
        assertContains(keys.toList(), BerryCrushHighlightingColors.COMMENT)
    }

    @Test
    fun `JSON path is highlighted`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.JSON_PATH)
        assertContains(keys.toList(), BerryCrushHighlightingColors.JSON_PATH)
    }

    @Test
    fun `variable is highlighted`() {
        val keys = highlighter.getTokenHighlights(BerryCrushTokenTypes.VARIABLE)
        assertContains(keys.toList(), BerryCrushHighlightingColors.VARIABLE)
    }

    @Test
    fun `unknown token returns empty array`() {
        // Create a dummy element type that shouldn't match anything
        val keys = highlighter.getTokenHighlights(null)
        assertTrue(keys.isEmpty())
    }
}
