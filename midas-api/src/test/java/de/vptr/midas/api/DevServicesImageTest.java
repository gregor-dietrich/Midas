package de.vptr.midas.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Guards that dev and test run the same postgres build as docker-compose.yml's db. Renovate bumps both pins in one PR
 * only while its regex manager still matches the properties file, and a bump by hand can miss one of them.
 */
class DevServicesImageTest {

    private static final Pattern COMPOSE = Pattern.compile("image: postgres:(\\S+)@(sha256:[0-9a-f]{64})");

    private static final Pattern DEV_SERVICES = Pattern
            .compile("# renovate: postgres:(\\S+)\\n[^\\n]*\\.devservices\\.image-name=postgres@(sha256:[0-9a-f]{64})");

    @Test
    void devServicesPinsComposeDbImage() throws Exception {
        final Matcher compose = find(COMPOSE, Path.of("../docker-compose.yml"));
        final Matcher devServices = find(DEV_SERVICES, Path.of("src/main/resources/application.properties"));

        assertEquals(compose.group(1), devServices.group(1), "postgres tag differs from docker-compose.yml's db");
        assertEquals(compose.group(2), devServices.group(2), "postgres digest differs from docker-compose.yml's db");
    }

    private static Matcher find(final Pattern pattern, final Path file) throws Exception {
        final Matcher matcher = pattern.matcher(Files.readString(file));
        assertTrue(matcher.find(), file + " no longer matches " + pattern + ", which renovate.json's managers read");
        return matcher;
    }
}
