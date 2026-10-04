package de.vptr.midas.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

/**
 * Every foreign key in the public schema has an index whose leading columns are the key's columns, in order. Without
 * one, deleting or re-keying a referenced row scans the whole referencing table. Dev Services runs the same Flyway
 * migrations as production, so this checks the production schema.
 */
@QuarkusTest
class ForeignKeyIndexTest {

    // A partial index does not cover every row; key columns are indkey[0..indnkeyatts-1]. Plain subscripts instead
    // of an array slice: Hibernate would read the slice's colon as a named parameter.
    private static final String UNINDEXED_FOREIGN_KEYS = """
            SELECT format('%s on %s (%s)', c.conname, CAST(c.conrelid AS regclass),
                   (SELECT string_agg(a.attname, ', ' ORDER BY k.ord)
                      FROM unnest(c.conkey) WITH ORDINALITY AS k(attnum, ord)
                      JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = k.attnum))
              FROM pg_constraint c
             WHERE c.contype = 'f'
               AND c.connamespace = CAST('public' AS regnamespace)
               AND NOT EXISTS (
                   SELECT 1 FROM pg_index i
                    WHERE i.indrelid = c.conrelid
                      AND i.indisvalid
                      AND i.indpred IS NULL
                      AND i.indnkeyatts >= cardinality(c.conkey)
                      AND NOT EXISTS (
                          SELECT 1 FROM generate_subscripts(c.conkey, 1) AS s(n)
                           WHERE i.indkey[s.n - 1] <> c.conkey[s.n]))
             ORDER BY 1
            """;

    @Inject
    EntityManager entityManager;

    @SuppressWarnings("unchecked")
    private List<String> unindexedForeignKeys() {
        return this.entityManager.createNativeQuery(UNINDEXED_FOREIGN_KEYS, String.class).getResultList();
    }

    @Test
    @TestTransaction
    void testEveryForeignKeyHasLeadingIndex() {
        assertEquals(List.of(), this.unindexedForeignKeys(),
                "foreign keys without an index on their leading column(s)");
    }

    @Test
    @TestTransaction
    void testQueryReportsForeignKeyWithOnlyTrailingIndex() {
        // Rolled back with the test transaction; an index with user_id second does not serve the key.
        this.entityManager.createNativeQuery("""
                CREATE TABLE fk_index_probe (id BIGINT PRIMARY KEY, user_id BIGINT REFERENCES users (id));
                CREATE INDEX fk_index_probe_trailing ON fk_index_probe (id, user_id)
                """).executeUpdate();

        assertEquals(List.of("fk_index_probe_user_id_fkey on fk_index_probe (user_id)"), this.unindexedForeignKeys());
    }
}
