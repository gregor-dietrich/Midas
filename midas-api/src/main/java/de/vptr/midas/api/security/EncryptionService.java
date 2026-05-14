package de.vptr.midas.api.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Service for encrypting and decrypting data at rest. */
@ApplicationScoped
public class EncryptionService {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Inject
    EncryptionKeyManager keyManager;

    // Lazily derived sub-keys — derived once per JVM lifecycle via HKDF-Expand.
    @Nullable
    private volatile SecretKey encryptionSubKey;
    @Nullable
    private volatile SecretKey hmacSubKey;

    private SecretKey encryptionSubKey() {
        if (this.encryptionSubKey == null) {
            synchronized (this) {
                if (this.encryptionSubKey == null) {
                    this.encryptionSubKey = deriveSubKey(this.keyManager.getKey(), "encryption");
                }
            }
        }
        return this.encryptionSubKey;
    }

    private SecretKey hmacSubKey() {
        if (this.hmacSubKey == null) {
            synchronized (this) {
                if (this.hmacSubKey == null) {
                    this.hmacSubKey = deriveSubKey(this.keyManager.getKey(), "blind-index");
                }
            }
        }
        return this.hmacSubKey;
    }

    /**
     * Derives a 256-bit AES sub-key from the master key using one step of HKDF-Expand (RFC 5869 §2.3): OKM =
     * HMAC-SHA256(masterKey, context || 0x01). The master key already has 256 bits of entropy, so the Extract phase is
     * omitted.
     */
    @SuppressWarnings("PMD.HardCodedCryptoKey")
    private static SecretKey deriveSubKey(final SecretKey masterKey, final String context) {
        try {
            final var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(masterKey.getEncoded(), "HmacSHA256"));
            final var contextBytes = context.getBytes(StandardCharsets.UTF_8);
            final var input = new byte[contextBytes.length + 1];
            System.arraycopy(contextBytes, 0, input, 0, contextBytes.length);
            input[contextBytes.length] = 0x01;
            return new SecretKeySpec(mac.doFinal(input), "AES");
        } catch (final RuntimeException | GeneralSecurityException e) {
            throw new IllegalStateException("Key derivation failed", e);
        }
    }

    /** Encrypts a string into the envelope format. */
    @Nullable
    public String encrypt(@Nullable final String plainText) {
        if (plainText == null) {
            return null;
        }

        try {
            final var cipher = Cipher.getInstance(ALGORITHM);
            final var iv = new byte[IV_LENGTH];

            SECURE_RANDOM.nextBytes(iv);
            final GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, this.encryptionSubKey(), spec);
            final var cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            final var base64Iv = Base64.getEncoder().encodeToString(iv);
            final var base64CipherText = Base64.getEncoder().encodeToString(cipherText);

            return "1|" + base64Iv + "|" + base64CipherText;
        } catch (final RuntimeException | GeneralSecurityException e) {
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    /** Decrypts an enveloped string. */
    @Nullable
    @SuppressWarnings("StringSplitter")
    public String decrypt(@Nullable final String cipherTextEnv) {
        if (cipherTextEnv == null) {
            return null;
        }

        final var parts = cipherTextEnv.split("\\|");
        if (parts.length != 3 || !"1".equals(parts[0])) {
            return cipherTextEnv;
        }

        try {
            final var iv = Base64.getDecoder().decode(parts[1]);
            final var cipherText = Base64.getDecoder().decode(parts[2]);
            final var cipher = Cipher.getInstance(ALGORITHM);
            final var spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, this.encryptionSubKey(), spec);
            final var plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (final RuntimeException | GeneralSecurityException e) {
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    /** Generates a blind index (HMAC) for exact matching. */
    @Nullable
    public String generateBlindIndex(@Nullable final String plainText) {
        if (plainText == null) {
            return null;
        }

        try {
            final var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(this.hmacSubKey().getEncoded(), "HmacSHA256"));
            final var hash = mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (final RuntimeException | GeneralSecurityException e) {
            throw new IllegalStateException("Blind index generation failed", e);
        }
    }
}
