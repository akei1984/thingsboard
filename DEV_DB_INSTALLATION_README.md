# Development Database Installation - Fast Reference

## Problem
`install_dev_db.sh` was writing logs to `/var/log/thingsboard` which is not writable in development environments.

## Solution
Now supports configurable log directories via `LOG_DIR` environment variable.

## 30-Second Quick Start

```bash
# Option 1: Default (logs to target/logs)
./packaging/java/scripts/install/install_dev_db.sh

# Option 2: Custom directory
export LOG_DIR="/tmp/thingsboard-install"
./packaging/java/scripts/install/install_dev_db.sh

# Option 3: One-liner
LOG_DIR="/tmp/tb-logs" ./packaging/java/scripts/install/install_dev_db.sh
```

## How It Works

1. Script checks `LOG_DIR` environment variable
2. If not set, defaults to `${project.basedir}/target/logs`
3. Auto-creates directory if needed
4. Passes path to Java as `-Dinstall.logFolder=<path>`
5. Logback uses this instead of `/var/log/thingsboard`

## Documentation

- **Quick Start**: [QUICK_START_DEV_DB.md](./QUICK_START_DEV_DB.md)
- **Full Guide**: [INSTALL_DEV_DB_GUIDE.md](./INSTALL_DEV_DB_GUIDE.md)
- **Technical**: [LOG_DIR_CONFIGURATION_SUMMARY.md](./LOG_DIR_CONFIGURATION_SUMMARY.md)
- **Checklist**: [IMPLEMENTATION_CHECKLIST.md](./IMPLEMENTATION_CHECKLIST.md)

## What Changed

✅ Modified 2 files (6 lines of code)
- `packaging/java/scripts/install/install_dev_db.sh` - Added LOG_DIR support
- `packaging/java/scripts/install/logback.xml` - Added runtime override

✅ Created 4 documentation files (500+ lines)
- Quick start guide
- Comprehensive guide
- Technical summary
- Implementation checklist

## Features

✅ **Zero Configuration** - Works immediately with defaults
✅ **Environment Variable** - Easy override with LOG_DIR
✅ **Auto-Create** - Script creates directory if missing
✅ **Backward Compatible** - No changes to production builds
✅ **Well Documented** - Multiple levels of documentation

## Common Use Cases

### Development (default)
```bash
./packaging/java/scripts/install/install_dev_db.sh
# Logs → target/logs/install.log
```

### CI/CD Pipeline
```bash
export LOG_DIR="/var/tmp/ci-logs"
./packaging/java/scripts/install/install_dev_db.sh
# Logs → /var/tmp/ci-logs/install.log
```

### Docker/Container
```bash
export LOG_DIR="/logs"
./packaging/java/scripts/install/install_dev_db.sh
# Logs → /logs/install.log
```

### Project-Local
```bash
LOG_DIR="./build/logs" ./packaging/java/scripts/install/install_dev_db.sh
# Logs → ./build/logs/install.log
```

## Troubleshooting

**Still getting permission errors?**
```bash
mkdir -p /tmp/thingsboard-logs
chmod 755 /tmp/thingsboard-logs
export LOG_DIR="/tmp/thingsboard-logs"
./packaging/java/scripts/install/install_dev_db.sh
```

**Can't find logs?**
```bash
# Check default location
cat target/logs/install.log

# Or search everywhere
find . -name "install*.log" -type f 2>/dev/null
```

## Commits

- `01a5fdedc4` - Core implementation
- `77db07ba9f` - Quick start guide
- `024fb86c0c` - Technical summary
- `d7ed8859a0` - Implementation checklist

## Status

✅ **Complete and Ready to Use**
- Tested and verified
- Fully backward compatible
- Production ready
- Well documented

---

For more details, see the documentation files listed above.
