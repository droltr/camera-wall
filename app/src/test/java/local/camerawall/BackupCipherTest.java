package local.camerawall;

import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

public final class BackupCipherTest {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final char[] PASSWORD = "a-long-test-passphrase".toCharArray();

    @Test public void roundTripsUtf8Payload() throws Exception {
        byte[] original = "camera names and settings · Türkçe".getBytes(UTF8);
        BackupCipher.EncryptedData encrypted = BackupCipher.encrypt(PASSWORD, original);

        byte[] restored = BackupCipher.decrypt(PASSWORD, encrypted.salt, encrypted.iv,
            encrypted.ciphertext, encrypted.mac);

        assertArrayEquals(original, restored);
        Arrays.fill(restored, (byte) 0);
    }

    @Test public void wrongPassphraseIsRejectedBeforeDecryption() throws Exception {
        BackupCipher.EncryptedData encrypted = BackupCipher.encrypt(PASSWORD, new byte[] {1, 2, 3});

        expectAuthenticationFailure("a-different-test-passphrase".toCharArray(), encrypted.salt,
            encrypted.iv, encrypted.ciphertext, encrypted.mac);
    }

    @Test public void ciphertextTamperingIsRejected() throws Exception {
        BackupCipher.EncryptedData encrypted = BackupCipher.encrypt(PASSWORD, new byte[] {4, 5, 6});
        byte[] changedCiphertext = encrypted.ciphertext.clone();
        changedCiphertext[0] ^= 1;

        expectAuthenticationFailure(PASSWORD, encrypted.salt, encrypted.iv, changedCiphertext, encrypted.mac);
    }

    @Test public void ivTamperingIsRejected() throws Exception {
        BackupCipher.EncryptedData encrypted = BackupCipher.encrypt(PASSWORD, new byte[] {7, 8, 9});
        byte[] changedIv = encrypted.iv.clone();
        changedIv[0] ^= 1;

        expectAuthenticationFailure(PASSWORD, encrypted.salt, changedIv, encrypted.ciphertext, encrypted.mac);
    }

    @Test public void shortPassphraseIsRejected() throws Exception {
        try {
            BackupCipher.encrypt("too-short".toCharArray(), new byte[] {1});
            fail("Expected a short passphrase to be rejected");
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }

    private void expectAuthenticationFailure(char[] passphrase, byte[] salt, byte[] iv,
            byte[] ciphertext, byte[] mac) throws Exception {
        try {
            BackupCipher.decrypt(passphrase, salt, iv, ciphertext, mac);
            fail("Expected authentication to fail");
        } catch (SecurityException expected) {
            // MAC is verified before the ciphertext is decrypted.
        }
    }
}
