package local.camerawall;

import android.content.Context;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Passphrase-protected, authenticated backup of local cameras and app settings. */
final class SettingsBackup {
    private static final String FORMAT = "camera-wall-encrypted-backup";
    private static final String CIPHER_NAME = "AES-256-CBC-HMAC-SHA256";
    private static final String KDF_NAME = "PBKDF2WithHmacSHA1";
    private static final int VERSION = 1;
    private static final int MAX_BACKUP_BYTES = 8 * 1024 * 1024;
    private static final int MAX_CAMERAS = 1024;
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private SettingsBackup() { }

    static byte[] create(Context context, char[] passphrase) throws Exception {
        if (passphrase == null || passphrase.length < 12) throw new IOException("Backup passphrase is too short");
        JSONObject payload = new JSONObject();
        payload.put("format", "camera-wall-data");
        payload.put("version", VERSION);
        payload.put("settings", new AppSettings(context).backupSnapshot());
        JSONArray cameras = new JSONArray();
        for (CameraSpec camera : new CameraRepository(context).getCameras()) cameras.put(camera.toJson());
        payload.put("cameras", cameras);

        byte[] plaintext = payload.toString().getBytes(UTF8);
        BackupCipher.EncryptedData encrypted;
        try {
            encrypted = BackupCipher.encrypt(passphrase, plaintext);
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }

        JSONObject backup = new JSONObject();
        backup.put("format", FORMAT);
        backup.put("version", VERSION);
        backup.put("cipher", CIPHER_NAME);
        backup.put("kdf", KDF_NAME);
        backup.put("iterations", BackupCipher.ITERATIONS);
        backup.put("salt", Base64.encodeToString(encrypted.salt, Base64.NO_WRAP));
        backup.put("iv", Base64.encodeToString(encrypted.iv, Base64.NO_WRAP));
        backup.put("data", Base64.encodeToString(encrypted.ciphertext, Base64.NO_WRAP));
        backup.put("mac", Base64.encodeToString(encrypted.mac, Base64.NO_WRAP));
        byte[] output = backup.toString().getBytes(UTF8);
        if (output.length > MAX_BACKUP_BYTES) throw new IOException("Backup is too large");
        return output;
    }

    static void restore(Context context, InputStream input, char[] passphrase) throws Exception {
        if (passphrase == null || passphrase.length < 12) throw new IOException("Backup passphrase is invalid");
        JSONObject backup = new JSONObject(new String(readBounded(input), UTF8));
        if (!FORMAT.equals(backup.optString("format")) || backup.optInt("version", -1) != VERSION
            || !CIPHER_NAME.equals(backup.optString("cipher")) || !KDF_NAME.equals(backup.optString("kdf"))
            || backup.optInt("iterations", -1) != BackupCipher.ITERATIONS) throw new IOException("Unsupported backup format");

        byte[] salt = Base64.decode(backup.getString("salt"), Base64.DEFAULT);
        byte[] iv = Base64.decode(backup.getString("iv"), Base64.DEFAULT);
        byte[] ciphertext = Base64.decode(backup.getString("data"), Base64.DEFAULT);
        byte[] suppliedMac = Base64.decode(backup.getString("mac"), Base64.DEFAULT);
        if (salt.length != BackupCipher.SALT_BYTES || iv.length != BackupCipher.IV_BYTES || ciphertext.length == 0
            || ciphertext.length > MAX_BACKUP_BYTES || ciphertext.length % 16 != 0 || suppliedMac.length != 32) {
            throw new IOException("Invalid backup data");
        }

        byte[] plaintext = BackupCipher.decrypt(passphrase, salt, iv, ciphertext, suppliedMac);
        try {
            restorePayload(context, new JSONObject(new String(plaintext, UTF8)));
        } finally {
            Arrays.fill(plaintext, (byte) 0);
        }
    }

    private static void restorePayload(Context context, JSONObject payload) throws Exception {
        if (!"camera-wall-data".equals(payload.optString("format")) || payload.optInt("version", -1) != VERSION) {
            throw new IOException("Invalid backup payload");
        }
        JSONObject nextSettings = payload.getJSONObject("settings");
        JSONArray array = payload.getJSONArray("cameras");
        if (array.length() > MAX_CAMERAS) throw new IOException("Too many cameras in backup");
        ArrayList<CameraSpec> nextCameras = new ArrayList<>();
        for (int index = 0; index < array.length(); index++) {
            JSONObject item = array.getJSONObject(index);
            String name = item.getString("name");
            String url = item.getString("url");
            String username = item.optString("username", "");
            String password = item.optString("password", "");
            if (name.trim().isEmpty() || name.length() > 128 || !url.startsWith("rtsp://") || url.length() > 4096
                || username.length() > 512 || password.length() > 512) throw new IOException("Invalid camera entry in backup");
            RtspCredentials.Values values = RtspCredentials.normalize(url, username, password);
            nextCameras.add(new CameraSpec(name, values.url, values.username, values.password));
        }

        CameraRepository cameras = new CameraRepository(context);
        AppSettings settings = new AppSettings(context);
        List<CameraSpec> previousCameras = cameras.getCameras();
        JSONObject previousSettings = settings.backupSnapshot();
        if (!settings.isValidBackup(nextSettings)) throw new IOException("Invalid app settings in backup");
        if (!cameras.replaceAll(nextCameras)) throw new IOException("Could not save camera backup");
        if (!settings.restoreBackup(nextSettings)) {
            cameras.replaceAll(previousCameras);
            settings.restoreBackup(previousSettings);
            throw new IOException("Could not restore app settings");
        }
    }

    private static byte[] readBounded(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int count;
        while ((count = input.read(buffer)) != -1) {
            total += count;
            if (total > MAX_BACKUP_BYTES) throw new IOException("Backup file is too large");
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

}
