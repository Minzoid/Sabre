# Sabre — Project Build Specification

Oct 14, 2026 · @Minzoid

## Overview

Sabre is a lightweight, framework-agnostic Java database access library. It removes the need to hand-write JDBC boilerplate (connections, PreparedStatements, ResultSet mapping) for everyday CRUD and query work, while never blocking the developer from dropping to raw SQL for complex cases.

**Core philosophy:**

- Code-free by default for common operations (save, find, update, delete, filtered queries).
- Escape hatch always available: raw SQL still benefits from pooling, parameterization, and object mapping.
- Framework-agnostic: works in any Java project (CLI tools, game servers, microservices, Android backends) without requiring Spring or any specific framework.
- JDBC-generic: no database-specific SQL dialects baked in — works with any JDBC-compliant driver (MySQL, PostgreSQL, SQLite, etc.).

## Project Coordinates

| Property | Value |
| --- | --- |
| Group ID | moon.studio |
| Artifact ID | Sabre |
| Version | 1.0 |
| JDK | 21 |
| Build tool | Maven |
| Packaging | jar |

```
<groupId>moon.studio</groupId>
<artifactId>Sabre</artifactId>
<version>1.0</version>
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

## Architecture Layers

Sabre is built as three stacked layers, each usable on its own or combined:

1. **Core layer** — connection pooling (via HikariCP), safe statement execution, resource cleanup, and translation of checked `SQLException`s into unchecked runtime exceptions.
2. **Query builder layer** — fluent Java methods (`select().from().where()`) that generate parameterized SQL under the hood. Built on top of the Core layer.
3. **Mapping layer (opt-in)** — annotation-driven POJO ↔ row mapping via reflection. Provides `save()`, `findById()`, `update()`, `delete()` for any `@Table`-annotated class. Built on top of the Query builder layer.

A **raw SQL escape hatch** (`db.raw(...)`) cuts across all three layers — it still goes through the Core layer for pooling/parameterization and can still return mapped objects, but lets the developer write the exact SQL they need for joins, aggregates, or DB-specific features.

## Package Structure

```
moon.studio.sabre
├── core
│   ├── Sabre.java            // entry point: connect(), raw(), close()
│   ├── ConnectionManager.java  // wraps HikariCP pool
│   └── SabreException.java   // unchecked exception wrapper
├── query
│   ├── QueryBuilder.java      // fluent select/where/orderBy
│   └── Condition.java         // WHERE clause representation
├── mapping
│   ├── EntityMapper.java       // reflection-based object<->row mapping
│   ├── Repository.java         // generic save/find/update/delete
│   └── annotations
│       ├── Table.java
│       ├── Id.java
│       └── Column.java
└── util
    ├── ResultSetMapper.java
    └── SqlTypeConverter.java
```

## Annotations & Reflection Mapping

| Annotation | Target | Purpose |
| --- | --- | --- |
| `@Table("name")` | Class | Maps the class to a DB table |
| `@Id` | Field | Marks the primary key field |
| `@Column("name")` | Field | Maps a field to a column name (optional — defaults to field name) |
| `@Transient` | Field | Excludes a field from mapping |

**How the mapper works:**

1. `EntityMapper` reads the `Class<T>` passed into a method (e.g. `findById(User.class, 1)`).
2. It inspects the class's annotations via `java.lang.reflect` to resolve table name, primary key field, and column mappings (cached per class after first lookup, for performance).
3. For reads: it builds the SQL, executes it, then for each `ResultSet` row creates an instance via the no-arg constructor and sets fields with `Field.setAccessible(true)` + `Field.set(...)`.
4. For writes: it reads field values via `Field.get(...)` and binds them as `PreparedStatement` parameters in the generated INSERT/UPDATE.

## Core API Surface

| Method | Purpose |
| --- | --- |
| `Sabre.connect(url, user, pass)` | Opens a pooled connection, returns a `Sabre` instance |
| `db.save(obj)` | Inserts a mapped object |
| `db.findById(Class<T>, id)` | Selects a single row by primary key, mapped to `T` |
| `db.findAll(Class<T>)` | Selects all rows, mapped to `List<T>` |
| `db.update(obj)` | Updates a mapped object by its `@Id` |
| `db.delete(Class<T>, id)` | Deletes a row by primary key |
| `db.query(Class<T>)` | Starts a fluent `QueryBuilder` for filtered/sorted selects |
| `db.raw(Class<T>, sql, params...)` | Executes raw SQL, maps results to `T` |
| `db.close()` | Releases the connection pool |

**Example:**

```java
Sabre db = Sabre.connect("jdbc:mysql://localhost/mydb", "user", "pass");

@Table("users")
class User {
    @Id int id;
    String name;
    int age;
}

User u = new User(); u.name = "Minzoid"; u.age = 22;
db.save(u);

User found = db.findById(User.class, 1);

List<User> adults = db.query(User.class)
    .where("age", ">", 18)
    .orderBy("name")
    .list();

List<User> report = db.raw(User.class,
    "SELECT * FROM users u JOIN orders o ON u.id=o.user_id WHERE o.total > ?", 100);
```

## Dependencies & Build Tooling

| Dependency | Scope | Reason |
| --- | --- | --- |
| HikariCP | compile | Connection pooling (industry standard, tiny footprint) |
| JDBC drivers (mysql-connector-j, postgresql, sqlite-jdbc) | provided by end user | Sabre talks only `java.sql.*`; the consuming project supplies whichever driver it needs on its classpath |
| JUnit 5 | test | Unit testing |

Everything else (query builder, mapping, reflection) is hand-built — that's the library's actual value, not something to outsource.

**Build:** Maven, JDK 21 target, produces a standard `.jar` artifact publishable to a local repo or Maven Central under `moon.studio:Sabre:1.0`.

## Development Roadmap (v1.0)

1. **Core layer** — `Sabre.connect()`, HikariCP integration, exception wrapping, resource-safe execution.
2. **Mapping layer basics** — `@Table`/`@Id`/`@Column`, `EntityMapper`, `save()`, `findById()`, `findAll()`, `update()`, `delete()`.
3. **Query builder** — `where()`, `orderBy()`, `limit()`, parameterized SQL generation.
4. **Raw SQL escape hatch** — `db.raw(...)` with mapping support.
5. **Testing** — unit tests against SQLite (in-memory) and MySQL/PostgreSQL via Testcontainers.
6. **Docs & examples** — README, Javadoc, a sample project.
7. **Release** — publish `moon.studio:Sabre:1.0` jar.

## Non-Goals & Constraints

- **No Spring dependency** — must work in any plain Java 21 project, with or without a framework.
- **No lazy loading** — all mapped data is fetched eagerly; no proxy objects or N+1 surprises.
- **No session/first-level cache** — unlike Hibernate, Sabre doesn't track entity state between calls; it's a direct mapper, not an ORM session.
- **No dialect-specific SQL generation** — generated SQL stays ANSI-standard; DB-specific features go through the raw SQL escape hatch.
- **No annotation processing/code generation** — mapping is pure runtime reflection, keeping the build simple.
