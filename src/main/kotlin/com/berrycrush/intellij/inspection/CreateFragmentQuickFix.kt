package com.berrycrush.intellij.inspection

import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import javax.swing.ButtonGroup
import javax.swing.DefaultComboBoxModel
import javax.swing.JComboBox
import javax.swing.JComponent

/**
 * Quick fix to create a missing fragment file.
 *
 * Shows a dialog allowing the user to:
 * - Create a new fragment file in a chosen location
 * - Append to an existing fragment file
 */
class CreateFragmentQuickFix(
    private val fragmentName: String
) : LocalQuickFix {

    override fun getName(): String = "Create fragment '$fragmentName'..."

    override fun getFamilyName(): String = "BerryCrush"

    override fun startInWriteAction(): Boolean = false

    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo {
        return IntentionPreviewInfo.Html(
            "Creates a new <code>$fragmentName.fragment</code> file or appends to an existing fragment file."
        )
    }

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val containingFile = descriptor.psiElement?.containingFile ?: return
        val defaultDir = containingFile.containingDirectory?.virtualFile

        // Show dialog on EDT
        ApplicationManager.getApplication().invokeLater {
            val dialog = CreateFragmentDialog(project, fragmentName, defaultDir)
            if (dialog.showAndGet()) {
                when (dialog.getSelectedMode()) {
                    CreateMode.NEW_FILE -> {
                        val targetDir = dialog.getSelectedDirectory()
                        if (targetDir != null) {
                            createNewFragmentFile(project, targetDir, dialog.getFileName())
                        }
                    }
                    CreateMode.APPEND -> {
                        val targetFile = dialog.getSelectedExistingFile()
                        if (targetFile != null) {
                            appendToExistingFile(project, targetFile)
                        }
                    }
                }
            }
        }
    }

    private fun createNewFragmentFile(project: Project, directory: VirtualFile, fileName: String) {
        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val file = directory.createChildData(this, fileName)
                file.setBinaryContent(generateFragmentTemplate().toByteArray())
                FileEditorManager.getInstance(project).openFile(file, true)
            } catch (e: Exception) {
                Messages.showErrorDialog(project, "Failed to create file: ${e.message}", "Create Fragment")
            }
        }
    }

    private fun appendToExistingFile(project: Project, file: VirtualFile) {
        WriteCommandAction.runWriteCommandAction(project) {
            try {
                val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@runWriteCommandAction
                val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
                    ?: return@runWriteCommandAction

                val appendContent = "\n\n" + generateFragmentDefinition()
                document.insertString(document.textLength, appendContent)
                PsiDocumentManager.getInstance(project).commitDocument(document)
                FileEditorManager.getInstance(project).openFile(file, true)
            } catch (e: Exception) {
                Messages.showErrorDialog(project, "Failed to append to file: ${e.message}", "Create Fragment")
            }
        }
    }

    private fun generateFragmentTemplate(): String {
        return """# Fragment: $fragmentName
#
# Reusable step fragment. Include in scenarios with:
#   include $fragmentName

${generateFragmentDefinition()}
"""
    }

    private fun generateFragmentDefinition(): String {
        return """fragment: $fragmentName
  # TODO: Add steps here
  given: setup
    # Add directives"""
    }

    enum class CreateMode { NEW_FILE, APPEND }
}

/**
 * Dialog for choosing how to create the fragment.
 */
