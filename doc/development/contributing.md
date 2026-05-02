# Contributing

Thank you for your interest in contributing to the BerryCrush IntelliJ plugin!

## Getting Started

### Prerequisites

- **JDK 21** or higher
- **IntelliJ IDEA** 2025.3 or later (Community or Ultimate)
- **Gradle** 8.x (wrapper included)
- **Git**

### Clone the Repository

```bash
git clone https://github.com/yourusername/berrycrush-workspace.git
cd berrycrush-workspace/intellij
```

### Open in IDE

1. Open IntelliJ IDEA
2. **File** → **Open**
3. Select the `intellij` directory
4. Import as Gradle project

### Build

```bash
./gradlew build
```

### Run Plugin in Sandbox

```bash
./gradlew runIde
```

This launches a new IntelliJ instance with the plugin installed.

## Development Workflow

### Branch Naming

- `feature/` - New features
- `bugfix/` - Bug fixes
- `docs/` - Documentation updates
- `refactor/` - Code refactoring

Example:
```bash
git checkout -b feature/add-rename-refactoring
```

### Making Changes

1. Create a feature branch
2. Make changes
3. Write/update tests
4. Run checks locally
5. Commit with clear messages
6. Push and create PR

### Commit Messages

Follow conventional commits:

```
feat: add rename support for fragments
fix: handle null pointer in fragment resolution
docs: update navigation documentation
refactor: extract common PSI utilities
test: add tests for safe delete processor
```

## Code Style

### Kotlin

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html).

Key points:
- Use 4 spaces for indentation
- Max line length: 120 characters
- Use meaningful names
- Add KDoc for public APIs

### Run Code Style Checks

```bash
./gradlew detekt
```

### Auto-format

Configure IntelliJ to format on save:
- **Settings** → **Tools** → **Actions on Save**
- Enable **Reformat code**

## Testing

### Run Tests

```bash
./gradlew test
```

### Test Types

| Type | Location | Purpose |
|------|----------|---------|
| Unit | `src/test/kotlin/unit/` | Test individual components |
| Integration | `src/test/kotlin/integration/` | Test component interaction |
| UI | `src/test/kotlin/ui/` | Test UI components |

### Writing Tests

Use `BasePlatformTestCase`:

```kotlin
class MyFeatureTest : BasePlatformTestCase() {
    
    override fun getTestDataPath(): String = "src/test/testData/myFeature"
    
    fun testBasicFunctionality() {
        myFixture.configureByText("test.scenario", """
            Feature: Test
            Scenario: Test scenario
              Given I call ^test
        """.trimIndent())
        
        // Test assertions
        val file = myFixture.file as BerryCrushFile
        assertNotNull(file.feature)
    }
}
```

### Test Data Files

Place test data in `src/test/testData/`:

```
src/test/testData/
├── completion/
│   ├── operationCompletion.scenario
│   └── fragmentCompletion.scenario
├── navigation/
│   ├── gotoFragment.scenario
│   └── findUsages.scenario
└── refactoring/
    ├── rename/
    └── safeDelete/
```

## Building

### Full Build

```bash
./gradlew build
```

### Build Plugin Distribution

```bash
./gradlew buildPlugin
```

Output: `build/distributions/berrycrush-intellij-*.zip`

### Run All Checks

```bash
./gradlew check
```

Includes:
- Compilation
- Tests
- Detekt (code style)
- Spotbugs (bug detection)

## Project Structure

```
intellij/
├── build.gradle.kts          # Build configuration
├── settings.gradle.kts       # Settings
├── gradle.properties         # Gradle properties
├── src/
│   ├── main/
│   │   ├── kotlin/           # Source code
│   │   └── resources/
│   │       ├── META-INF/
│   │       │   └── plugin.xml  # Plugin descriptor
│   │       └── messages/
│   │           └── Bundle.properties
│   └── test/
│       ├── kotlin/           # Test code
│       └── testData/         # Test data files
├── config/
│   ├── detekt/              # Detekt configuration
│   └── spotbugs/            # Spotbugs configuration
└── doc/                     # Documentation
```

## Plugin Configuration

### plugin.xml

Main plugin descriptor at `src/main/resources/META-INF/plugin.xml`:

```xml
<idea-plugin>
    <id>com.berrycrush.intellij</id>
    <name>BerryCrush</name>
    <vendor>BerryCrush</vendor>
    
    <depends>com.intellij.modules.platform</depends>
    
    <extensions defaultExtensionNs="com.intellij">
        <!-- Extension declarations -->
    </extensions>
    
    <actions>
        <!-- Action declarations -->
    </actions>
</idea-plugin>
```

### Adding New Features

1. Create implementation classes
2. Register in `plugin.xml`
3. Add tests
4. Update documentation

## Pull Request Process

### Before Submitting

1. Rebase on latest `main`
2. Run `./gradlew check`
3. All tests must pass
4. Code must be formatted
5. Update documentation if needed

### PR Description

Include:
- Summary of changes
- Related issues
- Testing done
- Screenshots (for UI changes)

### Review Process

1. Submit PR
2. Address review comments
3. Squash commits if requested
4. Merge when approved

## Release Process

### Version Numbering

Follow semantic versioning: `MAJOR.MINOR.PATCH`

- MAJOR: Breaking changes
- MINOR: New features
- PATCH: Bug fixes

### Release Checklist

1. Update version in `gradle.properties`
2. Update CHANGELOG
3. Create release branch
4. Build and test
5. Create GitHub release
6. Upload to JetBrains Marketplace

## Getting Help

### Resources

- [IntelliJ Platform SDK](https://plugins.jetbrains.com/docs/intellij/)
- [Plugin Development Forum](https://intellij-support.jetbrains.com/hc/en-us/community/topics/200366979-IntelliJ-IDEA-Open-API-and-Plugin-Development)
- [Kotlin Documentation](https://kotlinlang.org/docs/)

### Contact

- GitHub Issues for bugs/features
- Discussions for questions

## License

By contributing, you agree that your contributions will be licensed under the project's license.
