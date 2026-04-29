# BerryCrush IntelliJ Plugin

IntelliJ IDEA plugin providing language support for BerryCrush scenario and fragment files.

## Features

- **File Types**: Support for `.scenario` and `.fragment` files with dedicated icons
- **Syntax Highlighting**: Color coding for keywords, directives, strings, variables, JSON paths
- **Code Folding**: Collapse features, scenarios, fragments, backgrounds, and examples
- **Structure View**: Navigate document structure hierarchically
- **Completion**: Basic keyword and directive completion
- **Commenter**: Toggle line comments with `Ctrl+/` / `Cmd+/`
- **Brace Matching**: Highlight matching brackets and braces

## Requirements

- IntelliJ IDEA 2024.3+ (build 243 to 261.*)

## Installation

1. Download `berrycrush-intellij-x.x.x.zip` from releases
2. In IntelliJ IDEA: Settings → Plugins → ⚙️ → Install Plugin from Disk
3. Select the downloaded zip file
4. Restart IDE

## Development

### Build

```bash
./gradlew buildPlugin
```

The plugin zip will be created at `build/distributions/berrycrush-intellij-x.x.x.zip`.

### Run IDE with Plugin

```bash
./gradlew runIde
```

This launches a sandboxed IntelliJ IDEA with the plugin installed.

### Test

```bash
./gradlew test
```

## Project Structure

```
intellij/
├── src/main/kotlin/com/berrycrush/intellij/
│   ├── language/       # Language & file types
│   ├── lexer/          # Token types & lexer
│   ├── parser/         # Parser definition
│   ├── psi/            # PSI elements
│   ├── highlighting/   # Syntax highlighter & colors
│   ├── folding/        # Code folding
│   ├── structure/      # Structure view
│   ├── completion/     # Completion contributor
│   └── commenter/      # Line commenter
├── src/main/resources/
│   ├── META-INF/plugin.xml
│   └── icons/          # File type icons
└── build.gradle.kts
```

## License

See [LICENSE](LICENSE).
