# Syntax Highlighting

The BerryCrush plugin provides rich syntax highlighting for `.scenario` and `.fragment` files.

## File Types

### Scenario Files (`.scenario`)

Used for test scenarios containing features, scenarios, and test steps.

**Icon**: ![Scenario Icon](images/scenario-icon.png)

### Fragment Files (`.fragment`)

Used for reusable step fragments that can be included in scenarios.

**Icon**: ![Fragment Icon](images/fragment-icon.png)

## Color Scheme

The plugin uses distinct colors for different syntax elements:

| Element | Default Color | Example |
|---------|---------------|---------|
| Block Keywords | Blue | `scenario:`, `fragment:` |
| Step Keywords | Blue | `given`, `when`, `then`, `and`, `but` |
| Directives | Purple | `assert`, `extract`, `include`, `call` |
| Operation References | Orange | `^createUser`, `^getPetById` |
| Variables | Green | `{{userId}}`, `{{authToken}}` |
| Strings | Brown | `"value"`, `'value'` |
| Numbers | Magenta | `200`, `3.14` |
| JSONPath | Cyan | `$.name`, `$.items[0].id` |
| Comments | Gray | `# This is a comment` |
| Tags | Yellow | `@smoke`, `@critical` |

## Customizing Colors

You can customize the colors in IntelliJ settings:

1. Go to **Settings** → **Editor** → **Color Scheme** → **BerryCrush**
2. Select an element type
3. Modify foreground/background colors
4. Apply changes

## Block Keywords

These keywords start new blocks:

```berrycrush
scenario: Create user
  given ...

fragment: setup-steps         # Fragment block (in .fragment files)
  given ...
```

## Step Keywords

Step keywords are interchangeable (they have the same behavior):

```berrycrush
given precondition step
  call ^createUser
when action step
  call ^getUser
then assertion step
  assert status 200
and additional step
  assert $.name exists
but contrasting step
  assert $.deleted equals false
```

## Directives

Special keywords that perform actions:

```berrycrush
call ^operationId                    # API call
assert status 200                    # Assertion
assert $.name equals "John"
extract $.id => userId               # Extract value
include login-fragment               # Include fragment
body: {"key": "value"}               # Inline body
```

## References

### Operation References

References to OpenAPI operations (clickable for navigation):

```berrycrush
call ^createUser              # Operation: createUser
call ^pet.findByStatus        # Namespaced operation
```

### Fragment References

References to fragment definitions:

```berrycrush
include login-steps           # Include fragment named "login-steps"
include ^setup                # Caret prefix optional
```

### Parameterized Fragment Includes

Fragments can accept parameters as indented key-value pairs:

```berrycrush
include create_user
  name: "John Doe"
  email: "john@example.com"
  age: 30
```

Parameters support various value types:

```berrycrush
include configure_entity
  name: "Fluffy"              # String value
  count: 123                   # Number value
  active: true                 # Boolean value
  data: {"key": "val"}        # JSON object
  tags: ["a", "b"]            # JSON array
  owner: {{currentUser}}       # Variable reference
```

## Variables

Dynamic values using mustache syntax:

```berrycrush
given I have a user ID
  userId = "123"
when I retrieve the user
  call ^getUser
    id: {{userId}}                  # Variable reference
  extract $.name => name
  assert $.greeting equals "Hello {{name}}"  # Interpolation in string
```

## Comments

Single-line comments start with `#`:

```berrycrush
# This is a full-line comment
scenario: My Test  # Inline comment

# Multi-line comments:
# Each line needs
# its own hash symbol
```

## Data Tables

Key-value pairs for parameters:

```berrycrush
given I create a user
  call ^createUser
    name: John Doe
    email: john@example.com
    role: admin
```

## Doc Strings

Multi-line strings using triple quotes:

```berrycrush
given I create a user
  call ^createUser
    body:
      """
      {
        "name": "John",
        "email": "john@example.com"
      }
      """
```

## Tags

Tags for categorization and filtering:

```berrycrush
@smoke @critical
scenario: Login flow
  ...
```

## JSONPath Expressions

Expressions for accessing JSON values:

```berrycrush
assert $.name equals "John"         # Root property
assert $.users[0].name exists        # Array index
assert $.items[*].id exists          # All items
extract $..id => allIds              # Deep search
```

## Troubleshooting

### Highlighting Not Working

1. Verify file extension is `.scenario` or `.fragment`
2. Check plugin is enabled in **Settings** → **Plugins**
3. Try **File** → **Invalidate Caches / Restart**

### Colors Look Wrong

1. Check your color scheme supports the plugin
2. Reset to defaults: **Settings** → **Editor** → **Color Scheme** → **BerryCrush** → Reset

### Specific Elements Not Highlighted

Some elements require valid syntax. Check for:
- Missing colons after keywords
- Unclosed strings or brackets
- Invalid JSON in doc strings
