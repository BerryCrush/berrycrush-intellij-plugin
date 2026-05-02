# PSI Structure

This document describes the PSI (Program Structure Interface) implementation for BerryCrush files.

## Overview

PSI is the IntelliJ Platform's way of representing code structure. It provides:
- A tree structure representing file contents
- Navigation between elements
- Modification support
- Integration with IDE features

## Element Hierarchy

```
BerryCrushFile (PsiFile)
├── BerryCrushFeatureElement
│   └── name: String
├── BerryCrushScenarioElement
│   ├── name: String
│   └── steps: List<BerryCrushStepElement>
├── BerryCrushScenarioOutlineElement
│   ├── name: String
│   ├── steps: List<BerryCrushStepElement>
│   └── examples: BerryCrushExamplesElement
├── BerryCrushFragmentElement
│   ├── name: String
│   └── steps: List<BerryCrushStepElement>
└── BerryCrushBackgroundElement
    └── steps: List<BerryCrushStepElement>
```

## Core Elements

### BerryCrushFile

The root element representing a `.scenario` or `.fragment` file.

```kotlin
class BerryCrushFile(viewProvider: FileViewProvider) : 
    PsiFileBase(viewProvider, BerryCrushLanguage) {
    
    fun getFeature(): BerryCrushFeatureElement?
    fun getScenarios(): List<BerryCrushScenarioElement>
    fun getFragments(): List<BerryCrushFragmentElement>
}
```

### BerryCrushFeatureElement

Represents a `Feature:` declaration.

```kotlin
interface BerryCrushFeatureElement : BerryCrushNamedElement {
    fun getKeyword(): PsiElement
    fun getDescription(): String?
    fun getTags(): List<BerryCrushTagElement>
}
```

### BerryCrushScenarioElement

Represents a `Scenario:` block.

```kotlin
interface BerryCrushScenarioElement : BerryCrushNamedElement {
    fun getKeyword(): PsiElement
    fun getSteps(): List<BerryCrushStepElement>
    fun getTags(): List<BerryCrushTagElement>
}
```

### BerryCrushFragmentElement

Represents a `Fragment:` definition.

```kotlin
interface BerryCrushFragmentElement : BerryCrushNamedElement, PsiNameIdentifierOwner {
    fun getKeyword(): PsiElement
    fun getSteps(): List<BerryCrushStepElement>
    
    override fun getName(): String?
    override fun setName(name: String): PsiElement
    override fun getNameIdentifier(): PsiElement?
}
```

### BerryCrushStepElement

Represents a step (Given/When/Then/And/But).

```kotlin
interface BerryCrushStepElement : BerryCrushElement {
    fun getKeyword(): BerryCrushKeyword
    fun getText(): String
    fun getOperationRef(): BerryCrushOperationRefElement?
    fun getDataTable(): BerryCrushDataTableElement?
    fun getDocString(): BerryCrushDocStringElement?
}
```

### BerryCrushIncludeElement

Represents an `include` directive.

```kotlin
interface BerryCrushIncludeElement : BerryCrushElement {
    fun getFragmentName(): String
    fun getReference(): PsiReference
    fun resolve(): BerryCrushFragmentElement?
}
```

## Named Elements

Elements that can be renamed implement `PsiNameIdentifierOwner`:

```kotlin
interface BerryCrushNamedElement : PsiNameIdentifierOwner, NavigatablePsiElement {
    override fun getName(): String?
    override fun setName(name: String): PsiElement
    override fun getNameIdentifier(): PsiElement?
}
```

### Implementation Example

```kotlin
class BerryCrushFragmentElementImpl(node: ASTNode) : 
    BerryCrushElementImpl(node), BerryCrushFragmentElement {
    
    override fun getName(): String? {
        return nameIdentifier?.text
    }
    
    override fun setName(name: String): PsiElement {
        nameIdentifier?.let { identifier ->
            val newElement = BerryCrushElementFactory.createIdentifier(project, name)
            identifier.replace(newElement)
        }
        return this
    }
    
    override fun getNameIdentifier(): PsiElement? {
        return findChildByType(BerryCrushTypes.TEXT)
    }
}
```

## Element Types

### Token Types

Defined by the lexer:

```kotlin
object BerryCrushTypes {
    // Keywords
    val FEATURE = IElementType("FEATURE", BerryCrushLanguage)
    val SCENARIO = IElementType("SCENARIO", BerryCrushLanguage)
    val FRAGMENT = IElementType("FRAGMENT", BerryCrushLanguage)
    val GIVEN = IElementType("GIVEN", BerryCrushLanguage)
    val WHEN = IElementType("WHEN", BerryCrushLanguage)
    val THEN = IElementType("THEN", BerryCrushLanguage)
    val AND = IElementType("AND", BerryCrushLanguage)
    val BUT = IElementType("BUT", BerryCrushLanguage)
    val INCLUDE = IElementType("INCLUDE", BerryCrushLanguage)
    
    // Content
    val TEXT = IElementType("TEXT", BerryCrushLanguage)
    val COMMENT = IElementType("COMMENT", BerryCrushLanguage)
    val OP_REF = IElementType("OP_REF", BerryCrushLanguage)
    val VARIABLE = IElementType("VARIABLE", BerryCrushLanguage)
    
    // Table
    val PIPE = IElementType("PIPE", BerryCrushLanguage)
    val TABLE_CELL = IElementType("TABLE_CELL", BerryCrushLanguage)
}
```

### Composite Types

Built by the parser:

```kotlin
object BerryCrushElementTypes {
    val FEATURE = BerryCrushElementType("FEATURE")
    val SCENARIO = BerryCrushElementType("SCENARIO")
    val FRAGMENT = BerryCrushElementType("FRAGMENT")
    val STEP = BerryCrushElementType("STEP")
    val DATA_TABLE = BerryCrushElementType("DATA_TABLE")
    val INCLUDE = BerryCrushElementType("INCLUDE")
}
```

