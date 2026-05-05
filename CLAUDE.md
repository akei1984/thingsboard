# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build

**Full build (all modules, skip packaging):**
```bash
MAVEN_OPTS="-Xmx1024m" NODE_OPTIONS="--max_old_space_size=4096" \
  mvn -T6 license:format clean install -DskipTests -Dpkg.skip=true
```

**Rebuild specific modules only:**
```bash
./build.sh msa/tb-node,msa/web-ui
```

**Compile Protobuf definitions:**
```bash
./build_proto.sh
```

**Frontend dev server (hot reload):**
```bash
cd ui-ngx && npm start
```

**Frontend production build:**
```bash
cd ui-ngx && npm run build:prod
```

## Testing

Set these env vars before running tests to avoid OOM:
```bash
export MAVEN_OPTS="-Xmx1024m"
export NODE_OPTIONS="--max_old_space_size=4096"
export SUREFIRE_JAVA_OPTS="-Xmx1200m -Xss256k -XX:+ExitOnOutOfMemoryError"
```

**All non-heavy modules (parallel):**
```bash
mvn test -pl='!application,!dao,!ui-ngx,!msa/js-executor,!msa/web-ui' -T4
```

**DAO layer:**
```bash
mvn test -pl dao -Dparallel=packages -DforkCount=4
```

**Single test class:**
```bash
mvn test -pl application -Dtest=MyTestClass
```

**Application module tests are grouped by package** (see `TEST_FAST.md` for full parallel strategy). Key groups:
```bash
mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.controller.**' -DforkCount=6 -Dparallel=classes
mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.service.**'    -DforkCount=6 -Dparallel=packages
mvn test -pl application -Dtest='!**/nosql/**,org.thingsboard.server.transport.mqtt.**' -DforkCount=6 -Dparallel=classes
```

Add `-Dsurefire.rerunFailingTestsCount=2 -Dsurefire.failOnFlakeCount=5` for flaky-test tolerance.

**Testcontainers + Docker API mismatch fix:** add `{"min-api-version": "1.32"}` to Docker daemon config. If containers can't be found, delete `~/.testcontainers.properties`.

## Linting / Code Quality

**Reformat Apache license headers** (required before commits):
```bash
mvn license:format
```

**Frontend lint:**
```bash
cd ui-ngx && npm run lint
```

**Validate YAML config files:**
```bash
python3 tools/src/main/python/check_yml_file.py <file>
```

## pkg.skip flags

Use `-Dpkg.skip=true` to skip all packaging (bootjar, deb, rpm, zip). Tests use the regular `.jar`, not the fat boot jar — safe to skip for all test runs.

## Architecture

ThingsBoard is a **multi-module Maven monorepo** structured as a deployable monolith with optional microservice split. Version: `4.4.0-SNAPSHOT`. Java 25, Spring Boot 3.5.13.

### Module Map

| Directory | Role |
|---|---|
| `application/` | Main Spring Boot server — REST controllers, multi-tenant service layer, actor system |
| `common/` | Shared libraries: actor model, cache, cluster API (gRPC/Protobuf), DAO API, message queue abstraction, edge API, version control |
| `dao/` | Data access layer — pluggable backends: PostgreSQL (default), Cassandra (NoSQL), hybrid |
| `rule-engine/` | Rule chain engine — `rule-engine-api` (interfaces) + `rule-engine-components` (built-in nodes) |
| `transport/` | Device connectivity protocols: HTTP, MQTT, CoAP, LWM2M, SNMP |
| `netty-mqtt/` | Custom Netty-based MQTT broker used by the MQTT transport |
| `edqs/` | Distributed query/queue system backed by RocksDB |
| `ui-ngx/` | Angular 20 SPA — dashboards, device management, widget framework |
| `msa/` | Docker image builds for microservice mode: `tb-node`, `web-ui`, `js-executor`, `vc-executor` |
| `rest-client/` | Java REST client library for the ThingsBoard API |
| `packaging/` | DEB/RPM/ZIP packaging scripts |

### Key Design Patterns

**Actor model:** `common/actor` implements a lightweight Akka-style actor system. Device sessions, rule chains, and tenant state are managed as actors — avoid bypassing this with direct service calls where actors are in play.

**DAO abstraction:** `common/dao-api` defines repository interfaces; `dao/` provides SQL and Cassandra implementations. The active backend is selected via Spring profiles (`sql` vs `hybrid` vs `cassandra`).

**Message queue abstraction:** `common/queue` wraps Kafka, RabbitMQ, and in-memory queues behind a common interface. Transport services push messages onto this queue; the rule engine consumes them.

**Multi-tenancy:** All entities carry `tenantId` and most carry `customerId`. Security context flows through `TenantId`/`CustomerId` value objects — always propagate these when adding new service methods.

**Transport session lifecycle:** Devices connect through a transport module → transport publishes to the queue → `application/` rule engine processes → responses flow back via the same queue channel.

### Frontend (ui-ngx)

Angular 20, TypeScript 5.9, Angular Material, TailwindCSS 3, ECharts for charts, Leaflet for maps. Built with the Angular CLI's esbuild builder. The `npm start` dev server proxies API calls to a locally running ThingsBoard backend.

### Protobuf / gRPC

Inter-service and cluster communication uses Protobuf 3.25.5 + gRPC 1.76.0. Proto files live in `common/proto/` and `common/cluster-api/`. Run `./build_proto.sh` after changing `.proto` files.
