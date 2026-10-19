package com.petcarehub;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.sql.DriverManager;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@EnabledIfEnvironmentVariable(named="RUN_SQLSERVER_TESTS", matches="true")
@Testcontainers(disabledWithoutDocker=true)
class SqlServerConstraintTest {
    @Container static final MSSQLServerContainer<?> sql = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();
    @Test void nullableUniqueIndexesAndNonnegativeStock() throws Exception {
        // Container-only scratch database; never connects to the developer's SQL Server.
        try (var connection=DriverManager.getConnection(sql.getJdbcUrl(),sql.getUsername(),sql.getPassword()); var statement=connection.createStatement()) {
            runScript(statement, "schema-sqlserver.sql");
            assertEquals(0, count(statement, "SELECT COUNT(*) FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.inventory') AND is_unique=1 AND has_filter=0 AND is_primary_key=0"));
            for (int repeat = 0; repeat < 2; repeat++) migrate(statement);
            statement.execute("INSERT INTO dbo.product_categories(name) VALUES(N'Audit')");
            statement.execute("INSERT INTO dbo.products(name,sku,category_id) SELECT N'A','audit-a',id FROM dbo.product_categories WHERE name=N'Audit'");
            statement.execute("INSERT INTO dbo.products(name,sku,category_id) SELECT N'B','audit-b',id FROM dbo.product_categories WHERE name=N'Audit'");
            assertEquals(2, statement.executeUpdate("INSERT INTO dbo.inventory(product_id,quantity) SELECT id,1 FROM dbo.products WHERE sku IN ('audit-a','audit-b')"));
            assertThrows(SQLException.class,()->statement.execute("INSERT INTO dbo.inventory(product_id,quantity) SELECT id,1 FROM dbo.products WHERE sku='audit-a'"));
            assertThrows(SQLException.class,()->statement.execute("UPDATE dbo.inventory SET quantity=-1"));
            assertEquals(2, statement.executeUpdate("UPDATE dbo.inventory SET quantity=quantity-1 WHERE quantity>=1"));
            assertEquals(0, statement.executeUpdate("UPDATE dbo.inventory SET quantity=quantity-1 WHERE quantity>=1"));
            statement.execute("INSERT INTO dbo.users(username,email,password_hash,first_name,last_name) VALUES('cart-test','cart@example.test','unused','Test','Cart')");
            statement.execute("INSERT INTO dbo.carts(customer_id) SELECT id FROM dbo.users WHERE username='cart-test'");
            assertEquals(2, statement.executeUpdate("INSERT INTO dbo.cart_items(cart_id,product_id,item_type,quantity) SELECT c.id,p.id,'PRODUCT',1 FROM dbo.carts c CROSS JOIN dbo.products p WHERE p.sku IN ('audit-a','audit-b')"));
            assertThrows(SQLException.class, () -> statement.execute("INSERT INTO dbo.cart_items(cart_id,product_id,item_type,quantity) SELECT c.id,p.id,'PRODUCT',1 FROM dbo.carts c CROSS JOIN dbo.products p WHERE p.sku='audit-a'"));
            statement.execute("INSERT INTO dbo.species(name) VALUES(N'SQLTest')");
            statement.execute("INSERT INTO dbo.pets(name,species_id) SELECT N'Pet A',id FROM dbo.species WHERE name=N'SQLTest'");
            statement.execute("INSERT INTO dbo.pets(name,species_id) SELECT N'Pet B',id FROM dbo.species WHERE name=N'SQLTest'");
            assertEquals(2, statement.executeUpdate("INSERT INTO dbo.inventory(pet_id,quantity) SELECT id,1 FROM dbo.pets"));
            assertThrows(SQLException.class, () -> statement.execute("INSERT INTO dbo.inventory(pet_id,quantity) SELECT id,1 FROM dbo.pets WHERE name=N'Pet A'"));
            assertEquals(2, statement.executeUpdate("INSERT INTO dbo.cart_items(cart_id,pet_id,item_type,quantity) SELECT c.id,p.id,'PET',1 FROM dbo.carts c CROSS JOIN dbo.pets p"));
            runScript(statement, "schema-sqlserver.sql"); migrate(statement);
            assertEquals(4, count(statement, "SELECT COUNT(*) FROM dbo.inventory"));
            assertEquals(4, count(statement, "SELECT COUNT(*) FROM dbo.cart_items"));
        }
    }

