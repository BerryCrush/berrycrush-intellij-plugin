package org.berrycrush.intellij.run

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Callable
import javax.swing.JList

private val berryCrushAnnotations =
    listOf(
        "org.berrycrush.junit.BerryCrushScenarios",
        "org.berrycrush.junit.BerryCrushConfiguration",
        "org.berrycrush.junit.BerryCrushSpec",
    )

internal data class ClassCandidate<T>(
    val value: T,
    val qualifiedName: String,
    val inPreferredModule: Boolean,
)

internal data class TestClassChoice(
    val className: String,
    val qualifiedName: String,
    val packageName: String,
    val moduleName: String?,
)

/**
 * Shared utilities for scenario execution from gutter and context-menu entry points.
 */
object BerryCrushScenarioExecutionSupport {
    const val DEFAULT_TEST_CLASS_PROPERTY: String = "berryCrush.defaultTestClass"

    private sealed interface TestClassChooserResult {
        data object Empty : TestClassChooserResult

        data class Single(
            val qualifiedName: String?,
        ) : TestClassChooserResult

        data class Multiple(
            val choices: List<TestClassChoice>,
        ) : TestClassChooserResult
    }

    internal fun <T> selectPreferredCandidate(
        candidates: List<ClassCandidate<T>>,
    ): T? = if (candidates.isEmpty()) {
        null
    } else {
        candidates
            .sortedWith(
                compareByDescending<ClassCandidate<T>> { it.inPreferredModule }
                    .thenBy { it.qualifiedName },
            )
            .first()
            .value
    }

    fun findBerryCrushTestClasses(project: Project): List<PsiClass> {
        val scope = GlobalSearchScope.allScope(project)
        val psiFacade = JavaPsiFacade.getInstance(project)
        val result = mutableSetOf<PsiClass>()

        berryCrushAnnotations.forEach { annotationFqn ->
            val annotation = psiFacade.findClass(annotationFqn, scope)
                ?: return@forEach

            AnnotatedElementsSearch
                .searchPsiClasses(annotation, scope)
                .forEach { psiClass ->
                    result.add(psiClass)
                }
        }

        return result.sortedBy {
            it.qualifiedName ?: it.name ?: ""
        }
    }

    fun selectPreferredTestClass(
        candidates: List<PsiClass>,
        preferredModule: Module?,
    ): PsiClass? = selectPreferredCandidate(mapClassCandidates(candidates, preferredModule))

    fun resolveTestClass(
        project: Project,
        preferredModule: Module?,
        fallbackClassFqn: String?,
    ): PsiClass? = resolveTestClass(
        project = project,
        preferredModule = preferredModule,
        fallbackClassFqn = fallbackClassFqn,
        chooseWhenMultiple = false,
    )

    fun resolveTestClass(
        project: Project,
        preferredModule: Module?,
        fallbackClassFqn: String?,
        chooseWhenMultiple: Boolean,
    ): PsiClass? {
        val discovered = findBerryCrushTestClasses(project)

        return if (chooseWhenMultiple && discovered.size > 1) {
            null
        } else {
            selectPreferredTestClass(discovered, preferredModule)
                ?: if (fallbackClassFqn.isNullOrBlank()) {
                    null
                } else {
                    resolveFallbackClass(project, fallbackClassFqn)
                }
        }
    }

    fun resolveModuleForClass(
        project: Project,
        testClass: PsiClass,
    ): Module? = testClass.containingFile?.virtualFile?.let { virtualFile ->
        ProjectRootManager
            .getInstance(project)
            .fileIndex
            .getModuleForFile(virtualFile)
    }

    fun configureForClassRun(
        configuration: BerryCrushRunConfiguration,
        testClass: PsiClass,
        module: Module?,
        configName: String,
        vmOptions: String,
    ) {
        configuration.persistentData.TEST_OBJECT =
            JUnitConfiguration.TEST_CLASS

        configuration.persistentData.MAIN_CLASS_NAME =
            testClass.qualifiedName

        configuration.vmParameters = vmOptions
        configuration.name = configName

        module?.let {
            configuration.setModule(it)
        }
    }

