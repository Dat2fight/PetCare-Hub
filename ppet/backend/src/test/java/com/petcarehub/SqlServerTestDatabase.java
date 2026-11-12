package com.petcarehub;

import org.testcontainers.containers.MSSQLServerContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Test-only lifecycle: never accepts an application database name or DB_URL. */
final class SqlServerTestDatabase implements AutoCloseable {
    private final MSSQLServerContainer<?> container;
    private final String serverUrl;
    private final String username;
    private final String password;
    private final Map<String, Identity> created = new LinkedHashMap<>();

    private record Identity(int id, Timestamp createdAt) {}

    private SqlServerTestDatabase(MSSQLServerContainer<?> container, String serverUrl,
                                  String username, String password) {
        this.container = container;
        this.serverUrl = serverUrl;
        this.username = username;
        this.password = password;
    }

    static SqlServerTestDatabase start() {
        String mode = System.getenv().getOrDefault("SQLSERVER_TEST_MODE", "container");
        if (mode.equals("local")) {
            if (!"true".equals(System.getenv("SQLSERVER_TEST_ALLOW_CREATE"))) {
                throw new IllegalArgumentException("Local tests require SQLSERVER_TEST_ALLOW_CREATE=true");
            }
            int port = Integer.parseInt(System.getenv().getOrDefault("SQLSERVER_TEST_PORT", "1433"));
            String user = System.getenv().getOrDefault("SQLSERVER_TEST_USERNAME", "");
            String pass = System.getenv().getOrDefault("SQLSERVER_TEST_PASSWORD", "");
            if (user.isBlank() != pass.isBlank()) {
                throw new IllegalArgumentException("Supply both SQLSERVER_TEST_USERNAME and SQLSERVER_TEST_PASSWORD, or neither for Windows authentication");
            }
            return new SqlServerTestDatabase(null, localServerUrl(port, user.isBlank()), user, pass);
        }
        if (!mode.equals("container")) throw new IllegalArgumentException("Unknown SQLSERVER_TEST_MODE");
        var container = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();
        container.start();
        String server = container.getJdbcUrl().replaceAll("(?i);databaseName=[^;]*", "");
        return new SqlServerTestDatabase(container, server, container.getUsername(), container.getPassword());
    }

    static String localServerUrl(int port, boolean integrated) {
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid SQL Server test port");
        // Deliberately loopback-only. No free-form JDBC URL, database, instance or remote host.
        return "jdbc:sqlserver://localhost:" + port
                + ";encrypt=true;trustServerCertificate=true;loginTimeout=10"
                + (integrated ? ";integratedSecurity=true;authenticationScheme=NativeAuthentication" : "");
    }

    String newDatabase() throws SQLException {
        String name = "petcarehub_test_" + UUID.randomUUID().toString().replace("-", "");
        try (var c = adminConnection(); var s = c.createStatement()) {
            // CREATE must succeed; never reuse, reset or drop a pre-existing database.
            s.execute("CREATE DATABASE [" + name + "]");
            Identity identity = identity(c, name);
            if (identity == null) throw new SQLException("Created test database was not found");
            created.put(name, identity);
        }
        return url(name);
    }

    String username() { return username; }
    String password() { return password; }

    Connection connect(String url) throws SQLException {
        if (created.keySet().stream().noneMatch(name -> url(name).equals(url))) {
            throw new IllegalArgumentException("Refusing a connection outside this test run's databases");
        }
        var connection = DriverManager.getConnection(url, username, password);
        try {
            requireOwned(connection.getCatalog(), created.keySet());
            return connection;
        } catch (RuntimeException ex) {
            connection.close();
            throw ex;
        }
    }

    static void requireOwned(String name, Set<String> owned) {
        if (name == null || !name.matches("petcarehub_test_[0-9a-f]{32}") || !owned.contains(name)) {
            throw new IllegalArgumentException("Refusing to access or delete a database not created by this test run");
        }
    }

    private String url(String name) { return serverUrl + ";databaseName=" + name; }

    private Connection adminConnection() throws SQLException {
        return DriverManager.getConnection(url("master"), username, password);
    }

    private static Identity identity(Connection connection, String name) throws SQLException {
        try (var query = connection.prepareStatement("SELECT database_id,create_date FROM sys.databases WHERE name=?")) {
            query.setString(1, name);
            try (var rows = query.executeQuery()) {
                return rows.next() ? new Identity(rows.getInt(1), rows.getTimestamp(2)) : null;
            }
        }
    }

    @Override public void close() throws SQLException {
        SQLException failure = null;
        try {
            for (var entry : created.entrySet()) {
                String name = entry.getKey();
                requireOwned(name, created.keySet());
                try (var c = adminConnection(); var s = c.createStatement()) {
                    if (!entry.getValue().equals(identity(c, name))) {
                        throw new SQLException("Test database identity changed; refusing cleanup: " + name);
                    }
                    // Do not force-disconnect other sessions. A leak must fail cleanup visibly.
                    s.execute("DROP DATABASE [" + name + "]");
                } catch (SQLException ex) {
                    if (failure == null) failure = ex;
                    else failure.addSuppressed(ex);
                }
            }
        } finally {
            if (container != null) container.stop();
        }
        if (failure != null) throw failure;
    }
}
