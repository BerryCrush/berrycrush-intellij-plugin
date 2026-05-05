# Getting Started

This guide walks you through creating and running your first BerryCrush test scenario.

## Prerequisites

- [BerryCrush plugin installed](installation.md)
- A project with BerryCrush library configured
- An OpenAPI specification file (optional, but recommended)

## Project Setup

If you don't have a project yet, create one with BerryCrush dependencies:

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    testImplementation("org.berrycrush:berrycrush-junit:{version}")
}
```

### Maven

```xml
<dependency>
    <groupId>org.berrycrush</groupId>
    <artifactId>berrycrush-junit</artifactId>
    <version>{version}</version>
    <scope>test</scope>
</dependency>
```

## Create Your First Scenario

### Step 1: Create a Scenario File

1. Right-click on `src/test/resources` (or your test resources folder)
2. Select **New** → **File**
3. Name it `my-first-test.scenario`

### Step 2: Write a Simple Scenario

```berrycrush
scenario: Simple GET request
  when I retrieve a pet
    call ^getPetById
      petId: 1
  then the response is successful
    assert status 200
    assert $.name exists
```

### Step 3: Understand the Syntax

| Element | Description |
|---------|-------------|
| `scenario:` | A single test case |
| `given/when/then` | Step keywords (all equivalent, use for readability) |
| `^operationId` | Reference to an OpenAPI operation |
| `assert` | Validates the response |
| `$.path` | JSONPath expression |

## Syntax Highlighting

The plugin provides color coding for different elements:

- **Keywords** (scenario, given, when, then, etc.) - blue
- **Operation references** (^operationId) - orange
- **Variables** ({{varName}}) - green
- **Strings** ("...") - brown
- **Comments** (#...) - gray

## Using Navigation

### Go to Operation Definition

1. Hold `Cmd` (Mac) or `Ctrl` (Windows/Linux)
2. Click on an operation reference like `^getPetById`
3. Jump directly to the OpenAPI specification

### Find Usages

1. Place cursor on a fragment name
2. Press `Alt+F7`
3. See all files that include this fragment

## Creating Fragments

Fragments are reusable step sequences.

### Step 1: Create a Fragment File

Create `login.fragment`:

```berrycrush
fragment: login-as-admin
  given I have admin credentials
    call ^login
      body: {"username": "admin", "password": "secret"}
    assert status 200
    extract $.token => authToken
```

### Step 2: Use the Fragment

In your scenario file:

```berrycrush
scenario: Admin can view users
  given I am authenticated
    include login-as-admin
  when I list users
    call ^getUsers
      header_Authorization: "Bearer {{authToken}}"
  then the response is successful
    assert status 200
```

## Running Tests

BerryCrush tests are executed through JUnit. Create a test class that references your scenarios:

### Step 1: Create a Test Class

```kotlin
import org.berrycrush.junit.BerryCrushScenarios

@BerryCrushScenarios(locations = ["my-first-test.scenario"])
class MyFirstTest
```

### Step 2: Run from the Gutter

1. Click the green **▶** icon next to the class name
2. Select **Run 'MyFirstTest'**

### From the Keyboard

1. Place cursor inside the test class
2. Press `Ctrl+Shift+R` (Mac) or `Ctrl+Shift+F10` (Windows/Linux)

### Test Results Navigation

After tests run, **double-click** on a scenario name in the test results tree to navigate directly to the `.scenario` file.

!!! note "Gradle Projects: Configure IntelliJ Test Runner"
    For Gradle-based projects, you must configure IntelliJ to use its built-in
    test runner instead of Gradle for navigation to work.
    See [Test Runner Configuration](configuration/settings.md#test-runner-configuration)
    for details. Maven projects typically work without changes.

## Inspections

The plugin checks your code for issues:

| Inspection | What it checks |
|------------|----------------|
| Missing Fragment | Fragment name doesn't exist |
| Undefined Operation | Operation not in OpenAPI spec |
| Undefined Step | Custom step not defined |

Errors appear with red underlines. Hover to see the issue.

## Quick Fixes

Some issues have automatic fixes:

1. Place cursor on an error (red underline)
2. Press `Alt+Enter`
3. Select the quick fix

Example: "Create fragment 'login-steps'" automatically creates a new fragment file.

## Next Steps

- [Syntax Highlighting](features/syntax.md) - Detailed syntax reference
- [Navigation](features/navigation.md) - All navigation features
- [Test Runner](features/test-runner.md) - Advanced test execution
- [Refactoring](features/refactoring.md) - Rename and refactor

## Common Patterns

### API Test Pattern

```berrycrush
scenario: CRUD operations
  # Create
  given I create a new user
    call ^createUser
      body: {"name": "Test User"}
    assert status 201
    extract $.id => userId
  
  # Read
  when I retrieve the user
    call ^getUser
      id: {{userId}}
    assert status 200
    assert $.name equals "Test User"
  
  # Delete
  then I delete the user
    call ^deleteUser
      id: {{userId}}
    assert status 204
```

### Authentication Pattern

```berrycrush
# auth.fragment
fragment: authenticate
  given I have credentials
    call ^login
      body: {"email": "{{email}}", "password": "{{password}}"}
    assert status 200
    extract $.token => token
```

```berrycrush
# test.scenario
scenario: Authenticated request
  given I setup credentials
    email = "user@example.com"
    password = "secret"
  and I am authenticated
    include authenticate
  when I access protected resource
    call ^protectedEndpoint
      header_Authorization: "Bearer {{token}}"
  then the response is successful
    assert status 200
```