    fun buildVmOptions(
        scenarioFile: String,
        scenarioName: String? = null,
        keywordType: String? = null,
    ): String {
        val options =
            mutableListOf(
                buildVmOption(
                    "berryCrush.scenarioFile",
                    scenarioFile,
                ),
            )

        when (keywordType) {
            "Scenario", "Outline" -> {
                if (!scenarioName.isNullOrBlank()) {
                    options.add(
                        buildVmOption(
                            "berryCrush.scenarioName",
                            scenarioName,
                        ),
                    )
                }
            }

            "Feature" -> {
                if (!scenarioName.isNullOrBlank()) {
                    options.add(
                        buildVmOption(
                            "berryCrush.featureName",
                            scenarioName,
                        ),
                    )
                }
            }
        }

        return options.joinToString(" ")
    }

    fun buildVmOption(
        key: String,
        value: String,
    ): String = if (value.contains(' ') || value.contains('\t')) {
        "-D$key=\"$value\""
    } else {
        "-D$key=$value"
    }

    fun createOrUpdateConfiguration(
        project: Project,
        configName: String,
    ): BerryCrushRunConfiguration? {
        val configType =
            ConfigurationTypeUtil.findConfigurationType(
                BerryCrushConfigurationType::class.java,
            )

        val factory = configType.configurationFactories[0]

        val runManager = RunManager.getInstance(project)
        var settings = runManager.findConfigurationByName(configName)

        if (settings == null) {
            settings = runManager.createConfiguration(
                configName,
                factory,
            )
            runManager.addConfiguration(settings)
        }

        val configuration =
            settings.configuration as? BerryCrushRunConfiguration
                ?: return null

        runManager.selectedConfiguration = settings

        return configuration
    }

    fun showNoTestClassWarning(project: Project) {
        Messages.showWarningDialog(
            project,
            "No BerryCrush test class found.\n\n" +
                "Create a test class with @Suite and @BerryCrushConfiguration annotations " +
                "that includes this scenario file.",
            "No Test Class Found",
        )
    }

    private fun resolveFallbackClass(
        project: Project,
        fallbackClassFqn: String,
    ): PsiClass? {
        val fallbackClass =
            JavaPsiFacade
                .getInstance(project)
                .findClass(
                    fallbackClassFqn,
                    GlobalSearchScope.projectScope(project),
                )
                ?: return null

        return fallbackClass.takeIf {
            isBerryCrushTestClass(it)
        }
    }

    private fun isBerryCrushTestClass(
        psiClass: PsiClass,
    ): Boolean {
        return psiClass.annotations.any { annotation ->
            val qualifiedName =
                annotation.qualifiedName
                    ?: return@any false

            berryCrushAnnotations.any { expected ->
                qualifiedName == expected ||
                    qualifiedName.endsWith(".$expected")
            }
        }
    }

    fun sortCandidates(
        candidates: List<PsiClass>,
        preferredModule: Module?,
    ): List<PsiClass> = mapClassCandidates(candidates, preferredModule)
        .sortedWith(
            compareByDescending<ClassCandidate<PsiClass>> {
                it.inPreferredModule
            }.thenBy {
                it.qualifiedName
            },
        )
        .map {
            it.value
        }

    internal fun buildChoiceContextLabel(
        packageName: String,
        moduleName: String?,
        includeModuleName: Boolean,
    ): String {
        if (
            packageName.isEmpty() &&
            (!includeModuleName || moduleName.isNullOrBlank())
        ) {
            return ""
        }

        val details =
            listOfNotNull(
                packageName.takeIf {
                    it.isNotEmpty()
                },
                moduleName?.takeIf {
                    includeModuleName && it.isNotBlank()
                },
            ).joinToString(" - ")

        return if (details.isEmpty()) {
            ""
        } else {
            " ($details)"
        }
    }

    private fun toTestClassChoices(
        sortedCandidates: List<PsiClass>,
        preferredModule: Module?,
    ): List<TestClassChoice> = sortedCandidates.map { candidate ->
        val qualifiedName =
            candidate.qualifiedName
                ?: candidate.name
                ?: "Unknown"

        val packageName =
            qualifiedName.substringBeforeLast('.', "")

        val className =
            candidate.name
                ?: qualifiedName.substringAfterLast(
                    '.',
                    "Unknown",
                )

        val candidateModule =
            ModuleUtilCore.findModuleForPsiElement(candidate)

        val moduleName =
            candidateModule
                ?.takeIf {
                    preferredModule == null ||
                        it != preferredModule
                }
                ?.name

        TestClassChoice(
            className = className,
            qualifiedName = qualifiedName,
            packageName = packageName,
            moduleName = moduleName,
        )
    }

