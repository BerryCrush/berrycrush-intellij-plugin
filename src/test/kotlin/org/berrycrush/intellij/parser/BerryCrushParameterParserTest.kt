package org.berrycrush.intellij.parser

import org.berrycrush.intellij.BerryCrushTestCase
import org.berrycrush.intellij.psi.BerryCrushParameterIncludeRefElement
import org.berrycrush.intellij.psi.BerryCrushParametersElement
import org.junit.jupiter.api.Test

class BerryCrushParameterParserTest : BerryCrushTestCase() {
    @Test
    fun `parameter block with name`() {
        val file =
            createScenarioFile(
                "parameter with name",
                """
                  parameters: name
                    param1: value1
                    param2: value2
                """.trimIndent(),
            )
        val psiFile = findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        val parameters = findChildOfType(psiFile, BerryCrushParametersElement::class.java)
        assertNotNull("Parameters block should be found", parameters)
        assertEquals("name", parameters?.name)
    }

    @Test
    fun `parameter include reference resolves to named parameters block`() {
        val file = createFragmentFile(
            "parameter include",
            """
                parameters: shared
                  key0: value0

                parameters:
                  << shared
                  key1: value1
            """.trimIndent(),
        )

        val psiFile = findFile(file)
        assertNotNull("PSI file should be created", psiFile)

        consume {
            val includeRef = findChildOfType(psiFile, BerryCrushParameterIncludeRefElement::class.java)
            assertNotNull("Parameter include reference should be parsed", includeRef)
            assertEquals("shared", includeRef?.name)
            assertEquals("shared", includeRef?.reference?.resolve()?.let { (it as? BerryCrushParametersElement)?.name })
        }
    }
}
