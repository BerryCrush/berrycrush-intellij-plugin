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

    private fun getAffectedFile(event: VFileEvent): VirtualFile? {
        return when (event) {
            is VFileCreateEvent -> event.file
            is VFileDeleteEvent -> event.file
            is VFileContentChangeEvent -> event.file
            is VFileMoveEvent -> event.file
            is VFilePropertyChangeEvent -> event.file
            else -> event.file
        }
    }

    private fun isOpenAPISpecFile(file: VirtualFile): Boolean {
        // First try filename-based heuristic (fast, no PSI needed)
        if (hasOpenAPIFilename(file)) {
            return true
        }

        // Then try content-based detection using BerryCrushOperationReference
        // This requires project context and PSI, so may not work during VFS events
        return checkOpenAPIContent(file)
    }

    private fun hasOpenAPIFilename(file: VirtualFile): Boolean {
        val name = file.name.lowercase()
        return name.contains("openapi") || 
               name.contains("swagger") ||
               name == "api.yaml" || 
               name == "api.yml" || 
               name == "api.json"
    }

    private fun checkOpenAPIContent(file: VirtualFile): Boolean {
        try {
            for (project in ProjectManager.getInstance().openProjects) {
                if (project.isDisposed) continue
                val psiManager = PsiManager.getInstance(project)
                val psiFile = psiManager.findFile(file)
                if (psiFile != null && BerryCrushOperationReference.isOpenAPISpec(psiFile)) {
                    return true
                }
            }
        } catch (e: Exception) {
            LOG.debug("Could not check OpenAPI spec content", e)
        }
        return false
    }

    private fun scheduleAnalysisRestart(requestIndexRebuild: Boolean) {
        // Debounce multiple rapid changes
        if (!pendingRestart.compareAndSet(false, true)) return

        // Use invokeLater with delay to ensure VFS and indexes are fully updated
        ApplicationManager.getApplication().invokeLater({
            try {
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
            } catch (e: Exception) {
                LOG.debug("Error scheduling analysis restart", e)
                pendingRestart.set(false)
            }
        }, ModalityState.nonModal())
    }

    private fun restartAnalysisForProject(project: com.intellij.openapi.project.Project) {
        if (project.isDisposed) return

        try {
            // Commit all documents to ensure PSI is up to date
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
                            analyzer.restart(psiFile)
                        }
                    }
            }
        } catch (e: Exception) {
            LOG.debug("Error restarting analysis for BerryCrush files", e)
        }
    }
}
