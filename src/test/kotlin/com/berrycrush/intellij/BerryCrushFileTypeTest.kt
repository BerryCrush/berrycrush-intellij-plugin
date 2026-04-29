package com.berrycrush.intellij

import com.berrycrush.intellij.language.BerryCrushLanguage
import com.berrycrush.intellij.language.FragmentFileType
import com.berrycrush.intellij.language.ScenarioFileType
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertSame

/**
 * Unit tests for BerryCrush file types and language.
 */
class BerryCrushFileTypeTest {

    @Test
    fun `scenario file type has correct extension`() {
        assertEquals("scenario", ScenarioFileType.defaultExtension)
    }

    @Test
    fun `fragment file type has correct extension`() {
        assertEquals("fragment", FragmentFileType.defaultExtension)
    }

    @Test
    fun `scenario file type has unique display name`() {
        assertEquals("BerryCrush Scenario", ScenarioFileType.displayName)
    }

    @Test
    fun `fragment file type has unique display name`() {
        assertEquals("BerryCrush Fragment", FragmentFileType.displayName)
    }

    @Test
    fun `file types have different display names`() {
        assertNotEquals(ScenarioFileType.displayName, FragmentFileType.displayName)
    }

    @Test
    fun `both file types share same language`() {
        assertSame(BerryCrushLanguage, ScenarioFileType.language)
        assertSame(BerryCrushLanguage, FragmentFileType.language)
    }

    @Test
    fun `language has correct ID`() {
        assertEquals("BerryCrush", BerryCrushLanguage.id)
    }

    @Test
    fun `scenario file type has icon`() {
        val icon = ScenarioFileType.icon
        kotlin.test.assertNotNull(icon)
    }

    @Test
    fun `fragment file type has icon`() {
        val icon = FragmentFileType.icon
        kotlin.test.assertNotNull(icon)
    }
}
