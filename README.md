# Sabre

**Sabre** is a lightweight, framework-agnostic Java database access library that eliminates boilerplate JDBC code for everyday CRUD operations while providing an unhindered raw SQL escape hatch when needed.

## ✨ Key Features

- **Zero-Boilerplate CRUD**: Insert, read, update, and delete entities with clean, single-line method calls
- **Fluent Query Builder**: Chain `.where()`, `.orderBy()`, `.limit()`, and `.offset()` into injection-safe parameterized queries
- **Opt-In Reflection Mapping**: Minimal annotations (`@Table`, `@Id`, `@Column`, `@Transient`) with cached metadata for high throughput
- **Raw SQL Escape Hatch**: Run custom queries, joins, and aggregates while retaining automatic connection pooling and object mapping
- **Connection Pooling**: Built on HikariCP for top-tier connection management and thread safety
- **Unchecked Exceptions**: Wraps checked `SQLException`s into `SabreException` — no repetitive try-catch blocks
- **Framework-Agnostic & Driver-Agnostic**: Works in standalone CLIs, game servers, microservices, or full enterprise applications with any standard JDBC driver (SQLite, MySQL, PostgreSQL, MariaDB, etc.)
- **Java 21+**: Leverages modern Java features and requires Java 21 or higher

## 🚀 Quick Start

### 1. Add Sabre to Your Project

First, install Sabre locally (or deploy to your internal repository):

```bash
mvn clean install
```

This installs `moon.studio:Sabre:1.0` into your local `~/.m2/repository`.

#### Maven (`pom.xml`)
```xml
<dependencies>
    <!-- Sabre ORM / Database Library -->
    <dependency>
        <groupId>moon.studio</groupId>
        <artifactId>Sabre</artifactId>
        <version>1.0</version>
    </dependency>

    <!-- Choose your JDBC driver: -->
    <!-- Example: SQLite -->
    <dependency>
        <groupId>org.xerial</groupId>
        <artifactId>sqlite-jdbc</artifactId>
        <version>3.45.3.0</version>
    </dependency>

    <!-- Example: MySQL -->
    <!--
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.3.0</version>
    </dependency>
    -->

    <!-- Example: PostgreSQL -->
    <!--
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <version>42.7.3</version>
    </dependency>
    -->
</dependencies>
```

#### Gradle (`build.gradle.kts`)
```kotlin
dependencies {
    implementation("moon.studio:Sabre:1.0")

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

// MySQL / PostgreSQL (with credentials)
Sabre db = Sabre.connect("jdbc:mysql://localhost:3306/mydb", "db_user", "db_password");

// Using try-with-resources (recommended):
try (Sabre db = Sabre.connect("jdbc:sqlite:app.db")) {
    // Database operations here
}
```

### 3. Define Your Entity

Annotate your POJOs using Sabre annotations:

```java
import moon.studio.sabre.mapping.annotations.Table;
import moon.studio.sabre.mapping.annotations.Id;
import moon.studio.sabre.mapping.annotations.Column;
import moon.studio.sabre.mapping.annotations.Transient;

@Table("users")
public class User {

    @Id
    private int id; // Auto-incremented or manually assigned

    private String name;

    private int age;

    @Column("email_address")
    private String email; // Maps to column 'email_address'

    @Transient
    private String sessionToken; // Ignored by Sabre

    // Constructors
    public User() {}

    public User(String name, int age, String email) {
        this.name = name;
        this.age = age;
        this.email = email;
    }

    // Getters and setters...
    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}
```

## 📖 Complete Usage Guide

### CRUD Operations

#### Create (`save`)
```java
User user = new User("Alice", 25, "alice@example.com");
db.save(user);

System.out.println("Generated ID: " + user.getId()); // e.g. 1
```

#### Read by ID (`findById`)
```java
User user = db.findById(User.class, 1);
if (user != null) {
    System.out.println("Found user: " + user.getName());
}
```

#### Read All (`findAll`)
```java
List<User> users = db.findAll(User.class);
users.forEach(u -> System.out.println(u.getName()));
```

#### Update (`update`)
```java
User user = db.findById(User.class, 1);
user.setName("Alice Cooper");
user.setAge(26);

db.update(user);
```

#### Delete (`delete`)
```java
db.delete(User.class, 1);
```

