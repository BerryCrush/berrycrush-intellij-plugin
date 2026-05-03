---
name: kotlin
description: Kotlin best practices for IntelliJ plugin development. Use this when writing or refactoring Kotlin code in the IntelliJ plugin.
argument-hint: Kotlin patterns and IntelliJ SDK conventions
user-invocable: true
---

# Kotlin Best Practices for IntelliJ Plugin Development

This skill provides guidance for writing clean, idiomatic Kotlin code in the IntelliJ BerryCrush plugin.

## Technology Stack

| Technology | Version | Notes |
|------------|---------|-------|
| Kotlin | 2.3.20 | Primary language |
| Java | 21 | Target JVM |
| IntelliJ Platform | 2025.3+ | Plugin SDK |
| JUnit 5 | | Plugin testing |

## 1. Kotlin Fundamentals

### Expression Functions
Use expression functions for single-return statements:

```kotlin
// AVOID: Block with single return
fun getName(): String {
    return "name"
}

// GOOD: Expression function
fun getName(): String = "name"

// GOOD: Multi-line expression
fun extractName(element: PsiElement): String =
    element.text.trim().takeWhile { it != '\n' }
```

### Immutability
Prefer immutable data structures:

```kotlin
// GOOD: Use val for read-only properties
val elements = listOf(element1, element2)

// AVOID: Mutable when not needed
var elements = mutableListOf<PsiElement>() // Only if mutation is required
```

### Data Classes
Use data classes for value objects:

```kotlin
data class StepInfo(
    val keyword: String,
    val description: String,
    val offset: Int,
)
```

### Null Safety with Safe Calls
Prefer `?.let` over early returns when possible:

```kotlin
// AVOID: Multiple returns with null check
fun processElement(element: PsiElement?): String? {
    val e = element ?: return null
    return extractName(e)
}

// GOOD: Use ?.let for cleaner flow
fun processElement(element: PsiElement?): String? =
    element?.let { extractName(it) }

// GOOD: Chain multiple safe calls
fun getScenarioName(element: PsiElement?): String? =
    element
        ?.let { PsiTreeUtil.getParentOfType(it, BerryCrushScenarioElement::class.java) }
        ?.scenarioName
```

Use early returns only when there's significant processing after the null check:

```kotlin
// OK: Early return when significant logic follows
fun processComplexElement(element: PsiElement?) {
    val e = element ?: return
    // Many lines of processing...
    val type = determineType(e)
    val children = collectChildren(e)
    processResults(type, children)
}
```

### Extension Functions
Use extension functions for common operations:

```kotlin
// GOOD: Extension for PSI utilities
fun PsiElement.isBlockElement(): Boolean =
    this is BerryCrushScenarioElement ||
    this is BerryCrushFragmentElement ||
    this is BerryCrushFeatureElement

fun PsiElement.findParentOfType(type: Class<out PsiElement>): PsiElement? =
    PsiTreeUtil.getParentOfType(this, type)
```

### Scope Functions
Use appropriate scope functions:

```kotlin
// let - Transform nullable
val result = element?.let { transform(it) }

// also - Side effects while keeping subject
return descriptor.also { logger.debug("Created descriptor: $it") }

// apply - Configure an object
return FoldingDescriptor(node, range).apply {
    // Configure if needed
}

// run - Execute with receiver
return element.run {
    textRange.let { FoldingDescriptor(node, it) }
}
```

## 2. IntelliJ SDK Patterns

### PSI Element Access
Use `PsiTreeUtil` for safe PSI navigation:

```kotlin
// GOOD: Find children of specific type
val steps = PsiTreeUtil.findChildrenOfType(file, BerryCrushStepElement::class.java)

// GOOD: Find parent of specific type
val scenario = PsiTreeUtil.getParentOfType(element, BerryCrushScenarioElement::class.java)

// GOOD: Get next/previous sibling of type
val nextStep = PsiTreeUtil.getNextSiblingOfType(element, BerryCrushStepElement::class.java)
```

### AST Node Access
Access node properties safely:

```kotlin
// GOOD: Safe access to element type
val elementType = element.node?.elementType ?: return null

// GOOD: Check multiple types
val isBlock = elementType in setOf(
    BerryCrushElementTypes.SCENARIO,
    BerryCrushElementTypes.FRAGMENT,
    BerryCrushElementTypes.FEATURE,
)
```

### Thread Safety
Read/write operations must be done in appropriate context:

```kotlin
// READ action for PSI access
ApplicationManager.getApplication().runReadAction {
    // Access PSI here
}

// WRITE action for PSI modification
WriteCommandAction.runWriteCommandAction(project) {
    // Modify PSI here
}
```

### Index Access
Use indices efficiently:

```kotlin
// GOOD: Use FileBasedIndex for cross-file lookups
val fragments = FileBasedIndex.getInstance()
    .getContainingFiles(FragmentIndex.NAME, fragmentName, GlobalSearchScope.projectScope(project))

// GOOD: Use StubIndex for PSI element lookups
val elements = StubIndex.getElements(
    BerryCrushStubKeys.SCENARIO,
    name,
    project,
    scope,
    BerryCrushScenarioElement::class.java
)
```

