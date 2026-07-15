package com.gentlemanstore;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Offline validacija Liquibase changelog-a: parsira master changelog i sve
 * uključene migracije (V1–V25) i pokreće liquibase validate bez žive baze.
 * Hvata sintaksne greške, nepostojeće include fajlove i duple changeset ID-eve
 * pre nego što migracija stigne do startup-a aplikacije.
 */
public class LiquibaseChangelogValidationTest {

    @Test
    void changelogParsesAndValidatesOffline() {
        assertDoesNotThrow(() -> {
            Database database = DatabaseFactory.getInstance().openDatabase(
                    "offline:postgresql",
                    null, null, null, null, null, null,
                    new ClassLoaderResourceAccessor());
            try (Liquibase liquibase = new Liquibase(
                    "db/changelog/db.changelog-master.xml",
                    new ClassLoaderResourceAccessor(),
                    database)) {
                liquibase.validate();
            }
        });
    }
}
