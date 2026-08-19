# Changelog

All notable changes to the BerryCrush IntelliJ Plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.0.1]

### Fixed
- Fixing missing syntax highlighting for `examples` and tags

## [2.0.0]

### Added
- Scenario file context-menu Run/Debug support with standard IntelliJ execution actions
- Syntax inspection for invalid scenario files, with highlighting and quick fixes
- `webhook` directive highlighting

### Changed
- Unified scenario execution support between context-menu and gutter run paths
- PSI Element base inspection, highlighting
- Removing unnecessary renaming processors
- General code cleanup

## [1.0.1]

### Fixed
- Removing until restriction

## [1.0.0]

### Added
- Syntax highlighting for `.scenario` and `.fragment` files
- Code completion for OpenAPI operations, fragments, and keywords
- Navigation support (Go to Definition, Find Usages)
- Refactoring support (Rename fragment, Rename variable)
- Inspections for missing fragments and undefined operations
- Quick fixes for creating missing fragments
- Test runner integration with gutter icons
- OpenAPI specification integration
- Documentation

### Technical
- Built for IntelliJ IDEA 2025.3+
- Requires JDK 21+
- Uses IntelliJ Platform Gradle Plugin 2.x