## 3. Common PSI Patterns

### Traversing Siblings
When PSI structure is flat (elements are siblings, not nested):

```kotlin
fun collectFollowingSiblings(start: PsiElement, predicate: (PsiElement) -> Boolean): List<PsiElement> {
    val result = mutableListOf<PsiElement>()
    var sibling = start.nextSibling
    while (sibling != null) {
        if (predicate(sibling)) {
            result.add(sibling)
        }
        sibling = sibling.nextSibling
    }
    return result
}
```

### Finding Elements by Offset
When you need elements within a range:

```kotlin
fun findElementsInRange(file: PsiFile, startOffset: Int, endOffset: Int): List<PsiElement> {
    val allElements = PsiTreeUtil.findChildrenOfType(file, PsiElement::class.java)
    return allElements.filter { 
        it.textOffset >= startOffset && it.textOffset < endOffset 
    }
}
```

### Safe Text Extraction
Handle multi-line text safely:

```kotlin
fun extractFirstLine(element: PsiElement): String {
    return element.text.trim()
        .takeWhile { it != '\n' && it != '\r' }
        .take(60)
}
```

### Pattern Matching with Regex
Use case-insensitive patterns for DSL keywords:

```kotlin
// GOOD: Case-insensitive regex for keyword matching
private val SCENARIO_PATTERN = Regex("""[Ss]cenario:\s*(.+)""")

fun extractScenarioName(text: String): String? {
    val match = SCENARIO_PATTERN.find(text.lines().first())
    return match?.groupValues?.get(1)?.trim()
}
```

## 4. Structure View Patterns

### TreeElement Implementation
```kotlin
class MyStructureViewElement(private val element: PsiElement) : StructureViewTreeElement {
    
    override fun getValue(): Any = element
    
    override fun getPresentation(): ItemPresentation = object : ItemPresentation {
        override fun getPresentableText(): String? = extractDisplayText()
        override fun getLocationString(): String? = null
        override fun getIcon(unused: Boolean): Icon? = getElementIcon()
    }
    
    override fun getChildren(): Array<TreeElement> {
        return collectChildren().toTypedArray()
    }
    
    private fun collectChildren(): List<TreeElement> = when (element) {
        is BerryCrushFile -> collectBlocks()
        is BerryCrushScenarioElement -> collectSteps()
        else -> emptyList()
    }
}
```

## 5. Code Folding Patterns

### FoldingBuilder Implementation
```kotlin
class MyFoldingBuilder : FoldingBuilder {
    
    override fun buildFoldRegions(node: ASTNode, document: Document): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        collectFoldingRegions(node, document, descriptors)
        return descriptors.toTypedArray()
    }
    
    private fun collectFoldingRegions(
        node: ASTNode,
        document: Document,
        descriptors: MutableList<FoldingDescriptor>
    ) {
        if (node.elementType in FOLDABLE_TYPES) {
            calculateFoldRange(node, document)?.let { range ->
                if (range.length > MIN_FOLD_LENGTH) {
                    descriptors.add(FoldingDescriptor(node, range))
                }
            }
        }
        
        // Recursively process children
        var child = node.firstChildNode
        while (child != null) {
            collectFoldingRegions(child, document, descriptors)
            child = child.treeNext
        }
    }
}
```

## 6. Testing Patterns

### Test Base Class
```kotlin
abstract class BerryCrushTestCase : BasePlatformTestCase() {
    
    override fun getTestDataPath(): String = "src/test/resources/testData"
    
    protected fun createScenarioFile(name: String, content: String): VirtualFile {
        return myFixture.tempDirFixture.createFile("$name.scenario", content)
    }
    
    protected fun createFragmentFile(name: String, content: String): VirtualFile {
        return myFixture.tempDirFixture.createFile("$name.fragment", content)
    }
}
```

### PSI Structure Tests
```kotlin
fun testScenarioHasSteps() {
    val file = createScenarioFile("test", """
        scenario: Test scenario
          given a condition
          when an action
          then a result
    """.trimIndent())
    
    val psiFile = psiManager.findFile(file)
    assertNotNull("PSI file should exist", psiFile)
    
    val scenarios = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushScenarioElement::class.java)
    assertEquals("Should find 1 scenario", 1, scenarios.size)
    
    val steps = PsiTreeUtil.findChildrenOfType(psiFile, BerryCrushStepElement::class.java)
    assertEquals("Should find 3 steps", 3, steps.size)
}
```

### Assertion Patterns
```kotlin
// GOOD: Descriptive assertion messages
assertEquals("Should find 1 scenario", 1, scenarios.size)
assertNotNull("PSI file should be created", psiFile)
assertTrue("Element should be a block", isBlockElement(element))

// GOOD: Test actual method behavior, not reimplemented logic
val result = element.scenarioName  // Call the actual method
assertEquals("Scenario name", "My Scenario", result)

// AVOID: Copying implementation logic
val text = element.text  // Don't reimplement the logic
val match = Regex("...").find(text)  // This duplicates the implementation
```