### Fluent Query Builder

Build dynamic, parameterized queries without writing string SQL:

#### Basic Filters & Sorting
```java
List<User> adults = db.query(User.class)
    .where("age", ">", 18)
    .orderBy("name", "ASC")
    .list();
```

#### Equality Shortcut
```java
// Equivalent to .where("name", "=", "Alice")
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

#### Pagination (`limit` & `offset`)
```java
List<User> pageOne = db.query(User.class)
    .orderBy("id", "ASC")
    .limit(10)
    .offset(0)
    .list();
```

#### Counting
```java
long totalAdults = db.query(User.class)
    .where("age", ">=", 18)
    .count();
```

### Raw SQL Escape Hatch

When you need complex joins, aggregates, or database-specific syntax:

#### Querying with Result Mapping (`raw`)
```java
List<User> users = db.raw(User.class,
    "SELECT * FROM users WHERE age >= ? AND email_address IS NOT NULL", 
    21
);
```

#### Executing DDL / Updates (`execute`)
```java
// Create schema
db.execute("""
    CREATE TABLE IF NOT EXISTS users (
        id            INTEGER PRIMARY KEY AUTOINCREMENT,
        name          TEXT NOT NULL,
        age           INTEGER NOT NULL,
        email_address TEXT
    )
""");

// Bulk update with parameters
int affected = db.execute(
    "UPDATE users SET age = age + 1 WHERE age < ?", 
    18
);
```

### Exception Handling

Sabre converts all low-level `java.sql.SQLException`s into `SabreException` (an unchecked runtime exception):

```java
try {
    db.save(user);
} catch (SabreException e) {
    System.err.println("Database operation failed: " + e.getMessage());
    Throwable cause = e.getCause(); // Original SQLException if needed
}
```

Or use try-with-resources for automatic cleanup:

```java
try (Sabre db = Sabre.connect("jdbc:sqlite:app.db")) {
    // All database operations here
} // db.close() called automatically
```

## 🔧 Advanced Configuration

### Connection Pool Tuning

Sabre uses HikariCP under the hood. For advanced connection pool configuration, you can access the underlying `HikariDataSource`:

```java
import com.zaxxer.hikari.HikariDataSource;
import moon.studio.sabre.core.ConnectionManager;

// Get the connection manager from Sabre instance
ConnectionManager cm = db.getConnectionManager(); // Add this getter if needed
HikariDataSource ds = cm.getDataSource();

// Configure pool properties
ds.setMaximumPoolSize(20);
ds.setMinimumIdle(5);
ds.setConnectionTimeout(30000);
ds.setIdleTimeout(600000);
ds.setMaxLifetime(1800000);
```

## 📋 Supported Java Types

Sabre handles bidirectional mapping between standard Java types and SQL data types:

| Java Type | SQL Type |
|-----------|----------|
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

## 🧪 Running Tests

```bash
mvn test
```

The integration tests use an in-memory SQLite database and cover:
- CRUD operations
- Query builder functionality
- Raw SQL execution
- Annotation handling (@Column, @Transient)
- Transaction safety

## 📦 Building the JAR

```bash
mvn clean package
```

The JAR will be available in `target/Sabre-1.0.jar`.

## 📄 License

Sabre is developed by **Moon Studio** and is available under the MIT License.

## 💡 Best Practices

1. **Use try-with-resources** for automatic connection cleanup
2. **Keep entities simple** - POJOs with getters/setters work best
3. **Use @Transient** for fields that shouldn't be persisted
4. **Leverage the query builder** for type-safe, injection-free queries
5. **Use raw SQL** only when necessary for complex operations
6. **Close connections** explicitly if not using try-with-resources
7. **Match Java types** to appropriate SQL types for optimal performance

## ❓ Troubleshooting

- **"No suitable driver"**: Ensure you've included the correct JDBC driver dependency
- **"Table doesn't exist"**: Verify your `@Table` annotation matches the actual table name
- **"Column not found"**: Check your `@Column` annotations or ensure field names match column names
- **Connection leaks**: Always close Sabre instances or use try-with-resources
- **Performance issues**: Ensure you're using Java 21+ and consider connection pool tuning

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

*Sabre makes database access simple without sacrificing power. Get back to writing your application logic, not JDBC boilerplate.*#   S a b r e  
 