package org.berrycrush.intellij

import com.intellij.openapi.application.AccessToken
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LoggedErrorProcessor
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Base test case for BerryCrush IntelliJ plugin tests.
 *
 * Provides:
 * - In-memory file system with project fixture
 * - File indexing support
 * - PSI access
 */
abstract class BerryCrushTestCase : BasePlatformTestCase() {
    private var vueErrorSuppressionToken: AccessToken? = null

    override fun setUp() {
        super.setUp()
        vueErrorSuppressionToken =
            LoggedErrorProcessor.executeWith(
                object : LoggedErrorProcessor() {
                    override fun processError(
                        category: String,
                        message: String,
                        details: Array<String>,
                        t: Throwable?,
                    ): Set<Action> {
                        val isKnownVueStartupError =
                            message.contains("VueLspServerSupportProvider") ||
                                message.contains("org.jetbrains.plugins.vue") ||
                                t?.stackTraceToString()?.contains("org.jetbrains.vuejs") == true

                        return if (isKnownVueStartupError) {
                            Action.NONE
                        } else {
                            super.processError(category, message, details, t)
                        }
                    }
                },
            )
    }

    override fun tearDown() {
        try {
            vueErrorSuppressionToken?.finish()
            vueErrorSuppressionToken = null
        } finally {
            super.tearDown()
        }
    }

    override fun getTestDataPath(): String = "src/test/testData"

    /**
     * Creates a scenario file in the test fixture and ensures it's indexed.
     */
    protected fun createScenarioFile(
        fileName: String,
        content: String,
    ): VirtualFile {
        val psiFile = myFixture.addFileToProject("$fileName.scenario", content)
        return psiFile.virtualFile
    }

    /**
     * Creates a fragment file in the test fixture and ensures it's indexed.
     */
    protected fun createFragmentFile(
        fileName: String,
        content: String,
    ): VirtualFile {
        val psiFile = myFixture.addFileToProject("$fileName.fragment", content)
        return psiFile.virtualFile
    }

    /**
     * Creates a Kotlin file with step definitions.
     */
    protected fun createStepDefinitions(
        fileName: String,
        content: String,
    ): VirtualFile {
        val psiFile = myFixture.addFileToProject("$fileName.kt", content)
        return psiFile.virtualFile
    }
}
