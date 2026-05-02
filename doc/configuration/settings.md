# Settings

Configure the BerryCrush plugin to match your workflow and project needs.

## Accessing Settings

1. **Mac**: IntelliJ IDEA → **Settings** (or `Cmd+,`)
2. **Windows/Linux**: **File** → **Settings** (or `Ctrl+Alt+S`)
3. Navigate to **Languages & Frameworks** → **BerryCrush**

## General Settings

### OpenAPI Detection

Configure how the plugin finds OpenAPI files:

| Setting | Description | Default |
|---------|-------------|---------|
| **Auto-detect** | Search standard locations | Enabled |
| **Search parent directories** | Look in parent folders | Enabled |
| **Max depth** | How many levels up to search | 5 |

### Custom OpenAPI Paths

Add explicit paths to OpenAPI files:

```
src/main/resources/openapi/api.yaml
api/swagger.json
```

Supports glob patterns:
```
**/openapi/*.yaml
src/**/api.*.json
```

## Editor Settings

### Syntax Highlighting

Customize colors at **Settings** → **Editor** → **Color Scheme** → **BerryCrush**:

| Element | Description |
|---------|-------------|
| **Keyword** | Given, When, Then, And, But |
| **Fragment Name** | Fragment definition names |
| **Operation Reference** | `^operationId` references |
| **Variable** | `{{variable}}` placeholders |
| **Comment** | `#` comments |
| **String** | Quoted text |
| **Number** | Numeric values |
| **Table Delimiter** | Pipe characters in tables |

### Fonts

BerryCrush files use editor font settings:

**Settings** → **Editor** → **Font**

### Line Numbers

Toggle line numbers in **Settings** → **Editor** → **General** → **Appearance**

## Code Style

### Indentation

**Settings** → **Editor** → **Code Style** → **BerryCrush**

| Setting | Default |
|---------|---------|
| Tab size | 2 |
| Indent size | 2 |
| Continuation indent | 4 |
| Use tabs | No |

### Tables

| Setting | Default |
|---------|---------|
| Align pipe characters | Yes |
| Pad cell content | Yes |
| Trim whitespace | Yes |

## Inspection Settings

### Configure Inspections

**Settings** → **Editor** → **Inspections** → **BerryCrush**

Available inspections:

| Inspection | Default | Severity |
|------------|---------|----------|
| Missing fragment reference | Enabled | Error |
| Unknown OpenAPI operation | Enabled | Warning |
| Undefined custom step | Disabled | Weak Warning |
| Undefined assertion | Disabled | Weak Warning |

### Scope

Apply inspections to specific scopes:
- **All Files**: Check everywhere
- **Project Files**: Exclude libraries
- **Custom Scope**: Define your own

## Live Templates

### Built-in Templates

Type abbreviation and press `Tab`:

| Abbreviation | Expansion |
|--------------|-----------|
| `sc` | Scenario template |
| `fr` | Fragment template |
| `fe` | Feature template |
| `gi` | Given step |
| `wh` | When step |
| `th` | Then step |
| `ta` | Data table |
| `in` | Include directive |

### Custom Templates

Create your own at **Settings** → **Editor** → **Live Templates** → **BerryCrush**:

1. Click **+** → **Live Template**
2. Set abbreviation and description
3. Enter template text with variables
4. Set applicable context to "BerryCrush"

Example template:
```berrycrush
scenario: $NAME$
  given $PRECONDITION$
    call ^$OPERATION$
  then verify response
    assert status $STATUS$
$END$
```

## File Templates

### Create New Templates

**Settings** → **Editor** → **File and Code Templates**

Default `.scenario` template:
```berrycrush
scenario: ${SCENARIO_NAME}
  given base URL is set
    baseUrl = "${BASE_URL}"
  when I call the operation
    call ^${OPERATION}
  then verify response
    assert status 200
```

Default `.fragment` template:
```berrycrush
fragment: ${NAME}
  given ${STEP}
    # TODO: add implementation
```

## Project Settings

### Per-Project Configuration

Create `.berrycrush.yaml` in project root:

```yaml
# OpenAPI configuration
openapi:
  paths:
    - src/main/resources/openapi/*.yaml
  auto-detect: true
  
# Default values
defaults:
  base-url: http://localhost:8080
  
# Feature flags
features:
  strict-mode: false
  validate-schemas: true
```

### Share Settings

Share settings with team via version control:

1. Create `.idea/berrycrush.xml` with settings
2. Commit to repository
3. Team members get same configuration

## IDE Settings

### Memory Allocation

For large projects, increase memory:

**Help** → **Edit Custom VM Options**

```
-Xmx4096m
-Xms1024m
```

### Index Settings

Exclude directories from indexing if not needed:

**Settings** → **Project Structure** → **Modules** → **Excluded**

## Export/Import Settings

### Export

1. **File** → **Manage IDE Settings** → **Export Settings**
2. Select "BerryCrush" settings
3. Save to file

### Import

1. **File** → **Manage IDE Settings** → **Import Settings**
2. Select the exported file
3. Choose "BerryCrush" settings
4. Restart IDE if prompted

## Troubleshooting

### Settings Not Applied

1. Check correct scope (project vs global)
2. Restart IDE
3. Invalidate caches

### Missing Options

1. Verify plugin is installed and enabled
2. Check plugin version compatibility
3. Update plugin to latest version

### Reset to Defaults

1. Go to specific settings section
2. Click **Reset** or ⟲ icon
3. Confirm reset
