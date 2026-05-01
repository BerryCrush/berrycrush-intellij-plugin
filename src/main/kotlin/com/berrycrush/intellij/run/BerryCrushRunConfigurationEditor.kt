package com.berrycrush.intellij.run

import com.berrycrush.intellij.language.ScenarioFileType
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent

/**
 * UI editor for BerryCrush run configuration settings.
 *
 * Provides form fields for:
 * - Scenario file path (with file browser)
 * - Scenario name filter (optional)
 * - Feature name filter (optional)
 */
class BerryCrushRunConfigurationEditor(
    private val project: Project,
) : SettingsEditor<BerryCrushRunConfiguration>() {

    private val scenarioFileField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            project,
            FileChooserDescriptorFactory.createSingleFileDescriptor(ScenarioFileType),
        )
    }

    private val scenarioNameField = JBTextField()
    private val featureNameField = JBTextField()

    override fun resetEditorFrom(configuration: BerryCrushRunConfiguration) {
        scenarioFileField.text = configuration.scenarioFilePath
        scenarioNameField.text = configuration.scenarioName ?: ""
        featureNameField.text = configuration.featureName ?: ""
    }

    override fun applyEditorTo(configuration: BerryCrushRunConfiguration) {
        configuration.scenarioFilePath = scenarioFileField.text
        configuration.scenarioName = scenarioNameField.text.takeIf { it.isNotBlank() }
        configuration.featureName = featureNameField.text.takeIf { it.isNotBlank() }
    }

    override fun createEditor(): JComponent {
        return panel {
            row("Scenario file:") {
                cell(scenarioFileField)
                    .align(AlignX.FILL)
                    .comment("Select the .scenario file to run")
            }
            row("Scenario name:") {
                cell(scenarioNameField)
                    .align(AlignX.FILL)
                    .comment("Optional: Run only this specific scenario (leave empty for all)")
            }
            row("Feature name:") {
                cell(featureNameField)
                    .align(AlignX.FILL)
                    .comment("Optional: Run only scenarios in this feature (leave empty for all)")
            }
        }
    }
}
