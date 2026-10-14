package moon.studio.sabre;

import moon.studio.sabre.core.Sabre;
import moon.studio.sabre.core.SabreException;
import moon.studio.sabre.mapping.annotations.Column;
import moon.studio.sabre.mapping.annotations.Id;
import moon.studio.sabre.mapping.annotations.Table;
import moon.studio.sabre.mapping.annotations.Transient;

import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SabreIntegrationTest {

    private static Sabre db;

    @Table("users")
    static class User {
        @Id int id;
        String name;
        int age;
        @Column("email_address") String email;
        @Transient String temp;

        User() {}

        User(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }
    }

    static class BaseEntity {
        String createdBy;
    }

    @Table("products")
    static class Product extends BaseEntity {
        @Id String sku;
        String title;
        double price;
        boolean inStock;

        Product() {}

        Product(String sku, String title, double price, boolean inStock, String createdBy) {
            this.sku = sku;
            this.title = title;
            this.price = price;
            this.inStock = inStock;
            this.createdBy = createdBy;
        }
    }

    @Table("audit_logs")
    static class AuditLog {
        @Id long id;
        String action;
        LocalDate logDate;
        LocalDateTime timestamp;

        AuditLog() {}

        AuditLog(String action, LocalDate logDate, LocalDateTime timestamp) {
            this.action = action;
            this.logDate = logDate;
            this.timestamp = timestamp;
        }
    }

    static class UnannotatedEntity {
        @Id int id;
    }

    @Table("missing_id")
    static class MissingIdEntity {
        int value;
    }

    @Table("multi_id")
    static class MultiIdEntity {
        @Id int id1;
        @Id int id2;
    }

    @BeforeAll
    static void setup() {
        db = Sabre.connect("jdbc:sqlite::memory:");
        db.execute("""
            CREATE TABLE users (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                name          TEXT    NOT NULL,
                age           INTEGER NOT NULL,
                email_address TEXT
            )
        """);
        db.execute("""
            CREATE TABLE products (
                sku       TEXT PRIMARY KEY,
                title     TEXT    NOT NULL,
                price     REAL    NOT NULL,
                inStock   INTEGER NOT NULL,
                createdBy TEXT
            )
        """);
        db.execute("""
            CREATE TABLE audit_logs (
                id        INTEGER PRIMARY KEY AUTOINCREMENT,
                action    TEXT NOT NULL,
                logDate   TEXT,
                timestamp TEXT
            )
        """);
    }

    @AfterAll
    static void teardown() {
        if (db != null) db.close();
    }

    @Test
    @Order(1)
    void save_insertsRowAndSetsGeneratedId() {
        User u = new User("Minzoid", 22, "minzoid@moon.studio");
        db.save(u);
        assertTrue(u.id > 0, "Generated id should be set on the entity");
    }

    @Test
    @Order(2)
    void findById_returnsMappedEntity() {
        User found = db.findById(User.class, 1);
        assertNotNull(found);
        assertEquals("Minzoid", found.name);
        assertEquals(22, found.age);
        assertEquals("minzoid@moon.studio", found.email);
    }

    @Test
    @Order(3)
    void findById_returnsNullForMissingId() {
        assertNull(db.findById(User.class, 999));
    }

    @Test
    @Order(4)
    void save_multipleEntities() {
        db.save(new User("Alice", 30, "alice@example.com"));
        db.save(new User("Bob", 17, "bob@example.com"));
        db.save(new User("Charlie", 25, null));
    }

    @Test
    @Order(5)
    void findAll_returnsAllRows() {
        assertEquals(4, db.findAll(User.class).size());
    }

    @Test
    @Order(6)
    void update_changesExistingRow() {
        User u = db.findById(User.class, 1);
        assertNotNull(u);

        u.name = "Minzoid_Updated";
        u.age = 23;
        db.update(u);

        User updated = db.findById(User.class, 1);
        assertNotNull(updated);
        assertEquals("Minzoid_Updated", updated.name);
        assertEquals(23, updated.age);
    }

    @Test
    @Order(7)
    void delete_removesRow() {
        db.delete(User.class, 3);
        assertNull(db.findById(User.class, 3));
        assertEquals(3, db.findAll(User.class).size());
    }

    @Test
    @Order(8)
    void queryBuilder_whereAndOrderBy() {
        List<User> adults = db.query(User.class)
                .where("age", ">", 18)
                .orderBy("name")
                .list();

        assertFalse(adults.isEmpty());
        for (User u : adults) {
            assertTrue(u.age > 18, "Expected age > 18 but got " + u.age);
        }
    }

    @Test
    @Order(9)
    void queryBuilder_whereEquals() {
        User u = db.query(User.class).where("name", "Alice").first();
        assertNotNull(u);
        assertEquals("Alice", u.name);
    }

    @Test
    @Order(10)
    void queryBuilder_limit() {
        assertEquals(2, db.query(User.class).limit(2).list().size());
    }

    @Test
    @Order(11)
    void queryBuilder_count() {
        assertTrue(db.query(User.class).where("age", ">", 18).count() > 0);
    }

    @Test
    @Order(12)
    void rawQuery_returnsTypedResults() {
        List<User> results = db.raw(User.class,
                "SELECT * FROM users WHERE age >= ? ORDER BY age DESC", 20);

        assertFalse(results.isEmpty());
        for (User u : results) {
            assertTrue(u.age >= 20);
        }
    }

    @Test
    @Order(13)
    void rawExecute_modifiesData() {
        assertEquals(1, db.execute("UPDATE users SET age = age + 1 WHERE name = ?", "Alice"));

        User alice = db.query(User.class).where("name", "Alice").first();
        assertNotNull(alice);
        assertEquals(31, alice.age);
    }

    @Test
    @Order(14)
    void transientField_isNotPersisted() {
        User u = new User("TransientTest", 40, "t@test.com");
        u.temp = "should_not_be_persisted";
        db.save(u);

        User found = db.findById(User.class, u.id);
        assertNotNull(found);
        assertNull(found.temp, "@Transient field should not be read from DB");
    }

    @Test
    @Order(15)
    void columnAnnotation_mapsToCustomColumnName() {
        User u = db.findById(User.class, 1);
        assertNotNull(u);
        assertNotNull(u.email, "@Column('email_address') should map correctly");
    }

    @Test
    @Order(16)
    void manualStringId_insertsAndFinds() {
        Product p = new Product("SKU-1001", "Mechanical Keyboard", 129.99, true, "admin");
        db.save(p);

        Product found = db.findById(Product.class, "SKU-1001");
        assertNotNull(found);
        assertEquals("SKU-1001", found.sku);
        assertEquals("Mechanical Keyboard", found.title);
        assertEquals(129.99, found.price, 0.001);
        assertTrue(found.inStock);
    }

    @Test
    @Order(17)
    void manualStringId_updatesAndDeletes() {
        Product p = db.findById(Product.class, "SKU-1001");
        assertNotNull(p);

        p.price = 99.99;
        p.inStock = false;
        db.update(p);

        Product updated = db.findById(Product.class, "SKU-1001");
        assertNotNull(updated);
        assertEquals(99.99, updated.price, 0.001);
        assertFalse(updated.inStock);

        db.delete(Product.class, "SKU-1001");
        assertNull(db.findById(Product.class, "SKU-1001"));
    }

    @Test
    @Order(18)
    void inheritedFields_mappedFromSuperclass() {
        Product p = new Product("SKU-2002", "Wireless Mouse", 49.50, true, "superadmin");
        db.save(p);

        Product found = db.findById(Product.class, "SKU-2002");
        assertNotNull(found);
        assertEquals("superadmin", found.createdBy);
    }

    @Test
    @Order(19)
    void queryBuilder_multipleConditionsAndOperators() {
        List<User> filtered = db.query(User.class)
                .where("age", ">=", 20)
                .where("age", "<=", 31)
                .where("name", "!=", "Bob")
                .list();

        assertFalse(filtered.isEmpty());
        for (User u : filtered) {
            assertTrue(u.age >= 20 && u.age <= 31);
            assertNotEquals("Bob", u.name);
        }
    }

    @Test
    @Order(20)
    void queryBuilder_likeOperator() {
        List<User> matching = db.query(User.class)
                .where("email_address", "LIKE", "%@example.com")
                .list();

        assertFalse(matching.isEmpty());
        for (User u : matching) {
            assertTrue(u.email.endsWith("@example.com"));
        }
    }

    @Test
    @Order(21)
    void queryBuilder_paginationWithOffsetAndLimit() {
        List<User> paginated = db.query(User.class)
                .orderBy("id", "ASC")
                .offset(1)
                .limit(2)
                .list();

        assertEquals(2, paginated.size());
    }

    @Test
    @Order(22)
    void queryBuilder_firstReturnsNullWhenNotFound() {
        User notFound = db.query(User.class)
                .where("name", "NonExistentNameXYZ")
                .first();

        assertNull(notFound);
    }

    @Test
    @Order(23)
    void temporalTypes_saveAndRetrieveLocalDateAndLocalDateTime() {
        LocalDate today = LocalDate.of(2026, 10, 14);
        LocalDateTime now = LocalDateTime.of(2026, 10, 14, 12, 0, 0);

        AuditLog log = new AuditLog("USER_LOGIN", today, now);
        db.save(log);
        assertTrue(log.id > 0);

        AuditLog found = db.findById(AuditLog.class, log.id);
        assertNotNull(found);
        assertEquals("USER_LOGIN", found.action);
        assertEquals(today, found.logDate);
        assertNotNull(found.timestamp);
    }

    @Test
    @Order(24)
    void entityMapper_throwsExceptionWhenTableAnnotationMissing() {
        assertThrows(SabreException.class, () -> db.findById(UnannotatedEntity.class, 1));
    }

    @Test
    @Order(25)
    void entityMapper_throwsExceptionWhenIdAnnotationMissing() {
        assertThrows(SabreException.class, () -> db.findById(MissingIdEntity.class, 1));
    }

    @Test
    @Order(26)
    void entityMapper_throwsExceptionWhenMultipleIdsPresent() {
        assertThrows(SabreException.class, () -> db.findById(MultiIdEntity.class, 1));
    }

    @Test
    @Order(27)
    void connectionManager_providesAccessToUnderlyingPool() {
        assertNotNull(db.getConnectionManager());
        assertNotNull(db.getConnectionManager().getDataSource());
        assertFalse(db.getConnectionManager().getDataSource().isClosed());
    }

    @Test
    @Order(28)
    void autoCloseable_tryWithResourcesClosesPool() {
        Sabre testDb = Sabre.connect("jdbc:sqlite::memory:");
        try (testDb) {
            testDb.execute("CREATE TABLE t (x INT)");
            testDb.execute("INSERT INTO t VALUES (42)");
        }
        assertTrue(testDb.getConnectionManager().getDataSource().isClosed());
    }
}
