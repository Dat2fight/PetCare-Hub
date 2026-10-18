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
            for (String resource : new String[]{"schema-sqlserver.sql", "migrations/001_inventory_nullable_unique.sql", "migrations/002_cart_product_unique.sql",
                    "schema-sqlserver.sql", "migrations/001_inventory_nullable_unique.sql", "migrations/002_cart_product_unique.sql"}) {
                try (var input=getClass().getClassLoader().getResourceAsStream(resource)) {
                    assertNotNull(input);
                    String script=new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                    for (String batch : script.split("(?im)^\\s*GO\\s*$")) if (!batch.isBlank()) statement.execute(batch);
                }
            }
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
        }
    }
}
