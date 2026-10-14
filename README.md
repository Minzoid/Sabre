# Sabre

Sabre is a lightweight, framework-agnostic Java database access library that eliminates boilerplate JDBC code for everyday CRUD operations while providing an unhindered raw SQL escape hatch when needed.

## Key Features

- Zero-Boilerplate CRUD: Insert, read, update, and delete entities with clean, single-line method calls.
- Fluent Query Builder: Chain `.where()`, `.orderBy()`, `.limit()`, and `.offset()` into injection-safe parameterized queries.
- Opt-In Reflection Mapping: Minimal annotations (`@Table`, `@Id`, `@Column`, `@Transient`) with cached metadata for high throughput.
- Raw SQL Escape Hatch: Run custom queries, joins, and aggregates while retaining automatic connection pooling and object mapping.
- Connection Pooling: Built on HikariCP for connection management and thread safety.
- Unchecked Exceptions: Wraps checked `SQLException`s into `SabreException` to remove repetitive try-catch blocks.
- Framework-Agnostic and Driver-Agnostic: Works in standalone CLIs, game servers, microservices, or full enterprise applications with any standard JDBC driver (SQLite, MySQL, PostgreSQL, MariaDB, etc.).
- Java 21+: Leverages modern Java features and requires Java 21 or higher.

## Quick Start

### 1. Add Sabre to Your Project

Install Sabre locally into your local Maven cache:

```bash
mvn clean install
```

#### Maven (pom.xml)

Add Sabre and your database's JDBC driver to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>moon.studio</groupId>
        <artifactId>sabre</artifactId>
        <version>1.0</version>
    </dependency>

    <!-- Example: SQLite Driver -->
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.45.3.0</version>
    </dependency>

    <!-- Example: MySQL Driver -->
    <!--
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.3.0</version>
    </dependency>
    -->

    <!-- Example: PostgreSQL Driver -->
    <!--
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.3</version>
    </dependency>
    -->
</dependencies>
```

#### Gradle (build.gradle.kts)

```kotlin
dependencies {
    implementation("moon.studio:sabre:1.0")

    // Choose your JDBC driver:
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    // implementation("com.mysql:mysql-connector-j:8.3.0")
    // implementation("org.postgresql:postgresql:42.7.3")
}
```

### 2. Initialize a Connection

```java
import moon.studio.sabre.core.Sabre;

// SQLite (file-based or in-memory)
Sabre db = Sabre.connect("jdbc:sqlite:app.db");

// MySQL or PostgreSQL with credentials
Sabre db = Sabre.connect("jdbc:mysql://localhost:3306/mydb", "db_user", "db_password");

// Using try-with-resources:
try (Sabre db = Sabre.connect("jdbc:sqlite:app.db")) {
    // Database operations here
}
```

### 3. Define an Entity

Annotate your model classes with Sabre annotations:

```java
import moon.studio.sabre.mapping.annotations.Table;
import moon.studio.sabre.mapping.annotations.Id;
import moon.studio.sabre.mapping.annotations.Column;
import moon.studio.sabre.mapping.annotations.Transient;

@Table("users")
public class User {

    @Id
    private int id;

    private String name;

    private int age;

    @Column("email_address")
    private String email;

    @Transient
    private String sessionToken;

    public User() {}

