# OpenAPI Integration

The BerryCrush plugin integrates with OpenAPI specifications to provide intelligent code assistance for API operations.

## Overview

When you reference an API operation with `^operationId`, the plugin:
- Validates the operation exists
- Provides autocomplete for operation IDs
- Shows operation details on hover
- Navigates to the OpenAPI definition

## OpenAPI File Detection

The plugin uses content-based detection to find OpenAPI specification files.

### Detection Method

The plugin scans all `.yaml`, `.yml`, and `.json` files in the project and checks their content for OpenAPI markers:

| Marker | Version |
|--------|---------|
| `openapi: 3.x` | OpenAPI 3.x (YAML) |
| `swagger: 2.x` | Swagger/OpenAPI 2.x (YAML) |
| `"openapi": "3.x"` | OpenAPI 3.x (JSON) |
| `"swagger": "2.x"` | Swagger/OpenAPI 2.x (JSON) |

### Example Detection

A file is recognized as an OpenAPI spec if it contains:

**YAML format:**
```yaml
openapi: 3.1.0
info:
  title: My API
```

**JSON format:**
```json
{
  "openapi": "3.0.0",
  "info": { "title": "My API" }
}
```

### Filename Hints

Files with these patterns are checked first (optimization):
- Filenames containing `openapi` or `swagger`
- `api.yaml`, `api.yml`, `api.json`

## Referencing Operations

Use the `^` prefix to reference an OpenAPI operation:

```berrycrush
scenario: Create a user
  when I create a new user
    call ^createUser
      body: {"name": "John"}
    assert status 201
```

The `^createUser` references the operation with `operationId: createUser` in your OpenAPI spec.

## Autocompletion

### Operation IDs

Type `^` and press `Ctrl+Space`:

```berrycrush
call ^    # Shows: createUser, getUser, deleteUser, ...
```

### Parameters

After selecting an operation, get parameter suggestions:

```berrycrush
call ^getUser
  #    Shows: userId, ...
```

### Response Codes

For assertions:

```berrycrush
assert status     # Shows: 200, 201, 400, 404, ...
```

## Hover Information

Hover over `^operationId` to see:

- Operation summary
- HTTP method and path
- Parameters (path, query, header, body)
- Response codes
- Description

Example tooltip:
```
getUser
GET /users/{userId}

Parameters:
- userId (path, required): The user ID

Responses:
- 200: User found
- 404: User not found
```

## Navigation

### Go to Definition

1. Hold `Ctrl` / `Cmd` and click on `^operationId`
2. Opens the OpenAPI file at the operation definition

### Find Usages

1. Place cursor on operation ID in OpenAPI file
2. Press `Alt+F7`
3. See all scenarios using this operation

## Multiple OpenAPI Files

### Per-Scenario Configuration

Associate specific OpenAPI files with scenarios:

```berrycrush
# openapi: ./api/users.openapi.yaml
scenario: User Management
  ...
```

### Project-Wide Configuration

In `.berrycrush.yaml` or project settings:

```yaml
openapi:
  - path: src/main/resources/openapi/main-api.yaml
    prefix: api
  - path: src/test/resources/mock-api.yaml
    prefix: mock
```

## Validation

### Missing Operation Warning

If an operation doesn't exist in any OpenAPI file:

```berrycrush
call ^nonExistent    # Warning: Unknown operation 'nonExistent'
```

### Parameter Validation

The plugin validates required parameters:

```berrycrush
call ^getUser    # Warning: Missing required path parameter 'userId'
```

## OpenAPI Versions

Supported versions:

| Version | Support |
|---------|---------|
| OpenAPI 3.1 | Full |
| OpenAPI 3.0 | Full |
| Swagger 2.0 | Partial |

## Schema Support

### Request Body Completion

For operations with a request body:

```berrycrush
call ^createUser
  body:       # Shows fields from User schema
    name: "value"
    email: "value"
```

### Response Assertions

Use JSONPath to assert response fields:

```berrycrush
assert $.name equals "John"    # Validated against response schema
```

## Troubleshooting

### Operations Not Found

1. Verify OpenAPI file is in a recognized location
2. Check file follows naming conventions
3. Ensure `operationId` is defined for each operation
4. Invalidate caches and restart IDE

### Autocomplete Not Working

1. Ensure OpenAPI file is valid (no syntax errors)
2. Check file is indexed
3. Verify plugin is enabled
4. Look for parsing errors in Event Log

### Wrong OpenAPI File Used

1. Check file proximity rules
2. Use explicit `# openapi:` directive
3. Configure project-level settings

### Schema Parsing Errors

1. Validate OpenAPI file with external tool
2. Check for circular references
3. Ensure all `$ref` references are valid

## Best Practices

### Operation Naming

Use descriptive, consistent operation IDs:

```yaml
# Good
operationId: createUser
operationId: getUserById
operationId: updateUserEmail

# Avoid
operationId: create
operationId: user
operationId: postUserUpdate
```

### Organization

```
project/
├── src/
│   ├── main/
│   │   └── resources/
│   │       └── openapi/
│   │           ├── api.openapi.yaml
│   │           └── schemas/
│   │               └── user.yaml
│   └── test/
│       └── resources/
│           └── scenarios/
│               └── user-tests.scenario
```

### Version Control

Keep OpenAPI files in version control alongside scenarios:
- Scenarios reference operations that exist
- Changes to API require scenario updates
- CI can verify consistency

## Advanced Features

### Schema Inheritance

The plugin understands `allOf`, `oneOf`, and `anyOf` for autocomplete:

```yaml
User:
  allOf:
    - $ref: '#/components/schemas/Person'
    - type: object
      properties:
        role:
          type: string
```

### External References

Supports `$ref` to external files:

```yaml
$ref: './schemas/user.yaml#/components/schemas/User'
```

### Discriminators

For polymorphic schemas, autocomplete shows all variants.
