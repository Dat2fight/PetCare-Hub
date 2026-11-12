package com.petcarehub;

import com.petcarehub.entity.Pet;
import com.petcarehub.entity.Species;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Every URL is derived from this disposable container, never from DB_URL. */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "RUN_SQLSERVER_TESTS", matches = "true")
@Testcontainers // Explicit opt-in must fail, rather than silently pass, when Docker is unavailable.
class FlywayMigrationTest {
    @Container
    static final MSSQLServerContainer<?> sql =
            new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();

    @Test void emptyDatabaseStartsApplicationWithValidateAndPersistsVietnamese() throws Exception {
        String url = newDatabase();
        try (var context = new SpringApplicationBuilder(PetCareHubApplication.class).run(
                "--spring.profiles.active=test", "--server.port=0",
                "--spring.datasource.url=" + url,
                "--spring.datasource.username=" + sql.getUsername(),
                "--spring.datasource.password=" + sql.getPassword(),
                "--spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver",
                "--spring.flyway.enabled=true", "--spring.flyway.baseline-on-migrate=false",
                "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.jpa.database-platform=org.hibernate.dialect.SQLServerDialect",
                "--app.bootstrap.enabled=false", "--app.order-expiry.interval-ms=3600000")) {
            assertTrue(context.isActive());
            var flyway = context.getBean(Flyway.class);
            assertEquals("15", flyway.info().current().getVersion().toString());
            var emf = context.getBean(EntityManagerFactory.class);
            try (var em = emf.createEntityManager()) {
                em.getTransaction().begin();
                Species species = Species.builder().name("qa_Mèo tiếng Việt").build();
                em.persist(species);
                Pet pet = Pet.builder().species(species).name("qa_Bé mèo lông trắng")
                        .description("Chăm sóc thú cưng – tiếng Việt. ".repeat(200))
                        .price(new BigDecimal("1234567890.12"))
                        .imageUrl("https://example.test/" + "a".repeat(400)).build();
                em.persist(pet);
                em.getTransaction().commit();
                em.clear();
                Pet actual = em.find(Pet.class, pet.getId());
                assertEquals(pet.getName(), actual.getName());
                assertEquals(pet.getDescription(), actual.getDescription());
                assertEquals(pet.getPrice(), actual.getPrice());
                assertEquals(pet.getImageUrl(), actual.getImageUrl());
            }
            assertEquals(0, flyway.migrate().migrationsExecuted);
            flyway.validate();
        }
    }

    @Test void baselineUpgradesPartialLegacySchemaWithoutChangingData() throws Exception {
        upgradeLegacy(false);
    }

    @Test void baselineReplaysCompleteLegacySchemaWithoutChangingData() throws Exception {
        upgradeLegacy(true);
    }

    private void upgradeLegacy(boolean complete) throws Exception {
        String url = newDatabase();
        try (var connection = connect(url); var statement = connection.createStatement()) {
            List<String[]> manifest = manifest();
            int applied = complete ? 14 : 4;
            for (int i = 0; i < applied; i++) runScript(statement, manifest.get(i)[2]);
            statement.execute("INSERT INTO dbo.users(username,email,password_hash,first_name,last_name) "
                    + "VALUES('qa_customer','qa_customer@example.test','unused',N'Nguyễn',N'Khách hàng')");
            statement.execute("INSERT INTO dbo.species(name) VALUES(N'qa_Mèo')");
            statement.execute("INSERT INTO dbo.pets(name,species_id,price,description) "
                    + "SELECT N'qa_Mèo trắng',id,1234567890.12,N'Tiếng Việt giữ nguyên' FROM dbo.species");
            statement.execute("INSERT INTO dbo.inventory(pet_id,quantity) SELECT id,1 FROM dbo.pets");
            statement.execute("INSERT INTO dbo.orders(customer_id,order_number,notes,total_amount) "
                    + "SELECT id,'qa_legacy',N'Đơn hàng trước nâng cấp',1234567890.12 FROM dbo.users");
            statement.execute("INSERT INTO dbo.pet_images(pet_id,image_url) SELECT id,'https://example.test/qa.jpg' FROM dbo.pets");
            statement.execute("INSERT INTO dbo.inventory_transactions(inventory_id,transaction_type,quantity,performed_by,notes) "
                    + "SELECT i.id,'STOCK_IN',1,u.id,N'Nhập kho cũ' FROM dbo.inventory i CROSS JOIN dbo.users u");
            String before = snapshot(statement);
            Flyway flyway = flyway(url);
            // Refuse nonempty databases until an operator explicitly onboards them.
            assertThrows(FlywayException.class, flyway::migrate);
            flyway.baseline();
            assertEquals(14, flyway.migrate().migrationsExecuted);
            assertEquals(before, snapshot(statement));
            assertEquals(0, count(statement, "SELECT @@TRANCOUNT"));
            assertEquals(0, count(statement, "SELECT COUNT(*) FROM dbo.orders WHERE paid_at IS NOT NULL OR delivered_at IS NOT NULL"));
            assertEquals(0, flyway.migrate().migrationsExecuted);
            flyway.validate();
            assertEquals(before, snapshot(statement));
            assertThrows(FlywayException.class, flyway::clean);
            assertConstraints(connection);
        }
    }

