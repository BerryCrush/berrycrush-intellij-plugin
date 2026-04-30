/**
 * Line marker provider for Java and Kotlin files showing usages of @Step and @Assertion annotated methods
 * in scenario and fragment files.
 */
package com.berrycrush.intellij.navigation

import com.berrycrush.intellij.index.StepUsageIndex
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.impl.source.tree.LeafPsiElement

/**
 * Provides gutter icons for @Step and @Assertion annotated methods in Java and Kotlin files,
 * showing navigation to usages in scenario/fragment files.
 */
class StepAnnotationLineMarkerProvider : LineMarkerProvider {

    companion object {
        private const val STEP_ANNOTATION_FQN = "org.berrycrush.step.Step"
        private const val ASSERTION_ANNOTATION_FQN = "org.berrycrush.assertion.Assertion"
    }

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        // For Kotlin files, add debug for elements with KtNamedFunction parent
        val file = element.containingFile
        val isKotlin = file?.name?.endsWith(".kt") == true
        
        if (isKotlin) {
            var current: PsiElement? = element.parent
            var depth = 0
            while (current != null && depth < 3) {
                val className = current.javaClass.name
                if (className.contains("KtNamedFunction")) {
                    // Found KtNamedFunction parent
                    try {
                        val nameIdMethod = current.javaClass.getMethod("getNameIdentifier")
                        val nameIdentifier = nameIdMethod.invoke(current) as? PsiElement
                        val isMatch = (nameIdentifier == element)
                        
                        if (isMatch || isAncestorOf(element, nameIdentifier)) {
                            // This is the method name - check for annotations directly
                            val ktAnnotations = getKotlinAnnotations(current)
                            
                            // Check for @Step or @Assertion
                            val hasStep = ktAnnotations.any { it.contains("Step") }
                            val hasAssertion = ktAnnotations.any { it.contains("Assertion") }
                            
                            // Get pattern if @Step or @Assertion
                            val pattern = getKotlinAnnotationPattern(current, if (hasStep) "Step" else if (hasAssertion) "Assertion" else null)
                            
                            // @Step methods use step keywords (Given/When/Then/And/But)
                            if (hasStep && pattern != null) {
                                val usages = StepUsageIndex.findStepUsagesAllScope(element.project, pattern)
                                if (usages.isNotEmpty()) {
                                    return LineMarkerInfo(
                                        element,
                                        element.textRange,
                                        com.intellij.icons.AllIcons.Gutter.ImplementingMethod,
                                        { "Found ${usages.size} usage(s) in scenario files" },
                                        { _, _ ->
                                            if (usages.size == 1) {
                                                usages.first().navigate(true)
                                            } else {
                                                showUsagesPopup(element, usages)
                                            }
                                        },
                                        GutterIconRenderer.Alignment.LEFT,
                                        { "Step usages" }
                                    )
                                }
                            }
                            
                            // @Assertion methods use assert directive (assert pet name is "Fluffy")
                            if (hasAssertion && pattern != null) {
                                // @Assertion methods are invoked via assert directive
                                val usages = StepUsageIndex.findAssertionUsagesAllScope(element.project, pattern)
                                if (usages.isNotEmpty()) {
                                    return LineMarkerInfo(
                                        element,
                                        element.textRange,
                                        AllIcons.Gutter.ImplementingMethod,
                                        { "Found ${usages.size} usage(s) in scenario files" },
                                        { _, _ ->
                                            if (usages.size == 1) {
                                                usages.first().navigate(true)
                                            } else {
                                                showUsagesPopup(element, usages)
                                            }
                                        },
                                        GutterIconRenderer.Alignment.LEFT,
                                        { "Assertion usages" }
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Silently ignore errors
                    }
                }
                current = current.parent
                depth++
            }
        }
        
        // For Java files, use the original approach
        val method = findContainingMethod(element)
        
        if (method == null) return null

        // Check for @Step annotation
        val stepAnnotation = findAnnotation(method, STEP_ANNOTATION_FQN)
        if (stepAnnotation != null) {
            val pattern = getPatternFromAnnotation(stepAnnotation)
            if (pattern != null) {
                val usages = StepUsageIndex.findStepUsagesAllScope(element.project, pattern)
                if (usages.isNotEmpty()) {
                    return LineMarkerInfo(
                        element,
                        element.textRange,
                        AllIcons.Gutter.ImplementingMethod,
                        { "Found ${usages.size} usage(s) in scenario files" },
                        { _, _ ->
                            if (usages.size == 1) {
                                usages.first().navigate(true)
                            } else {
                                showUsagesPopup(element, usages)
                            }
                        },
                        GutterIconRenderer.Alignment.LEFT,
                        { "Step usages" }
                    )
                }
            }
        }

        // Check for @Assertion annotation
        val assertionAnnotation = findAnnotation(method, ASSERTION_ANNOTATION_FQN)
        if (assertionAnnotation != null) {
            val pattern = getPatternFromAnnotation(assertionAnnotation)
            if (pattern != null) {
                val usages = StepUsageIndex.findAssertionUsagesAllScope(element.project, pattern)
                if (usages.isNotEmpty()) {
                    return LineMarkerInfo(
                        element,
                        element.textRange,
                        AllIcons.Gutter.ImplementingMethod,
                        { "Found ${usages.size} usage(s) in scenario files" },
                        { _, _ ->
                            if (usages.size == 1) {
                                usages.first().navigate(true)
                            } else {
                                showUsagesPopup(element, usages)
                            }
                        },
                        GutterIconRenderer.Alignment.LEFT,
                        { "Assertion usages" }
                    )
                }
            }
        }

        return null
    }

    /**
     * Find containing method for both Java (PsiIdentifier) and Kotlin
     * Returns the method only if this element is the method name identifier
     */
    private fun findContainingMethod(element: PsiElement): PsiMethod? {
        // For Java: PsiIdentifier whose parent is PsiMethod
        if (element is PsiIdentifier) {
            val parent = element.parent
            if (parent is PsiMethod && parent.nameIdentifier == element) {
                return parent
            }
        }
        
        // For Kotlin: The element is a LeafPsiElement, we need to check if it's a function name
        // by walking up the parent chain to find a KtNamedFunction
        val file = element.containingFile
        val isKotlin = file?.name?.endsWith(".kt") == true
        
        if (isKotlin) {
            // Check if this element is the identifier of a named function
            var current: PsiElement? = element.parent
            var maxDepth = 5 // Don't walk too far
            
            while (current != null && maxDepth > 0) {
                // Check if current element is a KtNamedFunction (via class name)
                val className = current.javaClass.name
                if (className.contains("KtNamedFunction") || className.contains("KtFunction")) {
                    // Check if the element is the name identifier of this function
                    try {
                        val nameIdMethod = current.javaClass.getMethod("getNameIdentifier")
                        val nameIdentifier = nameIdMethod.invoke(current) as? PsiElement
                        if (nameIdentifier == element || isAncestorOf(element, nameIdentifier)) {
                            // Now find the light method for this function
                            val lightMethod = findLightMethod(current)
                            if (lightMethod != null) {
                                return lightMethod
                            }
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                current = current.parent
                maxDepth--
            }
        }
        
        return null
    }
    
    /**
     * Check if element is the nameIdentifier or a child of it
     */
    private fun isAncestorOf(element: PsiElement, potentialAncestor: PsiElement?): Boolean {
        if (potentialAncestor == null) return false
        var current: PsiElement? = element
        while (current != null) {
            if (current == potentialAncestor) return true
            current = current.parent
        }
        return false
    }
    
    /**
     * Get annotation names from a Kotlin function via reflection
     */
    private fun getKotlinAnnotations(ktFunction: PsiElement): List<String> {
        val result = mutableListOf<String>()
        try {
            // Try getAnnotationEntries
            val annotationsMethod = ktFunction.javaClass.methods.find { 
                it.name == "getAnnotationEntries" && it.parameterCount == 0
            }
            if (annotationsMethod != null) {
                annotationsMethod.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val annotations = annotationsMethod.invoke(ktFunction) as? List<*>
                annotations?.forEach { ann ->
                    if (ann != null) {
                        try {
                            // Get the short name
                            val shortNameMethod = ann.javaClass.methods.find { it.name == "getShortName" }
                            if (shortNameMethod != null) {
                                val shortName = shortNameMethod.invoke(ann)
                                val nameMethod = shortName?.javaClass?.methods?.find { it.name == "asString" || it.name == "getIdentifier" }
                                if (nameMethod != null) {
                                    val name = nameMethod.invoke(shortName)?.toString()
                                    if (name != null) {
                                        result.add(name)
                                    }
                                } else {
                                    result.add(shortName.toString())
                                }
                            }
                            // Also try to get the text directly
                            val textMethod = ann.javaClass.methods.find { it.name == "getText" }
                            if (textMethod != null && result.isEmpty()) {
                                val text = textMethod.invoke(ann)?.toString()
                                if (text != null) {
                                    result.add(text)
                                }
                            }
                        } catch (e: Exception) {
                            // Try getText as fallback
                            try {
                                val text = ann.toString()
                                result.add(text)
                            } catch (e2: Exception) {
                                // ignore
                            }
                        }
                    }
                }
            }
            
            // Fallback: try getModifierList and getAnnotations
            if (result.isEmpty()) {
                val modListMethod = ktFunction.javaClass.methods.find { 
                    it.name == "getModifierList" && it.parameterCount == 0
                }
                if (modListMethod != null) {
                    modListMethod.isAccessible = true
                    val modList = modListMethod.invoke(ktFunction)
                    if (modList != null) {
                        val annotationsMethod2 = modList.javaClass.methods.find { it.name == "getAnnotationEntries" }
                        if (annotationsMethod2 != null) {
                            @Suppress("UNCHECKED_CAST")
                            val annotations2 = annotationsMethod2.invoke(modList) as? List<*>
                            annotations2?.forEach { ann ->
                                if (ann != null) {
                                    result.add(ann.toString())
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return result
    }
    
    /**
     * Get pattern value from Kotlin @Step or @Assertion annotation
     */
    private fun getKotlinAnnotationPattern(ktFunction: PsiElement, annotationName: String?): String? {
        if (annotationName == null) return null
        try {
            val annotationsMethod = ktFunction.javaClass.methods.find { 
                it.name == "getAnnotationEntries" && it.parameterCount == 0
            }
            if (annotationsMethod != null) {
                annotationsMethod.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val annotations = annotationsMethod.invoke(ktFunction) as? List<*>
                annotations?.forEach { ann ->
                    if (ann != null) {
                        // Get the annotation text directly via getText()
                        val text = try {
                            val getTextMethod = ann.javaClass.getMethod("getText")
                            getTextMethod.invoke(ann) as? String
                        } catch (e: Exception) {
                            ann.toString()
                        } ?: return@forEach
                        
                        // Check if this is the annotation we want
                        if (text.contains("@$annotationName") || text.contains("@${annotationName}(")) {
                            // Parse the pattern from the annotation text
                            // Handle: @Step("pattern"), @Step(pattern = "pattern"), @Step(value = "pattern")
                            
                            // Try named parameter: pattern = "..." or value = "..."
                            val namedMatch = Regex("""(?:pattern|value)\s*=\s*"([^"]+)"""").find(text)
                            if (namedMatch != null) {
                                return namedMatch.groupValues[1]
                            }
                            
                            // Try positional argument: @Step("...")
                            val positionalMatch = Regex("""@\w+\s*\(\s*"([^"]+)"""").find(text)
                            if (positionalMatch != null) {
                                return positionalMatch.groupValues[1]
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return null
    }

    /**
     * Find light PsiMethod for a Kotlin function using reflection
     */
    private fun findLightMethod(ktFunction: PsiElement): PsiMethod? {
        // Check if the function element itself implements PsiMethod
        if (ktFunction is PsiMethod) {
            return ktFunction
        }
        
        // Try direct toLightMethods call
        try {
            val toLightMethodsMethod = ktFunction.javaClass.methods.find { 
                it.name == "toLightMethods" && it.parameterCount == 0
            }
            if (toLightMethodsMethod != null) {
                toLightMethodsMethod.isAccessible = true
                @Suppress("UNCHECKED_CAST")
                val methods = toLightMethodsMethod.invoke(ktFunction) as? List<PsiMethod>
                if (!methods.isNullOrEmpty()) {
                    return methods.first()
                }
            }
        } catch (e: Exception) {
            // Continue trying
        }
        
        // Try via getContainingClass
        try {
            val containingClassMethod = ktFunction.javaClass.methods.find { 
                it.name == "getContainingClass" && it.parameterCount == 0
            }
            if (containingClassMethod != null) {
                containingClassMethod.isAccessible = true
                val containingClass = containingClassMethod.invoke(ktFunction) as? com.intellij.psi.PsiClass
                if (containingClass != null) {
                    val funcNameMethod = ktFunction.javaClass.getMethod("getName")
                    val funcName = funcNameMethod.invoke(ktFunction) as? String
                    if (funcName != null) {
                        return containingClass.findMethodsByName(funcName, false).firstOrNull()
                    }
                }
            }
        } catch (e: Exception) {
            // Continue trying
        }
        
        // Try via getContainingClassOrObject and toLightClass
        try {
            val containingClassMethod = ktFunction.javaClass.methods.find { 
                it.name == "getContainingClassOrObject" && it.parameterCount == 0
            }
            if (containingClassMethod != null) {
                containingClassMethod.isAccessible = true
                val containingClass = containingClassMethod.invoke(ktFunction)
                if (containingClass != null) {
                    val toLightClassMethod = containingClass.javaClass.methods.find { 
                        it.name == "toLightClass" && it.parameterCount == 0
                    }
                    if (toLightClassMethod != null) {
                        toLightClassMethod.isAccessible = true
                        val lightClass = toLightClassMethod.invoke(containingClass) as? com.intellij.psi.PsiClass
                        if (lightClass != null) {
                            val funcNameMethod = ktFunction.javaClass.getMethod("getName")
                            val funcName = funcNameMethod.invoke(ktFunction) as? String
                            if (funcName != null) {
                                return lightClass.findMethodsByName(funcName, false).firstOrNull()
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Continue trying
        }
        
        return null
    }

    /**
     * Find an annotation by FQN on a method
     */
    private fun findAnnotation(method: PsiMethod, annotationFqn: String): PsiAnnotation? {
        return method.annotations.find { annotation ->
            annotation.qualifiedName == annotationFqn ||
            annotation.qualifiedName?.endsWith(".${annotationFqn.substringAfterLast('.')}") == true
        }
    }

    /**
     * Get the pattern value from a @Step or @Assertion annotation
     */
    private fun getPatternFromAnnotation(annotation: PsiAnnotation): String? {
        // Try "pattern" attribute first
        val patternAttr = annotation.findAttributeValue("pattern")
        if (patternAttr != null) {
            val text = patternAttr.text
            // Remove quotes
            if (text.startsWith("\"") && text.endsWith("\"")) {
                return text.substring(1, text.length - 1)
            }
            return text
        }

        // Try "value" attribute (default attribute)
        val valueAttr = annotation.findAttributeValue("value")
        if (valueAttr != null) {
            val text = valueAttr.text
            if (text.startsWith("\"") && text.endsWith("\"")) {
                return text.substring(1, text.length - 1)
            }
            return text
        }

        return null
    }

    /**
     * Show a popup for multiple usages
     */
    private fun showUsagesPopup(element: PsiElement, usages: List<PsiElement>) {
        val project = element.project

        // Create popup with all usages
        val popup = com.intellij.openapi.ui.popup.JBPopupFactory.getInstance()
            .createPopupChooserBuilder(usages.map { usage ->
                val file = usage.containingFile
                val lineNumber = getLineNumber(usage)
                "${file.name}:$lineNumber"
            })
            .setTitle("Usages in Scenario Files")
            .setItemChosenCallback { selected ->
                val index = usages.indices.find { i ->
                    val usage = usages[i]
                    val file = usage.containingFile
                    val lineNumber = getLineNumber(usage)
                    "${file.name}:$lineNumber" == selected
                }
                if (index != null) {
                    usages[index].navigate(true)
                }
            }
            .createPopup()

        val editor = com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            popup.showInBestPositionFor(editor)
        }
    }

    /**
     * Get line number for a PSI element
     */
    private fun getLineNumber(element: PsiElement): Int {
        val document = com.intellij.psi.PsiDocumentManager.getInstance(element.project)
            .getDocument(element.containingFile)
        return if (document != null) {
            document.getLineNumber(element.textOffset) + 1
        } else {
            0
        }
    }

    // Navigate extension for PsiElement
    private fun PsiElement.navigate(requestFocus: Boolean) {
        val navigatable = this as? com.intellij.pom.Navigatable
        if (navigatable != null && navigatable.canNavigate()) {
            navigatable.navigate(requestFocus)
        }
    }
}
