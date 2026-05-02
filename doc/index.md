# BerryCrush IntelliJ Plugin Documentation

Welcome to the BerryCrush IntelliJ Plugin documentation. This plugin provides comprehensive IDE support for writing and running BerryCrush BDD tests.

## Quick Start

1. [Installation](installation.md) - How to install the plugin
2. [Getting Started](getting-started.md) - Your first BerryCrush scenario

## Features

- [Syntax Highlighting](features/syntax.md) - File types and color coding
- [Navigation](features/navigation.md) - Go to Definition, Find Usages
- [Refactoring](features/refactoring.md) - Rename, Safe Delete
- [Inspections](features/inspections.md) - Code analysis and error detection
- [Quick Fixes](features/quickfixes.md) - One-click fixes for common issues
- [Test Runner](features/test-runner.md) - Running BerryCrush tests
- [OpenAPI Integration](features/openapi.md) - Working with OpenAPI specs

## Configuration

- [Plugin Settings](configuration/settings.md) - Customizing the plugin

## For Developers

- [Architecture](development/architecture.md) - Plugin structure overview
- [PSI Structure](development/psi.md) - Understanding the PSI tree
- [Contributing](development/contributing.md) - How to contribute
- [Development Setup](development/setup.md) - Building from source

## What is BerryCrush?

BerryCrush is an OpenAPI-driven BDD testing framework that allows you to write human-readable test scenarios that directly execute against your API specifications.

```berrycrush
scenario: Create new user
  given I create a new user
    call ^createUser
      body: {"name": "John", "email": "john@example.com"}
    assert status 201
    extract $.id => userId
  when I retrieve the user
    call ^getUser
      id: {{userId}}
    assert status 200
    assert $.name equals "John"
```

## Supported File Types

| Extension | Description |
|-----------|-------------|
| `.scenario` | Test scenario files containing scenarios and steps |
| `.fragment` | Reusable step fragments that can be included in scenarios |

## Support

- [GitHub Issues](https://github.com/berrycrush/berrycrush/issues) - Report bugs and request features
- [BerryCrush Documentation](https://berrycrush.github.io/) - Full framework documentation
