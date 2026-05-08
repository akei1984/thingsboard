# Quick Start: Installing Development Database

## The Problem
`install_dev_db.sh` was trying to write logs to `/var/log/thingsboard`, which may not be writable in development environments.

## The Solution
Use a configurable log directory via environment variable.

## 3-Minute Quick Start

### Default (uses `target/logs`)
```bash
cd /Users/alexanderkeidel/Dev/thingsboard
./packaging/java/scripts/install/install_dev_db.sh
```

### Custom Location (recommended for most cases)
```bash
export LOG_DIR="/tmp/thingsboard-install"
./packaging/java/scripts/install/install_dev_db.sh
```

### One-Liner
```bash
LOG_DIR="/tmp/tb-logs" ./packaging/java/scripts/install/install_dev_db.sh
```

## Verify It Works

After running the script, check for logs:
```bash
# If using default location
cat target/logs/install.log

# If using custom location
cat /tmp/thingsboard-install/install.log
```

## Environment Variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `LOG_DIR` | `${project.basedir}/target/logs` | Directory for installation logs |

## Common Locations

When setting `LOG_DIR`, use:

```bash
# System temp (fast, ephemeral)
export LOG_DIR="/tmp/thingsboard-dev-logs"

# Project directory (persistent)
export LOG_DIR="./logs"

# Home directory (persistent)
export LOG_DIR="~/.thingsboard/logs"
```

## How It Works

1. **Script detects** `LOG_DIR` environment variable
2. **Creates** the directory if needed
3. **Passes** it to Java as `-Dinstall.logFolder=<path>`
4. **Logback** uses this instead of `/var/log/thingsboard`

## Troubleshooting

**Still getting permission errors?**
```bash
mkdir -p /tmp/thingsboard-install
chmod 755 /tmp/thingsboard-install
export LOG_DIR="/tmp/thingsboard-install"
./packaging/java/scripts/install/install_dev_db.sh
```

**Can't find logs?**
```bash
# Check where logs are being written
find . -name "install*.log" -type f 2>/dev/null
find /tmp -name "install*.log" -type f 2>/dev/null
```

## What Changed

✅ `packaging/java/scripts/install/install_dev_db.sh`
- Now supports `LOG_DIR` environment variable
- Auto-creates log directory
- Passes log path to Java

✅ `packaging/java/scripts/install/logback.xml`
- Now reads runtime log folder from Java property
- Falls back to default if not provided

✅ **No build changes needed** - Already shipped!

## Full Documentation

See [INSTALL_DEV_DB_GUIDE.md](./INSTALL_DEV_DB_GUIDE.md) for comprehensive details.
