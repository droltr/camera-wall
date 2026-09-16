package local.camerawall;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/** API 21-compatible authenticated encryption for user-selected settings backups. */
final class BackupCipher {
    static final int ITERATIONS = 200000;
    static final int SALT_BYTES = 16;
    static final int IV_BYTES = 16;
    static final int MAC_BYTES = 32;
    private static final int KEY_BYTES = 64;
    private static final String KDF_NAME = "PBKDF2WithHmacSHA1";
    private static final String MAC_NAME = "HmacSHA256";
    private static final String MAC_CONTEXT = "CameraWall backup version 1";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    static final class EncryptedData {
        final byte[] salt;
        final byte[] iv;
        final byte[] ciphertext;
        final byte[] mac;

        EncryptedData(byte[] salt, byte[] iv, byte[] ciphertext, byte[] mac) {
            this.salt = salt;
            this.iv = iv;
            this.ciphertext = ciphertext;
            this.mac = mac;
        }
    }

    private BackupCipher() { }

    static EncryptedData encrypt(char[] passphrase, byte[] plaintext) throws Exception {
        if (passphrase == null || passphrase.length < 12) throw new IllegalArgumentException("Passphrase is too short");
        byte[] salt = randomBytes(SALT_BYTES);
        byte[] iv = randomBytes(IV_BYTES);
        byte[] keys = deriveKeys(passphrase, salt);
        try {
            byte[] encryptionKey = Arrays.copyOfRange(keys, 0, 32);
            byte[] ciphertext;
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new IvParameterSpec(iv));
                ciphertext = cipher.doFinal(plaintext);
            } finally {
                Arrays.fill(encryptionKey, (byte) 0);
            }
            return new EncryptedData(salt, iv, ciphertext, authenticate(salt, iv, ciphertext, keys));
        } finally {
            Arrays.fill(keys, (byte) 0);
        }
    }

    static byte[] decrypt(char[] passphrase, byte[] salt, byte[] iv, byte[] ciphertext, byte[] suppliedMac) throws Exception {
        if (passphrase == null || passphrase.length < 12 || salt == null || salt.length != SALT_BYTES
            || iv == null || iv.length != IV_BYTES || ciphertext == null || ciphertext.length == 0
            || ciphertext.length % 16 != 0 || suppliedMac == null || suppliedMac.length != MAC_BYTES) {
            throw new IllegalArgumentException("Invalid encrypted backup fields");
        }
        byte[] keys = deriveKeys(passphrase, salt);
        try {
            byte[] expectedMac = authenticate(salt, iv, ciphertext, keys);
            if (!MessageDigest.isEqual(expectedMac, suppliedMac)) throw new SecurityException("Backup authentication failed");

            byte[] encryptionKey = Arrays.copyOfRange(keys, 0, 32);
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new IvParameterSpec(iv));
                return cipher.doFinal(ciphertext);
            } finally {
                Arrays.fill(encryptionKey, (byte) 0);
            }
        } finally {
            Arrays.fill(keys, (byte) 0);
        }
    }

    private static byte[] deriveKeys(char[] passphrase, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(passphrase, salt, ITERATIONS, KEY_BYTES * 8);
        try {
            return SecretKeyFactory.getInstance(KDF_NAME).generateSecret(spec).getEncoded();
        } finally {
            spec.clearPassword();
        }
    }

    private static byte[] authenticate(byte[] salt, byte[] iv, byte[] ciphertext, byte[] keys) throws Exception {
        Mac mac = Mac.getInstance(MAC_NAME);
        byte[] macKey = Arrays.copyOfRange(keys, 32, 64);
        try {
            mac.init(new SecretKeySpec(macKey, MAC_NAME));
            mac.update(MAC_CONTEXT.getBytes(UTF8));
            mac.update(salt);
            mac.update(iv);
            return mac.doFinal(ciphertext);
        } finally {
            Arrays.fill(macKey, (byte) 0);
        }
    }

    private static byte[] randomBytes(int length) {
        byte[] result = new byte[length];
        new SecureRandom().nextBytes(result);
        return result;
    }
}
