package com.berrycrush.intellij.listener

import com.berrycrush.intellij.index.FragmentIndex
import com.berrycrush.intellij.reference.BerryCrushOperationReference
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.AsyncFileListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.util.indexing.FileBasedIndex
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Listens for file system changes that affect BerryCrush inspections.
 *
 * When fragment files, OpenAPI specs, or Java/Kotlin files change,
 * triggers re-analysis of open scenario files to update inspection results.
 */
class BerryCrushFileChangeListener : AsyncFileListener {

    companion object {
        private val LOG = Logger.getInstance(BerryCrushFileChangeListener::class.java)
        private val pendingRestart = AtomicBoolean(false)
    }

    override fun prepareChange(events: MutableList<out VFileEvent>): AsyncFileListener.ChangeApplier? {
        val relevantChanges = events.filter { isRelevantFileChange(it) }
        if (relevantChanges.isEmpty()) return null

        val hasFragmentChanges = relevantChanges.any { 
            getAffectedFile(it)?.extension?.lowercase() == "fragment" 
        }

        return object : AsyncFileListener.ChangeApplier {
            override fun afterVfsChange() {
                scheduleAnalysisRestart(hasFragmentChanges)
            }
        }
    }

    private fun isRelevantFileChange(event: VFileEvent): Boolean {
        val file = getAffectedFile(event) ?: return false
        val extension = file.extension?.lowercase() ?: return false

        return when (extension) {
            // Fragment files - affect include directives
            "fragment" -> true
            // Scenario files - when one scenario changes, others may need refresh
            "scenario" -> true
            // OpenAPI specs - check file content using BerryCrushOperationReference
            "yaml", "yml", "json" -> isOpenAPISpecFile(file)
            // Java/Kotlin files (may contain @Step/@Assertion)
            "java", "kt" -> true
            else -> false
        }
    }

    private fun getAffectedFile(event: VFileEvent): VirtualFile? = event.file

    private fun isOpenAPISpecFile(file: VirtualFile): Boolean {
        val fileName = file.name.lowercase()
        
        // Filename heuristic: files with openapi/swagger in name are likely OpenAPI specs
        if (fileName.contains("openapi") || fileName.contains("swagger")) {
            // Still verify minimal content structure
            return checkOpenAPIContent(file, relaxedCheck = true)
        }
        
        // For other yaml/json files, do full content check
        return checkOpenAPIContent(file, relaxedCheck = false)
    }

    private fun checkOpenAPIContent(file: VirtualFile, relaxedCheck: Boolean): Boolean = runCatching {
        // Try to read file content directly first (works in tests)
        val content = file.inputStream?.bufferedReader()?.use { it.readText() }
        if (content != null) {
            return@runCatching BerryCrushOperationReference.isOpenAPISpec(content, relaxedLengthCheck = relaxedCheck)
        }
        
        // Fall back to PSI-based check
        ProjectManager.getInstance().openProjects.filter{ !it.isDisposed }.any { project ->
            val psiManager = PsiManager.getInstance(project)
            val psiFile = psiManager.findFile(file)
            psiFile != null && BerryCrushOperationReference.isOpenAPISpec(psiFile)
        }
    }.onFailure { e ->
        LOG.debug("Could not check OpenAPI spec content", e)
    }.getOrElse { false }

    private fun scheduleAnalysisRestart(requestIndexRebuild: Boolean) {
        // Debounce multiple rapid changes
        if (!pendingRestart.compareAndSet(false, true)) return

        // Use invokeLater with delay to ensure VFS and indexes are fully updated
        ApplicationManager.getApplication().invokeLater({
            runCatching {
                for (project in ProjectManager.getInstance().openProjects) {
                    if (project.isDisposed) continue

                    // Request fragment index rebuild if fragment files changed
                    if (requestIndexRebuild) {
                        FileBasedIndex.getInstance().requestRebuild(FragmentIndex.KEY)
                    }

                    // Wait for indexing to complete, then restart analysis
                    DumbService.getInstance(project).runWhenSmart {
                        restartAnalysisForProject(project)
                        pendingRestart.set(false)
                    }
                }
            }.onFailure { e ->
                LOG.debug("Error scheduling analysis restart", e)
                pendingRestart.set(false)
            }
        }, ModalityState.nonModal())
    }

    private fun restartAnalysisForProject(project: Project) {
        if (project.isDisposed) return

        runCatching {
            PsiDocumentManager.getInstance(project).commitAllDocuments()

            val fileEditorManager = FileEditorManager.getInstance(project)
            val psiManager = PsiManager.getInstance(project)
            val analyzer = DaemonCodeAnalyzer.getInstance(project)

            // Find all open scenario/fragment files and restart analysis
            ReadAction.run<RuntimeException> {
                fileEditorManager.openFiles
                    .filter { it.extension == "scenario" || it.extension == "fragment" }
                    .forEach { file ->
                        psiManager.findFile(file)?.let { psiFile ->
                            LOG.debug("Restarting analysis for: ${file.name}")
                            analyzer.restart(psiFile, "Restart analysis")
                        }
                    }
            }
        }.onFailure { e ->
            LOG.debug("Error restarting analysis for BerryCrush files", e)
        }
    }
}
