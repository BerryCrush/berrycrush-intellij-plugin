package com.berrycrush.intellij.run

import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.junit.JUnitConfiguration
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

/**
 * Shared utilities for scenario execution from gutter and context-menu entry points.
 */
object BerryCrushScenarioExecutionSupport {
    const val DEFAULT_TEST_CLASS_PROPERTY: String = "berryCrush.defaultTestClass"

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

    internal fun <T> selectPreferredCandidate(candidates: List<ClassCandidate<T>>): T? {
        if (candidates.isEmpty()) {
            return null
        }

        return candidates
            .sortedWith(compareByDescending<ClassCandidate<T>> { it.inPreferredModule }.thenBy { it.qualifiedName })
            .first()
            .value
    }

    fun findBerryCrushTestClasses(project: Project): List<PsiClass> {
        val scope = GlobalSearchScope.projectScope(project)
        val psiFacade = JavaPsiFacade.getInstance(project)
        val result = mutableSetOf<PsiClass>()

        berryCrushAnnotations.forEach { annotationFqn ->
            val annotation = psiFacade.findClass(annotationFqn, scope) ?: return@forEach
            AnnotatedElementsSearch.searchPsiClasses(annotation, scope).forEach { psiClass ->
                result.add(psiClass)
            }
        }

        return result.sortedBy { it.qualifiedName ?: it.name ?: "" }
    }

    fun selectPreferredTestClass(
        candidates: List<PsiClass>,
        preferredModule: Module?,
    ): PsiClass? {
        val mappedCandidates =
            candidates
                .mapNotNull { psiClass ->
                    val qualifiedName = psiClass.qualifiedName ?: return@mapNotNull null
                    val classModule = ModuleUtilCore.findModuleForPsiElement(psiClass)
                    ClassCandidate(
                        value = psiClass,
                        qualifiedName = qualifiedName,
                        inPreferredModule = preferredModule != null && preferredModule == classModule,
                    )
                }

        return selectPreferredCandidate(mappedCandidates)
    }

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
        if (chooseWhenMultiple && discovered.size > 1) {
            return null
        }

        val preferredClass = selectPreferredTestClass(discovered, preferredModule)
        if (preferredClass != null) {
            return preferredClass
        }

        if (fallbackClassFqn.isNullOrBlank()) {
            return null
        }

        return resolveFallbackClass(project, fallbackClassFqn)
    }

    fun resolveModuleForClass(
        project: Project,
        testClass: PsiClass,
    ): Module? {
        val virtualFile = testClass.containingFile?.virtualFile ?: return null
        return ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(virtualFile)
    }

    fun configureForClassRun(
        configuration: BerryCrushRunConfiguration,
        testClass: PsiClass,
        module: Module?,
        configName: String,
        vmOptions: String,
    ) {
        configuration.persistentData.TEST_OBJECT = JUnitConfiguration.TEST_CLASS
        configuration.persistentData.MAIN_CLASS_NAME = testClass.qualifiedName
        configuration.vmParameters = vmOptions
        configuration.name = configName
        module?.let { configuration.setModule(it) }
    }

    fun buildVmOptions(
        scenarioFile: String,
        scenarioName: String? = null,
        keywordType: String? = null,
    ): String {
        val options = mutableListOf(buildVmOption("berryCrush.scenarioFile", scenarioFile))

        when (keywordType) {
            "Scenario", "Outline" -> {
                if (!scenarioName.isNullOrBlank()) {
                    options.add(buildVmOption("berryCrush.scenarioName", scenarioName))
                }
            }
            "Feature" -> {
                if (!scenarioName.isNullOrBlank()) {
                    options.add(buildVmOption("berryCrush.featureName", scenarioName))
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
        val configType = ConfigurationTypeUtil.findConfigurationType(BerryCrushConfigurationType::class.java)
        val factory = configType.configurationFactories[0]

        val runManager = RunManager.getInstance(project)
        var settings = runManager.findConfigurationByName(configName)

        if (settings == null) {
            settings = runManager.createConfiguration(configName, factory)
            runManager.addConfiguration(settings)
        }

        val configuration = settings.configuration as? BerryCrushRunConfiguration ?: return null
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
                .findClass(fallbackClassFqn, GlobalSearchScope.projectScope(project))
                ?: return null

        return fallbackClass.takeIf { isBerryCrushTestClass(it) }
    }

    private fun isBerryCrushTestClass(psiClass: PsiClass): Boolean {
        return psiClass.annotations.any { annotation ->
            val qualifiedName = annotation.qualifiedName ?: return@any false
            berryCrushAnnotations.any { expected ->
                qualifiedName == expected || qualifiedName.endsWith(".$expected")
            }
        }
    }

    fun sortCandidates(
        candidates: List<PsiClass>,
        preferredModule: Module?,
    ): List<PsiClass> {
        val sortedCandidates =
            candidates
                .mapNotNull { candidate ->
                    val qualifiedName = candidate.qualifiedName ?: return@mapNotNull null
                    val classModule = ModuleUtilCore.findModuleForPsiElement(candidate)
                    ClassCandidate(
                        value = candidate,
                        qualifiedName = qualifiedName,
                        inPreferredModule = preferredModule != null && preferredModule == classModule,
                    )
                }.sortedWith(compareByDescending<ClassCandidate<PsiClass>> { it.inPreferredModule }.thenBy { it.qualifiedName })
                .map { it.value }

        return sortedCandidates
    }

    fun showTestClassChooser(
        project: Project,
        candidates: List<PsiClass>,
        preferredModule: Module?,
        onSelected: (PsiClass) -> Unit,
    ) {
        val sortedCandidates = sortCandidates(candidates, preferredModule)
        if (sortedCandidates.isEmpty()) {
            showNoTestClassWarning(project)
            return
        }

        if (sortedCandidates.size == 1) {
            onSelected(sortedCandidates[0])
            return
        }

        val popup =
            JBPopupFactory
                .getInstance()
                .createPopupChooserBuilder(sortedCandidates)
                .setTitle("Select Test Class")
                .setItemChosenCallback { selectedClass ->
                    onSelected(selectedClass)
                }.setRenderer(
                    object : ColoredListCellRenderer<PsiClass>() {
                        override fun customizeCellRenderer(
                            list: javax.swing.JList<out PsiClass>,
                            value: PsiClass?,
                            index: Int,
                            selected: Boolean,
                            hasFocus: Boolean,
                        ) {
                            if (value != null) {
                                icon = com.intellij.icons.AllIcons.Nodes.Class
                                append(value.name ?: "Unknown")
                                value.qualifiedName?.let { qualifiedName ->
                                    val packageName = qualifiedName.substringBeforeLast('.', "")
                                    if (packageName.isNotEmpty()) {
                                        append(" ($packageName)", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                                    }
                                }
                            }
                        }
                    },
                ).createPopup()

        popup.showInFocusCenter()
    }

    fun chooseTestClass(
        project: Project,
        candidates: List<PsiClass>,
        preferredModule: Module?,
    ): PsiClass? {
        val sortedCandidates = sortCandidates(candidates, preferredModule)
        if (sortedCandidates.isEmpty()) {
            return null
        }

        val selectedIndex =
            Messages.showDialog(
                project,
                "Multiple BerryCrush test classes were found. Select one to run this scenario file.",
                "Select BerryCrush Test Class",
                sortedCandidates.map { it.qualifiedName ?: it.name ?: "Unknown" }.toTypedArray(),
                0,
                null,
            )

        if (selectedIndex < 0 || selectedIndex >= sortedCandidates.size) {
            return null
        }

        return sortedCandidates[selectedIndex]
    }
}
