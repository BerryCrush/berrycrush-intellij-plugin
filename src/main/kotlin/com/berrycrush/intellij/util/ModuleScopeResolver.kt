package com.berrycrush.intellij.util

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope

/**
 * Utility for resolving module-scoped search scopes.
 *
 * Provides methods to find the appropriate search scope based on module dependencies,
 * enabling more accurate navigation in multi-module projects.
 */
object ModuleScopeResolver {

    /**
     * Finds the module containing the given PSI element.
     *
     * @param element The PSI element to find the module for
     * @return The module containing the element, or null if not found
     */
    fun findModuleForElement(element: PsiElement): Module? {
        val virtualFile = element.containingFile?.virtualFile ?: return null
        return ModuleUtil.findModuleForFile(virtualFile, element.project)
    }

    /**
     * Finds the module containing the given virtual file.
     *
     * @param file The virtual file to find the module for
     * @param project The project context
     * @return The module containing the file, or null if not found
     */
    fun findModuleForFile(file: VirtualFile, project: Project): Module? {
        return ModuleUtil.findModuleForFile(file, project)
    }

    /**
     * Gets the search scope for forward navigation (scenario → @Step method).
     *
     * This scope includes the module's own sources plus all compile-time dependencies.
     * Use this when searching for @Step/@Assertion implementations from a scenario file.
     *
     * When module is detected: searches module + dependencies (includes test if scenario is in test)
     * When module is not detected (fallback): searches project scope
     *
     * @param element The PSI element (e.g., step text in scenario file)
     * @return A search scope limited to the module's dependencies, or project scope as fallback
     */
    fun getModuleDependencyScope(element: PsiElement): GlobalSearchScope {
        val module = findModuleForElement(element)
        return getModuleDependencyScope(module, element.project)
    }

    /**
     * Gets the search scope for forward navigation (scenario → @Step method).
     *
     * @param module The module to get dependency scope for
     * @param project The project context
     * @return A search scope limited to the module's dependencies, or project scope as fallback
     */
    fun getModuleDependencyScope(module: Module?, project: Project): GlobalSearchScope {
        if (module == null) {
            // Fallback: search project sources only (excludes external libraries)
            // This is a reasonable fallback as @Step methods are usually in the project
            return GlobalSearchScope.projectScope(project)
        }

        // Get scope including the module and all its dependencies
        return module.getModuleWithDependenciesScope()
    }

    /**
     * Gets the search scope for reverse navigation (@Step method → scenario usages).
     *
     * This scope includes modules that depend on the given module.
     * Use this when searching for usages of a @Step/@Assertion method in scenario files.
     *
     * When module is detected: searches dependent modules
     * When module is not detected (fallback): searches project scope
     *
     * @param element The PSI element (e.g., method name in @Step method)
     * @return A search scope limited to dependent modules, or project scope as fallback
     */
    fun getDependentModulesScope(element: PsiElement): GlobalSearchScope {
        val module = findModuleForElement(element)
        return getDependentModulesScope(module, element.project)
    }

    /**
     * Gets the search scope for reverse navigation (@Step method → scenario usages).
     *
     * @param module The module to find dependents for
     * @param project The project context
     * @return A search scope limited to dependent modules, or project scope as fallback
     */
    fun getDependentModulesScope(module: Module?, project: Project): GlobalSearchScope {
        if (module == null) {
            // Fallback: search project sources only (excludes external libraries)
            // This is a reasonable fallback as scenarios are usually in the project
            return GlobalSearchScope.projectScope(project)
        }

        // Get scope including modules that depend on this module
        return module.getModuleWithDependentsScope()
    }

    /**
     * Checks if a module can access another module (is it in the dependency chain).
     *
     * @param sourceModule The module that wants to access
     * @param targetModule The module being accessed
     * @return true if sourceModule has targetModule in its dependency chain
     */
    fun canAccess(sourceModule: Module?, targetModule: Module?): Boolean {
        if (sourceModule == null || targetModule == null) {
            return true // Fallback: allow access
        }

        if (sourceModule == targetModule) {
            return true
        }

        // Check if targetModule is in sourceModule's dependencies
        val dependencyScope = sourceModule.getModuleWithDependenciesScope()
        val targetScope = GlobalSearchScope.moduleScope(targetModule)

        // If target module's scope intersects with source's dependency scope, they're connected
        return !GlobalSearchScope.EMPTY_SCOPE.equals(
            dependencyScope.intersectWith(targetScope)
        )
    }

    /**
     * Creates a scope that includes both scenario files (.scenario, .fragment) in dependent
     * modules and limits to BerryCrush file types.
     *
     * @param module The module containing the @Step/@Assertion method
     * @param project The project context
     * @return A filtered scope for BerryCrush files in dependent modules
     */
    fun getBerryCrushFilesInDependentModules(module: Module?, project: Project): GlobalSearchScope {
        val baseScope = getDependentModulesScope(module, project)
        // The file type filtering is handled by the index, so we just return the module scope
        return baseScope
    }
}
