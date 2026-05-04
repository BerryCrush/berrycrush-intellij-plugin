# Test Runner

The BerryCrush plugin integrates with IntelliJ's JUnit test runner for seamless test execution.

## Overview

BerryCrush tests run via IntelliJ's native JUnit runner. The plugin registers a `testFramework` extension that recognizes BerryCrush test classes and enables JUnit integration.

### How It Works

1. Classes annotated with `@BerryCrushScenarios` or `@BerryCrushSpec` are recognized as test classes
2. Gutter icons appear for running tests
3. Clicking the gutter creates a JUnit run configuration
4. IntelliJ's JUnit runner executes the tests
5. Test results appear in the Run tool window with navigation support

## Running Tests

### From Gutter Icons

Click the green play button (▶) that appears next to:
- Test classes annotated with `@BerryCrushScenarios` or `@BerryCrushSpec`
- Methods annotated with `@ScenarioTest`

### From Context Menu

1. Right-click on a test class or method
2. Select **Run 'ClassName'** or **Run 'methodName()'**

### From Keyboard

| Action | Mac | Windows/Linux |
|--------|-----|---------------|
| Run current | `Ctrl+Shift+R` | `Ctrl+Shift+F10` |
| Debug current | `Ctrl+Shift+D` | `Ctrl+Shift+F9` |
| Run last | `Ctrl+R` | `Shift+F10` |
| Debug last | `Ctrl+D` | `Shift+F9` |

## Run Configurations

The plugin uses IntelliJ's native JUnit run configurations. When you click a gutter icon, a JUnit configuration is created automatically.

### Manual Creation

1. **Run** → **Edit Configurations**
2. Click **+** → **JUnit**
3. Select the test class or method
4. Save

## Test Results

### Run Tool Window

After execution, view results in **Run** tool window:

- ✅ Green check: Test passed
- ❌ Red X: Test failed
- ⏭️ Skip icon: Test skipped

### Navigation

**Double-click** on a scenario name in the test results tree to navigate directly to the `.scenario` file at the corresponding line.

This works because BerryCrush provides `FileSource` information to the JUnit Platform, which IntelliJ's test runner uses for navigation.

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
- Set breakpoints in step implementations

## Filtering Tests

### By Tags

Use `@BerryCrushTags` annotation to filter scenarios:

```kotlin
@BerryCrushScenarios(locations = ["scenarios/*.scenario"])
@BerryCrushTags(include = ["smoke"], exclude = ["slow"])
class SmokeTests

### By Scenario Location

Use `@BerryCrushScenarios` to specify which scenarios to run:

```kotlin
@BerryCrushScenarios(locations = ["scenarios/auth/*.scenario"])
class AuthTests
```

## Environment Variables

Configure test properties in your test class:

```kotlin
@BerryCrushSpec(paths = ["petstore.yaml"], baseUrl = "http://localhost:8080")
class PetstoreTests {
    // ...
}
```

Or use JUnit's `@BeforeEach` with `BerryCrushConfiguration`:

```kotlin
@BerryCrushSpec(paths = ["petstore.yaml"])
class PetstoreTests {
    @BeforeEach
    fun setup(config: BerryCrushConfiguration) {
        config.baseUrl = System.getenv("BASE_URL") ?: "http://localhost:8080"
    }
}

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