    @Test void checksumMismatchStopsValidation(@TempDir Path directory) throws Exception {
        String url = newDatabase();
        flyway(url).migrate();
        for (String[] entry : manifest()) {
            Files.write(directory.resolve(entry[1]), resource("db/migration/" + entry[1]));
        }
        String last = "V15__unicode_descriptions.sql";
        Files.write(directory.resolve(last), resource("db/migration/" + last));
        Files.writeString(directory.resolve(last), "\nSELECT 42;\n", java.nio.file.StandardOpenOption.APPEND);
        Flyway changed = Flyway.configure().dataSource(url, sql.getUsername(), sql.getPassword())
                .defaultSchema("dbo").locations("filesystem:" + directory.toAbsolutePath()).load();
        assertThrows(FlywayException.class, changed::validate);
    }

    @Test void failedCartUpgradePreservesDuplicatesAndCanBeRetriedAfterReconciliation() throws Exception {
        String url = newDatabase();
        try (var connection = connect(url); var statement = connection.createStatement()) {
            runScript(statement, "schema-sqlserver.sql");
            statement.execute("DROP INDEX UX_cart_items_cart_product_present ON dbo.cart_items");
            statement.execute("INSERT INTO dbo.users(username,email,password_hash,first_name,last_name) VALUES('qa_dup','qa_dup@example.test','unused',N'Test',N'Cart')");
            statement.execute("INSERT INTO dbo.carts(customer_id) SELECT id FROM dbo.users");
            statement.execute("INSERT INTO dbo.product_categories(name) VALUES(N'qa_Category')");
            statement.execute("INSERT INTO dbo.products(name,sku,category_id) SELECT N'qa_Product','qa_sku',id FROM dbo.product_categories");
            for (int i = 0; i < 2; i++) statement.execute("INSERT INTO dbo.cart_items(cart_id,product_id,item_type,quantity) SELECT c.id,p.id,'PRODUCT',1 FROM dbo.carts c CROSS JOIN dbo.products p");
            Flyway flyway = flyway(url);
            flyway.baseline();
            assertThrows(FlywayException.class, flyway::migrate);
            assertEquals(2, count(statement, "SELECT COUNT(*) FROM dbo.cart_items"));
            assertEquals(0, count(statement, "SELECT @@TRANCOUNT"));
            // ONLY this disposable fixture is reconciled; production requires an audited decision.
            statement.execute("UPDATE dbo.cart_items SET quantity=2 WHERE id=(SELECT MIN(id) FROM dbo.cart_items)");
            statement.execute("DELETE FROM dbo.cart_items WHERE id=(SELECT MAX(id) FROM dbo.cart_items)");
            flyway.repair(); // Remove the failed nontransactional history entry, not a checksum mismatch.
            flyway.migrate();
            assertEquals(2, count(statement, "SELECT SUM(quantity) FROM dbo.cart_items"));
            flyway.validate();
        }
    }

