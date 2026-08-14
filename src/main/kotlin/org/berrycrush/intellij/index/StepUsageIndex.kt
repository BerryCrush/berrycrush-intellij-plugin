/**
 * Index for tracking step usage in scenario and fragment files.
 * Maps step patterns (like "there is an account") to the files using them.
 */
package org.berrycrush.intellij.index

import com.intellij.jvm.dfa.analysis.ui.inspection.presentation.PsiElementLineLocator.getStartLine
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.startOffset
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import org.berrycrush.intellij.language.FragmentFileType
import org.berrycrush.intellij.language.ScenarioFileType
import org.berrycrush.intellij.psi.BerryCrushAssertElement
import org.berrycrush.intellij.psi.BerryCrushStepElement
import java.io.DataInput
import java.io.DataOutput

private const val ASSERT_PREFIX = "ASSERT:"

/**
 * Index that maps step text patterns to their locations in scenario/fragment files.
 * Used for reverse navigation from @Step/@Assertion methods to usages.
 */
class StepUsageIndex : FileBasedIndexExtension<String, StepUsageData>() {
    companion object {
        val NAME: ID<String, StepUsageData> = ID.create("org.berrycrush.intellij.index.StepUsageIndex")

        /**
         * Find all usages of a step pattern within the given scope.
         *
         * Use this for module-scoped search, passing in the result of
         * ModuleScopeResolver.getDependentModulesScope() for reverse navigation.
         *
         * @param project The project
         * @param stepPattern The @Step pattern to search for
         * @param scope The search scope (e.g., dependent modules scope)
         * @return List of PSI elements where the step is used
         */
        fun findStepUsagesInScope(
            project: Project,
            stepPattern: String,
            scope: GlobalSearchScope,
        ): List<PsiElement> {
            val usages = mutableListOf<PsiElement>()
            val psiManager = PsiManager.getInstance(project)

            // Convert pattern to regex for matching
            val regex = patternToRegex(stepPattern)

            FileBasedIndex.getInstance().processAllKeys(NAME, { key ->
                if (!key.startsWith(ASSERT_PREFIX) && matchesPattern(key, regex)) {
                    findElements(key, psiManager, usages, scope)
                }
                true
            }, scope, null)

            return usages
        }

        private fun findElements(
            key: String,
            psiManager: PsiManager,
            usages: MutableList<PsiElement>,
            scope: GlobalSearchScope,
        ) {
            FileBasedIndex.getInstance().processValues(
                NAME,
                key,
                null,
                { file, data ->
                    val psiFile = psiManager.findFile(file)
                    if (psiFile != null) {
                        // Find the element at the line offset
                        val element = psiFile.findElementAt(data.offset)
                        if (element != null) {
                            usages.add(element)
                        }
                    }
                    true
                },
                scope,
            )
        }

        /**
         * Find all usages of an assertion pattern within the given scope.
         *
         * Use this for module-scoped search, passing in the result of
         * ModuleScopeResolver.getDependentModulesScope() for reverse navigation.
         *
         * @param project The project
         * @param assertionPattern The @Assertion pattern to search for
         * @param scope The search scope (e.g., dependent modules scope)
         * @return List of PSI elements where the assertion is used
         */
        fun findAssertionUsagesInScope(
            project: Project,
            assertionPattern: String,
            scope: GlobalSearchScope,
        ): List<PsiElement> {
            val usages = mutableListOf<PsiElement>()
            val psiManager = PsiManager.getInstance(project)

            // Convert pattern to regex for matching
            val regex = patternToRegex(assertionPattern)

            FileBasedIndex.getInstance().processAllKeys(NAME, { key ->
                if (key.startsWith(ASSERT_PREFIX) && matchesPattern(key.removePrefix(ASSERT_PREFIX), regex)) {
                    findElements(key, psiManager, usages, scope)
                }
                true
            }, scope, null)

            return usages
        }

        /**
         * Convert a @Step/@Assertion pattern to a regex.
         * Patterns support curly-brace placeholders: {string}, {int}, {float}, {word}, {number}, {any}.
         */
        internal fun patternToRegex(pattern: String): Regex {
            // Convert pattern placeholders to regex
            // Use Regex.escapeReplacement to prevent backslash interpretation
            val regexPattern =
                pattern
                    // Handle curly-brace placeholders
                    .replace(Regex("""\{int}"""), Regex.escapeReplacement("""(-?\d+)"""))
                    .replace(Regex("""\{string}"""), Regex.escapeReplacement("""("[^"]*"|'[^']*'|[^\s]+)"""))
                    .replace(Regex("""\{word}"""), Regex.escapeReplacement("""(\w+)"""))
                    .replace(Regex("""\{float}"""), Regex.escapeReplacement("""(-?\d+\.?\d*)"""))
                    .replace(Regex("""\{number}"""), Regex.escapeReplacement("""(-?\d+\.?\d*)"""))
                    .replace(Regex("""\{any}"""), Regex.escapeReplacement("""(.+?)"""))
                    // Anchor the pattern
                    .let { "^$it$" }

            return try {
                Regex(regexPattern, RegexOption.IGNORE_CASE)
            } catch (_: Exception) {
                // If regex compilation fails, try escaping and doing simple match
                try {
                    Regex("^${Regex.escape(pattern)}$", RegexOption.IGNORE_CASE)
                } catch (_: Exception) {
                    // Last resort: literal match
                    Regex(Regex.escape(pattern), RegexOption.IGNORE_CASE)
                }
            }
        }

        /**
         * Check if text matches a pattern regex
         */
        internal fun matchesPattern(
            text: String,
            regex: Regex,
        ): Boolean = regex.matches(text)
    }

