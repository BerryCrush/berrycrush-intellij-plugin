package com.berrycrush.intellij.run

import com.intellij.execution.Executor
import com.intellij.execution.junit.JUnitConfiguration
import com.intellij.execution.junit2.ui.properties.JUnitConsoleProperties
import com.intellij.execution.testframework.JavaTestLocator
import com.intellij.execution.testframework.sm.FileUrlProvider
import com.intellij.execution.testframework.sm.runner.SMTestLocator
import com.intellij.execution.Location
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope

/**
 * Console properties for BerryCrush tests.
 *
 * Extends JUnitConsoleProperties to add support for file:// URL navigation.
 * BerryCrush tests report their source location using JUnit Platform's FileSource,
 * which generates URLs like `file:///path/to/scenario.scenario:line:column`.
 *
 * The default JUnit console uses JavaTestLocator which only handles java:// URLs.
 * This class uses a combined locator that handles both java:// and file:// URLs,
 * enabling double-click navigation to scenario files in the test tree.
 */
class BerryCrushConsoleProperties(
    config: JUnitConfiguration,
    executor: Executor
) : JUnitConsoleProperties(config, executor) {

    /**
     * Returns a combined test locator that supports both java:// and file:// URLs.
     *
     * - java:// URLs (from standard JUnit tests) → delegated to JavaTestLocator
     * - file:// URLs (from BerryCrush scenario tests) → handled by FileUrlProvider
     */
    override fun getTestLocator(): SMTestLocator {
        return BerryCrushTestLocator
    }
}

/**
 * Combined test locator that handles both java:// and file:// URLs.
 *
 * This is equivalent to SMTestRunnerConnectionUtil.CombinedTestLocator but implemented
 * explicitly to avoid internal API dependencies.
 */
private object BerryCrushTestLocator : SMTestLocator, DumbAware {

    override fun getLocation(
        protocol: String,
        path: String,
        project: Project,
        scope: GlobalSearchScope
    ): List<Location<*>> {
        return getLocation(protocol, path, null, project, scope)
    }

    override fun getLocation(
        protocol: String,
        path: String,
        metainfo: String?,
        project: Project,
        scope: GlobalSearchScope
    ): List<Location<*>> {
        // Handle file:// URLs (BerryCrush scenario locations)
        if (protocol == "file") {
            return FileUrlProvider.INSTANCE.getLocation(protocol, path, project, scope)
        }

        // Handle java:// URLs (standard JUnit class/method locations)
        // Only use JavaTestLocator when indexing is available
        if (!DumbService.getInstance(project).isUsableInCurrentContext(JavaTestLocator.INSTANCE)) {
            return emptyList()
        }
        return JavaTestLocator.INSTANCE.getLocation(protocol, path, project, scope)
    }

    override fun getLocation(
        stacktraceLine: String,
        project: Project,
        scope: GlobalSearchScope
    ): List<Location<*>> {
        // Delegate stacktrace parsing to FileUrlProvider (handles file:line format)
        return FileUrlProvider.INSTANCE.getLocation(stacktraceLine, project, scope)
    }

    override fun getLocationCacheModificationTracker(project: Project): ModificationTracker {
        return JavaTestLocator.INSTANCE.getLocationCacheModificationTracker(project)
    }
}
