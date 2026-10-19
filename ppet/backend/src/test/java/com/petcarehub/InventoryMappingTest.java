package com.petcarehub;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
class InventoryMappingTest {
    @Autowired JdbcTemplate jdbc;

    @Test void hibernateDoesNotRecreateUnfilteredInventoryUniqueness() {
        // Checks generated ORM DDL only. H2 cannot prove SQL Server filtered-index/NULL semantics;
        // those are covered separately by the opt-in SqlServerConstraintTest.
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS "
                + "WHERE TABLE_NAME='INVENTORY' AND CONSTRAINT_TYPE='UNIQUE'", Integer.class));
    }
}
