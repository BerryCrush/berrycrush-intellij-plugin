# Inspections

The BerryCrush plugin analyzes your code and highlights potential issues.

## Overview

Inspections run automatically as you type and highlight issues with:
- **Red underline**: Error - will likely cause test failure
- **Yellow underline**: Warning - potential problem
- **Weak underline**: Weak warning - style or best practice issue

## Available Inspections

### Missing Fragment Reference

**Level**: Error (red underline)

**What it checks**: The fragment name in an `include` directive exists.

**Example**:
```berrycrush
include login-steps    # Error if login-steps fragment doesn't exist
```

**How to fix**:
1. Create the missing fragment
2. Fix the fragment name if typo
3. Use quick fix: `Alt+Enter` → **Create fragment 'login-steps'**

### Undefined OpenAPI Operation

**Level**: Warning (yellow underline)

**What it checks**: The operation referenced with `^` exists in an OpenAPI specification.

**Example**:
```berrycrush
call ^nonExistentOperation    # Warning if operation not in any OpenAPI spec
```

**How to fix**:
1. Add the operation to your OpenAPI spec
2. Fix the operation ID if typo
3. Ensure OpenAPI file is in the correct location

### Undefined Custom Step

**Level**: Weak Warning (light underline)

**What it checks**: A step that doesn't match built-in patterns has a corresponding `@Step` annotated method.

**Example**:
```berrycrush
given I am logged in as admin    # Warning if no @Step method matches
```

**How to fix**:
1. Create a `@Step` annotated method
2. Fix the step text to match existing method
3. Ignore if using expression patterns

### Undefined Assertion

**Level**: Weak Warning (light underline)

**What it checks**: Custom assertions have corresponding `@Assertion` annotated methods.

**Example**:
```berrycrush
then validate user status    # Warning if no @Assertion method matches
```

**How to fix**:
1. Create an `@Assertion` annotated method
2. Use built-in assertions instead
3. Fix the assertion name

## Managing Inspections

### Enable/Disable Inspections

1. Go to **Settings** → **Editor** → **Inspections**
2. Expand **BerryCrush**
3. Check/uncheck specific inspections

### Change Severity

1. Go to **Settings** → **Editor** → **Inspections**
2. Expand **BerryCrush**
3. Select an inspection
4. Change **Severity** dropdown

### Suppress for a Line

Add a comment to suppress:
```berrycrush
# noinspection BerryCrushMissingFragment
include legacy-steps    # This won't show the error
```

### Suppress for File

Add at the top of the file:
```berrycrush
# noinspection BerryCrushMissingFragment
scenario: Legacy Tests
  ...
```

## Inspection Tool Window

View all issues in the project:

1. **Analyze** → **Inspect Code**
2. Select scope (file, directory, or project)
3. Review results in the inspection window

### Features

- Group by severity, file, or inspection type
- Quick fix directly from results
- Export results to HTML

## Automatic Refresh

Inspections update automatically when:
- You edit the file
- Related files change (fragments, OpenAPI specs)
- Project is re-indexed

If inspections seem stale:
1. **File** → **Invalidate Caches / Restart**
2. Wait for indexing to complete

## Quick Fixes

Some inspections offer quick fixes:

| Inspection | Quick Fix |
|------------|-----------|
| Missing Fragment | Create fragment file |
| Undefined Operation | (No automatic fix) |
| Undefined Step | (No automatic fix) |
| Undefined Assertion | (No automatic fix) |

### Using Quick Fixes

1. Place cursor on the error
2. Press `Alt+Enter`
3. Select the quick fix
4. Configure options if prompted
5. Apply

## Best Practices

### Clean Before Commit

Before committing, run:
1. **Analyze** → **Inspect Code** on changed files
2. Fix all errors
3. Review warnings

### Team Settings

Share inspection settings:
1. Export profile: **Settings** → **Editor** → **Inspections** → ⚙️ → **Export**
2. Add to version control
3. Team members import the profile

## Troubleshooting

### False Positives

If inspection reports incorrect error:
1. Verify file syntax is correct
2. Check related files exist and are indexed
3. Report issue if persistent

### Missing Inspections

If expected inspections don't run:
1. Check inspection is enabled in settings
2. Verify file type is recognized
3. Ensure plugin is enabled

### Performance Issues

If inspections are slow:
1. Reduce scope (file instead of project)
2. Disable resource-intensive inspections
3. Increase IDE memory allocation
