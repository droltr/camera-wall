package local.camerawall;

import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;
import android.text.InputType;
import java.net.HttpURLConnection;
import java.net.URL;

public final class SettingsActivity extends BaseSectionActivity {
    @Override String sectionTitle() { return "Ayarlar"; }
    @Override BottomNavigationBar.Destination destination() { return BottomNavigationBar.Destination.SETTINGS; }

    @Override View createContentView() {
        AppSettings settings = new AppSettings(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(22), dp(16), dp(22), dp(18));
        android.widget.TextView title = new android.widget.TextView(this); title.setText("Ayarlar"); title.setTextColor(Ui.PRIMARY); title.setTextSize(26); title.setTypeface(null, android.graphics.Typeface.BOLD); content.addView(title);
        android.widget.TextView intro = new android.widget.TextView(this); intro.setText("Görüntü ve bağlantı tercihlerini yönetin."); intro.setTextColor(Ui.SECONDARY); intro.setTextSize(14); intro.setPadding(0, dp(3), 0, dp(16)); content.addView(intro);
        addSectionLabel(content, "GÖRÜNTÜ");
        EditText interval = numberField("Sayfa geçiş süresi (sn)", settings.pageIntervalSeconds()); content.addView(interval);
        CheckBox auto = check("Otomatik sayfa geçişi", settings.autoPage()); content.addView(auto);
        EditText cache = numberField("Ağ önbelleği (ms)", settings.networkCacheMs()); content.addView(cache);
        CheckBox tcp = check("RTSP TCP kullan", settings.rtspTcp()); content.addView(tcp);
        android.widget.TextView decoderNote = new android.widget.TextView(this); decoderNote.setText("Yazılım video çözme etkin · eski cihazlarda daha kararlı oynatma"); decoderNote.setTextColor(Ui.SECONDARY); decoderNote.setTextSize(13); decoderNote.setPadding(dp(4), dp(6), dp(4), dp(6)); content.addView(decoderNote);
        CheckBox labels = check("Kamera etiketlerini göster", settings.showLabels()); content.addView(labels);
        CheckBox keep = check("Ekranı açık tut", settings.keepScreenOn()); content.addView(keep);
        Button save = new Button(this); save.setText("Ayarları kaydet"); Ui.button(save, true); content.addView(save);
        save.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            settings.save(parse(interval, AppSettings.DEFAULT_PAGE_INTERVAL_SECONDS), auto.isChecked(), parse(cache, AppSettings.DEFAULT_NETWORK_CACHE_MS), tcp.isChecked(), false, labels.isChecked(), keep.isChecked());
            Toast.makeText(SettingsActivity.this, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show();
        }});
        Button reset = new Button(this); reset.setText("Varsayılanlara dön"); Ui.button(reset, false); content.addView(reset);
        reset.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { settings.reset(); recreate(); }});
        addSectionLabel(content, "KAMERA SUNUCUSU");
        android.widget.TextView serverTitle = new android.widget.TextView(this); serverTitle.setText("go2rtc sunucusu"); serverTitle.setTextColor(Ui.PRIMARY); serverTitle.setTextSize(19); content.addView(serverTitle);
        EditText server = textField("Adres (ör. http://192.168.1.10:1984)", settings.go2rtcUrl()); content.addView(server);
        EditText user = textField("Kullanıcı adı (isteğe bağlı)", settings.go2rtcUser()); content.addView(user);
        EditText password = textField("Parola (isteğe bağlı)", settings.go2rtcPassword()); password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD); content.addView(password);
        android.widget.TextView warning = new android.widget.TextView(this); warning.setText("HTTP şifrelenmez. Yalnızca güvendiğiniz yerel ağda kullanın; kimlik doğrulaması kapalıysa ağdaki istemciler yayınlara erişebilir."); warning.setTextColor(0xffffcc66); warning.setPadding(0, dp(8), 0, dp(8)); content.addView(warning);
        Button testServer = new Button(this); testServer.setText("Sunucu bağlantısını test et"); Ui.button(testServer, false); content.addView(testServer);
        testServer.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            settings.saveGo2rtc(server.getText().toString(), user.getText().toString(), password.getText().toString());
            testServer.setEnabled(false);
            new Thread(new Runnable() { @Override public void run() { boolean ok = false; try { URL url = new URL(settings.go2rtcUrl().replaceAll("/$", "") + "/api/streams"); HttpURLConnection c = (HttpURLConnection) url.openConnection(); c.setConnectTimeout(3000); c.setReadTimeout(3000); c.setRequestMethod("GET"); ok = c.getResponseCode() >= 200 && c.getResponseCode() < 400; c.disconnect(); } catch (Exception ignored) { }
                final boolean result = ok; runOnUiThread(new Runnable() { @Override public void run() { testServer.setEnabled(true); Toast.makeText(SettingsActivity.this, result ? "go2rtc bağlantısı başarılı" : "go2rtc bağlantısı başarısız", Toast.LENGTH_SHORT).show(); }}); }}).start();
        }});
        scroll.addView(content); return scroll;
    }

    private EditText numberField(String hint, int value) { EditText field = new EditText(this); field.setHint(hint); field.setText(String.valueOf(value)); field.setTextColor(Ui.PRIMARY); field.setHintTextColor(Ui.SECONDARY); field.setInputType(2); styleField(field); return field; }
    private CheckBox check(String label, boolean checked) { CheckBox box = new CheckBox(this); box.setText(label); box.setTextColor(Ui.PRIMARY); box.setButtonTintList(android.content.res.ColorStateList.valueOf(Ui.ACCENT)); box.setChecked(checked); return box; }
    private EditText textField(String hint, String value) { EditText field = new EditText(this); field.setHint(hint); field.setText(value); field.setTextColor(Ui.PRIMARY); field.setHintTextColor(Ui.SECONDARY); styleField(field); return field; }
    private void styleField(EditText field) { field.setSingleLine(true); field.setPadding(dp(12), dp(8), dp(12), dp(8)); field.setBackground(Ui.shape(Ui.SURFACE, Ui.OUTLINE, dp(12))); }
    private void addSectionLabel(LinearLayout parent, String label) { android.widget.TextView heading = new android.widget.TextView(this); heading.setText(label); heading.setTextColor(Ui.ACCENT); heading.setTextSize(11); heading.setTypeface(null, android.graphics.Typeface.BOLD); heading.setPadding(0, dp(14), 0, dp(5)); parent.addView(heading); }
    private int parse(EditText field, int fallback) { try { return Integer.parseInt(field.getText().toString()); } catch (RuntimeException e) { return fallback; } }
}
