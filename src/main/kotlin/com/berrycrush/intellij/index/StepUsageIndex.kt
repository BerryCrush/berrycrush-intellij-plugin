/**
 * Index for tracking step usage in scenario and fragment files.
 * Maps step patterns (like "there is an account") to the files using them.
 */
package com.berrycrush.intellij.index

import com.berrycrush.intellij.language.FragmentFileType
import com.berrycrush.intellij.language.ScenarioFileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.DataIndexer
import com.intellij.util.indexing.FileBasedIndex
import com.intellij.util.indexing.FileBasedIndexExtension
import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import com.intellij.util.io.DataExternalizer
import com.intellij.util.io.EnumeratorStringDescriptor
import com.intellij.util.io.KeyDescriptor
import java.io.DataInput
import java.io.DataOutput

/**
 * Index that maps step text patterns to their locations in scenario/fragment files.
 * Used for reverse navigation from @Step/@Assertion methods to usages.
 */
class StepUsageIndex : FileBasedIndexExtension<String, StepUsageData>() {

    companion object {
        val NAME: ID<String, StepUsageData> = ID.create("com.berrycrush.intellij.index.StepUsageIndex")

        /**
         * Extract step text from a line (removes Given/When/Then/And/But prefix)
         */
        fun extractStepText(line: String): String? {
            val trimmed = line.trim()
            val lowerTrimmed = trimmed.lowercase()

            val prefixes = listOf("given ", "when ", "then ", "and ", "but ")
            for (prefix in prefixes) {
                if (lowerTrimmed.startsWith(prefix)) {
                    return trimmed.substring(prefix.length).trim()
                }
            }
            return null
        }

        /**
         * Extract assertion text from a line (removes Assert: prefix)
         */
        fun extractAssertionText(line: String): String? {
            val trimmed = line.trim()
            if (trimmed.lowercase().startsWith("assert")) {
                return trimmed.substring(6).trim()
            }
            return null
        }

        /**
         * Find all usages of a step pattern in the project.
         */
        fun findStepUsages(
            project: com.intellij.openapi.project.Project,
            stepPattern: String
        ): List<PsiElement> {
            return findStepUsagesWithScope(project, stepPattern, GlobalSearchScope.projectScope(project))
        }

        /**
         * Find all usages of a step pattern using allScope (includes all indexed files).
         */
        fun findStepUsagesAllScope(
            project: com.intellij.openapi.project.Project,
            stepPattern: String
        ): List<PsiElement> {
            return findStepUsagesWithScope(project, stepPattern, GlobalSearchScope.allScope(project))
        }

        private fun findStepUsagesWithScope(
            project: com.intellij.openapi.project.Project,
            stepPattern: String,
            scope: GlobalSearchScope
        ): List<PsiElement> {
            val usages = mutableListOf<PsiElement>()
            val psiManager = PsiManager.getInstance(project)

            // Convert pattern to regex for matching
            val regex = patternToRegex(stepPattern)

            FileBasedIndex.getInstance().processAllKeys(NAME, { key ->
                if (!key.startsWith("ASSERT:") && matchesPattern(key, regex)) {
                    FileBasedIndex.getInstance().processValues(
                        NAME, key, null,
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
                        scope
                    )
                }
                true
            }, scope, null)

            return usages
        }

        /**
         * Find all usages of an assertion pattern in the project.
         */
        fun findAssertionUsages(
            project: com.intellij.openapi.project.Project,
            assertionPattern: String
        ): List<PsiElement> {
            return findAssertionUsagesWithScope(project, assertionPattern, GlobalSearchScope.projectScope(project))
        }

        /**
         * Find all usages of an assertion pattern using allScope (includes all indexed files).
         */
        fun findAssertionUsagesAllScope(
            project: com.intellij.openapi.project.Project,
            assertionPattern: String
        ): List<PsiElement> {
            return findAssertionUsagesWithScope(project, assertionPattern, GlobalSearchScope.allScope(project))
        }

        private fun findAssertionUsagesWithScope(
            project: com.intellij.openapi.project.Project,
            assertionPattern: String,
            scope: GlobalSearchScope
        ): List<PsiElement> {
            val usages = mutableListOf<PsiElement>()
            val psiManager = PsiManager.getInstance(project)

            // Convert pattern to regex for matching
            val regex = patternToRegex(assertionPattern)

            FileBasedIndex.getInstance().processAllKeys(NAME, { key ->
                if (key.startsWith("ASSERT:") && matchesPattern(key.removePrefix("ASSERT:"), regex)) {
                    FileBasedIndex.getInstance().processValues(
                        NAME, key, null,
                        { file, data ->
                            val psiFile = psiManager.findFile(file)
                            if (psiFile != null) {
                                val element = psiFile.findElementAt(data.offset)
                                if (element != null) {
                                    usages.add(element)
                                }
                            }
                            true
                        },
                        scope
                    )
                }
                true
            }, scope, null)

            return usages
        }

        /**
         * Convert a @Step/@Assertion pattern to a regex.
         * Patterns support placeholders like (string), (number), (int), {string}, etc.
         */
        private fun patternToRegex(pattern: String): Regex {
            // Convert pattern placeholders to regex
            val regexPattern = pattern
                // Handle parentheses-style placeholders
                .replace(Regex("""\(int\)""", RegexOption.IGNORE_CASE), """(-?\d+)""")
                .replace(Regex("""\(number\)""", RegexOption.IGNORE_CASE), """(-?\d+\.?\d*)""")
                .replace(Regex("""\(string\)""", RegexOption.IGNORE_CASE), """("[^"]*"|'[^']*'|[^\s]+)""")
                .replace(Regex("""\(word\)""", RegexOption.IGNORE_CASE), """(\w+)""")
                .replace(Regex("""\(float\)""", RegexOption.IGNORE_CASE), """(-?\d+\.?\d*)""")
                .replace(Regex("""\(any\)""", RegexOption.IGNORE_CASE), """(.+?)""")
                // Handle curly-brace placeholders
                .replace(Regex("""\{int\}"""), """(-?\d+)""")
                .replace(Regex("""\{string\}"""), """("[^"]*"|'[^']*'|[^\s]+)""")
                .replace(Regex("""\{word\}"""), """(\w+)""")
                .replace(Regex("""\{float\}"""), """(-?\d+\.?\d*)""")
                .replace(Regex("""\{number\}"""), """(-?\d+\.?\d*)""")
                .replace(Regex("""\{any\}"""), """(.+?)""")
                // Escape regex special characters that are not placeholders
                .let { escapedPattern ->
                    // Anchor the pattern
                    "^$escapedPattern$"
                }
            
            return try {
                Regex(regexPattern, RegexOption.IGNORE_CASE)
            } catch (e: Exception) {
                // If regex compilation fails, try escaping and doing simple match
                try {
                    Regex("^${Regex.escape(pattern)}$", RegexOption.IGNORE_CASE)
                } catch (e2: Exception) {
                    // Last resort: literal match
                    Regex(Regex.escape(pattern), RegexOption.IGNORE_CASE)
                }
            }
        }

        /**
         * Check if text matches a pattern regex
         */
        private fun matchesPattern(text: String, regex: Regex): Boolean {
            return regex.matches(text)
        }

        /**
         * Get all indexed keys for debugging purposes (project scope)
         */
        fun getAllIndexedKeys(project: com.intellij.openapi.project.Project): List<String> {
            val keys = mutableListOf<String>()
            val scope = GlobalSearchScope.projectScope(project)
            FileBasedIndex.getInstance().processAllKeys(NAME, { key ->
                keys.add(key)
                true
            }, scope, null)
            return keys
        }

        /**
         * Get all indexed keys using allScope for debugging
         */
        fun getAllIndexedKeysAllScope(project: com.intellij.openapi.project.Project): List<String> {
            val keys = mutableListOf<String>()
            val scope = GlobalSearchScope.allScope(project)
            FileBasedIndex.getInstance().processAllKeys(NAME, { key ->
                keys.add(key)
                true
            }, scope, null)
            return keys
        }
    }

