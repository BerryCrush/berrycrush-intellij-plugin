package org.berrycrush.intellij

import com.intellij.lang.ASTNode
import com.intellij.lang.FileASTNode
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestExecutionPolicy
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.intellij.testFramework.runInEdtAndGet
import com.intellij.testFramework.runInEdtAndWait
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach

/**
 * Base test case for BerryCrush IntelliJ plugin tests.
 *
 * Provides:
 * - In-memory file system with project fixture
 * - File indexing support
 * - PSI access
 */
abstract class BerryCrushTestCase {
    protected lateinit var myFixture: CodeInsightTestFixture

    protected val project: Project get() = myFixture.project

    protected val psiManager: PsiManager get() = myFixture.psiManager

    protected fun renameElementAtCaret(newName: String) = runInEdtAndGet {
        myFixture.renameElementAtCaret(newName)
    }

    protected fun findFile(virtualFile: VirtualFile): PsiFile? = consume { psiManager.findFile(virtualFile) }

    protected fun PsiFile.node(): FileASTNode = consume { this.node }
    protected fun FileASTNode.firstChildNode(): ASTNode? = consume { this.firstChildNode }
    protected fun PsiDocumentManager.document(psiFile: PsiFile): Document? = consume { this.getDocument(psiFile) }

    protected inline fun <reified T : PsiElement> findChildOfType(element: PsiElement?, clazz: Class<out T>): T? = consume {
        PsiTreeUtil.findChildOfType(element, clazz)
    }

    protected inline fun <reified T : PsiElement> findChildrenOfType(element: PsiElement?, clazz: Class<out T>): Collection<T> = consume {
        PsiTreeUtil.findChildrenOfType(element, clazz)
    }

    protected inline fun <reified T> consume(crossinline func: () -> T): T = ReadAction.compute<T, Throwable> {
        func()
    }

    @BeforeEach
    fun setUp() {
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().let { factory ->
            val builder = factory.createLightFixtureBuilder("BerryCrushTest")
            val policy = IdeaTestExecutionPolicy.current()
            val tempDirTestFixture = policy?.createTempDirTestFixture() ?: LightTempDirTestFixtureImpl(true)
            factory.createCodeInsightFixture(builder.fixture, tempDirTestFixture)
        }

        myFixture.testDataPath = "src/test/testData"
        myFixture.setUp()
    }

    @AfterEach
    fun tearDown() {
        runCatching {
            myFixture.tearDown()
        }
    }

    /**
     * Creates a scenario file in the test fixture and ensures it's indexed.
     */
    protected fun createScenarioFile(
        fileName: String,
        content: String,
    ): VirtualFile {
        val psiFile = myFixture.addFileToProject("$fileName.scenario", content)
        runInEdtAndWait {}
        IndexingTestUtil.waitUntilIndexesAreReady(project)
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
        runInEdtAndWait {}
        IndexingTestUtil.waitUntilIndexesAreReady(project)
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

    // for now
    fun assertNull(value: Any?) {
        Assertions.assertNull(value)
    }

    fun assertNull(message: String, value: Any?) {
        Assertions.assertNull(value, message)
    }

    fun assertNotNull(value: Any?) {
        Assertions.assertNotNull(value)
    }

    fun assertNotNull(message: String, value: Any?) {
        Assertions.assertNotNull(value, message)
    }

    fun assertTrue(condition: Boolean) {
        Assertions.assertTrue(condition)
    }

    fun assertTrue(message: String, condition: Boolean) {
        Assertions.assertTrue(condition, message)
    }

    fun assertFalse(condition: Boolean) {
        Assertions.assertFalse(condition)
    }

    fun assertFalse(message: String, condition: Boolean) {
        Assertions.assertFalse(condition, message)
    }

    fun assertEquals(expected: Any?, actual: Any?) {
        Assertions.assertEquals(expected, actual)
    }

    fun assertEquals(message: String, expected: Any?, actual: Any?) {
        Assertions.assertEquals(expected, actual, message)
    }

    fun assertSame(expected: Any?, actual: Any?) {
        Assertions.assertSame(expected, actual)
    }

    fun assertSame(message: String, expected: Any?, actual: Any?) {
        Assertions.assertSame(expected, actual, message)
    }
}
