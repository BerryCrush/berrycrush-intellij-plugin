# Test Runner

The BerryCrush plugin integrates with IntelliJ's test runner for seamless test execution.

## Running Tests

### From Gutter Icons

Click the green play button (▶) that appears next to:
- `scenario:` - Run a single scenario

### From Context Menu

1. Right-click inside a `.scenario` file
2. Select **Run 'filename.scenario'**

### From Keyboard

| Action | Mac | Windows/Linux |
|--------|-----|---------------|
| Run current | `Ctrl+Shift+R` | `Ctrl+Shift+F10` |
| Debug current | `Ctrl+Shift+D` | `Ctrl+Shift+F9` |
| Run last | `Ctrl+R` | `Shift+F10` |
| Debug last | `Ctrl+D` | `Shift+F9` |

## Run Configurations

### Automatic Creation

The plugin automatically creates run configurations when you:
- Click a gutter icon
- Use **Run** context menu
- Use keyboard shortcuts

### Manual Creation

1. **Run** → **Edit Configurations**
2. Click **+** → **BerryCrush**
3. Configure options
4. Save

### Configuration Options

| Option | Description |
|--------|-------------|
| **File** | `.scenario` or `.fragment` file to run |
| **Scenario** | Specific scenario name (optional) |
| **Tags** | Filter by tags (e.g., `@smoke`) |
| **OpenAPI** | Path to OpenAPI spec |
| **Environment** | Environment variables |
| **Working Directory** | Execution directory |

## Test Results

### Run Tool Window

After execution, view results in **Run** tool window:

- ✅ Green check: Test passed
- ❌ Red X: Test failed
- ⏭️ Skip icon: Test skipped

### Features

- Click test name to jump to source
- View full output in console
- Re-run failed tests only
- Export results

### Output Console

The console shows:
- HTTP requests sent
- Response status codes
- Assertion results
- Variable extractions
- Error messages

## Debugging

### Enable Debug Mode

1. Click the debug icon (🐛) instead of run
2. Or use `Ctrl+Shift+D` / `Ctrl+Shift+F9`

### Debugging Features

- See HTTP request/response bodies
- View variable values
- Step-by-step execution (if configured)

## Filtering Tests

### By Tags

Run scenarios with specific tags:

```berrycrush
@smoke @critical
scenario: Important test
  ...

@regression
scenario: Regular test
  ...
```

Run command:
```
--tags @smoke
```

### By Scenario Name

Specify scenario name in run configuration:
- Exact match: `Create user`
- Pattern: `*user*`

### By File

Select specific `.scenario` file in configuration.

## Environment Variables

Set environment variables for tests:

1. Edit run configuration
2. Click **Environment variables**
3. Add key-value pairs

Example:
```
BASE_URL=https://api.test.com
API_KEY=test-key-123
```

Access in scenarios:
```berrycrush
given base URL is {{env.BASE_URL}}
when I call ^endpoint with header:
  | X-API-Key | {{env.API_KEY}} |
```

## Multiple Configurations

Create configurations for different environments:

- **Local Tests**: `BASE_URL=http://localhost:8080`
- **Staging Tests**: `BASE_URL=https://staging.api.com`
- **Production Tests**: `BASE_URL=https://api.com`

Quick switch between configurations in the toolbar dropdown.

## Continuous Testing

### Auto-Rerun

IntelliJ can automatically rerun tests on file save:

1. In **Run** window, click **Toggle auto-test**
2. Tests rerun when files change

### Watch Mode

Keep test runner active:
1. Run tests
2. Modify scenario file
3. Tests automatically re-execute

## Parallel Execution

For faster execution (if supported by BerryCrush):

In run configuration:
- Set **Parallel execution** option
- Specify number of threads

## Troubleshooting

### Tests Not Starting

1. Verify BerryCrush library is in classpath
2. Check Java SDK is configured
3. Ensure file is in test source root

### Can't Find Scenarios

1. Check file extension is `.scenario`
2. Verify file is in indexed directory
3. Rebuild project

### Connection Errors

1. Verify target server is running
2. Check `BASE_URL` configuration
3. Review firewall/proxy settings

### Assertion Failures

1. Click failed assertion to see details
2. Review expected vs actual values
3. Check JSONPath expressions

## Best Practices

### Organize by Feature

```
src/test/resources/
├── features/
│   ├── user/
│   │   ├── create-user.scenario
│   │   └── delete-user.scenario
│   └── auth/
│       ├── login.scenario
│       └── logout.scenario
└── fragments/
    ├── auth/
    │   └── login-steps.fragment
    └── common/
        └── setup.fragment
```

### Use Tags Effectively

```berrycrush
@smoke           # Quick validation
@regression      # Full test suite
@wip             # Work in progress (skip in CI)
@critical        # Must-pass tests
```

### Keep Tests Fast

- Use fragments for common setup
- Minimize unnecessary API calls
- Clean up test data efficiently
