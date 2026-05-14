package de.vptr.midas.api.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
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
            cipher.init(Cipher.ENCRYPT_MODE, this.keyManager.getKey(), spec);
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
            cipher.init(Cipher.DECRYPT_MODE, this.keyManager.getKey(), spec);
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
            mac.init(new SecretKeySpec(this.keyManager.getKey().getEncoded(), "HmacSHA256"));
            final var hash = mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (final RuntimeException | GeneralSecurityException e) {
            throw new IllegalStateException("Blind index generation failed", e);
        }
    }
}