## References

### Fragment Reference

```kotlin
class BerryCrushFragmentReference(
    element: BerryCrushIncludeElement
) : PsiReferenceBase<BerryCrushIncludeElement>(element) {
    
    override fun resolve(): PsiElement? {
        val fragmentName = element.fragmentName
        return findFragmentByName(fragmentName)
    }
    
    override fun getVariants(): Array<Any> {
        return getAllFragmentNames().map { name ->
            LookupElementBuilder.create(name)
                .withIcon(BerryCrushIcons.FRAGMENT)
        }.toTypedArray()
    }
    
    private fun findFragmentByName(name: String): BerryCrushFragmentElement? {
        return StubIndex.getElements(
            FragmentNameIndex.KEY,
            name,
            element.project,
            GlobalSearchScope.projectScope(element.project),
            BerryCrushFragmentElement::class.java
        ).firstOrNull()
    }
}
```

### Operation Reference

```kotlin
class BerryCrushOperationReference(
    element: BerryCrushOperationRefElement
) : PsiReferenceBase<BerryCrushOperationRefElement>(element) {
    
    override fun resolve(): PsiElement? {
        val operationId = element.operationId
        val openApiService = element.project.service<OpenApiService>()
        return openApiService.getOperationElement(operationId)
    }
}
```

## Stubs

Stubs enable faster indexing by storing essential data without full PSI parsing.

### Fragment Stub

```kotlin
interface BerryCrushFragmentStub : StubElement<BerryCrushFragmentElement> {
    val name: String
}

class BerryCrushFragmentStubImpl(
    parent: StubElement<*>,
    override val name: String
) : StubBase<BerryCrushFragmentElement>(parent, BerryCrushStubTypes.FRAGMENT),
    BerryCrushFragmentStub
```

### Stub Element Type

```kotlin
object BerryCrushFragmentStubType : 
    IStubElementType<BerryCrushFragmentStub, BerryCrushFragmentElement>("FRAGMENT", BerryCrushLanguage) {
    
    override fun createStub(
        psi: BerryCrushFragmentElement,
        parentStub: StubElement<*>
    ): BerryCrushFragmentStub {
        return BerryCrushFragmentStubImpl(parentStub, psi.name ?: "")
    }
    
    override fun serialize(stub: BerryCrushFragmentStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
    }
    
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): BerryCrushFragmentStub {
        val name = dataStream.readName()?.string ?: ""
        return BerryCrushFragmentStubImpl(parentStub, name)
    }
    
    override fun createPsi(stub: BerryCrushFragmentStub): BerryCrushFragmentElement {
        return BerryCrushFragmentElementImpl(stub, this)
    }
}
```

## Traversal

### Finding Elements

```kotlin
// Find first matching element
val feature = PsiTreeUtil.findChildOfType(file, BerryCrushFeatureElement::class.java)

// Find all matching elements
val scenarios = PsiTreeUtil.findChildrenOfType(file, BerryCrushScenarioElement::class.java)

// Find parent
val scenario = PsiTreeUtil.getParentOfType(step, BerryCrushScenarioElement::class.java)

// Find sibling
val nextStep = PsiTreeUtil.getNextSiblingOfType(step, BerryCrushStepElement::class.java)
```

### Visitor Pattern

```kotlin
class BerryCrushVisitor : PsiElementVisitor() {
    fun visitFragment(element: BerryCrushFragmentElement) {
        visitElement(element)
    }
    
    fun visitScenario(element: BerryCrushScenarioElement) {
        visitElement(element)
    }
    
    fun visitStep(element: BerryCrushStepElement) {
        visitElement(element)
    }
}

// Usage
file.accept(object : BerryCrushVisitor() {
    override fun visitFragment(element: BerryCrushFragmentElement) {
        // Process fragment
    }
})
```

## Element Factory

Create new PSI elements programmatically:

```kotlin
object BerryCrushElementFactory {
    fun createIdentifier(project: Project, name: String): PsiElement {
        val text = "Fragment: $name\n  Given step"
        val file = createFile(project, text)
        return file.firstChild.findChildByType(BerryCrushTypes.TEXT)!!
    }
    
    fun createFragment(project: Project, name: String): BerryCrushFragmentElement {
        val text = "Fragment: $name\n  Given placeholder step"
        val file = createFile(project, text)
        return PsiTreeUtil.findChildOfType(file, BerryCrushFragmentElement::class.java)!!
    }
    
    private fun createFile(project: Project, text: String): BerryCrushFile {
        return PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.fragment", BerryCrushFileType, text) as BerryCrushFile
    }
}
```

## Best Practices

### Thread Safety

Always access PSI in read/write actions:

```kotlin
// Read
ApplicationManager.getApplication().runReadAction {
    val name = element.name
}

// Write
WriteCommandAction.runWriteCommandAction(project) {
    element.setName(newName)
}
```

### Smart Pointers

Don't hold PSI references across operations:

```kotlin
// Bad - element may become invalid
val element = findElement()
// ... later ...
element.name  // May throw

// Good - use smart pointer
val pointer = SmartPointerManager.createPointer(element)
// ... later ...
pointer.element?.name  // Safe
```

### Avoid Redundant Parsing

Use stubs when possible:

```kotlin
// Slower - parses full file
val fragments = PsiManager.getInstance(project)
    .findFile(virtualFile)
    ?.children
    ?.filterIsInstance<BerryCrushFragmentElement>()

// Faster - uses stub index
val fragments = StubIndex.getElements(
    FragmentNameIndex.KEY,
    name,
    project,
    scope,
    BerryCrushFragmentElement::class.java
)
```