    override fun getName(): ID<String, StepUsageData> = NAME

    override fun getVersion(): Int = 4  // Bumped to force reindex with assertion support

    override fun dependsOnFileContent(): Boolean = true

    override fun getInputFilter(): FileBasedIndex.InputFilter {
        return FileBasedIndex.InputFilter { file ->
            file.extension == ScenarioFileType.EXTENSION ||
            file.extension == FragmentFileType.EXTENSION
        }
    }

    override fun getKeyDescriptor(): KeyDescriptor<String> = EnumeratorStringDescriptor.INSTANCE

    override fun getValueExternalizer(): DataExternalizer<StepUsageData> = StepUsageDataExternalizer()

    override fun getIndexer(): DataIndexer<String, StepUsageData, FileContent> {
        return DataIndexer { inputData ->
            val result = mutableMapOf<String, StepUsageData>()
            val content = inputData.contentAsText.toString()
            val lines = content.lines()

            var offset = 0
            for ((lineNumber, line) in lines.withIndex()) {
                // Check for step keywords
                val stepText = extractStepText(line)
                if (stepText != null && stepText.isNotBlank()) {
                    // Store with lowercase key for case-insensitive matching
                    result[stepText] = StepUsageData(
                        offset = offset + line.indexOfFirst { !it.isWhitespace() },
                        lineNumber = lineNumber + 1,
                        stepType = extractStepType(line)
                    )
                }

                // Check for assertion
                val assertionText = extractAssertionText(line)
                if (assertionText != null && assertionText.isNotBlank()) {
                    // Prefix with ASSERT: to distinguish from steps
                    result["ASSERT:$assertionText"] = StepUsageData(
                        offset = offset + line.indexOfFirst { !it.isWhitespace() },
                        lineNumber = lineNumber + 1,
                        stepType = "assert"
                    )
                }

                offset += line.length + 1 // +1 for newline
            }

            result
        }
    }

    private fun extractStepType(line: String): String {
        val trimmed = line.trim().lowercase()
        return when {
            trimmed.startsWith("given") -> "given"
            trimmed.startsWith("when") -> "when"
            trimmed.startsWith("then") -> "then"
            trimmed.startsWith("and") -> "and"
            trimmed.startsWith("but") -> "but"
            else -> "step"
        }
    }
}

/**
 * Data stored for each step usage
 */
data class StepUsageData(
    val offset: Int,
    val lineNumber: Int,
    val stepType: String
)

/**
 * Externalizer for StepUsageData
 */
class StepUsageDataExternalizer : DataExternalizer<StepUsageData> {
    override fun save(out: DataOutput, value: StepUsageData) {
        out.writeInt(value.offset)
        out.writeInt(value.lineNumber)
        out.writeUTF(value.stepType)
    }

    override fun read(input: DataInput): StepUsageData {
        return StepUsageData(
            offset = input.readInt(),
            lineNumber = input.readInt(),
            stepType = input.readUTF()
        )
    }
}
