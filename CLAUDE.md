# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A Spring Boot 3.1.0 backend for a Taobao-style e-commerce platform ("taobao_system") serving an admin web frontend. It exposes REST APIs for user/shop/product/order/cart management, JWT-based authentication, and Aliyun OSS file uploads. The server runs on port 8085 with the `/api` context-path.

## Build & Run Commands

Java 17 + Maven. Use the Maven wrapper if present (`.mvn/` directory exists).

```bash
# Build & test
mvn clean compile                # compile only
mvn clean package                # full build → target/taobao-0.0.1-SNAPSHOT.jar
mvn test                        # run unit tests (currently no tests exist)

# Run locally
mvn spring-boot:run             # dev server on http://localhost:8085/api
java -jar target/taobao-0.0.1-SNAPSHOT.jar

# Single test (none defined yet — see Testing section)
mvn test -Dtest=ClassName#methodName
```

## Architecture (Layered)

```
controller/   → REST endpoints, DTO ↔ POJO mapping, JWT extraction
service/      → Business interfaces
service/serviceImpl/ → Business logic, transaction boundaries, password hashing
mapper/       → MyBatis mapper interfaces (org.taobao.mapper)
resources/mapper/*.xml → SQL queries (note: NOT in same package as Java mappers)
pojo/         → DB-mapped entities (Lombok @Data/@Builder)
dto/          → Request bodies for incoming APIs
vo/           → Response bodies for outgoing APIs (e.g. UserProfileVO, PageResult)
exception/    → Domain exceptions (AccountNotFoundException, etc.) extending RuntimeException
constant/     → String constants for status codes, error messages
context/      → BaseContext — ThreadLocal holding current userId per request
interceptor/  → TokenInterceptor — JWT validation
config/       → WebConfig (interceptor registration), MyBatisConfig (camelCase mapping)
utils/        → JwtUtils, AliyunOSSOperator, AliyunOSSProperties
```

Package root: `org.taobao`. Application entry: `TliasWebManagementApplication`.

## Cross-Cutting Patterns

**Unified response envelope** — every controller returns `Result<T>` (`org.taobao.pojo.Result`) with `code`/`msg`/`data`. Use the static factories: `Result.success(data)`, `Result.error(msg)`, `Result.badRequest(msg)`, `Result.unauthorized(msg)`, `Result.forbidden(msg)`, `Result.notFound(msg)`. Codes mirror HTTP status (200/400/401/403/404/500).

**JWT auth flow** — `TokenInterceptor` (registered in `WebConfig`) parses the JWT from the `token` header (falls back to `Authorization: Bearer <token>`), writes the userId to `BaseContext`, and rejects with 401 if missing/invalid. After request completion, `afterCompletion` clears `BaseContext` to prevent leaks. Controllers read the current user via `BaseContext.getCurrentId()`. Public endpoints excluded: `/user/login`, `/user/register`, `/error`, `/user/avatar/**`, `/upload`, `/product/home/list`.

**Password storage** — MD5 via `DigestUtils.md5DigestAsHex()` (`UserServiceImpl.login`). Note: this is weak hashing; replace with BCrypt for new auth flows.

**Pagination** — PageHelper (`com.github.pagehelper`) plus a custom `PageResult<T>` VO built via `PageResult.build(list, total, pageNum, pageSize)`.

**File uploads** — `AliyunOSSOperator.upload(bytes, filename)` returns the full public URL. Files are stored at `yyyy/MM/<uuid>.<ext>`. Credentials come from env vars `OSS_ACCESS_KEY_ID` / `OSS_ACCESS_KEY_SECRET`. URL parsing to extract the relative path uses a hardcoded "find the 4th slash" pattern — duplicated in `AdminController` and `UserController` (consider extracting to a utility).

**Status enums as strings** — see `StatusConstant` (`active`/`inactive`/`locked`), `ShopStatusConstant` (`normal`/`closed`/`auditing`), and the user-type field (`operator`/`merchant`/`customer`/`visitor`). Merchants register as `locked` and require admin audit (which auto-creates their shop — see `AdminController.auditMerchant`).

## Database

- MySQL at `47.118.20.132:3306/taobao_system` (see `application.yml`)
- MyBatis mapper XML location: `classpath:mapper/*.xml` (NOT under `org.taobao.mapper`)
- Auto-camelCase mapping enabled (`map-underscore-to-camel-case: true`) — DB columns like `user_id` map to `userId`
- Aliases package: `org.taobao.pojo`
- SQL is logged to stdout (`StdOutImpl`) at DEBUG for `org.taobao.mapper` — useful when debugging queries

## Configuration Hot Spots

`src/main/resources/application.yml`:
- `server.port: 8085`, `server.servlet.context-path: /api`
- Multipart upload limit: 10MB
- OSS endpoint/bucket/region hardcoded for `cn-beijing` / bucket `taobao-hqh`
- DB credentials are checked in (rotate before sharing)

## Documentation Files

- `admin_api_docs.md` — Admin controller endpoint reference
- `Apifox接口测试用例.md` — Apifox test cases (Chinese)
- `1.txt` — appears to be raw notes, review before relying on it

## API Surface (high level)

| Base path | Controller | Purpose |
|---|---|---|
| `/user` | `UserController` | login, register, profile (uses BaseContext for current user) |
| `/admin` | `AdminController` | dashboard stats, user/merchant management, order moderation |
| `/shop` | `ShopController` | shop CRUD, "my shop" lookup, merchant self-service |
| `/product` | `ProductController` | product + SKU management, home list (public) |
| `/order` | `OrderController` | order create/cancel, status statistics |
| `/cart` | `CartController` | cart CRUD, batch update |
| `/upload` | `UploadController` | file upload to OSS |
| `/user/address` | `UserAddressController` | shipping addresses |

The frontend URL parsing pattern in `AdminController.java:230-234` and `UserController.java:214-219` is fragile — if you change the OSS endpoint scheme, fix both call sites.

## Testing

No automated tests exist in `src/test/java/org/` yet (only the empty `taobao` package). When adding tests:
- Use JUnit 5 + Spring Boot Test (`spring-boot-starter-test` is in `pom.xml`)
- MyBatis test starter is also pre-declared
- For DB-touching tests, use Testcontainers (add as a new dep) since the dev DB is a shared remote instance

## Tooling Notes

- Lombok is wired in `maven-compiler-plugin`'s `annotationProcessorPaths` — IDE needs the Lombok plugin
- Logback config: `src/main/resources/logback.xml`
- Database schema and seed data are NOT in this repo — coordinate with whoever owns the remote MySQL at `47.118.20.132`