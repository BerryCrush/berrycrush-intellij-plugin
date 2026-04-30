package com.berrycrush.intellij.reference

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

/**
 * Reference from `call ^operationId` to the OpenAPI specification.
 */
class BerryCrushOperationReference(
    element: PsiElement,
    textRange: TextRange,
    private val operationId: String
) : PsiReferenceBase<PsiElement>(element, textRange, true) {

    override fun resolve(): PsiElement? {
        val project = element.project
        return findOperationInOpenAPI(project, operationId)
    }

    override fun getVariants(): Array<Any> {
        val project = element.project
        return findAllOperationIds(project).toTypedArray()
    }

    companion object {
        /**
         * Find an operation in OpenAPI spec files.
         * If exact operation not found, returns first OpenAPI file as fallback.
         */
        fun findOperationInOpenAPI(project: Project, operationId: String): PsiElement? {
            val psiManager = PsiManager.getInstance(project)

            // Find all OpenAPI spec files
            val specFiles = findOpenAPIFiles(project)

            var firstOpenAPIFile: PsiFile? = null

            for (file in specFiles) {
                val psiFile = psiManager.findFile(file) ?: continue
                if (firstOpenAPIFile == null) {
                    firstOpenAPIFile = psiFile
                }
                val operation = findOperationInFile(psiFile, operationId)
                if (operation != null) {
                    return operation
                }
            }

            // Fallback: return first OpenAPI file if operation not found
            return firstOpenAPIFile
        }

        /**
         * Find all OpenAPI spec files in the project.
         * Scans all YAML, YML, and JSON files and checks if they're OpenAPI specs.
         */
        fun findOpenAPIFiles(project: Project): List<VirtualFile> {
            val scope = GlobalSearchScope.allScope(project)
            val files = mutableListOf<VirtualFile>()
            val psiManager = PsiManager.getInstance(project)

            // Search for all YAML files
            FilenameIndex.getAllFilesByExt(project, "yaml", scope).forEach { file ->
                if (isOpenAPISpec(psiManager.findFile(file))) {
                    files.add(file)
                }
            }

            FilenameIndex.getAllFilesByExt(project, "yml", scope).forEach { file ->
                if (isOpenAPISpec(psiManager.findFile(file))) {
                    files.add(file)
                }
            }

            // Search for all JSON files
            FilenameIndex.getAllFilesByExt(project, "json", scope).forEach { file ->
                if (isOpenAPISpec(psiManager.findFile(file))) {
                    files.add(file)
                }
            }

            return files.distinctBy { it.path }
        }

        /**
         * Check if a file is an OpenAPI specification.
         */
        private fun isOpenAPISpec(psiFile: PsiFile?): Boolean {
            if (psiFile == null) return false

            val text = psiFile.text
            if (text.length < 100) return false // Too short to be OpenAPI

            // Check for OpenAPI 3.x marker
            if (text.contains(Regex("""openapi:\s*['"]?3\."""))) {
                return true
            }

            // Check for Swagger/OpenAPI 2.x marker
            if (text.contains(Regex("""swagger:\s*['"]?2\."""))) {
                return true
            }

            // JSON format check
            if (text.contains(Regex(""""openapi"\s*:\s*"3\."""))) {
                return true
            }
            if (text.contains(Regex(""""swagger"\s*:\s*"2\."""))) {
                return true
            }

            return false
        }

        /**
         * Find operation ID in a single OpenAPI file.
         */
        private fun findOperationInFile(psiFile: PsiFile, operationId: String): PsiElement? {
            val text = psiFile.text

            // Search for operationId in YAML/JSON
            // Look for patterns like: operationId: getUserById or "operationId": "getUserById"
            val patterns = listOf(
                Regex("""operationId:\s*['"]?($operationId)['"]?"""),
                Regex(""""operationId"\s*:\s*"($operationId)"""")
            )

            for (pattern in patterns) {
                val match = pattern.find(text)
                if (match != null) {
                    // Find the position of the operationId value, not the key
                    val operationIdMatch = match.groups[1]
                    if (operationIdMatch != null) {
                        val offset = operationIdMatch.range.first
                        return psiFile.findElementAt(offset)
                    }
                }
            }

            return null
        }

        /**
         * Extract all operation IDs from the project's OpenAPI specs.
         */
        fun findAllOperationIds(project: Project): List<String> {
            val operationIds = mutableListOf<String>()
            val specFiles = findOpenAPIFiles(project)
            val psiManager = PsiManager.getInstance(project)

            for (file in specFiles) {
                val psiFile = psiManager.findFile(file) ?: continue
                extractOperationIdsFromFile(psiFile.text, operationIds)
            }

            return operationIds.distinct()
        }

        /**
         * Extract operation IDs from file content.
         */
        private fun extractOperationIdsFromFile(text: String, result: MutableList<String>) {
            // YAML format: operationId: someId
            Regex("""operationId:\s*['"]?(\w+)['"]?""").findAll(text).forEach {
                result.add(it.groupValues[1])
            }

            // JSON format: "operationId": "someId"
            Regex(""""operationId"\s*:\s*"(\w+)"""").findAll(text).forEach {
                result.add(it.groupValues[1])
            }
        }
    }
}
