package de.vptr.midas.gui.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Stores authorization headers behind opaque session ids. */
@ApplicationScoped
public final class SessionAuthorizationStore {

    static final Duration SESSION_TTL = Duration.ofMinutes(30);

    private static final int MAX_SESSIONS = 10_000;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Inject
    SessionKeyManager keyManager;

    private final Clock clock;
    private final Cache<String, AuthorizationEntry> cache;
    @Nullable
    private SecretKey encryptionKey;

    /**
     * CDI no-arg constructor. The encryption key is set via {@link #init()} after injection.
     */
    public SessionAuthorizationStore() {
        this.clock = Clock.systemUTC();
        this.cache = Caffeine.newBuilder().expireAfterWrite(SESSION_TTL).maximumSize(MAX_SESSIONS).build();
    }

    /** Package-private constructor for unit tests — bypasses CDI and key manager. */
    SessionAuthorizationStore(final Clock clock, final SecretKey encryptionKey) {
        this.clock = clock;
        this.encryptionKey = encryptionKey;
        this.cache = Caffeine.newBuilder().expireAfterWrite(SESSION_TTL).maximumSize(MAX_SESSIONS).build();
    }

    @PostConstruct
    void init() {
        this.encryptionKey = this.keyManager.getKey();
    }

    /**
     * Creates a new session id and stores the authorization header behind it.
     *
     * @param authorizationHeader
     *            the backend authorization header
     * @return generated opaque session id
     */
    public String createSession(final String authorizationHeader) {
        final String sessionId = UUID.randomUUID().toString();
        this.cache.put(sessionId,
                new AuthorizationEntry(encrypt(authorizationHeader), this.clock.instant().plus(SESSION_TTL)));
        return sessionId;
    }

    /**
     * Returns authorization header for the given session id.
     *
     * @param sessionId
     *            opaque session id
     * @return authorization header or null
     */
    @Nullable
    public String resolveAuthorization(@Nullable final String sessionId) {
        if (sessionId == null) {
            return null;
        }
        final var now = this.clock.instant();
        final var entry = this.cache.getIfPresent(sessionId);
        if (entry == null || entry.isExpired(now)) {
            this.cache.invalidate(sessionId);
            return null;
        }
        this.cache.put(sessionId, entry.refresh(now.plus(SESSION_TTL)));
        return this.decrypt(entry.authorizationHeader());
    }

    /**
     * Removes stored authorization for the given session id.
     *
     * @param sessionId
     *            opaque session id
     */
    public void revokeSession(@Nullable final String sessionId) {
        if (sessionId != null) {
            this.cache.invalidate(sessionId);
        }
    }

    int storedSessionCount() {
        this.cache.cleanUp();
        return (int) this.cache.estimatedSize();
    }

    private String encrypt(final String plaintext) {
        final SecretKey key = Objects.requireNonNull(this.encryptionKey, "encryptionKey not initialized");
        try {
            final var iv = new byte[GCM_IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            final var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            final var ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            final var combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt authorization header", e);
        }
    }

    private String decrypt(final String ciphertext) {
        final SecretKey key = Objects.requireNonNull(this.encryptionKey, "encryptionKey not initialized");
        try {
            final var combined = Base64.getDecoder().decode(ciphertext);
            final var iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            final var encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            final var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt authorization header", e);
        }
    }

    private record AuthorizationEntry(String authorizationHeader, Instant expiresAt) {
        private boolean isExpired(final Instant now) {
            return !this.expiresAt.isAfter(now);
        }

        private AuthorizationEntry refresh(final Instant expiresAt) {
            return new AuthorizationEntry(this.authorizationHeader, expiresAt);
        }
    }
}
