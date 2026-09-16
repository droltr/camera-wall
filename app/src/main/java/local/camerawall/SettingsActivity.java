package local.camerawall;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import android.util.Base64;
import android.text.InputType;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;

public final class SettingsActivity extends BaseSectionActivity {
    private static final int REQUEST_EXPORT_BACKUP = 7101;
    private static final int REQUEST_IMPORT_BACKUP = 7102;
    private char[] pendingBackupPassphrase;

    @Override protected void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            | android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    @Override String sectionTitle() { return "Ayarlar"; }
    @Override BottomNavigationBar.Destination destination() { return BottomNavigationBar.Destination.SETTINGS; }

    @Override View createContentView() {
        AppSettings settings = new AppSettings(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(22), dp(16), dp(22), dp(18));
        android.widget.TextView title = new android.widget.TextView(this); title.setText("Ayarlar"); title.setTextColor(Ui.PRIMARY); title.setTextSize(26); title.setTypeface(null, android.graphics.Typeface.BOLD); content.addView(title);
        android.widget.TextView intro = new android.widget.TextView(this); intro.setText("Görüntü ve bağlantı tercihlerini yönetin."); intro.setTextColor(Ui.SECONDARY); intro.setTextSize(14); intro.setPadding(0, dp(3), 0, dp(16)); content.addView(intro);
        addSectionLabel(content, "GÖRÜNTÜ");
        Spinner layout = selector(content, "Kamera görünümü", new String[] {"4 kamera · 2×2", "8 kamera · 4×2"}, settings.groupSize() == 4 ? 0 : 1);
        Spinner auto = selector(content, "Otomatik sayfa geçişi", new String[] {"Açık · sayfalar kendiliğinden değişir", "Kapalı · sayfaları kaydırarak değiştir"}, settings.autoPage() ? 0 : 1);
        Spinner interval = selector(content, "Sayfa süresi", new String[] {"3 saniye", "5 saniye", "10 saniye", "15 saniye", "30 saniye", "60 saniye"}, intervalPosition(settings.pageIntervalSeconds()));
        android.widget.TextView cacheTitle = new android.widget.TextView(this); cacheTitle.setText("Akış tamponu (milisaniye)"); cacheTitle.setTextColor(Ui.PRIMARY); cacheTitle.setTextSize(14); cacheTitle.setPadding(dp(4), dp(9), dp(4), 0); content.addView(cacheTitle);
        EditText cache = numberField("Örn. 800 ms", settings.networkCacheMs()); content.addView(cache);
        android.widget.TextView cacheNote = new android.widget.TextView(this);
        cacheNote.setText(cacheDescription(settings.networkCacheMs())); cacheNote.setTextColor(Ui.SECONDARY); cacheNote.setTextSize(12); cacheNote.setPadding(dp(4), dp(2), dp(4), dp(5)); content.addView(cacheNote);
        cache.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { cacheNote.setText(cacheDescription(parse(cache, AppSettings.DEFAULT_NETWORK_CACHE_MS))); }
            @Override public void afterTextChanged(android.text.Editable s) { }
        });
        CheckBox tcp = check("RTSP TCP kullan", settings.rtspTcp()); content.addView(tcp);
        android.widget.TextView decoderNote = new android.widget.TextView(this); decoderNote.setText("Yazılım video çözme etkin · eski cihazlarda daha kararlı oynatma"); decoderNote.setTextColor(Ui.SECONDARY); decoderNote.setTextSize(13); decoderNote.setPadding(dp(4), dp(6), dp(4), dp(6)); content.addView(decoderNote);
        CheckBox labels = check("Kamera etiketlerini göster", settings.showLabels()); content.addView(labels);
        CheckBox keep = check("Ekranı açık tut", settings.keepScreenOn()); content.addView(keep);
        Button save = new Button(this); save.setText("Ayarları kaydet"); Ui.button(save, true); content.addView(save);
        save.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            settings.save(intervalSeconds(interval.getSelectedItemPosition()), auto.getSelectedItemPosition() == 0, parse(cache, AppSettings.DEFAULT_NETWORK_CACHE_MS), tcp.isChecked(), false, labels.isChecked(), keep.isChecked());
            settings.saveGroupSize(layout.getSelectedItemPosition() == 0 ? 4 : 8);
            Toast.makeText(SettingsActivity.this, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show();
        }});
        Button reset = new Button(this); reset.setText("Varsayılanlara dön"); Ui.button(reset, false); content.addView(reset);
        reset.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { settings.reset(); recreate(); }});
        addSectionLabel(content, "KAMERA SUNUCUSU");
        android.widget.TextView serverTitle = new android.widget.TextView(this); serverTitle.setText("go2rtc sunucusu"); serverTitle.setTextColor(Ui.PRIMARY); serverTitle.setTextSize(19); content.addView(serverTitle);
        EditText server = textField("Adres (ör. http://go2rtc.local:1984)", settings.go2rtcUrl()); content.addView(server);
        EditText user = textField("Kullanıcı adı (isteğe bağlı)", settings.go2rtcUser()); content.addView(user);
        EditText password = textField("Parola (isteğe bağlı)", settings.go2rtcPassword()); password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); content.addView(password);
        android.widget.TextView warning = new android.widget.TextView(this); warning.setText("HTTP şifrelenmez. Yalnızca güvendiğiniz yerel ağda kullanın; kimlik doğrulaması kapalıysa ağdaki istemciler yayınlara erişebilir."); warning.setTextColor(0xffffcc66); warning.setPadding(0, dp(8), 0, dp(8)); content.addView(warning);
        Button testServer = new Button(this); testServer.setText("Sunucu bağlantısını test et"); Ui.button(testServer, false); content.addView(testServer);
        testServer.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            settings.saveGo2rtc(server.getText().toString(), user.getText().toString(), password.getText().toString());
            testServer.setEnabled(false);
            new Thread(new Runnable() { @Override public void run() { boolean ok = false; HttpURLConnection connection = null; try {
                    URL url = new URL(settings.go2rtcUrl().replaceAll("/$", "") + "/api/streams");
                    if (!"http".equalsIgnoreCase(url.getProtocol()) && !"https".equalsIgnoreCase(url.getProtocol())) throw new java.io.IOException("Unsupported server protocol");
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(3000); connection.setReadTimeout(3000); connection.setRequestMethod("GET");
                    String username = settings.go2rtcUser(); String passwordValue = settings.go2rtcPassword();
                    if (!username.isEmpty() || !passwordValue.isEmpty()) {
                        String credentials = username + ":" + passwordValue;
                        connection.setRequestProperty("Authorization", "Basic " + Base64.encodeToString(credentials.getBytes(Charset.forName("UTF-8")), Base64.NO_WRAP));
                    }
                    int status = connection.getResponseCode(); ok = status >= 200 && status < 400;
                } catch (Exception ignored) { }
                finally { if (connection != null) connection.disconnect(); }
                final boolean result = ok; runOnUiThread(new Runnable() { @Override public void run() { testServer.setEnabled(true); Toast.makeText(SettingsActivity.this, result ? "go2rtc bağlantısı başarılı" : "go2rtc bağlantısı başarısız", Toast.LENGTH_SHORT).show(); }}); }}).start();
        }});

        addSectionLabel(content, "YEDEKLEME VE GERİ YÜKLEME");
        android.widget.TextView backupNote = new android.widget.TextView(this);
        backupNote.setText("Yedek; kameraları, kullanıcı adlarını, parolaları ve uygulama tercihlerini içerir. Dosya, belirlediğiniz parolayla şifrelenir. Güçlü ve unutmayacağınız bir parola seçin.");
        backupNote.setTextColor(Ui.SECONDARY);
        backupNote.setTextSize(13);
        backupNote.setPadding(0, 0, 0, dp(8));
        content.addView(backupNote);
        Button exportBackup = new Button(this);
        exportBackup.setText("Şifreli yedek oluştur");
        Ui.button(exportBackup, true);
        content.addView(exportBackup);
        exportBackup.setOnClickListener(view -> showExportPasswordDialog());
        Button importBackup = new Button(this);
        importBackup.setText("Yedekten geri yükle");
        Ui.button(importBackup, false);
        LinearLayout.LayoutParams importParams = new LinearLayout.LayoutParams(-1, -2);
        importParams.topMargin = dp(7);
        content.addView(importBackup, importParams);
        importBackup.setOnClickListener(view -> chooseBackupToRestore());

        addSectionLabel(content, "HAKKINDA");
        android.widget.TextView about = new android.widget.TextView(this);
        about.setText("Camera Wall\nSürüm " + BuildConfig.VERSION_NAME
            + "\nAndroid " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")"
            + "\nMIT Lisansı · Android 5 ve üzeri");
        about.setTextColor(Ui.PRIMARY);
        about.setTextSize(14);
        about.setLineSpacing(dp(3), 1f);
        about.setPadding(dp(14), dp(12), dp(14), dp(12));
        about.setBackground(Ui.shape(Ui.SURFACE, Ui.OUTLINE, dp(14)));
        content.addView(about);
        scroll.addView(content); return scroll;
    }

    private void showExportPasswordDialog() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(22), dp(4), dp(22), 0);
        EditText password = textField("Yedek parolası (en az 12 karakter)", "");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        EditText confirmation = textField("Parolayı tekrar girin", "");
        confirmation.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(password);
        form.addView(confirmation);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Şifreli yedek oluştur")
            .setMessage("Yedek dosyası seçtiğiniz konuma kaydedilir. Kamera bilgilerini açmak için bu parola gerekir.")
            .setView(form)
            .setNegativeButton("İptal", null)
            .setPositiveButton("Devam", null)
            .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            char[] first = chars(password);
            char[] second = chars(confirmation);
            password.getText().clear();
            confirmation.getText().clear();
            if (first.length < 12) {
                Arrays.fill(first, '\0'); Arrays.fill(second, '\0');
                password.setError("En az 12 karakter girin");
                return;
            }
            if (!Arrays.equals(first, second)) {
                Arrays.fill(first, '\0'); Arrays.fill(second, '\0');
                confirmation.setError("Parolalar eşleşmiyor");
                return;
            }
            Arrays.fill(second, '\0');
            pendingBackupPassphrase = first;
            dialog.dismiss();
            launchExportPicker();
        }));
        dialog.show();
    }

    private void launchExportPicker() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "camera-wall-backup.json");
        try {
            startActivityForResult(intent, REQUEST_EXPORT_BACKUP);
        } catch (RuntimeException error) {
            clearPendingPassphrase();
            Toast.makeText(this, "Dosya kaydetme ekranı açılamadı", Toast.LENGTH_LONG).show();
        }
    }

    private void chooseBackupToRestore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        try {
            startActivityForResult(intent, REQUEST_IMPORT_BACKUP);
        } catch (RuntimeException error) {
            Toast.makeText(this, "Dosya seçme ekranı açılamadı", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_EXPORT_BACKUP) {
            if (resultCode != RESULT_OK || data == null || data.getData() == null) {
                clearPendingPassphrase();
                return;
            }
            writeEncryptedBackup(data.getData());
        } else if (requestCode == REQUEST_IMPORT_BACKUP && resultCode == RESULT_OK
            && data != null && data.getData() != null) {
            showImportPasswordDialog(data.getData());
        }
    }

    private void writeEncryptedBackup(final Uri target) {
        final char[] passphrase = pendingBackupPassphrase;
        pendingBackupPassphrase = null;
        if (passphrase == null) return;
        Toast.makeText(this, "Şifreli yedek hazırlanıyor", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            boolean success = false;
            try {
                byte[] encrypted = SettingsBackup.create(this, passphrase);
                OutputStream output = getContentResolver().openOutputStream(target, "w");
                if (output == null) throw new java.io.IOException("Could not open selected backup file");
                try (OutputStream stream = output) { stream.write(encrypted); stream.flush(); }
                Arrays.fill(encrypted, (byte) 0);
                success = true;
            } catch (Exception ignored) {
                success = false;
            } finally {
                Arrays.fill(passphrase, '\0');
            }
            final boolean result = success;
            runOnUiThread(() -> Toast.makeText(SettingsActivity.this,
                result ? "Şifreli yedek kaydedildi" : "Yedek kaydedilemedi", Toast.LENGTH_LONG).show());
        }, "camera-wall-backup-export").start();
    }

    private void showImportPasswordDialog(final Uri source) {
        EditText password = textField("Yedek parolası", "");
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(22), dp(4), dp(22), 0);
        form.addView(password);
        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("Yedeği geri yükle")
            .setMessage("Geri yükleme kayıtlı kamera listesini ve uygulama tercihlerini yedekteki değerlerle değiştirir.")
            .setView(form)
            .setNegativeButton("İptal", null)
            .setPositiveButton("Geri yükle", null)
            .create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            final char[] passphrase = chars(password);
            password.getText().clear();
            if (passphrase.length < 12) {
                Arrays.fill(passphrase, '\0');
                password.setError("Yedek parolasını girin");
                return;
            }
            dialog.dismiss();
            restoreEncryptedBackup(source, passphrase);
        }));
        dialog.show();
    }

    private void restoreEncryptedBackup(final Uri source, final char[] passphrase) {
        Toast.makeText(this, "Yedek doğrulanıyor", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            boolean success = false;
            try (InputStream input = getContentResolver().openInputStream(source)) {
                if (input == null) throw new java.io.IOException("Could not open backup file");
                SettingsBackup.restore(this, input, passphrase);
                success = true;
            } catch (Exception ignored) {
                success = false;
            } finally {
                Arrays.fill(passphrase, '\0');
            }
            final boolean result = success;
            runOnUiThread(() -> {
                Toast.makeText(SettingsActivity.this,
                    result ? "Ayarlar ve kameralar geri yüklendi" : "Yedek doğrulanamadı; parola veya dosyayı kontrol edin",
                    Toast.LENGTH_LONG).show();
                if (result) recreate();
            });
        }, "camera-wall-backup-import").start();
    }

    private char[] chars(EditText field) {
        CharSequence value = field.getText();
        char[] result = new char[value.length()];
        for (int index = 0; index < value.length(); index++) result[index] = value.charAt(index);
        return result;
    }

    private void clearPendingPassphrase() {
        if (pendingBackupPassphrase != null) {
            Arrays.fill(pendingBackupPassphrase, '\0');
            pendingBackupPassphrase = null;
        }
    }

    @Override protected void onDestroy() {
        clearPendingPassphrase();
        super.onDestroy();
    }

    private EditText numberField(String hint, int value) { EditText field = new EditText(this); field.setHint(hint); field.setText(String.valueOf(value)); field.setTextColor(Ui.PRIMARY); field.setHintTextColor(Ui.SECONDARY); field.setInputType(2); styleField(field); return field; }
    private Spinner selector(LinearLayout parent, String label, String[] values, int selected) {
        android.widget.TextView heading = new android.widget.TextView(this); heading.setText(label); heading.setTextColor(Ui.PRIMARY); heading.setTextSize(14); heading.setPadding(dp(4), dp(8), dp(4), 0); parent.addView(heading);
        Spinner spinner = new Spinner(this, Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, values);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); spinner.setAdapter(adapter); spinner.setSelection(selected);
        parent.addView(spinner, new LinearLayout.LayoutParams(-1, dp(44)));
        return spinner;
    }
    private int intervalSeconds(int position) { int[] values = {3, 5, 10, 15, 30, 60}; return values[Math.max(0, Math.min(values.length - 1, position))]; }
    private int intervalPosition(int seconds) { int[] values = {3, 5, 10, 15, 30, 60}; for (int i = 0; i < values.length; i++) if (seconds <= values[i]) return i; return values.length - 1; }
    private String cacheDescription(int milliseconds) { return milliseconds + " ms = " + (milliseconds / 1000.0f) + " saniye. Yüksek değer kesintileri azaltabilir, ancak görüntü gecikmesini artırır."; }
    private CheckBox check(String label, boolean checked) { CheckBox box = new CheckBox(this); box.setText(label); box.setTextColor(Ui.PRIMARY); box.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT)); box.setChecked(checked); return box; }
    private EditText textField(String hint, String value) { EditText field = new EditText(this); field.setHint(hint); field.setText(value); field.setTextColor(Ui.PRIMARY); field.setHintTextColor(Ui.SECONDARY); styleField(field); return field; }
    private void styleField(EditText field) { field.setSingleLine(true); field.setPadding(dp(12), dp(8), dp(12), dp(8)); field.setBackground(Ui.shape(Ui.SURFACE, Ui.OUTLINE, dp(12))); }
    private void addSectionLabel(LinearLayout parent, String label) { android.widget.TextView heading = new android.widget.TextView(this); heading.setText(label); heading.setTextColor(Ui.ACCENT); heading.setTextSize(11); heading.setTypeface(null, android.graphics.Typeface.BOLD); heading.setPadding(0, dp(14), 0, dp(5)); parent.addView(heading); }
    private int parse(EditText field, int fallback) { try { return Integer.parseInt(field.getText().toString()); } catch (RuntimeException e) { return fallback; } }
}
