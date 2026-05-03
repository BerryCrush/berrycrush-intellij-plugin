---
applyTo: "*.kt"
---

# Kotlin Conventions for IntelliJ Plugin

## Technology Stack

| Technology | Version |
|------------|---------|
| Kotlin | 2.3.20 |
| Java | 21 |
| IntelliJ Platform | 2025.3+ |

## 1. Expression Functions

Use expression functions for single-return statements:

```kotlin
// AVOID: Block with single return
fun getName(): String {
    return "name"
}

// GOOD: Expression function
fun getName(): String = "name"
```

## 2. Code Style

### Trailing Commas
Always use trailing commas in multi-line declarations:

```kotlin
val types = setOf(
    BerryCrushElementTypes.SCENARIO,
    BerryCrushElementTypes.FRAGMENT,
    BerryCrushElementTypes.FEATURE,  // Trailing comma
)
```

### Companion Objects
Place companion objects at the end of the class.

## 3. Null Safety

### Prefer Safe Calls with let
```kotlin
// AVOID: Multiple returns
fun processElement(element: PsiElement?): String? {
    val e = element ?: return null
    return extractName(e)
}

// GOOD: Use ?.let
fun processElement(element: PsiElement?): String? =
    element?.let { extractName(it) }
```

## 4. Iteration

### Use forEach Instead of for
```kotlin
// AVOID: for loop
for (element in elements) {
    process(element)
}

// GOOD: forEach
elements.forEach { process(it) }
```

### Functional Operations
```kotlin
// GOOD: Use filter/map chain
elements
    .filter { isValid(it) }
    .map { transform(it) }
```

## 5. Error Handling

### Use runCatching
```kotlin
// AVOID: try-catch
try {
    doProcessing(element)
} catch (e: Exception) {
    LOG.warn("Failed", e)
}

// GOOD: runCatching
runCatching { doProcessing(element) }
    .onFailure { LOG.warn("Failed", it) }
    .getOrElse { defaultValue }
```

## 6. PSI Access Patterns

### Use PsiTreeUtil
```kotlin
// Find children
PsiTreeUtil.findChildrenOfType(file, BerryCrushScenarioElement::class.java)

// Find parent
PsiTreeUtil.getParentOfType(element, BerryCrushFragmentElement::class.java)
```

### Safe Element Type Access
```kotlin
val elementType = element.node?.elementType
val isBlock = elementType in BLOCK_TYPES
```

## 7. Testing

### Test Actual Behavior
```kotlin
// GOOD: Test actual method
val name = element.scenarioName
assertEquals("My Scenario", name)

// AVOID: Reimplement logic in test
```

## 8. Build Verification

After any changes:

```bash
./gradlew clean build
./gradlew check
```

## Quick Reference

| Pattern | Usage |
|---------|-------|
| `fun foo() = expr` | Expression function |
| `element?.let { }` | Safe call with transform |
| `elements.forEach { }` | Iteration |
| `runCatching { }.getOrElse { }` | Error handling |
| `PsiTreeUtil.findChildrenOfType()` | Find children |
