# Architecture

This document describes the internal architecture of the BerryCrush IntelliJ plugin.

## Overview

The plugin follows IntelliJ Platform architecture patterns:

```
┌─────────────────────────────────────────────────────────────┐
│                     IntelliJ Platform                        │
├─────────────────────────────────────────────────────────────┤
│  Extension Points (Language, Editor, Navigation, etc.)      │
├─────────────────────────────────────────────────────────────┤
│                    BerryCrush Plugin                         │
│  ┌──────────────┬──────────────┬──────────────────────────┐ │
│  │   Language   │   Services   │     Functionality        │ │
│  │   Support    │              │                          │ │
│  │  ├─ Parser   │  ├─ OpenAPI  │  ├─ Navigation           │ │
│  │  ├─ Lexer    │  │  Provider │  ├─ Completion           │ │
│  │  └─ PSI      │  ├─ Fragment │  ├─ Refactoring          │ │
│  │              │  │  Index    │  ├─ Inspections          │ │
│  │              │  └─ Step     │  └─ Quick Fixes          │ │
│  │              │     Registry │                          │ │
│  └──────────────┴──────────────┴──────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

## Package Structure

```
com.berrycrush.intellij/
├── language/           # Language definition
│   ├── BerryCrushLanguage.kt
│   ├── BerryCrushFileType.kt
│   └── BerryCrushIcons.kt
├── parser/             # Parsing
│   ├── BerryCrushLexer.kt
│   ├── BerryCrushParser.kt
│   └── BerryCrushParserDefinition.kt
├── psi/                # PSI elements
│   ├── BerryCrushFile.kt
│   ├── BerryCrushElements.kt
│   └── BerryCrushElementTypes.kt
├── services/           # Application/project services
│   ├── OpenApiService.kt
│   ├── FragmentIndexService.kt
│   └── StepRegistry.kt
├── navigation/         # Navigation features
│   ├── BerryCrushFindUsagesProvider.kt
│   └── BerryCrushTargetElementEvaluator.kt
├── completion/         # Code completion
│   └── BerryCrushCompletionContributor.kt
├── refactoring/        # Refactoring support
│   ├── safedelete/
│   │   └── BerryCrushSafeDeleteProcessor.kt
│   └── rename/
│       └── BerryCrushRenameProcessor.kt
├── inspections/        # Code inspections
│   ├── MissingFragmentInspection.kt
│   └── UnknownOperationInspection.kt
├── quickfix/           # Quick fixes
│   └── CreateFragmentQuickFix.kt
└── highlighting/       # Syntax highlighting
    └── BerryCrushSyntaxHighlighter.kt
```

## Core Components

### Language Registration

**BerryCrushLanguage.kt** - Language singleton:

```kotlin
object BerryCrushLanguage : Language("BerryCrush") {
    override fun getDisplayName() = "BerryCrush"
}
```

**BerryCrushFileType.kt** - File type for `.scenario` and `.fragment`:

```kotlin
object BerryCrushFileType : LanguageFileType(BerryCrushLanguage) {
    override fun getName() = "BerryCrush"
    override fun getDefaultExtension() = "scenario"
}
```

### Parser

The parser converts text into PSI tree:

```
Text → Lexer → Tokens → Parser → PSI Tree
```

**Lexer** breaks text into tokens:
```
"given I call ^login" → [KEYWORD:"given", TEXT:"I call", OP_REF:"^login"]
```

**Parser** builds tree structure from tokens using grammar rules.

### PSI (Program Structure Interface)

PSI elements represent code structure:

```
BerryCrushFile
├── ScenarioElement
│   ├── KeywordElement ("scenario:")
│   ├── TextElement ("Create user")
│   └── StepElement
│       ├── KeywordElement ("when")
│       ├── TextElement ("I call")
│       └── OperationRefElement ("^createUser")
└── FragmentElement
    ├── KeywordElement ("fragment:")
    └── TextElement ("login-steps")
