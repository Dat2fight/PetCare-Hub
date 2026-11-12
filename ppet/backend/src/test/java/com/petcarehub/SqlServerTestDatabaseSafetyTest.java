package com.petcarehub;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class SqlServerTestDatabaseSafetyTest {
    @Test void rejectsApplicationDatabasesEvenIfAccidentallyAddedToOwnedSet() {
        for (String name : Set.of("petcarehub", "master", "petcarehub_test_existing", "petcarehub_test_'; DROP DATABASE petcarehub;--")) {
            assertThrows(IllegalArgumentException.class, () -> SqlServerTestDatabase.requireOwned(name, Set.of(name)));
        }
    }

    @Test void acceptsOnlyExactRandomNameCreatedByThisRun() {
        String owned = "petcarehub_test_" + "a".repeat(32);
        String other = "petcarehub_test_" + "b".repeat(32);
        assertDoesNotThrow(() -> SqlServerTestDatabase.requireOwned(owned, Set.of(owned)));
        assertThrows(IllegalArgumentException.class, () -> SqlServerTestDatabase.requireOwned(other, Set.of(owned)));
        assertThrows(IllegalArgumentException.class, () -> SqlServerTestDatabase.requireOwned(null, Set.of(owned)));
    }

    @Test void localConnectionsAreLoopbackOnlyAndDoNotSelectAnApplicationDatabase() {
        String url = SqlServerTestDatabase.localServerUrl(1433, true);
        assertTrue(url.startsWith("jdbc:sqlserver://localhost:1433;"));
        assertTrue(url.contains("integratedSecurity=true"));
        assertFalse(url.contains("databaseName"));
        assertThrows(IllegalArgumentException.class, () -> SqlServerTestDatabase.localServerUrl(0, true));
        assertThrows(IllegalArgumentException.class, () -> SqlServerTestDatabase.localServerUrl(65536, false));
    }
}
