# Installation

This guide explains how to install the BerryCrush IntelliJ Plugin.

## Requirements

- **IntelliJ IDEA** 2025.3 or later (Ultimate or Community Edition)
- **Java** 21 or later (for running BerryCrush tests)

## Install from ZIP File

### Step 1: Download the Plugin

Download the latest plugin ZIP file from the [GitHub Releases](https://github.com/berrycrush/berrycrush/releases) page.

Look for files named: `berrycrush-intellij-x.x.x.zip`

### Step 2: Install in IntelliJ IDEA

1. Open IntelliJ IDEA
2. Go to **Settings** (Ctrl+Alt+S on Windows/Linux, Cmd+, on macOS)
3. Navigate to **Plugins**
4. Click the **⚙️** (gear) icon
5. Select **Install Plugin from Disk...**
6. Select the downloaded ZIP file
7. Click **OK** and **Restart IDE** when prompted

### Step 3: Verify Installation

After restarting:

1. Create a new file with `.scenario` extension
2. The file should have a custom icon
3. Syntax highlighting should be active

## Install from JetBrains Marketplace

> **Coming Soon**: The plugin will be available on the JetBrains Marketplace.

Once available:

1. Open IntelliJ IDEA
2. Go to **Settings** → **Plugins**
3. Click **Marketplace** tab
4. Search for "BerryCrush"
5. Click **Install**
6. Restart IDE when prompted

## Updating the Plugin

### Manual Update

1. Download the new version ZIP file
2. Go to **Settings** → **Plugins**
3. Find "BerryCrush" in the **Installed** tab
4. Click **⚙️** → **Install Plugin from Disk...**
5. Select the new ZIP file
6. Restart IDE

### Marketplace Update (Future)

When installed from Marketplace, updates will be automatic.

## Uninstalling

1. Go to **Settings** → **Plugins**
2. Find "BerryCrush" in the **Installed** tab
3. Click the dropdown arrow next to "Disable"
4. Select **Uninstall**
5. Restart IDE

## Troubleshooting

### Plugin Not Loading

**Symptom**: No syntax highlighting, no custom file icons.

**Solutions**:
1. Verify IntelliJ version is 2025.3 or later
2. Check **Settings** → **Plugins** to ensure plugin is enabled
3. Try restarting IntelliJ IDEA
4. Check **Help** → **Show Log in Explorer/Finder** for errors

### Incompatibility Errors

**Symptom**: Error message about incompatible plugin.

**Solution**: Ensure you're using the correct plugin version for your IntelliJ version. Check the compatibility matrix in release notes.

### Java Not Found

**Symptom**: Test runner fails with "Java not found".

**Solutions**:
1. Ensure Java 21+ is installed
2. Set `JAVA_HOME` environment variable
3. Configure Java SDK in **Project Structure** → **Project** → **SDK**

## Platform Support

| Platform | Supported |
|----------|-----------|
| IntelliJ IDEA Ultimate | ✅ |
| IntelliJ IDEA Community | ✅ |
| Android Studio | ⚠️ Untested |
| Other JetBrains IDEs | ❌ |

## Next Steps

- [Getting Started](getting-started.md) - Write your first scenario
- [Features Overview](index.md#features) - Explore all features
