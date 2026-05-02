# Navigation

The BerryCrush plugin provides powerful navigation features to help you move through your test codebase efficiently.

## Go to Definition

Jump directly to the definition of any referenced element.

### How to Use

**Keyboard**: `Cmd+Click` (Mac) or `Ctrl+Click` (Windows/Linux)

**Alternative**: `Cmd+B` (Mac) or `Ctrl+B` (Windows/Linux)

### Supported Elements

| Element Type | Navigates To |
|--------------|--------------|
| Operation reference (`^operationId`) | OpenAPI operation definition |
| Fragment reference (`include name`) | Fragment definition in `.fragment` file |
| Step keyword | `@Step` annotated method in Java/Kotlin |
| Assert keyword | `@Assertion` annotated method in Java/Kotlin |

### Example

```berrycrush
scenario: User flow
  given I am logged in
    include login-steps         # Cmd+Click → jumps to login-steps fragment
  when I create a user
    call ^createUser             # Cmd+Click → jumps to OpenAPI createUser operation
  then validate user exists      # Cmd+Click → jumps to @Step method
```

## Find Usages

Find all places where an element is used.

### How to Use

1. Place cursor on the element
2. Press `Alt+F7`
3. View usages in the tool window

**Alternative**: Right-click → **Find Usages**

### Supported Elements

| Element Type | Finds |
|--------------|-------|
| Fragment definition | All `include` directives referencing it |
| `@Step` method | All step keywords using it |
| `@Assertion` method | All `assert` directives using it |
| OpenAPI operation | All `call` directives referencing it |

### Usage Preview

The Find Usages window shows:
- File name
- Line number
- Context (surrounding code)
- Grouping by file/directory

## Gutter Icons

Clickable icons appear in the editor gutter (left margin).

### Fragment Definitions

**Icon**: Downward arrow (↓)

Click to see all usages of this fragment.

```berrycrush
fragment: login-steps    # ↓ icon shows usage count
  given I have credentials
    call ^login
```

### Include Directives

**Icon**: Upward arrow (↑)

Click to navigate to the fragment definition.

```berrycrush
include login-steps      # ↑ icon links to definition
```

### Step Keywords

**Icon**: Method icon

Click to navigate to the `@Step` annotated method.

```berrycrush
given I am logged in     # Method icon if @Step method exists
```

### Operation References

**Icon**: API icon

Click to navigate to the OpenAPI operation.

```berrycrush
call ^createUser         # API icon links to OpenAPI spec
```

## Structure View

View the document structure hierarchically.

### How to Open

- **View** → **Tool Windows** → **Structure**
- Or press `Cmd+7` (Mac) / `Alt+7` (Windows/Linux)

### Elements Shown

```
📄 my-test.scenario
  └─� scenario: Create user
      ├─ given I create a user
      │   └─ call ^createUser
      └─ then I verify the user
          ├─ assert status 200
          └─ extract $.id => userId
  └─📋 scenario: Delete user
      ├─ when I delete the user
      │   └─ call ^deleteUser
      └─ then I verify deletion
          └─ assert status 204
```

### Features

- Click to navigate to element
- Sort by name or position
- Filter by element type
- Collapse/expand sections

## File Structure Popup

Quick navigation popup for current file.

### How to Use

Press `Cmd+F12` (Mac) or `Ctrl+F12` (Windows/Linux)

### Features

- Shows all scenarios and fragments
- Type to filter by name
- Press Enter to navigate

## Quick Navigate

### Go to File

Press `Cmd+Shift+O` (Mac) or `Ctrl+Shift+N` (Windows/Linux)

Type `.scenario` or `.fragment` to filter BerryCrush files.

### Recent Files

Press `Cmd+E` (Mac) or `Ctrl+E` (Windows/Linux)

Shows recently opened files including scenarios and fragments.

### Go to Symbol

Press `Cmd+Alt+O` (Mac) or `Ctrl+Alt+Shift+N` (Windows/Linux)

Type fragment names to find them across the project.

## Breadcrumbs

Shows current location in the file structure at the top of the editor.

```
Feature: User Management > Scenario: Create user > Given I call ^createUser
```

Click any breadcrumb to navigate to that element.

## Back/Forward Navigation

Navigate through your navigation history:

- **Back**: `Cmd+[` (Mac) or `Ctrl+Alt+Left` (Windows/Linux)
- **Forward**: `Cmd+]` (Mac) or `Ctrl+Alt+Right` (Windows/Linux)

## Code Folding

Collapse sections to focus on relevant code.

### Fold/Unfold

- **Fold**: `Cmd+-` (Mac) or `Ctrl+-` (Windows/Linux)
- **Unfold**: `Cmd+=` (Mac) or `Ctrl+=` (Windows/Linux)
- **Fold All**: `Cmd+Shift+-` (Mac) or `Ctrl+Shift+-`
- **Unfold All**: `Cmd+Shift+=` (Mac) or `Ctrl+Shift+=`

### Foldable Regions

- Features
- Scenarios
- Scenario Outlines
- Backgrounds
- Fragments
- Examples
- Doc Strings

## Troubleshooting

### Navigation Not Working

1. Ensure the target file exists
2. Check file is in the correct location
3. Rebuild project indexes: **File** → **Invalidate Caches**

### Missing Gutter Icons

1. Check element syntax is correct
2. Verify target element exists
3. Wait for indexing to complete (status bar)

### Find Usages Shows No Results

1. Verify element name matches exactly (case-sensitive)
2. Check target files are indexed
3. Ensure files are in source roots