    private static void assertConstraints(Connection connection) throws Exception {
        try (var s = connection.createStatement()) {
            s.execute("INSERT INTO dbo.users(username,email,password_hash,first_name,last_name) VALUES('qa_second','qa_second@example.test','unused',N'Test',N'Other')");
            s.execute("INSERT INTO dbo.orders(customer_id,order_number,checkout_key) SELECT id,username,'dfd0abf4-f46e-4c05-b4e8-e838f8bac58d' FROM dbo.users");
            assertThrows(SQLException.class, () -> s.execute("INSERT INTO dbo.orders(customer_id,order_number,checkout_key) SELECT id,'qa_duplicate','dfd0abf4-f46e-4c05-b4e8-e838f8bac58d' FROM dbo.users WHERE username='qa_customer'"));
            assertThrows(SQLException.class, () -> s.execute("UPDATE dbo.pets SET availability_status='INVALID'"));
            assertThrows(SQLException.class, () -> s.execute("UPDATE dbo.inventory SET quantity=-1"));
            connection.setAutoCommit(false);
            try {
                assertEquals(1, s.executeUpdate("UPDATE dbo.inventory SET quantity=quantity-1 WHERE quantity>0"));
                connection.rollback();
            } finally {
                connection.setAutoCommit(true);
            }
            assertEquals(1, count(s, "SELECT SUM(quantity) FROM dbo.inventory"));
        }
    }

    private static String snapshot(Statement s) throws SQLException {
        // Explicit legacy columns: new nullable/defaulted columns must not mask changed old data.
        var out = new StringBuilder();
        for (String query : List.of(
                "SELECT id,username,first_name,last_name,created_at,updated_at FROM dbo.users ORDER BY id",
                "SELECT id,name,species_id,price,description,availability_status,created_at,updated_at FROM dbo.pets ORDER BY id",
                "SELECT id,pet_id,quantity,updated_at FROM dbo.inventory ORDER BY id",
                "SELECT id,customer_id,order_number,notes,total_amount,status,created_at,updated_at FROM dbo.orders ORDER BY id",
                "SELECT id,pet_id,image_url FROM dbo.pet_images ORDER BY id",
                "SELECT id,inventory_id,quantity,performed_by,notes,created_at FROM dbo.inventory_transactions ORDER BY id")) {
            try (var rows = s.executeQuery(query)) {
                while (rows.next()) {
                    for (int i = 1; i <= rows.getMetaData().getColumnCount(); i++) out.append(rows.getString(i)).append('|');
                    out.append('\n');
                }
            }
        }
        return out.toString();
    }

    private static String newDatabase() throws SQLException {
        String name = "qa_flyway_" + UUID.randomUUID().toString().replace("-", "");
        try (var c = connect(sql.getJdbcUrl()); var s = c.createStatement()) {
            s.execute("CREATE DATABASE " + name);
        }
        return sql.getJdbcUrl() + ";databaseName=" + name;
    }

    private static Connection connect(String url) throws SQLException {
        return DriverManager.getConnection(url, sql.getUsername(), sql.getPassword());
    }

    private static Flyway flyway(String url) {
        return Flyway.configure().dataSource(url, sql.getUsername(), sql.getPassword())
                .defaultSchema("dbo").schemas("dbo").createSchemas(false).baselineVersion("1")
                .baselineOnMigrate(false).cleanDisabled(true).locations("classpath:db/migration").load();
    }

    static List<String[]> manifest() throws Exception {
        var entries = new ArrayList<String[]>();
        for (String line : new String(resource("db/migration/legacy-sha256.txt"), StandardCharsets.UTF_8).lines().toList()) {
            if (!line.isBlank()) entries.add(line.split("  "));
        }
        return entries;
    }

    static byte[] resource(String path) throws Exception {
        try (var input = FlywayMigrationTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, path);
            return input.readAllBytes();
        }
    }

    private static void runScript(Statement s, String path) throws Exception {
        String script = new String(resource(path), StandardCharsets.UTF_8);
        for (String batch : script.split("(?im)^\\s*GO\\s*$")) if (!batch.isBlank()) s.execute(batch);
    }

    private static int count(Statement s, String query) throws SQLException {
        try (var rows = s.executeQuery(query)) {
            assertTrue(rows.next());
            return rows.getInt(1);
        }
    }
}
