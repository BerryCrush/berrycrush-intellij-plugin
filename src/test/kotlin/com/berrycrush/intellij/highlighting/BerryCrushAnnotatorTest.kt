package com.berrycrush.intellij.highlighting

import com.berrycrush.intellij.BerryCrushTestCase
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.TextRange
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BerryCrushAnnotatorTest : BerryCrushTestCase() {

    fun testAnnotatorHighlightsOnlyFeatureKeywordInFeatureTitleLine() {
        val content = "feature: this should not be coloured"

        val infos = highlight(content)

        assertHasTokenHighlight(
            infos = infos,
            content = content,
            token = "feature:",
            key = BerryCrushHighlightingColors.BLOCK_KEYWORD,
        )
        assertNoHighlightInRange(
            infos = infos,
            content = content,
            text = "this should not be coloured",
        )
    }

    fun testAnnotatorHighlightsOnlyScenarioKeywordInScenarioTitleLine() {
        val content = "scenario: this should not be coloured"

        val infos = highlight(content)

        assertHasTokenHighlight(
            infos = infos,
            content = content,
            token = "scenario:",
            key = BerryCrushHighlightingColors.BLOCK_KEYWORD,
        )
        assertNoHighlightInRange(
            infos = infos,
            content = content,
            text = "this should not be coloured",
        )
    }

    fun testAnnotatorHighlightsOnlyStepKeywordInGivenStepLine() {
        val content = "given this should not be coloured either"

        val infos = highlight(content)

        assertHasTokenHighlight(
            infos = infos,
            content = content,
            token = "given",
            key = BerryCrushHighlightingColors.STEP_KEYWORD,
        )
        assertNoHighlightInRange(
            infos = infos,
            content = content,
            text = "this should not be coloured either",
        )
    }

    fun testAnnotatorHighlightsOnlyStepKeywordInAndStepLine() {
        val content = "and this should also not be coloured"

        val infos = highlight(content)

        assertHasTokenHighlight(
            infos = infos,
            content = content,
            token = "and",
            key = BerryCrushHighlightingColors.STEP_KEYWORD,
        )
        assertNoHighlightInRange(
            infos = infos,
            content = content,
            text = "this should also not be coloured",
        )
    }

    fun testAnnotatorHighlightsStepKeywordAndDirective() {
        val content = """
            scenario: Annotator checks
                            given I invoke API
                call ^listPets
        """.trimIndent()

        val infos = highlight(content)

        assertHasTokenHighlight(
            infos = infos,
            content = content,
            token = "given",
            key = BerryCrushHighlightingColors.STEP_KEYWORD,
        )
        assertHasTokenHighlight(
            infos = infos,
            content = content,
            token = "call",
            key = BerryCrushHighlightingColors.DIRECTIVE,
        )
    }

    fun testAnnotatorHighlightsIncludeParameterKeyRange() {
        val content = """
            scenario: Include params
              given using fragment
                include auth-flow
                  token: "abc"
        """.trimIndent()

        val infos = highlight(content)

        assertHasTokenHighlight(
            infos = infos,
            content = content,
            token = "token",
            key = BerryCrushHighlightingColors.PARAMETER_KEY,
        )
    }

    fun testAnnotatorHighlightingIsStableAcrossReopenAndEdit() {
        val initial = """
            scenario: Stability
              given base step
                call ^listPets
        """.trimIndent()

        val firstInfos = highlight(initial)
        val firstGivenCount = countHighlightsAtToken(
            infos = firstInfos,
            content = initial,
            token = "given",
            key = BerryCrushHighlightingColors.STEP_KEYWORD,
        )
        assertTrue(firstGivenCount > 0)

        myFixture.configureByText("stability.scenario", initial)
        val reopenInfos = myFixture.doHighlighting()
        val reopenGivenCount = countHighlightsAtToken(
            infos = reopenInfos,
            content = initial,
            token = "given",
            key = BerryCrushHighlightingColors.STEP_KEYWORD,
        )
        assertEquals(firstGivenCount, reopenGivenCount)

        val edited = initial + "\n  then verify result"
        myFixture.configureByText("stability.scenario", edited)
        val editedInfos = myFixture.doHighlighting()
        assertHasTokenHighlight(
            infos = editedInfos,
            content = edited,
            token = "then",
            key = BerryCrushHighlightingColors.STEP_KEYWORD,
        )
    }

    private fun highlight(content: String): List<HighlightInfo> {
        myFixture.configureByText("annotator.scenario", content)
        return myFixture.doHighlighting()
    }

    private fun assertHasTokenHighlight(
        infos: List<HighlightInfo>,
        content: String,
        token: String,
        key: TextAttributesKey,
    ) {
        val start = content.indexOf(token)
        assertTrue(start >= 0, "Token '$token' must exist in test content")
        val end = start + token.length

        assertTrue(
            infos.any {
                it.startOffset == start &&
                    it.endOffset == end &&
                    it.forcedTextAttributesKey == key
            },
            "Expected highlight for '$token' with key '${key.externalName}'",
        )
    }

    private fun countHighlightsAtToken(
        infos: List<HighlightInfo>,
        content: String,
        token: String,
        key: TextAttributesKey,
    ): Int {
        val start = content.indexOf(token)
        if (start < 0) return 0
        val end = start + token.length

        return infos.count {
            it.startOffset == start &&
                it.endOffset == end &&
                it.forcedTextAttributesKey == key
        }
    }

    private fun assertNoHighlightInRange(
        infos: List<HighlightInfo>,
        content: String,
        text: String,
    ) {
        val start = content.indexOf(text)
        assertTrue(start >= 0, "Text '$text' must exist in test content")
        val end = start + text.length

        val overlappingBerryCrushHighlights = infos.filter {
            val overlaps = it.startOffset < end && it.endOffset > start
            val key = it.forcedTextAttributesKey
            overlaps && key?.externalName?.startsWith("BERRYCRUSH_") == true
        }

        assertTrue(
            overlappingBerryCrushHighlights.isEmpty(),
            "Did not expect syntax highlight on '$text'. Overlaps: " +
                overlappingBerryCrushHighlights.joinToString { info ->
                    "[${info.startOffset}, ${info.endOffset}) ${info.forcedTextAttributesKey?.externalName}"
                },
        )
    }
}