private class CreateFragmentDialog(
    private val project: Project,
    private val fragmentName: String,
    private val defaultDir: VirtualFile?
) : DialogWrapper(project) {

    private var mode = CreateFragmentQuickFix.CreateMode.NEW_FILE
    private var fileName = "$fragmentName.fragment"
    private var selectedDir: VirtualFile? = defaultDir
    private var selectedExistingFile: VirtualFile? = null
    private val existingFiles: List<VirtualFile>

    init {
        title = "Create Fragment: $fragmentName"
        existingFiles = findExistingFragmentFiles()
        init()
    }

    private fun findExistingFragmentFiles(): List<VirtualFile> {
        val scope = GlobalSearchScope.projectScope(project)
        return FilenameIndex.getAllFilesByExt(project, "fragment", scope).toList()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = javax.swing.JPanel()
        mainPanel.layout = javax.swing.BoxLayout(mainPanel, javax.swing.BoxLayout.Y_AXIS)

        val newFileRadio = JBRadioButton("Create new file", true)
        val appendRadio = JBRadioButton("Append to existing file", false)
        val buttonGroup = ButtonGroup()
        buttonGroup.add(newFileRadio)
        buttonGroup.add(appendRadio)

        val fileNameField = JBTextField(fileName, 30)
        val dirLabel = javax.swing.JLabel(selectedDir?.path ?: "Select directory...")
        val browseButton = javax.swing.JButton("Browse...")

        val existingFilesCombo = JComboBox(DefaultComboBoxModel(
            existingFiles.map { it.name }.toTypedArray()
        ))
        existingFilesCombo.isEnabled = false

        newFileRadio.addActionListener {
            mode = CreateFragmentQuickFix.CreateMode.NEW_FILE
            fileNameField.isEnabled = true
            browseButton.isEnabled = true
            existingFilesCombo.isEnabled = false
        }

        appendRadio.addActionListener {
            mode = CreateFragmentQuickFix.CreateMode.APPEND
            fileNameField.isEnabled = false
            browseButton.isEnabled = false
            existingFilesCombo.isEnabled = true
            if (existingFiles.isNotEmpty() && existingFilesCombo.selectedIndex >= 0) {
                selectedExistingFile = existingFiles[existingFilesCombo.selectedIndex]
            }
        }

        browseButton.addActionListener {
            val descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor()
            descriptor.title = "Select Directory for Fragment File"
            FileChooser.chooseFile(descriptor, project, selectedDir) { chosen ->
                selectedDir = chosen
                dirLabel.text = chosen.path
            }
        }

        existingFilesCombo.addActionListener {
            if (existingFilesCombo.selectedIndex >= 0) {
                selectedExistingFile = existingFiles[existingFilesCombo.selectedIndex]
            }
        }

        fileNameField.document.addDocumentListener(object : javax.swing.event.DocumentListener {
            override fun insertUpdate(e: javax.swing.event.DocumentEvent) { fileName = fileNameField.text }
            override fun removeUpdate(e: javax.swing.event.DocumentEvent) { fileName = fileNameField.text }
            override fun changedUpdate(e: javax.swing.event.DocumentEvent) { fileName = fileNameField.text }
        })

        // Build panel using traditional Swing
        val radioPanel1 = javax.swing.JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT))
        radioPanel1.add(newFileRadio)

        val fileNamePanel = javax.swing.JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT))
        fileNamePanel.add(javax.swing.JLabel("    File name: "))
        fileNamePanel.add(fileNameField)

        val dirPanel = javax.swing.JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT))
        dirPanel.add(javax.swing.JLabel("    Directory: "))
        dirPanel.add(dirLabel)
        dirPanel.add(browseButton)

        val radioPanel2 = javax.swing.JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT))
        radioPanel2.add(appendRadio)

        val existingPanel = javax.swing.JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT))
        existingPanel.add(javax.swing.JLabel("    Existing file: "))
        existingPanel.add(existingFilesCombo)

        mainPanel.add(radioPanel1)
        mainPanel.add(fileNamePanel)
        mainPanel.add(dirPanel)
        mainPanel.add(javax.swing.Box.createVerticalStrut(10))
        mainPanel.add(radioPanel2)
        mainPanel.add(existingPanel)

        return mainPanel
    }

    fun getSelectedMode(): CreateFragmentQuickFix.CreateMode = mode
    fun getFileName(): String = if (fileName.endsWith(".fragment")) fileName else "$fileName.fragment"
    fun getSelectedDirectory(): VirtualFile? = selectedDir
    fun getSelectedExistingFile(): VirtualFile? = selectedExistingFile
}
