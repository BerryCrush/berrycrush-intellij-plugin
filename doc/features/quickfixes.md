# Quick Fixes

Quick fixes provide one-click solutions for common issues detected by inspections.

## Using Quick Fixes

### Keyboard Shortcut

1. Place cursor on an error (red or yellow underline)
2. Press `Alt+Enter`
3. Select a fix from the popup menu
4. Press `Enter` to apply

### Light Bulb Icon

When available, a 💡 light bulb appears:
1. Click the light bulb
2. Select a fix
3. Apply

### Context Menu

1. Right-click on the error
2. Select **Show Context Actions**
3. Choose a fix

## Available Quick Fixes

### Create Missing Fragment

**Applies to**: Missing Fragment Reference inspection

**What it does**: Creates a new `.fragment` file with the fragment definition.

**Example**:
```berrycrush
include login-steps    # Error: Fragment 'login-steps' not found
                       # Quick fix: Create fragment 'login-steps'
```

**After applying**:
Creates `login-steps.fragment`:
```berrycrush
fragment: login-steps
  given placeholder step
    # TODO: add implementation
```

### Options

When creating a fragment:
- **File name**: Defaults to fragment name
- **Directory**: Choose location for new file
- **Template**: Basic fragment structure

## Intention Actions

Beyond error fixes, intention actions offer enhancements:

### Convert to Fragment

Select steps and convert to a reusable fragment.

**Before**:
```berrycrush
scenario: Test 1
  given I login
    call ^login
    extract $.token => token
  
scenario: Test 2
  given I login
    call ^login
    extract $.token => token
```

**After**:
```berrycrush
# login-fragment.fragment
fragment: login-steps
  given I login
    call ^login
    extract $.token => token

# test.scenario
scenario: Test 1
  given I am logged in
    include login-steps
  
scenario: Test 2
  given I am logged in
    include login-steps
```

### Inline Fragment

Replace `include` directive with fragment contents.

**Before**:
```berrycrush
scenario: Test
  given I am ready
    include simple-step

# simple-step.fragment
fragment: simple-step
  given I ping the server
    call ^ping
```

**After**:
```berrycrush
scenario: Test
  given I ping the server
    call ^ping
```

## Preview Changes

For significant changes:

1. Press `Alt+Enter`
2. Hold `Alt` and press `Enter` again
3. Preview shows before/after
4. Confirm or cancel

## Batch Apply

Apply the same fix to multiple occurrences:

1. Select the fix
2. Look for **Fix all 'X' problems in file**
3. Apply to all

## Custom Quick Fixes

The plugin provides quick fixes for BerryCrush-specific issues. For general code issues, use IntelliJ's built-in intentions.

## Keyboard Reference

| Action | Shortcut |
|--------|----------|
| Show quick fixes | `Alt+Enter` |
| Show more actions | `Alt+Enter` twice |
| Preview change | Hold `Alt`, press `Enter` |

## Tips

### Priority Order

Quick fixes appear in priority order:
1. Error fixes (most important)
2. Warning fixes
3. Improvement suggestions
4. Other intentions

### Undo

All quick fixes can be undone:
- `Cmd+Z` (Mac)
- `Ctrl+Z` (Windows/Linux)

### Learning Shortcuts

Press `Ctrl+Shift+A` (or `Cmd+Shift+A`) and search for action names to learn their shortcuts.
