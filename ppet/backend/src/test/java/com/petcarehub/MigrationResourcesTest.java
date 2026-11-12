package com.petcarehub;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class MigrationResourcesTest {
    @Test void legacyCopiesMatchOriginalsAndPinnedChecksums() throws Exception {
        String manifest = text("db/migration/legacy-sha256.txt");
        var lines = manifest.lines().filter(line -> !line.isBlank()).toList();
        assertEquals(14, lines.size());
        for (int index = 0; index < lines.size(); index++) {
            String[] entry = lines.get(index).split("  ");
            assertEquals(3, entry.length);
            assertTrue(entry[1].startsWith("V" + (index + 1) + "__"));
            byte[] original = bytes(entry[2]);
            byte[] copy = bytes("db/migration/" + entry[1]);
            assertArrayEquals(original, copy, entry[1]);
            // Git converts CRLF on Windows; pin logical source bytes using LF on every OS.
            byte[] canonical = new String(copy, StandardCharsets.UTF_8).replace("\r\n", "\n")
                    .getBytes(StandardCharsets.UTF_8);
            assertEquals(entry[0], HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical)), entry[1]);
            if (new String(copy, StandardCharsets.UTF_8).contains("BEGIN TRANSACTION")) {
                assertEquals("executeInTransaction=false", text("db/migration/" + entry[1] + ".conf").strip());
            }
        }
    }

    @Test void defaultsRequireExplicitOnboardingAndNeverInitializeLegacyScripts() throws Exception {
        String defaults = text("application.yml");
        assertTrue(defaults.contains("baseline-on-migrate: ${FLYWAY_BASELINE_ON_MIGRATE:false}"));
        assertTrue(defaults.contains("clean-disabled: true"));
        assertTrue(defaults.contains("ddl-auto: validate"));
        assertTrue(defaults.contains("mode: never"));
        assertTrue(text("application-test.yml").contains("flyway:\n    enabled: false"));
    }

    private static String text(String path) throws Exception {
        return new String(bytes(path), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static byte[] bytes(String path) throws Exception {
        try (var stream = MigrationResourcesTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, path);
            return stream.readAllBytes();
        }
    }
}
