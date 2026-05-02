# Refactoring

The BerryCrush plugin provides refactoring support to safely rename and delete elements while keeping references in sync.

## Rename Fragment

Rename a fragment and automatically update all `include` references.

### How to Use

1. Place cursor on a fragment definition or reference
2. Press `Shift+F6` (or right-click → **Refactor** → **Rename**)
3. Type the new name
4. Press `Enter` to apply

### Example

**Before:**
```berrycrush
# login.fragment
fragment: login-steps
  given I have credentials
    call ^login

# test.scenario
scenario: Test
  given I am logged in
    include login-steps
```

**After renaming to `auth-steps`:**
```berrycrush
# login.fragment
fragment: auth-steps
  given I have credentials
    call ^login

# test.scenario
scenario: Test
  given I am logged in
    include auth-steps
```

### What Gets Updated

- Fragment definition line
- All `include` directives in `.scenario` files
- All `include` directives in other `.fragment` files

### Preview Changes

Press `Shift+F6` twice to open the rename dialog with preview.

The preview shows:
- All affected files
- Exact changes to be made
- Option to exclude specific occurrences

## Rename Variable

Rename a variable placeholder across the file.

### How to Use

1. Place cursor on a variable (e.g., `{{userId}}`)
2. Press `Shift+F6`
3. Type the new name
4. Press `Enter`

### Example

**Before:**
```berrycrush
scenario: Test
  given I have an ID
    id = "123"
  when I retrieve the user
    call ^getUser
      userId: {{id}}
  then I verify the ID
    assert $.id equals {{id}}
```

**After renaming `id` to `userId`:**
```berrycrush
scenario: Test
  given I have an ID
    userId = "123"
  when I retrieve the user
    call ^getUser
      userId: {{userId}}
  then I verify the ID
    assert $.id equals {{userId}}
```

### Scope

Variable rename is limited to the current file. Variables with the same name in other files are not affected.

## Safe Delete

Delete a fragment or fragment file while checking for usages.

### How to Use

1. Select the fragment element or file
2. Press `Cmd+Delete` (Mac) or `Delete` with `Safe Delete` option
3. If usages exist, review them in the dialog
4. Choose to delete anyway or cancel

### From File Tree

1. Right-click on a `.fragment` file
2. Select **Refactor** → **Safe Delete**
3. Review usages if any
4. Confirm deletion

### From Editor

1. Place cursor on `fragment:` definition line
2. Press `Cmd+Delete` (Mac) or equivalent
3. Review usages
4. Confirm deletion

### Usage Warning

When usages exist, you'll see:

```
Fragment 'login-steps' is included in:
  - test.scenario
  - integration.scenario
  - smoke.scenario

Delete anyway?
```

### Options

- **View Usages**: Open Find Usages to see details
- **Delete Anyway**: Proceed (will leave broken references)
- **Cancel**: Abort deletion

## Rename File

Rename a scenario or fragment file.

### How to Use

1. Right-click on file in Project view
2. Select **Refactor** → **Rename**
3. Type new name (extension will be preserved)
4. Press `Enter`

### Important Notes

- Renaming a `.fragment` file does NOT rename fragments inside it
- Fragment names are independent of file names
- Consider also renaming internal fragments to match

## Best Practices

### Before Renaming

1. Save all files
2. Commit current changes to version control
3. Preview changes before applying

### After Renaming

1. Run tests to verify nothing broke
2. Check version control diff
3. Commit the rename

### Naming Conventions

| Element | Convention | Example |
|---------|------------|---------|
| Fragment names | lowercase-with-dashes | `login-steps`, `create-user` |
| Variable names | camelCase | `userId`, `authToken` |
| File names | lowercase-with-dashes | `user-tests.scenario` |

## Troubleshooting

### Rename Not Finding All Usages

1. Ensure project is fully indexed
2. Check files are in source roots
3. Run **File** → **Invalidate Caches / Restart**

### Safe Delete Not Showing Usages

1. Verify include directives match exactly
2. Check for typos in fragment names
3. Ensure index is up to date

### Refactoring Option Disabled

1. Check cursor is on a renameable element
2. Ensure file is not read-only
3. Verify plugin is enabled

## Keyboard Shortcuts

| Action | Mac | Windows/Linux |
|--------|-----|---------------|
| Rename | `Shift+F6` | `Shift+F6` |
| Safe Delete | `Cmd+Delete` | `Delete` (with option) |
| Undo | `Cmd+Z` | `Ctrl+Z` |
| Redo | `Cmd+Shift+Z` | `Ctrl+Shift+Z` |
