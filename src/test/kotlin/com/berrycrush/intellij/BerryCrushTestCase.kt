package com.berrycrush.intellij

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.indexing.FileBasedIndex

/**
 * Base test case for BerryCrush IntelliJ plugin tests.
 *
 * Provides:
 * - In-memory file system with project fixture
 * - File indexing support
 * - PSI access
 */
abstract class BerryCrushTestCase : BasePlatformTestCase() {

    override fun getTestDataPath(): String {
        return "src/test/testData"
    }

    /**
     * Creates a scenario file in the test fixture and ensures it's indexed.
     */
    protected fun createScenarioFile(fileName: String, content: String): VirtualFile {
        val psiFile = myFixture.addFileToProject("$fileName.scenario", content)
        return psiFile.virtualFile
    }

    /**
     * Creates a fragment file in the test fixture and ensures it's indexed.
     */
    protected fun createFragmentFile(fileName: String, content: String): VirtualFile {
        val psiFile = myFixture.addFileToProject("$fileName.fragment", content)
        return psiFile.virtualFile
    }

    /**
     * Creates a Kotlin file with step definitions.
     */
    protected fun createStepDefinitions(fileName: String, content: String): VirtualFile {
        val psiFile = myFixture.addFileToProject("$fileName.kt", content)
        return psiFile.virtualFile
    }
}