    override fun getName(): ID<String, StepUsageData> = NAME

    override fun getVersion(): Int = 5 // Bumped to force reindex after shared step parser adoption

    override fun dependsOnFileContent(): Boolean = true

    override fun getInputFilter(): FileBasedIndex.InputFilter = FileBasedIndex.InputFilter { file ->
        file.extension == ScenarioFileType.EXTENSION ||
            file.extension == FragmentFileType.EXTENSION
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<StepUsageData> = StepUsageDataExternalizer()

    override fun getIndexer(): DataIndexer<String, StepUsageData, FileContent> = DataIndexer { inputData ->
        val result = mutableMapOf<String, StepUsageData>()
        PsiTreeUtil.findChildrenOfType(inputData.psiFile, BerryCrushStepElement::class.java).forEach { step ->
            val stepText = step.stepText
            if (!stepText.isNullOrBlank()) {
                result[stepText] =
                    StepUsageData(
                        offset = step.startOffset,
                        lineNumber = step.getStartLine() + 1,
                        stepType = step.keyword ?: "step",
                    )
            }
        }
        PsiTreeUtil.findChildrenOfType(inputData.psiFile, BerryCrushAssertElement::class.java).forEach { assertElement ->
            val assertionText = assertElement.assertionText
            if (!assertionText.isNullOrBlank()) {
                // Prefix with ASSERT: to distinguish from steps
                result["ASSERT:$assertionText"] =
                    StepUsageData(
                        offset = assertElement.startOffset,
                        lineNumber = assertElement.getStartLine() + 1,
                        stepType = "assert",
                    )
            }
        }
        result
    }
}

/**
 * Data stored for each step usage
 */
data class StepUsageData(
    val offset: Int,
    val lineNumber: Int,
    val stepType: String,
)

/**
 * Externalizer for StepUsageData
 */
class StepUsageDataExternalizer : DataExternalizer<StepUsageData> {
    override fun save(
        out: DataOutput,
        value: StepUsageData,
    ) {
        out.writeInt(value.offset)
        out.writeInt(value.lineNumber)
        out.writeUTF(value.stepType)
    }

    override fun read(input: DataInput): StepUsageData = StepUsageData(
        offset = input.readInt(),
        lineNumber = input.readInt(),
        stepType = input.readUTF(),
    )
}
