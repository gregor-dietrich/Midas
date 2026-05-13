package de.vptr.midas.api.security;

import jakarta.annotation.Nullable;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA converter for encrypting strings. */
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    @Override
    @Nullable
    public String convertToDatabaseColumn(@Nullable final String attribute) {
        if (attribute == null) {
            return null;
        }
        return CDI.current().select(EncryptionService.class).get().encrypt(attribute);
    }

    @Override
    @Nullable
    public String convertToEntityAttribute(@Nullable final String dbData) {
        if (dbData == null) {
            return null;
        }
        return CDI.current().select(EncryptionService.class).get().decrypt(dbData);
    }
}