    @Test void migratesGeneratedLegacyUniquenessWithoutLosingExistingStock() throws Exception {
        try (var connection=DriverManager.getConnection(sql.getJdbcUrl(),sql.getUsername(),sql.getPassword()); var statement=connection.createStatement()) {
            statement.execute("CREATE DATABASE inventory_legacy_test"); connection.setCatalog("inventory_legacy_test");
            runScript(statement, "schema-sqlserver.sql");
            statement.execute("DROP INDEX UX_inventory_pet_present ON dbo.inventory");
            statement.execute("DROP INDEX UX_inventory_product_present ON dbo.inventory");
            statement.execute("ALTER TABLE dbo.inventory ADD CONSTRAINT UK_hibernate_pet_example UNIQUE(pet_id)");
            statement.execute("CREATE UNIQUE INDEX UK_hibernate_product_example ON dbo.inventory(product_id)");
            statement.execute("INSERT INTO dbo.product_categories(name) VALUES(N'Legacy')");
            statement.execute("INSERT INTO dbo.products(name,sku,category_id) SELECT N'A','legacy-a',id FROM dbo.product_categories");
            statement.execute("INSERT INTO dbo.inventory(product_id,quantity) SELECT id,7 FROM dbo.products");
            migrate(statement); migrate(statement);
            assertEquals(7, count(statement, "SELECT quantity FROM dbo.inventory"));
            assertEquals(0, count(statement, "SELECT COUNT(*) FROM sys.indexes WHERE object_id=OBJECT_ID('dbo.inventory') AND is_unique=1 AND has_filter=0 AND is_primary_key=0"));
            statement.execute("INSERT INTO dbo.products(name,sku,category_id) SELECT N'B','legacy-b',id FROM dbo.product_categories");
            assertEquals(1, statement.executeUpdate("INSERT INTO dbo.inventory(product_id,quantity) SELECT id,3 FROM dbo.products WHERE sku='legacy-b'"));
            assertThrows(SQLException.class, () -> statement.execute("INSERT INTO dbo.inventory(product_id,quantity) SELECT id,1 FROM dbo.products WHERE sku='legacy-a'"));
        }
    }

    @Test void cartMigrationRejectsDuplicatesWithoutDeletingDataOrLeavingTransactionOpen() throws Exception {
        try (var connection=DriverManager.getConnection(sql.getJdbcUrl(),sql.getUsername(),sql.getPassword()); var statement=connection.createStatement()) {
            statement.execute("CREATE DATABASE cart_legacy_test"); connection.setCatalog("cart_legacy_test");
            runScript(statement, "schema-sqlserver.sql");
            statement.execute("DROP INDEX UX_cart_items_cart_product_present ON dbo.cart_items");
            statement.execute("INSERT INTO dbo.users(username,email,password_hash,first_name,last_name) VALUES('dup-test','dup@example.test','unused','Test','Cart')");
            statement.execute("INSERT INTO dbo.carts(customer_id) SELECT id FROM dbo.users");
            statement.execute("INSERT INTO dbo.product_categories(name) VALUES(N'Duplicate')");
            statement.execute("INSERT INTO dbo.products(name,sku,category_id) SELECT N'A','duplicate-a',id FROM dbo.product_categories");
            for (int i=0; i<2; i++) statement.execute("INSERT INTO dbo.cart_items(cart_id,product_id,item_type,quantity) SELECT c.id,p.id,'PRODUCT',1 FROM dbo.carts c CROSS JOIN dbo.products p");
            assertThrows(SQLException.class, () -> runScript(statement, "migrations/002_cart_product_unique.sql"));
            assertEquals(2, count(statement, "SELECT COUNT(*) FROM dbo.cart_items"));
            assertEquals(0, count(statement, "SELECT @@TRANCOUNT"));
        }
    }

    private void migrate(java.sql.Statement statement) throws Exception {
        for (String resource : new String[]{"migrations/001_inventory_nullable_unique.sql", "migrations/002_cart_product_unique.sql", "migrations/003_inventory_generated_unique.sql", "migrations/004_pet_purchase.sql"})
            runScript(statement, resource);
    }

    private void runScript(java.sql.Statement statement, String resource) throws Exception {
        try (var input=getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            String script=new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            for (String batch : script.split("(?im)^\\s*GO\\s*$")) if (!batch.isBlank()) statement.execute(batch);
        }
    }

    private int count(java.sql.Statement statement, String query) throws SQLException {
        try (var result=statement.executeQuery(query)) { assertTrue(result.next()); return result.getInt(1); }
    }
}
