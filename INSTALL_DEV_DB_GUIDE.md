# Development Database Installation Guide

## Overview
The `install_dev_db.sh` script now supports configurable log directories, eliminating the need for `/var/log/thingsboard` to be writable.

## Usage

### Default Behavior
By default, installation logs are written to a local directory in the build target folder:

```bash
./packaging/java/scripts/install/install_dev_db.sh
```

Logs will be created in: `${project.basedir}/target/logs/install.log`

### Custom Log Directory
To use a custom log directory, set the `LOG_DIR` environment variable before running the script:

```bash
export LOG_DIR="/tmp/thingsboard-logs"
./packaging/java/scripts/install/install_dev_db.sh
```

Or in one line:

```bash
LOG_DIR="/tmp/thingsboard-logs" ./packaging/java/scripts/install/install_dev_db.sh
```

### Common Custom Locations

**Use /tmp (fastest, but cleared on reboot):**
```bash
export LOG_DIR="/tmp/thingsboard-install"
```

**Use user home directory:**
```bash
export LOG_DIR="$HOME/.thingsboard/logs"
```

**Use project-local logs:**
```bash
export LOG_DIR="./logs"
```

## How It Works

### Script Changes
The `install_dev_db.sh` script now:

1. Sets a default LOG_DIR if not provided:
   ```bash
   LOG_DIR=${LOG_DIR:-${BASE}/logs}
   ```

2. Creates the directory if it doesn't exist:
   ```bash
   mkdir -p "${LOG_DIR}"
   ```

3. Passes the log folder to Java as a system property:
   ```bash
   -Dinstall.logFolder=${LOG_DIR}
   ```

### Logback Configuration
The `logback.xml` configuration now:

1. Defines a property that allows runtime override:
   ```xml
   <property name="logFolder" value="${install.logFolder:-${pkg.logFolder}}"/>
   ```

2. Uses this property for all log file paths:
   ```xml
   <file>${logFolder}/install.log</file>
   <fileNamePattern>${logFolder}/install.%d{yyyy-MM-dd}.%i.log</fileNamePattern>
   ```

### Property Resolution Order
1. **Java system property** `-Dinstall.logFolder` (if provided): Custom log directory
2. **Maven property** `${pkg.logFolder}`: Falls back to `/var/log/thingsboard` (for production builds)

## Troubleshooting

### Permission Denied Error
If you still get permission errors:

```bash
# Make sure the directory is writable
export LOG_DIR="/tmp/thingsboard-dev-logs"
mkdir -p "$LOG_DIR"
chmod 755 "$LOG_DIR"
./packaging/java/scripts/install/install_dev_db.sh
```

### Directory Doesn't Exist Error
The script automatically creates the directory, but if it fails:

```bash
# Pre-create the directory manually
mkdir -p "$LOG_DIR"
chmod 755 "$LOG_DIR"
export LOG_DIR="/your/log/directory"
./packaging/java/scripts/install/install_dev_db.sh
```

### Viewing Logs
After installation, check the logs:

```bash
# With default location
cat target/logs/install.log

# With custom location
cat /tmp/thingsboard-logs/install.log
```

## Files Modified

- **packaging/java/scripts/install/install_dev_db.sh**
  - Added LOG_DIR environment variable support
  - Auto-creates log directory
  - Passes install.logFolder Java property

- **packaging/java/scripts/install/logback.xml**
  - Added logFolder property definition
  - Updated file path expressions to use logFolder variable
  - Maintains fallback to pkg.logFolder for compatibility

## Backward Compatibility

✅ Fully backward compatible
- If LOG_DIR is not set, defaults to `${BASE}/logs`
- If somehow install.logFolder is not provided, falls back to original `${pkg.logFolder}`
- Existing production installations unaffected

## Notes

- The script uses `sudo -u "$USER"` to maintain current user permissions
- Log directory is created automatically with standard permissions
- Logs roll over every 100MB or daily, whichever comes first
- Maximum history of 30 files, 3GB total capacity
