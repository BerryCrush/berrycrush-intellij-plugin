package com.berrycrush.intellij.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.guessProjectDir
import java.io.File

/**
 * Execution state for BerryCrush run configurations.
 *
 * This state runs Gradle with BerryCrush filter system properties to
 * execute specific scenarios, features, or scenario files.
 *
 * ## Filtering Mechanism
 *
 * Uses system properties recognized by BerryCrush JUnit engine:
 * - `berryCrush.scenarioFile` - Filter by scenario file path
 * - `berryCrush.scenarioName` - Filter by scenario name
 * - `berryCrush.featureName` - Filter by feature name
 *
 * ## Execution Flow
 *
 * 1. Build Gradle command with appropriate system properties
 * 2. Run `./gradlew test -DberryCrush.scenarioFile=...`
 * 3. BerryCrush engine filters tests based on properties
 */
class BerryCrushRunState(
    private val configuration: BerryCrushRunConfiguration,
    environment: ExecutionEnvironment,
) : CommandLineState(environment) {

    override fun startProcess(): ProcessHandler {
        val project = environment.project
        val projectDir = project.guessProjectDir()
            ?: throw ExecutionException("Cannot determine project directory")

        val workingDir = findModuleOrProjectDir(projectDir.path)
        val commandLine = createCommandLine(workingDir)
        val processHandler = KillableColoredProcessHandler(commandLine)

        ProcessTerminatedListener.attach(processHandler)
        return processHandler
    }

    private fun createCommandLine(workingDir: File): GeneralCommandLine {
        val gradleWrapper = findGradleWrapper(workingDir)
            ?: throw ExecutionException("Gradle wrapper not found. Please ensure gradlew exists in: $workingDir")

        return GeneralCommandLine().apply {
            exePath = gradleWrapper.absolutePath
            setWorkDirectory(workingDir)

            // Add test task
            addParameter("test")

            // Add BerryCrush filter properties
            configuration.scenarioFilePath.takeIf { it.isNotBlank() }?.let { path ->
                // Extract just the filename for filtering
                val filename = File(path).name
                addParameter("-DberryCrush.scenarioFile=$filename")
            }

            configuration.scenarioName?.takeIf { it.isNotBlank() }?.let { name ->
                addParameter("-DberryCrush.scenarioName=$name")
            }

            configuration.featureName?.takeIf { it.isNotBlank() }?.let { name ->
                addParameter("-DberryCrush.featureName=$name")
            }

            // Enable console output
            addParameter("--console=plain")

            // Show test output
            addParameter("--info")

            // Rerun tests even if up-to-date
            addParameter("--rerun-tasks")
        }
    }

    /**
     * Find the module directory containing the scenario file, or fall back to project root.
     */
    private fun findModuleOrProjectDir(projectPath: String): File {
        val scenarioFile = File(configuration.scenarioFilePath)
        if (!scenarioFile.exists()) {
            return File(projectPath)
        }

        // Walk up from scenario file looking for build.gradle.kts or build.gradle
        var dir = scenarioFile.parentFile
        while (dir != null) {
            if (File(dir, "build.gradle.kts").exists() || File(dir, "build.gradle").exists()) {
                return dir
            }
            // Stop at project root
            if (dir.absolutePath == projectPath) {
                break
            }
            dir = dir.parentFile
        }

        return File(projectPath)
    }

    /**
     * Find Gradle wrapper script in the given directory or parent directories.
     */
    private fun findGradleWrapper(startDir: File): File? {
        val wrapperName = if (System.getProperty("os.name").lowercase().contains("win")) {
            "gradlew.bat"
        } else {
            "gradlew"
        }

        var dir: File? = startDir
        while (dir != null) {
            val wrapper = File(dir, wrapperName)
            if (wrapper.exists() && wrapper.canExecute()) {
                return wrapper
            }
            dir = dir.parentFile
        }
        return null
    }
}