## 7. Error Handling

### Use runCatching Instead of try-catch
```kotlin
// AVOID: Traditional try-catch
fun processElement(element: PsiElement): Result {
    try {
        return doProcessing(element)
    } catch (e: Exception) {
        LOG.warn("Failed to process element", e)
        return Result.Empty
    }
}

// GOOD: Use runCatching with fold
fun processElement(element: PsiElement): Result =
    runCatching { doProcessing(element) }
        .fold(
            onSuccess = { it },
            onFailure = { e ->
                LOG.warn("Failed to process element", e)
                Result.Empty
            }
        )

// GOOD: Use runCatching with getOrElse
fun processElement(element: PsiElement): Result =
    runCatching { doProcessing(element) }
        .onFailure { LOG.warn("Failed to process element", it) }
        .getOrElse { Result.Empty }

// GOOD: Use runCatching with getOrNull
fun processElement(element: PsiElement): Result? =
    runCatching { doProcessing(element) }
        .onFailure { LOG.warn("Failed to process element", it) }
        .getOrNull()
```

### Graceful Degradation
```kotlin
// GOOD: Return safe defaults instead of throwing
fun findScenarioName(element: PsiElement?): String =
    element
        ?.let { PsiTreeUtil.getParentOfType(it, BerryCrushScenarioElement::class.java) }
        ?.scenarioName
        ?: "Unknown"
```

## 8. Iteration

### Use forEach Instead of for
```kotlin
// AVOID: Traditional for loop
for (element in elements) {
    process(element)
}

// GOOD: Use forEach
elements.forEach { process(it) }

// GOOD: Use forEachIndexed when index is needed
elements.forEachIndexed { index, element ->
    processWithIndex(index, element)
}
```

### Use Functional Collection Operations
```kotlin
// AVOID: Imperative style
val results = mutableListOf<Result>()
for (element in elements) {
    if (isValid(element)) {
        results.add(transform(element))
    }
}
return results

// GOOD: Functional style
return elements
    .filter { isValid(it) }
    .map { transform(it) }

// GOOD: Use firstOrNull instead of find
val first = elements.firstOrNull { it.isValid }

// GOOD: Use any/all/none for boolean checks
val hasValid = elements.any { it.isValid }
val allValid = elements.all { it.isValid }
val noneValid = elements.none { it.isValid }
```

### Traversing AST Nodes
When traversing AST nodes, use while loop with functional processing:

```kotlin
// For AST traversal where forEach isn't available
private fun collectChildren(node: ASTNode, result: MutableList<TreeElement>) {
    var child = node.firstChildNode
    while (child != null) {
        if (shouldInclude(child)) {
            result.add(createElement(child))
        }
        child = child.treeNext
    }
}

// Alternative: Convert to sequence
private fun collectChildren(node: ASTNode): List<TreeElement> =
    generateSequence(node.firstChildNode) { it.treeNext }
        .filter { shouldInclude(it) }
        .map { createElement(it) }
        .toList()
```

## 9. Code Organization

### Constants
```kotlin
companion object {
    private val BLOCK_TYPES = setOf(
        BerryCrushElementTypes.SCENARIO,
        BerryCrushElementTypes.FRAGMENT,
        BerryCrushElementTypes.FEATURE,
    )
    
    private const val MIN_FOLD_LENGTH = 5
    private const val MAX_DISPLAY_LENGTH = 60
}
```

### Private Helpers
Extract reusable logic to private methods:

```kotlin
private fun isBlockElement(element: PsiElement): Boolean =
    element is BerryCrushScenarioElement ||
    element is BerryCrushFragmentElement ||
    element is BerryCrushFeatureElement ||
    element.node?.elementType in BLOCK_TYPES

private fun extractDisplayText(element: PsiElement): String {
    val text = element.text.trim().takeWhile { it != '\n' }
    return text.take(MAX_DISPLAY_LENGTH)
}
```

## 9. Build Verification

Always run full build after changes:

```bash
cd intellij
./gradlew clean build
```

Run checks to verify code quality:

```bash
./gradlew check
```

The check task includes:
- Unit tests
- Detekt (Kotlin static analysis)
- SpotBugs (Bug detection)
- CPD (Duplicate code detection)

## 10. Quick Reference

| Pattern | Use Case |
|---------|----------|
| `PsiTreeUtil.findChildrenOfType()` | Find all children of a type |
| `PsiTreeUtil.getParentOfType()` | Find containing parent |
| `element.node?.elementType` | Get element type safely |
| `element.nextSibling` | Navigate to next sibling |
| `element.text.takeWhile { ... }` | Extract first line |
| `runReadAction { }` | Safe PSI read access |
| `WriteCommandAction.runWriteCommandAction()` | Safe PSI write access |
