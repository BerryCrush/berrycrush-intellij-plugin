package com.berrycrush.intellij.run

import com.berrycrush.intellij.language.ScenarioFileType
import com.berrycrush.intellij.psi.BerryCrushFeatureElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
import com.intellij.execution.ExecutionManager
import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.RunManager
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.PsiTreeUtil
import javax.swing.Icon

/**
 * Provides run gutter icons for scenario/outline/feature keywords in .scenario files.
 *
 * This allows running individual scenarios directly from the scenario file gutter,
 * similar to how JUnit test methods have run icons.
 *
 * When clicked, it creates a BerryCrush run configuration with scenario filtering
 * using the `-DberryCrush.scenarioName=...` VM option.
 */
class BerryCrushScenarioRunLineMarkerProvider : RunLineMarkerContributor(), DumbAware {

    override fun getInfo(element: PsiElement): Info? {
        // Only handle leaf elements (IntelliJ performance guideline)
        if (element.firstChild != null) {
            return null
        }

        // Only handle .scenario files
        val file = element.containingFile ?: return null
        if (file.fileType != ScenarioFileType) {
            return null
        }

        // Check if this is at the start of a line (avoid duplicates)
        if (!isFirstOnLine(element)) {
            return null
        }

        // Check for scenario/outline/feature keywords
        val lineText = getFullLineText(element).trim().lowercase()
        val (keywordType, name) = when {
            lineText.startsWith("scenario:") -> {
                val scenarioElement = PsiTreeUtil.getParentOfType(element, BerryCrushScenarioElement::class.java)
                "Scenario" to (scenarioElement?.scenarioName ?: extractName(lineText, "scenario:"))
            }
            lineText.startsWith("outline:") -> {
                "Outline" to extractName(lineText, "outline:")
            }
            lineText.startsWith("feature:") -> {
                val featureElement = PsiTreeUtil.getParentOfType(element, BerryCrushFeatureElement::class.java)
                "Feature" to (featureElement?.featureName ?: extractName(lineText, "feature:"))
            }
            else -> return null
        }

        // Get the scenario file path for filtering
        val scenarioFilePath = file.virtualFile?.path ?: return null
        val scenarioFileName = file.virtualFile?.name ?: return null

        // Create custom run actions
        val runAction = RunScenarioAction(
            "Run '$name'",
            AllIcons.RunConfigurations.TestState.Run,
            DefaultRunExecutor.getRunExecutorInstance(),
            scenarioFileName,
            name,
            keywordType
        )
        val debugAction = RunScenarioAction(
            "Debug '$name'",
            AllIcons.RunConfigurations.TestState.Run,
            DefaultDebugExecutor.getDebugExecutorInstance(),
            scenarioFileName,
            name,
            keywordType
        )

        val icon = when (keywordType) {
            "Feature" -> AllIcons.RunConfigurations.TestState.Run_run
            else -> AllIcons.RunConfigurations.TestState.Run
        }

        return Info(
            icon,
            arrayOf(runAction, debugAction),
            { "Run $keywordType: $name" }
        )
    }

    private fun isFirstOnLine(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false
        val document = PsiDocumentManager.getInstance(element.project).getDocument(file) ?: return false
        val offset = element.textOffset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)

        val textBeforeElement = document.getText(com.intellij.openapi.util.TextRange(lineStart, offset)).trim()
        return textBeforeElement.isEmpty()
    }

    private fun getFullLineText(element: PsiElement): String {
        val file = element.containingFile ?: return ""
        val document = PsiDocumentManager.getInstance(element.project).getDocument(file) ?: return ""
        val offset = element.textOffset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        return document.getText(com.intellij.openapi.util.TextRange(lineStart, lineEnd))
    }

    private fun extractName(lineText: String, prefix: String): String {
        val afterPrefix = lineText.removePrefix(prefix).trim()
        val commentIndex = afterPrefix.indexOf('#')
        return if (commentIndex >= 0) {
            afterPrefix.substring(0, commentIndex).trim()
        } else {
            afterPrefix
        }
    }
}

/**
 * Action to run a specific scenario from a .scenario file.
 *
 * Finds BerryCrush test classes (with @Suite + @BerryCrushConfiguration or @BerryCrushScenarios)
 * and creates a JUnit configuration with VM options to filter to the specific scenario.
 */
