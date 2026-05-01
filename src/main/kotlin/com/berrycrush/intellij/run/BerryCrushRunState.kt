package com.berrycrush.intellij.run

import com.intellij.execution.ExecutionException
import com.intellij.execution.configurations.CommandLineState
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessTerminatedListener
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.guessProjectDir
import java.io.File

/**
 * Execution state for BerryCrush run configurations.
 *
 * This state generates a temporary test wrapper class that references the
 * selected scenario file, then runs Gradle to execute that test.
 *
 * ## Execution Flow
 *
 * 1. Generate temporary test class in `build/generated-berrycrush-tests/`
 * 2. Run `./gradlew test --tests "berrycrush.generated.BerryCrushWrapper_*"`
 * 3. Clean up generated files after test completes
 */
class BerryCrushRunState(
    private val configuration: BerryCrushRunConfiguration,
    environment: ExecutionEnvironment,
) : CommandLineState(environment) {

    private var generatedTestInfo: GeneratedTestInfo? = null

    override fun startProcess(): ProcessHandler {
        val project = environment.project
        val projectDir = project.guessProjectDir()
            ?: throw ExecutionException("Cannot determine project directory")

        val workingDir = findModuleOrProjectDir(projectDir.path)

        // Generate temporary test wrapper
        generatedTestInfo = TemporaryTestGenerator.generateWrapper(
            project = project,
            scenarioFilePath = configuration.scenarioFilePath,
            moduleDir = workingDir,
        ) ?: throw ExecutionException("Failed to generate test wrapper for: ${configuration.scenarioFilePath}")

        val commandLine = createCommandLine(workingDir, generatedTestInfo!!)
        val processHandler = KillableColoredProcessHandler(commandLine)

        // Add cleanup listener
        processHandler.addProcessListener(object : ProcessListener {
            override fun processTerminated(event: ProcessEvent) {
                generatedTestInfo?.let { info ->
                    TemporaryTestGenerator.cleanup(info)
                }
            }
        })

        ProcessTerminatedListener.attach(processHandler)
        return processHandler
    }

    private fun createCommandLine(workingDir: File, testInfo: GeneratedTestInfo): GeneralCommandLine {
        val gradleWrapper = findGradleWrapper(workingDir)
            ?: throw ExecutionException("Gradle wrapper not found. Please ensure gradlew exists in: $workingDir")

        return GeneralCommandLine().apply {
            exePath = gradleWrapper.absolutePath
            setWorkDirectory(workingDir)

            // Add test task
            addParameter("test")

            // Run only the generated test class
            addParameter("--tests")
            addParameter(testInfo.className)

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