    fun showTestClassChooser(
        project: Project,
        candidates: List<PsiClass>,
        preferredModule: Module?,
        onSelected: (PsiClass) -> Unit,
    ) {
        ReadAction
            .nonBlocking(
                Callable {
                    val sortedCandidates =
                        sortCandidates(
                            candidates,
                            preferredModule,
                        )

                    if (sortedCandidates.isEmpty()) {
                        TestClassChooserResult.Empty
                    } else if (sortedCandidates.size == 1) {
                        TestClassChooserResult.Single(
                            sortedCandidates[0].qualifiedName,
                        )
                    } else {
                        val choices =
                            toTestClassChoices(
                                sortedCandidates,
                                preferredModule,
                            )
                        TestClassChooserResult.Multiple(choices)
                    }
                },
            )
            .finishOnUiThread(
                ModalityState.defaultModalityState(),
            ) { result ->
                when (result) {
                    TestClassChooserResult.Empty -> {
                        showNoTestClassWarning(project)
                    }

                    is TestClassChooserResult.Single -> {
                        result.qualifiedName?.let {
                            resolveSelectedTestClass(
                                project,
                                it,
                                onSelected,
                            )
                        } ?: showNoTestClassWarning(project)
                    }

                    is TestClassChooserResult.Multiple -> {
                        showTestClassChooserPopup(
                            project,
                            result.choices,
                            onSelected,
                        )
                    }
                }
            }
            .submit(
                AppExecutorUtil.getAppExecutorService(),
            )
    }

    private fun showTestClassChooserPopup(
        project: Project,
        choices: List<TestClassChoice>,
        onSelected: (PsiClass) -> Unit,
    ) {
        val duplicateSimpleNames =
            choices
                .groupingBy {
                    it.className
                }
                .eachCount()
                .filterValues {
                    it > 1
                }
                .keys

        val popup =
            JBPopupFactory
                .getInstance()
                .createPopupChooserBuilder(choices)
                .setTitle("Select Test Class")
                .setItemChosenCallback { selectedChoice ->
                    resolveSelectedTestClass(
                        project,
                        selectedChoice.qualifiedName,
                        onSelected,
                    )
                }
                .setRenderer(TestClassListCellRenderer(duplicateSimpleNames))
                .createPopup()

        popup.showInFocusCenter()
    }

    private fun resolveSelectedTestClass(
        project: Project,
        qualifiedName: String,
        onSelected: (PsiClass) -> Unit,
    ) {
        fun resolveClassByQualifiedName(
            project: Project,
            qualifiedName: String,
        ): PsiClass? = JavaPsiFacade
            .getInstance(project)
            .findClass(qualifiedName, GlobalSearchScope.projectScope(project))

        ReadAction
            .nonBlocking(
                Callable {
                    resolveClassByQualifiedName(
                        project,
                        qualifiedName,
                    )
                },
            )
            .finishOnUiThread(
                ModalityState.defaultModalityState(),
            ) { selectedClass ->
                if (selectedClass != null) {
                    onSelected(selectedClass)
                } else {
                    showNoTestClassWarning(project)
                }
            }
            .submit(
                AppExecutorUtil.getAppExecutorService(),
            )
    }
}

private class TestClassListCellRenderer(private val duplicateSimpleNames: Set<String>) : ColoredListCellRenderer<TestClassChoice>() {
    override fun customizeCellRenderer(
        list: JList<out TestClassChoice>,
        value: TestClassChoice?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean,
    ) {
        if (value != null) {
            icon = AllIcons.Nodes.Class
            append(value.className)

            BerryCrushScenarioExecutionSupport.buildChoiceContextLabel(
                packageName = value.packageName,
                moduleName = value.moduleName,
                includeModuleName =
                value.className in
                    duplicateSimpleNames,
            )
                .takeIf {
                    it.isNotEmpty()
                }
                ?.let { label ->
                    append(
                        label,
                        SimpleTextAttributes
                            .GRAYED_ATTRIBUTES,
                    )
                }
        }
    }
}

private fun mapClassCandidates(
    candidates: List<PsiClass>,
    preferredModule: Module?,
): List<ClassCandidate<PsiClass>> = candidates.mapNotNull { candidate ->
    candidate.qualifiedName?.let { qualifiedName ->
        val classModule = ModuleUtilCore.findModuleForPsiElement(candidate)
        ClassCandidate(
            value = candidate,
            qualifiedName = qualifiedName,
            inPreferredModule =
            preferredModule != null &&
                preferredModule == classModule,
        )
    }
}
