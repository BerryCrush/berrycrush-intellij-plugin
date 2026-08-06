package com.berrycrush.intellij.run

import com.berrycrush.intellij.language.ScenarioFileType
import com.berrycrush.intellij.psi.BerryCrushFeatureElement
import com.berrycrush.intellij.psi.BerryCrushScenarioElement
import com.intellij.execution.Executor
import com.intellij.execution.ProgramRunnerUtil
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
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
class BerryCrushScenarioRunLineMarkerProvider :
    RunLineMarkerContributor(),
    DumbAware {
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
        val (keywordType, name) =
            when {
                lineText.startsWith("scenario:") -> {
                    val scenarioElement = PsiTreeUtil.getParentOfType(element, BerryCrushScenarioElement::class.java)
                    "Scenario" to (scenarioElement?.description ?: extractName(lineText, "scenario:"))
                }
                lineText.startsWith("outline:") -> {
                    val scenarioElement = PsiTreeUtil.getParentOfType(element, BerryCrushScenarioElement::class.java)
                    "Outline" to (scenarioElement?.description ?: extractName(lineText, "outline:"))
                }
                lineText.startsWith("feature:") -> {
                    val featureElement = PsiTreeUtil.getParentOfType(element, BerryCrushFeatureElement::class.java)
                    "Feature" to (featureElement?.description ?: extractName(lineText, "feature:"))
                }
                else -> return null
            }

        // Get the scenario file name for filtering
        val scenarioFileName = file.virtualFile?.name ?: return null

        // Create custom run actions
        val runAction =
            RunScenarioAction(
                "Run '$name'",
                AllIcons.RunConfigurations.TestState.Run,
                DefaultRunExecutor.getRunExecutorInstance(),
                scenarioFileName,
                name,
                keywordType,
            )
        val debugAction =
            RunScenarioAction(
                "Debug '$name'",
                AllIcons.RunConfigurations.TestState.Run,
                DefaultDebugExecutor.getDebugExecutorInstance(),
                scenarioFileName,
                name,
                keywordType,
            )

        val icon =
            when (keywordType) {
                "Feature" -> AllIcons.RunConfigurations.TestState.Run_run
                else -> AllIcons.RunConfigurations.TestState.Run
            }

        return Info(
            icon,
            arrayOf(runAction, debugAction),
            { "Run $keywordType: $name" },
        )
    }

    private fun isFirstOnLine(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false
        val document = PsiDocumentManager.getInstance(element.project).getDocument(file) ?: return false
        val offset = element.textOffset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)

        val textBeforeElement =
            document
                .getText(
                    com.intellij.openapi.util
                        .TextRange(lineStart, offset),
                ).trim()
        return textBeforeElement.isEmpty()
    }

    private fun getFullLineText(element: PsiElement): String {
        val file = element.containingFile ?: return ""
        val document = PsiDocumentManager.getInstance(element.project).getDocument(file) ?: return ""
        val offset = element.textOffset
        val lineNumber = document.getLineNumber(offset)
        val lineStart = document.getLineStartOffset(lineNumber)
        val lineEnd = document.getLineEndOffset(lineNumber)
        return document.getText(
            com.intellij.openapi.util
                .TextRange(lineStart, lineEnd),
        )
    }

    private fun extractName(
        lineText: String,
        prefix: String,
    ): String {
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
    private val keywordType: String,
) : AnAction(text, "Run $keywordType: $scenarioName", icon),
    DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val fallbackClass = System.getProperty(BerryCrushScenarioExecutionSupport.DEFAULT_TEST_CLASS_PROPERTY)

        // Find all BerryCrush test classes
        val testClasses = BerryCrushScenarioExecutionSupport.findBerryCrushTestClasses(project)

        when {
            testClasses.isEmpty() -> {
                val fallback = BerryCrushScenarioExecutionSupport.resolveTestClass(project, null, fallbackClass)
                if (fallback != null) {
                    runWithTestClass(project, fallback)
                    return
                }

                BerryCrushScenarioExecutionSupport.showNoTestClassWarning(project)
            }
            testClasses.size == 1 -> {
                // Run with the single test class
                runWithTestClass(project, testClasses[0])
            }
            else -> {
                // Show popup to let user choose
                BerryCrushScenarioExecutionSupport.showTestClassChooser(
                    project = project,
                    candidates = testClasses,
                    preferredModule = null,
                    onSelected = { selectedClass -> runWithTestClass(project, selectedClass) },
                )
            }
        }
    }

    private fun runWithTestClass(
        project: Project,
        testClass: PsiClass,
    ) {
        val configName = "BerryCrush: $scenarioName"
        val config = BerryCrushScenarioExecutionSupport.createOrUpdateConfiguration(project, configName) ?: return
        val module = BerryCrushScenarioExecutionSupport.resolveModuleForClass(project, testClass)

        BerryCrushScenarioExecutionSupport.configureForClassRun(
            configuration = config,
            testClass = testClass,
            module = module,
            configName = configName,
            vmOptions = buildVmOptions(),
        )

        val settings =
            com.intellij.execution.RunManager
                .getInstance(project)
                .selectedConfiguration ?: return

        // Run the configuration
        val environment =
            ExecutionEnvironmentBuilder
                .createOrNull(executor, settings)
                ?.build() ?: return

        ProgramRunnerUtil.executeConfiguration(environment, false, true)
    }

    private fun buildVmOptions(): String = BerryCrushScenarioExecutionSupport.buildVmOptions(
        scenarioFile = scenarioFile,
        scenarioName = scenarioName,
        keywordType = keywordType,
    )
}