```

## Services

### OpenAPI Service

**Purpose**: Parse and query OpenAPI specifications.

**Lifecycle**: Project-level service, lazy initialization.

```kotlin
@Service(Service.Level.PROJECT)
class OpenApiService(private val project: Project) {
    fun getOperation(operationId: String): Operation?
    fun getAllOperations(): List<Operation>
    fun getOperationsFromFile(file: VirtualFile): List<Operation>
}
```

### Fragment Index Service

**Purpose**: Index fragment definitions and includes for fast lookup.

**Uses**: IntelliJ's stub indexing infrastructure.

```kotlin
object IncludeUsageIndex : StringStubIndexExtension<BerryCrushIncludeElement>() {
    fun getFragmentUsages(name: String, project: Project): Collection<PsiElement>
}
```

### Step Registry

**Purpose**: Track custom `@Step` and `@Assertion` annotated methods.

```kotlin
@Service(Service.Level.PROJECT)
class StepRegistry(private val project: Project) {
    fun findStep(text: String): PsiMethod?
    fun findAssertion(text: String): PsiMethod?
}
```

## Extension Points

### Used Extension Points

| Extension Point | Implementation |
|-----------------|----------------|
| `lang.parserDefinition` | `BerryCrushParserDefinition` |
| `lang.syntaxHighlighterFactory` | `BerryCrushHighlighterFactory` |
| `lang.foldingBuilder` | `BerryCrushFoldingBuilder` |
| `completion.contributor` | `BerryCrushCompletionContributor` |
| `gotoDeclarationHandler` | `BerryCrushGotoHandler` |
| `findUsagesProvider` | `BerryCrushFindUsagesProvider` |
| `refactoring.safeDeleteProcessor` | `BerryCrushSafeDeleteProcessor` |
| `annotator` | `BerryCrushAnnotator` |
| `localInspection` | Various inspections |

### Provided Extension Points

The plugin provides extension points for customization:

```xml
<extensionPoint name="stepProvider" interface="...StepProvider"/>
<extensionPoint name="assertionProvider" interface="...AssertionProvider"/>
```

## Data Flow

### Completion Flow

```
1. User types "^"
2. Platform triggers CompletionContributor
3. BerryCrushCompletionContributor.fillCompletionVariants()
4. Query OpenApiService for operations
5. Create LookupElements for each operation
6. Return to platform for display
```

### Navigation Flow

```
1. User Ctrl+clicks on "^operationId"
2. Platform triggers GotoDeclarationHandler
3. BerryCrushGotoHandler.getGotoDeclarationTargets()
4. Query OpenApiService for operation location
5. Return PsiElement at definition
6. Platform navigates to element
```

### Inspection Flow

```
1. File is edited
2. Platform schedules inspection pass
3. Each LocalInspectionTool.checkFile() called
4. MissingFragmentInspection checks include directives
5. Problems registered with ProblemDescriptors
6. Platform displays highlights
```

## Threading Model

### Read/Write Actions

All PSI access must be wrapped appropriately:

```kotlin
// Reading PSI
ApplicationManager.getApplication().runReadAction {
    // Access PSI elements
}

// Writing PSI
WriteCommandAction.runWriteCommandAction(project) {
    // Modify PSI elements
}
```

### Background Processing

Long operations use background tasks:

```kotlin
ProgressManager.getInstance().run(
    object : Task.Backgroundable(project, "Processing...") {
        override fun run(indicator: ProgressIndicator) {
            // Long-running work
        }
    }
)
```

## Indexing

### Stub Indexes

Stubs provide persistent, serialized PSI subset for fast queries:

```kotlin
class BerryCrushFragmentStubElementType : IStubElementType<...>(...) {
    override fun createStub(psi: BerryCrushFragmentElement, parent: StubElement<*>)
    override fun serialize(stub: BerryCrushFragmentStub, dataStream: StubOutputStream)
    override fun deserialize(dataStream: StubInputStream, parent: StubElement<*>)
}
```

### File-Based Indexes

For cross-file data:

```kotlin
class IncludeUsageIndex : ScalarIndexExtension<String>() {
    override fun getIndexer(): DataIndexer<String, Void, FileContent>
    override fun getKeyDescriptor(): KeyDescriptor<String>
}
```

## Best Practices

### PSI Immutability

Never modify PSI directly. Use `WriteCommandAction`:

```kotlin
WriteCommandAction.runWriteCommandAction(project, "Rename") {
    element.setName(newName)
}
```

### Service Lifecycle

Use proper service levels:
- `Service.Level.APP` - Shared across all projects
- `Service.Level.PROJECT` - Per-project instance

### Memory Management

Avoid holding PSI references. Use `SmartPsiElementPointer`:

```kotlin
val pointer = SmartPointerManager.createPointer(element)
// Later...
val element = pointer.element // May be null if invalidated
```

### Testing

Use IntelliJ test framework:

```kotlin
class MyTest : BasePlatformTestCase() {
    fun testSomething() {
        myFixture.configureByText("test.scenario", "...")
        // Test logic
    }
}
```
