# Development Setup

This guide walks through setting up a development environment for the BerryCrush IntelliJ plugin.

## Prerequisites

### Required Software

| Software | Version | Download |
|----------|---------|----------|
| JDK | 21+ | [Adoptium](https://adoptium.net/) |
| IntelliJ IDEA | 2025.3+ | [JetBrains](https://www.jetbrains.com/idea/) |
| Git | 2.x | [Git](https://git-scm.com/) |

### Verify Installation

```bash
# Java
java -version
# Should show Java 21 or higher

# Gradle (optional, wrapper included)
./gradlew --version

# Git
git --version
```

## Clone Repository

```bash
git clone https://github.com/yourusername/berrycrush-workspace.git
cd berrycrush-workspace/intellij
```

## IDE Setup

### Open Project

1. Launch IntelliJ IDEA
2. **File** → **Open**
3. Navigate to `berrycrush-workspace/intellij`
4. Click **OK**
5. Select **Open as Project**

### Import Gradle Project

When prompted:
1. Select **Import Gradle Project**
2. Use Gradle wrapper
3. Enable auto-import

### Configure JDK

1. **File** → **Project Structure** (or `Cmd+;`)
2. **Project** → **SDK**
3. Select JDK 21 or add new one
4. **Apply** → **OK**

### Recommended Plugins

Install these plugins for better development experience:

| Plugin | Purpose |
|--------|---------|
| Grammar-Kit | BNF grammar support |
| PsiViewer | PSI tree inspection |
| Plugin DevKit | Plugin development support |

Install via **Settings** → **Plugins** → **Marketplace**

## Build Project

### From Terminal

```bash
./gradlew build
```

### From IDE

1. Open **Gradle** tool window
2. Navigate to **Tasks** → **build** → **build**
3. Double-click to run

### Expected Output

```
BUILD SUCCESSFUL in 30s
15 actionable tasks: 15 executed
```

## Run Plugin

### Launch Sandbox IDE

```bash
./gradlew runIde
```

Or from IDE:
1. **Gradle** → **Tasks** → **intellij** → **runIde**

This launches a new IntelliJ instance with your plugin installed.

### Test Plugin Features

In the sandbox IDE:
1. Create a new project
2. Create a `.scenario` file
3. Verify syntax highlighting works
4. Test completion, navigation, etc.

## Run Tests

### All Tests

```bash
./gradlew test
```

### Single Test Class

```bash
./gradlew test --tests "SafeDeleteTest"
```

### Single Test Method

```bash
./gradlew test --tests "SafeDeleteTest.testBasicDelete"
```

### From IDE

1. Right-click on test file
2. Select **Run 'TestName'**

## Debug Plugin

### Debug in Sandbox

1. Create a run configuration:
   - **Run** → **Edit Configurations**
   - Click **+** → **Gradle**
   - Task: `runIde`
   - Name: "Debug Plugin"
2. Set breakpoints in your code
3. Click **Debug** button

### Debug Tests

1. Open test file
2. Set breakpoints
3. Right-click test → **Debug 'TestName'**

## Project Structure

```
intellij/
├── build.gradle.kts           # Main build file
├── settings.gradle.kts        # Project settings
├── gradle.properties          # Properties (version, etc.)
├── gradlew                    # Gradle wrapper (Unix)
├── gradlew.bat                # Gradle wrapper (Windows)
│
├── src/main/
│   ├── kotlin/                # Kotlin source files
│   │   └── com/berrycrush/intellij/
│   │       ├── language/      # Language definitions
│   │       ├── parser/        # Lexer and parser
│   │       ├── psi/           # PSI elements
│   │       ├── services/      # Application services
│   │       ├── navigation/    # Go to, find usages
│   │       ├── completion/    # Code completion
│   │       ├── refactoring/   # Rename, safe delete
│   │       ├── inspections/   # Code inspections
│   │       └── highlighting/  # Syntax highlighting
│   │
│   └── resources/
│       └── META-INF/
│           └── plugin.xml     # Plugin descriptor
│
├── src/test/
│   ├── kotlin/                # Test files
│   └── testData/              # Test fixtures
│
├── config/
│   ├── detekt/                # Code style config
│   └── spotbugs/              # Bug detector config
│
└── gradle/
    └── libs.versions.toml     # Dependency versions
```

## Key Files

### plugin.xml

Plugin descriptor declaring all extensions:

```xml
<idea-plugin>
    <id>com.berrycrush.intellij</id>
    <name>BerryCrush</name>
    
    <extensions defaultExtensionNs="com.intellij">
        <fileType name="BerryCrush" 
                  implementationClass="...BerryCrushFileType"
                  extensions="scenario;fragment"/>
        <lang.parserDefinition 
                  language="BerryCrush"
                  implementationClass="...BerryCrushParserDefinition"/>
        <!-- More extensions... -->
    </extensions>
</idea-plugin>
```

### build.gradle.kts

Build configuration:

```kotlin
plugins {
    id("org.jetbrains.intellij") version "1.17.0"
    kotlin("jvm") version "1.9.22"
}

intellij {
    version.set("2025.3")
    plugins.set(listOf(/* dependencies */))
}

tasks {
    patchPluginXml {
        sinceBuild.set("253")
        untilBuild.set("253.*")
    }
}
```

### gradle.properties

```properties
pluginVersion=1.0.0
platformVersion=2025.3
kotlinVersion=1.9.22
```

## Common Tasks

### Clean Build

```bash
./gradlew clean build
```

### Check Code Style

```bash
./gradlew detekt
```

### Run All Checks

```bash
./gradlew check
```

Includes: compile, test, detekt, spotbugs

### Build Distribution

```bash
./gradlew buildPlugin
```

Output: `build/distributions/berrycrush-intellij-{version}.zip`

### Publish to Marketplace

```bash
./gradlew publishPlugin
```

Requires token in `gradle.properties`:
```properties
intellijPublishToken=your-token-here
```

## Troubleshooting

### Gradle Sync Failed

1. Check internet connection
2. **File** → **Invalidate Caches / Restart**
3. Delete `.gradle` directory and re-sync

### Tests Not Found

1. Ensure test files are in `src/test/kotlin`
2. Check test classes extend `BasePlatformTestCase`
3. Run **Gradle** → **Tasks** → **verification** → **test**

### Plugin Not Loading

1. Check `plugin.xml` syntax
2. Verify all extension classes exist
3. Check IntelliJ log: **Help** → **Show Log in Finder**

### Sandbox IDE Crashes

1. Increase memory in `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx4g
   ```
2. Clean and rebuild

### PSI Issues

1. Install **PsiViewer** plugin
2. Use **Tools** → **View PSI Structure**
3. Compare expected vs actual PSI tree

## Resources

- [IntelliJ Platform SDK Docs](https://plugins.jetbrains.com/docs/intellij/)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Grammar-Kit Documentation](https://github.com/JetBrains/Grammar-Kit)
- [IntelliJ Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template)
