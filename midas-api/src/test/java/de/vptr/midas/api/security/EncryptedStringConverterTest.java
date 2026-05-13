package de.vptr.midas.api.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class EncryptedStringConverterTest {

    @Test
    void testConvert() {
        final EncryptedStringConverter converter = new EncryptedStringConverter();
        final String plainText = "converter-test";

        final String dbValue = converter.convertToDatabaseColumn(plainText);
        assertNotNull(dbValue);
        assertTrue(dbValue.startsWith("1|"));

        final String entityValue = converter.convertToEntityAttribute(dbValue);
        assertEquals(plainText, entityValue);
    }

    @Test
    void testConvertNull() {
        final EncryptedStringConverter converter = new EncryptedStringConverter();
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
