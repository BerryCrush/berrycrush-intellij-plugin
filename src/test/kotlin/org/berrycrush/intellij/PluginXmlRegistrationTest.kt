package org.berrycrush.intellij

import org.junit.jupiter.api.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class PluginXmlRegistrationTest {
    @Test
    fun `plugin xml keeps annotator registration and removes syntax highlighter factory`() {
        val pluginXmlStream =
            PluginXmlRegistrationTest::class.java.classLoader
                .getResourceAsStream("META-INF/plugin.xml")
        assertNotNull(pluginXmlStream)

        val pluginXml = pluginXmlStream.bufferedReader().use { it.readText() }

        assertContains(pluginXml, "<annotator")
        assertContains(pluginXml, "language=\"BerryCrush\"")
        assertContains(pluginXml, "org.berrycrush.intellij.highlighting.BerryCrushAnnotator")
        assertContains(pluginXml, "<colorSettingsPage")
        assertContains(pluginXml, "org.berrycrush.intellij.highlighting.BerryCrushColorSettingsPage")

        assertFalse(pluginXml.contains("<lang.syntaxHighlighterFactory language=\"BerryCrush\""))
        assertFalse(pluginXml.contains("<renameHandler"))
    }
}