private class RunScenarioAction(
    text: String,
    icon: Icon,
    private val executor: Executor,
    private val scenarioFile: String,
    private val scenarioName: String,
    private val keywordType: String
) : AnAction(text, "Run $keywordType: $scenarioName", icon), DumbAware {

    companion object {
        // BerryCrush test classes are annotated with @Suite + @BerryCrushConfiguration
        // or with @BerryCrushScenarios
        private val BERRYCRUSH_ANNOTATIONS = listOf(
            "org.berrycrush.junit.BerryCrushScenarios",
            "org.berrycrush.junit.BerryCrushConfiguration"
        )
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        // Find all BerryCrush test classes
        val testClasses = findBerryCrushTestClasses(project)

        when {
            testClasses.isEmpty() -> {
                // Show notification that no test class was found
                com.intellij.openapi.ui.Messages.showWarningDialog(
                    project,
                    "No BerryCrush test class found.\n\n" +
                        "Create a test class with @Suite and @BerryCrushConfiguration annotations " +
                        "that includes this scenario file.",
                    "No Test Class Found"
                )
            }
            testClasses.size == 1 -> {
                // Run with the single test class
                runWithTestClass(project, testClasses[0])
            }
            else -> {
                // Show popup to let user choose
                showTestClassChooser(project, testClasses, e)
            }
        }
    }

    private fun showTestClassChooser(project: Project, testClasses: List<PsiClass>, e: AnActionEvent) {
        val popup = com.intellij.openapi.ui.popup.JBPopupFactory.getInstance()
            .createPopupChooserBuilder(testClasses)
            .setTitle("Select Test Class")
            .setItemChosenCallback { selectedClass ->
                runWithTestClass(project, selectedClass)
            }
            .setRenderer(object : com.intellij.ui.ColoredListCellRenderer<PsiClass>() {
                override fun customizeCellRenderer(
                    list: javax.swing.JList<out PsiClass>,
                    value: PsiClass?,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean
                ) {
                    if (value != null) {
                        icon = com.intellij.icons.AllIcons.Nodes.Class
                        append(value.name ?: "Unknown")
                        value.qualifiedName?.let { qn ->
                            val packageName = qn.substringBeforeLast('.', "")
                            if (packageName.isNotEmpty()) {
                                append(" ($packageName)", com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES)
                            }
                        }
                    }
                }
            })
            .createPopup()

        // Show popup at the mouse location or in the center
        val dataContext = e.dataContext
        val component = dataContext.getData(com.intellij.openapi.actionSystem.PlatformDataKeys.CONTEXT_COMPONENT)
        if (component != null) {
            popup.showUnderneathOf(component)
        } else {
            popup.showInFocusCenter()
        }
    }

    private fun runWithTestClass(project: Project, testClass: PsiClass) {
        // Get the registered BerryCrush configuration type
        val configType = com.intellij.execution.configurations.ConfigurationTypeUtil
            .findConfigurationType(BerryCrushConfigurationType::class.java)
        val factory = configType.configurationFactories[0]

        val runManager = RunManager.getInstance(project)
        val configName = "BerryCrush: $scenarioName"
        var settings = runManager.findConfigurationByName(configName)

        if (settings == null) {
            settings = runManager.createConfiguration(configName, factory)
            runManager.addConfiguration(settings)
        }

        val config = settings.configuration as? BerryCrushRunConfiguration ?: return

        // Configure to run the test class
        config.persistentData.TEST_OBJECT = JUnitConfiguration.TEST_CLASS
        config.persistentData.MAIN_CLASS_NAME = testClass.qualifiedName

        // Set the module from the test class's containing file
        val containingFile = testClass.containingFile
        if (containingFile != null) {
            val projectFileIndex = com.intellij.openapi.roots.ProjectRootManager.getInstance(project).fileIndex
            val virtualFile = containingFile.virtualFile
            if (virtualFile != null) {
                val module = projectFileIndex.getModuleForFile(virtualFile)
                if (module != null) {
                    config.setModule(module)
                }
            }
        }

        // Add VM options for scenario filtering
        val vmOptions = buildVmOptions()
        config.vmParameters = vmOptions

        runManager.selectedConfiguration = settings

        // Run the configuration
        val environment = ExecutionEnvironmentBuilder
            .createOrNull(executor, settings)
            ?.build() ?: return

        ProgramRunnerUtil.executeConfiguration(environment, false, true)
    }

    private fun buildVmOptions(): String {
        val options = mutableListOf<String>()

        // Add scenario file filter (quote if contains spaces)
        options.add(buildVmOption("berryCrush.scenarioFile", scenarioFile))

        // Add scenario/feature name filter based on keyword type
        when (keywordType) {
            "Scenario", "Outline" -> options.add(buildVmOption("berryCrush.scenarioName", scenarioName))
            "Feature" -> options.add(buildVmOption("berryCrush.featureName", scenarioName))
        }

        return options.joinToString(" ")
    }

    /**
     * Builds a VM option with proper quoting for values that contain spaces.
     */
    private fun buildVmOption(key: String, value: String): String {
        return if (value.contains(' ') || value.contains('\t')) {
            "-D$key=\"$value\""
        } else {
            "-D$key=$value"
        }
    }

    private fun findBerryCrushTestClasses(project: Project): List<PsiClass> {
        val scope = GlobalSearchScope.projectScope(project)
        val psiFacade = JavaPsiFacade.getInstance(project)
        val result = mutableSetOf<PsiClass>()

        // Search for classes with each BerryCrush annotation
        for (annotationFqn in BERRYCRUSH_ANNOTATIONS) {
            val annotation = psiFacade.findClass(annotationFqn, scope) ?: continue
            AnnotatedElementsSearch.searchPsiClasses(annotation, scope).forEach { psiClass ->
                result.add(psiClass)
            }
        }

        return result.toList().sortedBy { it.qualifiedName }
    }
}