    public User(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

## Usage Guide

### CRUD Operations

#### Create (save)

When `@Id` is an integer/long with default value `0`, Sabre inserts the record and writes the generated key back to the instance:

```java
User user = new User("Alice", 25, "alice@example.com");
db.save(user);

System.out.println("Generated ID: " + user.getId());
```

If an explicit ID (such as a String or UUID) is already populated, Sabre includes the ID column in the `INSERT` statement directly.

#### Read by ID (findById)

```java
User user = db.findById(User.class, 1);
if (user != null) {
    System.out.println("Found: " + user.getName());
}
```

#### Read All (findAll)

```java
List<User> users = db.findAll(User.class);
users.forEach(u -> System.out.println(u.getName()));
```

#### Update (update)

Updates all non-ID columns for the row matching `@Id`:

```java
User user = db.findById(User.class, 1);
user.setName("Alice Cooper");
user.setAge(26);

db.update(user);
```

#### Delete (delete)

```java
db.delete(User.class, 1);
```

### Fluent Query Builder

Build parameterized, SQL-injection safe queries fluently:

#### Filtering and Ordering

```java
List<User> adults = db.query(User.class)
    .where("age", ">", 18)
    .orderBy("name", "ASC")
    .list();
```

#### Equality Filter

```java
User alice = db.query(User.class)
    .where("name", "Alice")
    .first();
```

#### Multiple Conditions (AND)

```java
List<User> activeUsers = db.query(User.class)
    .where("age", ">=", 21)
    .where("email_address", "LIKE", "%@company.com")
    .orderBy("age", "DESC")
    .list();
```

#### Pagination (limit and offset)

```java
List<User> page = db.query(User.class)
    .orderBy("id", "ASC")
    .limit(10)
    .offset(20)
    .list();
```

#### Counting Rows

```java
long totalAdults = db.query(User.class)
    .where("age", ">=", 18)
    .count();
```

### Raw SQL Escape Hatch

#### Typed Query (raw)

Executes custom SQL (including joins and aggregates) and maps rows directly to entity instances:

```java
List<User> users = db.raw(User.class,
    "SELECT * FROM users WHERE age >= ? AND email_address IS NOT NULL",
    21
);
```

#### DDL and Updates (execute)

Executes statements that do not return rows and returns the number of affected rows:

```java
// Create schema table
db.execute("""
    CREATE TABLE IF NOT EXISTS users (
        id            INTEGER PRIMARY KEY AUTOINCREMENT,
        name          TEXT NOT NULL,
        age           INTEGER NOT NULL,
        email_address TEXT
    )
""");

// Execute batch updates
int updatedCount = db.execute("UPDATE users SET age = age + 1 WHERE age < ?", 18);
```

### Exception Handling

Sabre wraps checked `java.sql.SQLException`s into `SabreException` (an unchecked runtime exception):

```java
try {
    db.save(user);
} catch (SabreException e) {
    System.err.println("Database operation failed: " + e.getMessage());
    Throwable cause = e.getCause(); // Original SQLException
}
```

## Advanced Configuration

### Connection Pool Access

Sabre uses HikariCP internally. You can access the pool directly via `getConnectionManager()`:

```java
import com.zaxxer.hikari.HikariDataSource;
import moon.studio.sabre.core.ConnectionManager;

ConnectionManager cm = db.getConnectionManager();
HikariDataSource ds = (HikariDataSource) cm.getDataSource();

ds.setMaximumPoolSize(20);
ds.setMinimumIdle(5);
```

## Supported Java Types

Sabre handles bidirectional mapping between Java types and standard SQL column types:

| Java Type | SQL Type |
| --- | --- |
| `int` / `Integer` | `INTEGER` / `INT` |
| `long` / `Long` | `BIGINT` |
| `double` / `Double` | `DOUBLE` / `FLOAT` |
| `float` / `Float` | `FLOAT` / `REAL` |
| `boolean` / `Boolean` | `BOOLEAN` / `TINYINT` |
| `short` / `Short` | `SMALLINT` |
| `byte` / `Byte` | `TINYINT` |
| `String` | `VARCHAR` / `TEXT` |
| `BigDecimal` | `NUMERIC` / `DECIMAL` |
| `byte[]` | `BLOB` / `VARBINARY` |
| `java.time.LocalDate` | `DATE` |
| `java.time.LocalDateTime` | `TIMESTAMP` |
| `java.time.Instant` | `TIMESTAMP` |
| `java.sql.Date` | `DATE` |
| `java.sql.Timestamp` | `TIMESTAMP` |
| `java.util.Date` | `TIMESTAMP` |

## Building and Testing

### Run Tests

```bash
mvn test
```

### Package JAR

```bash
mvn clean package
```

The resulting library JAR will be generated at `target/Sabre-1.0.jar`.

## Best Practices

1. Use try-with-resources or call `db.close()` when shutting down your application.
2. Ensure entity classes have a no-argument constructor (public, package-private, or private).
3. Use `@Transient` on fields that should not be mapped to database columns.
4. Use the query builder or parameter bindings in `raw()` to prevent SQL injection.

## License

Sabre is developed by Moon Studio under the MIT License.