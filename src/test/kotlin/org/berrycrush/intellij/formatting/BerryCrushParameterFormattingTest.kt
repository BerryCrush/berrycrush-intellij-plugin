package org.berrycrush.intellij.formatting

import org.junit.jupiter.api.Test

class BerryCrushParameterFormattingTest : BerryCrushFormattingTestCase() {
    @Test
    fun `alias entry should be the one level down`() {
        val input = """
            parameters:
            binding:
            alias:
            listPets: "GET /pets"
        """.trimIndent()
        val expected = """
            parameters:
              binding:
                alias:
                  listPets: "GET /pets"
        """.trimIndent()
        doIdempotencyTest(input, expected)
    }

    @Test
    fun `alias entry connected by dot should be the one level down`() {
        val input = """
            parameters:
            binding:
            alias.listPets: "GET /pets"
        """.trimIndent()
        val expected = """
            parameters:
              binding:
                alias.listPets: "GET /pets"
        """.trimIndent()
        doIdempotencyTest(input, expected)
    }

    @Test
    fun testParametersBlockWithRepresentativeFixedKeys() {
        val input =
            listOf(
                "scenario: fixed keys",
                "parameters:",
                "logRequests:   true",
                "header.Authorization: Bearer token",
                "binding.default.baseUrl: \"https://api.example.com\"",
            ).joinToString("\n")

        val expected =
            listOf(
                "scenario: fixed keys",
                "  parameters:",
                "    logRequests: true",
                "    header.Authorization: Bearer token",
                "    binding.default.baseUrl: \"https://api.example.com\"",
            ).joinToString("\n")

        doFormattingTest(input, expected)
    }

    @Test
    fun testParametersBlockWithDottedFixedFamilies() {
        val input =
            listOf(
                "scenario: dotted families",
                "parameters:",
                "autoAssertions.enabled:true",
                "errorContext.maxBodySize:4096",
                "retry.maxAttempts:3",
                "binding.default.alias.listPets: listPets",
                "multiTest.timeoutMs: 500",
            ).joinToString("\n")

        val expected =
            listOf(
                "scenario: dotted families",
                "  parameters:",
                "    autoAssertions.enabled: true",
                "    errorContext.maxBodySize: 4096",
                "    retry.maxAttempts: 3",
                "    binding.default.alias.listPets: listPets",
                "    multiTest.timeoutMs: 500",
            ).joinToString("\n")

        doFormattingTest(input, expected)
    }

    @Test
    fun testParametersBlockWithFixedAndCustomKeys() {
        val input =
            listOf(
                "scenario: mixed keys",
                "parameters:",
                "logRequests: true",
                "custom-param: 123",
                "tenant.profile.name: acme",
            ).joinToString("\n")

        val expected =
            listOf(
                "scenario: mixed keys",
                "  parameters:",
                "    logRequests: true",
                "    custom-param: 123",
                "    tenant.profile.name: acme",
            ).joinToString("\n")

        doFormattingTest(input, expected)
    }

    @Test
    fun testNestedColonSeparatedParametersInParametersBlock() {
        val input =
            listOf(
                "scenario: nested colon params",
                "parameters:",
                "header:",
                "Request-ID: 12345",
                "custom:",
                "child: value",
            ).joinToString("\n")

        val expected =
            listOf(
                "scenario: nested colon params",
                "  parameters:",
                "    header:",
                "      Request-ID: 12345",
                "    custom:",
                "      child: value",
            ).joinToString("\n")

        doFormattingTest(input, expected)
    }

    @Test
    fun testParametersBlockFormattingIsIdempotent() {
        val input =
            listOf(
                "scenario: idempotent parameters",
                "parameters:",
                "header:",
                "Request-ID: 12345",
                "binding.default.baseUrl: \"https://api.example.com\"",
                "custom:",
                "child: value",
            ).joinToString("\n")

        val expected =
            listOf(
                "scenario: idempotent parameters",
                "  parameters:",
                "    header:",
                "      Request-ID: 12345",
                "    binding.default.baseUrl: \"https://api.example.com\"",
                "    custom:",
                "      child: value",
            ).joinToString("\n")

        doIdempotencyTest(input, expected)
    }
}
