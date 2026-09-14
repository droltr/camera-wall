package local.camerawall;

import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

public final class SettingsActivity extends BaseSectionActivity {
    @Override String sectionTitle() { return "Ayarlar"; }
    @Override BottomNavigationBar.Destination destination() { return BottomNavigationBar.Destination.SETTINGS; }

    @Override View createContentView() {
        AppSettings settings = new AppSettings(this);
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(24), dp(16), dp(24), dp(16));
        android.widget.TextView title = new android.widget.TextView(this); title.setText("Ayarlar"); title.setTextColor(Color.WHITE); title.setTextSize(24); content.addView(title);
        EditText interval = numberField("Sayfa geçiş süresi (sn)", settings.pageIntervalSeconds()); content.addView(interval);
        CheckBox auto = check("Otomatik sayfa geçişi", settings.autoPage()); content.addView(auto);
        EditText cache = numberField("Ağ önbelleği (ms)", settings.networkCacheMs()); content.addView(cache);
        CheckBox tcp = check("RTSP TCP kullan", settings.rtspTcp()); content.addView(tcp);
        CheckBox hw = check("Donanım hızlandırma", settings.hardwareAcceleration()); content.addView(hw);
        CheckBox labels = check("Kamera etiketlerini göster", settings.showLabels()); content.addView(labels);
        CheckBox keep = check("Ekranı açık tut", settings.keepScreenOn()); content.addView(keep);
        Button save = new Button(this); save.setText("Ayarları kaydet"); content.addView(save);
        save.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) {
            settings.save(parse(interval, AppSettings.DEFAULT_PAGE_INTERVAL_SECONDS), auto.isChecked(), parse(cache, AppSettings.DEFAULT_NETWORK_CACHE_MS), tcp.isChecked(), hw.isChecked(), labels.isChecked(), keep.isChecked());
            Toast.makeText(SettingsActivity.this, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show();
        }});
        Button reset = new Button(this); reset.setText("Varsayılanlara dön"); content.addView(reset);
        reset.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { settings.reset(); recreate(); }});
        scroll.addView(content); return scroll;
    }

    private EditText numberField(String hint, int value) { EditText field = new EditText(this); field.setHint(hint); field.setText(String.valueOf(value)); field.setTextColor(Color.WHITE); field.setHintTextColor(Color.LTGRAY); field.setInputType(2); return field; }
    private CheckBox check(String label, boolean checked) { CheckBox box = new CheckBox(this); box.setText(label); box.setTextColor(Color.WHITE); box.setChecked(checked); return box; }
    private int parse(EditText field, int fallback) { try { return Integer.parseInt(field.getText().toString()); } catch (RuntimeException e) { return fallback; } }
}
