package local.camerawall;

import android.app.AlertDialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.content.ClipData;
import android.content.ClipDescription;
import android.text.InputType;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import java.util.ArrayList;
import java.util.List;

public final class CameraListActivity extends BaseSectionActivity {
    private final Handler testHandler = new Handler();
    private MediaPlayer testPlayer;
    private LibVLC testLibVLC;
    private RtspScanner scanner;
    private final ArrayList<CameraSpec> allCameras = new ArrayList<>();
    private LinearLayout cameraRows;
    private TextView cameraSummary;
    @Override String sectionTitle() { return "Kameralar"; }
    @Override BottomNavigationBar.Destination destination() { return BottomNavigationBar.Destination.CAMERAS; }

    @Override View createContentView() {
        allCameras.clear();
        allCameras.addAll(new CameraRepository(this).getCameras());
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(22), dp(16), dp(22), dp(18));

        TextView heading = text("Kameralar", 26, Ui.PRIMARY);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        heading.setPadding(0, 0, 0, dp(2));
        content.addView(heading, new LinearLayout.LayoutParams(-1, -2));
        TextView intro = text("Kameralarınızı ekleyin, düzenleyin ve bağlantıyı kontrol edin.", 14, Ui.SECONDARY);
        intro.setPadding(0, 0, 0, dp(12));
        content.addView(intro);

        Button add = actionButton("+ Kamera ekle", true);
        add.setOnClickListener(view -> showAddDialog());
        Button importButton = actionButton("go2rtc ile eşitle", false);
        importButton.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { importGo2rtcStreams(); }});
        content.addView(actionRow(add, importButton), rowLayoutParams());
        Button discover = actionButton("ONVIF ile bul", false);
        discover.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { discoverOnvif(); }});
        Button scan = actionButton("RTSP adreslerini tara", false);
        scan.setOnClickListener(new View.OnClickListener() { @Override public void onClick(View v) { scanRtsp(); }});
        content.addView(actionRow(discover, scan), rowLayoutParams());

        EditText search = field("Kameralarda ara");
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        search.setBackground(Ui.shape(Ui.SURFACE, Ui.OUTLINE, dp(12)));
        search.setPadding(dp(14), dp(8), dp(14), dp(8));
        content.addView(search, rowLayoutParams());

        cameraSummary = text("", 13, Ui.SECONDARY);
        cameraSummary.setPadding(0, 0, 0, dp(6));
        content.addView(cameraSummary);
        TextView reorderHint = text("Sıralamayı değiştirmek için kamerayı tutup sürükleyin.", 12, Ui.SECONDARY);
        reorderHint.setPadding(0, 0, 0, dp(8));
        content.addView(reorderHint);

        cameraRows = new LinearLayout(this);
        cameraRows.setOrientation(LinearLayout.VERTICAL);
        content.addView(cameraRows, new LinearLayout.LayoutParams(-1, -2));
        renderCameraRows("");
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) { renderCameraRows(value.toString()); }
            @Override public void afterTextChanged(Editable value) { }
        });

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        return scroll;
    }

    private Button actionButton(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        Ui.button(button, primary);
        return button;
    }

    private LinearLayout actionRow(Button first, Button second) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams firstParams = new LinearLayout.LayoutParams(0, -2, 1f);
        firstParams.rightMargin = dp(5);
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(0, -2, 1f);
        secondParams.leftMargin = dp(5);
        row.addView(first, firstParams);
        row.addView(second, secondParams);
        return row;
    }

    private void renderCameraRows(String query) {
        if (cameraRows == null) return;
        cameraRows.removeAllViews();
        String term = query.trim().toLowerCase(java.util.Locale.ROOT);
        int matches = 0;
        for (int index = 0; index < allCameras.size(); index++) {
            CameraSpec camera = allCameras.get(index);
            if (!term.isEmpty() && !camera.name.toLowerCase(java.util.Locale.ROOT).contains(term)
                && !sanitizedEndpoint(camera.url).toLowerCase(java.util.Locale.ROOT).contains(term)) continue;
            cameraRows.addView(cameraRow(index, camera), rowLayoutParams());
            matches++;
        }
        cameraSummary.setText(term.isEmpty()
            ? allCameras.size() + " kayıtlı kamera"
            : matches + " / " + allCameras.size() + " kamera");
        if (matches == 0) {
            TextView empty = text(allCameras.isEmpty()
                ? "Henüz kamera yok. Üstteki “Kamera ekle” veya keşif seçeneklerini kullanın."
                : "Aramanızla eşleşen kamera yok.", 15, Ui.SECONDARY);
            empty.setPadding(dp(12), dp(14), dp(12), dp(14));
            cameraRows.addView(empty);
        }
    }

    private void showAddDialog() { showCameraDialog(-1, null); }

    private void discoverOnvif() {
        Toast.makeText(this, "ONVIF araması başlatıldı (4 sn)", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() { @Override public void run() { final java.util.Set<String> results; try { results = OnvifDiscovery.probe(4000); } catch (Exception error) { runOnUiThread(new Runnable() { @Override public void run() { Toast.makeText(CameraListActivity.this, "ONVIF araması başarısız", Toast.LENGTH_SHORT).show(); }}); return; }
            runOnUiThread(new Runnable() { @Override public void run() {
                ArrayList<CameraSpec> candidates = new ArrayList<>();
                java.util.HashSet<String> hosts = new java.util.HashSet<>();
                for (String endpoint : results) {
                    Uri uri = Uri.parse(endpoint);
                    String host = uri.getHost();
                    if (host == null || !hosts.add(host)) continue;
                    String authorityHost = host.indexOf(':') >= 0 ? "[" + host + "]" : host;
                    candidates.add(new CameraSpec("ONVIF " + host, "rtsp://" + authorityHost + ":554/", "", ""));
                }
                showCandidatePicker("ONVIF ile bulunanlar", candidates, true);
            }}); }}).start();
    }

    private void scanRtsp() {
        new AlertDialog.Builder(this).setTitle("RTSP taraması").setMessage("Yerel /24 ağ taranacak. Portlar: 554, 8554, 10554. Kimlik doğrulama veya yol denenmez.").setNegativeButton("İptal", null).setPositiveButton("Başlat", (dialog, which) -> startRtspScan()).show();
    }
    private void startRtspScan() {
        if (!RtspScanner.hasUsableWifiAddress(this)) {
            Toast.makeText(this, "Yerel Wi-Fi adresi alınamadı; ağ taraması başlatılmadı.", Toast.LENGTH_LONG).show();
            return;
        }
        final ArrayList<String> results = new ArrayList<>();
        scanner = new RtspScanner(this, new RtspScanner.Listener() { @Override public void onCandidate(final String host, final int port) { runOnUiThread(() -> results.add("rtsp://" + host + ":" + port)); }
            @Override public void onFinished() { runOnUiThread(() -> {
                if (scanner == null || isFinishing() || isDestroyed()) return;
                scanner = null;
                ArrayList<CameraSpec> candidates = new ArrayList<>();
                for (String result : results) {
                    Uri uri = Uri.parse(result);
                    String host = uri.getHost();
                    candidates.add(new CameraSpec(host == null ? "RTSP kamera" : "RTSP " + host, result, "", ""));
                }
                showCandidatePicker("RTSP tarama sonuçları", candidates, false);
            }); }});
        Toast.makeText(this, "Tarama başladı; durdurmak için geri dönün", Toast.LENGTH_SHORT).show();
    }

    private void showCandidatePicker(String title, final ArrayList<CameraSpec> candidates, boolean onvif) {
        if (candidates.isEmpty()) {
            new AlertDialog.Builder(this).setTitle(title)
                .setMessage("Kamera adayı bulunamadı.")
                .setPositiveButton("Tamam", null).show();
            return;
        }
        final String[] labels = new String[candidates.size()];
        final boolean[] selected = new boolean[candidates.size()];
        for (int index = 0; index < candidates.size(); index++) {
            CameraSpec candidate = candidates.get(index);
            labels[index] = candidate.name + "\n" + sanitizedEndpoint(candidate.url);
        }
        String note = onvif
            ? "Seçilen cihazlar RTSP taslağı olarak kaydedilir. ONVIF adresi video yolu değildir; adresi ve gerekli giriş bilgilerini sonradan Kameralar bölümünden düzenleyebilirsiniz. Kullanıcı adı ve parola isteğe bağlıdır."
            : "Seçilen adresler kaydedilir. Video yolu veya giriş bilgisi gerekiyorsa sonradan Kameralar bölümünden düzenleyebilirsiniz. Kullanıcı adı ve parola isteğe bağlıdır.";
        new AlertDialog.Builder(this).setTitle(title).setMessage(note)
            .setMultiChoiceItems(labels, selected, (dialog, which, checked) -> selected[which] = checked)
            .setNegativeButton("Vazgeç", null)
            .setPositiveButton("Seçilenleri kaydet", (dialog, which) -> saveSelectedCandidates(candidates, selected))
            .show();
    }

    private void saveSelectedCandidates(List<CameraSpec> candidates, boolean[] selected) {
        CameraRepository repository = new CameraRepository(this);
        List<CameraSpec> saved = repository.getCameras();
        int added = 0;
        for (int index = 0; index < candidates.size(); index++) {
            if (!selected[index]) continue;
            CameraSpec candidate = candidates.get(index);
            boolean duplicate = false;
            for (CameraSpec camera : saved) {
                if (camera.url.equalsIgnoreCase(candidate.url)) { duplicate = true; break; }
            }
            if (!duplicate) {
                saved.add(new CameraSpec(candidate.name, candidate.url, "", ""));
                added++;
            }
        }
        boolean success = added == 0 || repository.replaceAll(saved);
        Toast.makeText(this, success
            ? (added == 0 ? "Yeni kamera seçilmedi veya adaylar zaten kayıtlı" : added + " kamera kaydedildi; giriş bilgisi sonradan eklenebilir")
            : "Kamera adayları kaydedilemedi", Toast.LENGTH_LONG).show();
        if (success && added > 0) recreate();
    }

    private void importGo2rtcStreams() {
        final AppSettings settings = new AppSettings(this);
        final String base = settings.go2rtcUrl().replaceAll("/$", "");
        if (base.length() == 0) { Toast.makeText(this, "Önce Ayarlar'dan go2rtc adresini girin", Toast.LENGTH_SHORT).show(); return; }
        new AlertDialog.Builder(this)
            .setTitle("Kamera listesini eşitle")
            .setMessage("Liste go2rtc yayınlarıyla değiştirilir. Eşleşmeyen yerel kayıtlar kaldırılır; aynı kaynağa giden yayınlar tek kamera olarak tutulur.")
            .setNegativeButton("İptal", null)
            .setPositiveButton("Eşitle", (dialog, which) -> syncGo2rtcStreams(settings, base))
            .show();
    }

    void syncGo2rtcStreams(final AppSettings settings, final String base) {
        new Thread(new Runnable() { @Override public void run() {
            final ArrayList<CameraSpec> synced = new ArrayList<>();
            boolean saved = false;
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(base + "/api/streams").openConnection();
                connection.setConnectTimeout(4000); connection.setReadTimeout(4000);
                String auth = settings.go2rtcUser();
                if (auth.length() > 0) { String token = android.util.Base64.encodeToString((auth + ":" + settings.go2rtcPassword()).getBytes("UTF-8"), android.util.Base64.NO_WRAP); connection.setRequestProperty("Authorization", "Basic " + token); }
                int responseCode = connection.getResponseCode();
                if (responseCode < 200 || responseCode >= 300) throw new java.io.IOException("go2rtc request failed");
                StringBuilder body = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"))) {
                    String line; while ((line = reader.readLine()) != null) body.append(line);
                }
                JSONObject streams = new JSONObject(body.toString());
                if (streams.length() == 0) throw new java.io.IOException("go2rtc returned no streams");
                java.util.LinkedHashMap<String, List<String>> sourcesByName = new java.util.LinkedHashMap<>();
                java.util.Iterator<String> keys = streams.keys();
                while (keys.hasNext()) {
                    String name = keys.next();
                    JSONObject stream = streams.optJSONObject(name);
                    ArrayList<String> sources = new ArrayList<>();
                    org.json.JSONArray producers = stream == null ? null : stream.optJSONArray("producers");
                    if (producers != null) for (int index = 0; index < producers.length(); index++) {
                        JSONObject producer = producers.optJSONObject(index);
                        String source = producer == null ? "" : producer.optString("url", "").trim();
                        if (source.length() > 0) sources.add(source);
                    }
                    sourcesByName.put(name, sources);
                }
                java.net.URI serverUri = new java.net.URI(base); String host = serverUri.getHost();
                if (host == null) throw new java.io.IOException("Invalid go2rtc address");
                if (host.indexOf(':') >= 0) host = "[" + host + "]";
                for (String name : Go2rtcCameraSync.canonicalNames(sourcesByName)) {
                    String rtsp = "rtsp://" + host + ":8554/" + Uri.encode(name);
                    synced.add(new CameraSpec(name, rtsp, "", ""));
                }
                saved = new CameraRepository(CameraListActivity.this).replaceAll(synced);
            } catch (Exception ignored) { }
            finally { if (connection != null) connection.disconnect(); }
            final boolean success = saved;
            runOnUiThread(new Runnable() { @Override public void run() {
                Toast.makeText(CameraListActivity.this, success
                    ? synced.size() + " kamera go2rtc ile eşitlendi"
                    : "Eşitleme başarısız; mevcut kamera listesi korundu", Toast.LENGTH_LONG).show();
                if (success) recreate();
            }});
        }}).start();
    }

    private void showCameraDialog(final int editIndex, CameraSpec existing) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), 0);
        final EditText name = field("Kamera adı");
        final EditText url = field("RTSP adresi (rtsp://…)");
        final EditText username = field("Kullanıcı adı (isteğe bağlı)");
        final EditText password = field("Parola (isteğe bağlı)");
        TextView credentialNote = text("Kullanıcı adı ve parola boş bırakılabilir; gerekirse kamerayı kaydettikten sonra düzenleyin.", 13, Ui.SECONDARY);
        credentialNote.setPadding(0, dp(4), 0, dp(8));
        if (existing != null) {
            RtspCredentials.Values values = RtspCredentials.normalize(existing.url, existing.username, existing.password);
            name.setText(existing.name); url.setText(values.url);
            username.setText(values.username); password.setText(values.password);
        }
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(name); form.addView(url); form.addView(credentialNote); form.addView(username); form.addView(password);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
            .setTitle(editIndex < 0 ? "Kamera ekle" : "Kamerayı düzenle")
            .setView(form);
        if (editIndex >= 0) builder.setNeutralButton("Bağlantı testi", null);
        final AlertDialog dialog = builder.setNegativeButton("İptal", null)
            .setPositiveButton("Kaydet", null).create();
        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override public void onShow(android.content.DialogInterface ignored) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) {
                        String cameraName = name.getText().toString().trim();
                        String cameraUrl = url.getText().toString().trim();
                        if (cameraName.length() == 0 || !cameraUrl.startsWith("rtsp://")) {
                            url.setError("Ad ve geçerli rtsp:// adresi gerekli");
                            return;
                        }
                        RtspCredentials.Values values = RtspCredentials.normalize(cameraUrl,
                            username.getText().toString(), password.getText().toString());
                        CameraSpec camera = new CameraSpec(cameraName, values.url, values.username, values.password);
                        CameraRepository repository = new CameraRepository(CameraListActivity.this);
                        boolean saved = editIndex < 0 ? repository.add(camera) : repository.update(editIndex, camera);
                        if (!saved) {
                            url.setError("Kamera kaydedilemedi");
                            return;
                        }
                        dialog.dismiss();
                        recreate();
                    }
                });
                if (editIndex >= 0) {
                    dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(view -> {
                        String cameraName = name.getText().toString().trim();
                        String cameraUrl = url.getText().toString().trim();
                        if (cameraName.isEmpty() || !cameraUrl.startsWith("rtsp://")) {
                            url.setError("Bağlantı testi için ad ve rtsp:// adresi gerekli");
                            return;
                        }
                        RtspCredentials.Values values = RtspCredentials.normalize(cameraUrl,
                            username.getText().toString(), password.getText().toString());
                        testConnection(new CameraSpec(cameraName, values.url, values.username, values.password));
                    });
                }
            }
        });
        dialog.show();
    }

    private EditText field(String hint) {
        EditText field = new EditText(this);
        field.setHint(hint);
        field.setSingleLine(true);
        field.setTextColor(Color.WHITE);
        field.setHintTextColor(Color.GRAY);
        return field;
    }

    private View cameraRow(final int position, final CameraSpec camera) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(dp(16), dp(10), dp(16), dp(10));
        Ui.panel(row);

        LinearLayout titleLine = new LinearLayout(this);
        titleLine.setOrientation(LinearLayout.HORIZONTAL);
        titleLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView cameraName = text(camera.name, 18, Ui.PRIMARY);
        cameraName.setTypeface(null, android.graphics.Typeface.BOLD);
        titleLine.addView(cameraName, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView dragHandle = text("Sürükle  ↕", 13, Ui.ACCENT);
        dragHandle.setGravity(Gravity.CENTER);
        dragHandle.setPadding(dp(12), dp(8), dp(12), dp(8));
        dragHandle.setBackground(Ui.shape(Ui.SELECTED, Ui.OUTLINE, dp(12)));
        dragHandle.setContentDescription(camera.name + " sırasını değiştirmek için basılı tutup sürükleyin");
        dragHandle.setOnLongClickListener(view -> {
            ClipData data = ClipData.newPlainText("camera-position", String.valueOf(position));
            return view.startDrag(data, new View.DragShadowBuilder(row), Integer.valueOf(position), 0);
        });
        titleLine.addView(dragHandle, new LinearLayout.LayoutParams(-2, -2));
        row.addView(titleLine);

        TextView endpoint = text(sanitizedEndpoint(camera.url), 13, Ui.SECONDARY);
        endpoint.setPadding(0, dp(4), 0, 0);
        row.addView(endpoint);
        TextView state = text("●  KAYITLI", 11, Ui.ACCENT);
        state.setPadding(0, dp(4), 0, 0);
        row.addView(state);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button edit = new Button(this); edit.setText("Düzenle"); Ui.button(edit, false);
        edit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showCameraDialog(position, camera); }
        });
        Button remove = new Button(this); remove.setText("Sil"); Ui.button(remove, false); remove.setTextColor(Ui.DANGER);
        remove.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { confirmDelete(position, camera.name); }
        });
        actions.addView(edit, new LinearLayout.LayoutParams(0, -2, 1f));
        actions.addView(remove, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(actions);

        row.setOnDragListener((view, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    return event.getClipDescription() != null
                        && event.getClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN);
                case DragEvent.ACTION_DRAG_ENTERED:
                    row.setAlpha(0.72f);
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                case DragEvent.ACTION_DRAG_ENDED:
                    row.setAlpha(1f);
                    return true;
                case DragEvent.ACTION_DROP:
                    Object source = event.getLocalState();
                    if (!(source instanceof Integer)) return false;
                    int sourcePosition = (Integer) source;
                    int delta = position - sourcePosition;
                    if (delta != 0 && new CameraRepository(CameraListActivity.this).move(sourcePosition, delta)) {
                        Toast.makeText(CameraListActivity.this, "Kamera sırası güncellendi", Toast.LENGTH_SHORT).show();
                        recreate();
                    }
                    return true;
                default:
                    return true;
            }
        });
        row.setContentDescription(camera.name + ", kayıtlı kamera");
        return row;
    }

    private void confirmDelete(final int position, String name) {
        new AlertDialog.Builder(this)
            .setTitle("Kamera silinsin mi?")
            .setMessage(name)
            .setNegativeButton("İptal", null)
            .setPositiveButton("Sil", new android.content.DialogInterface.OnClickListener() {
                @Override public void onClick(android.content.DialogInterface dialog, int which) {
                    new CameraRepository(CameraListActivity.this).delete(position);
                    recreate();
                }
            }).show();
    }

    private void testConnection(final CameraSpec camera) {
        stopTestPlayer();
        AppSettings settings = new AppSettings(this);
        ArrayList<String> options = new ArrayList<>();
        options.add("--no-audio");
        if (settings.rtspTcp()) options.add("--rtsp-tcp");
        options.add("--network-caching=" + settings.networkCacheMs());
        testLibVLC = new LibVLC(this, options);
        testPlayer = new MediaPlayer(testLibVLC);
        testPlayer.setEventListener(new MediaPlayer.EventListener() {
            @Override public void onEvent(MediaPlayer.Event event) {
                if (event.type == MediaPlayer.Event.Playing) {
                    testHandler.post(new Runnable() { @Override public void run() {
                        Toast.makeText(CameraListActivity.this, camera.name + ": bağlantı başarılı", Toast.LENGTH_SHORT).show(); stopTestPlayer();
                    }});
                } else if (event.type == MediaPlayer.Event.EncounteredError || event.type == MediaPlayer.Event.EndReached) {
                    testHandler.post(new Runnable() { @Override public void run() {
                        Toast.makeText(CameraListActivity.this, camera.name + ": bağlantı başarısız", Toast.LENGTH_SHORT).show(); stopTestPlayer();
                    }});
                }
            }
        });
        Media media = new Media(testLibVLC, camera.playbackUri(settings.go2rtcUrl()));
        if (settings.rtspTcp()) media.addOption(":rtsp-tcp");
        media.addOption(":no-audio");
        media.addOption(":network-caching=" + settings.networkCacheMs());
        testPlayer.setMedia(media); media.release(); testPlayer.play();
        testHandler.postDelayed(new Runnable() { @Override public void run() {
            if (testPlayer != null) { Toast.makeText(CameraListActivity.this, camera.name + ": zaman aşımı", Toast.LENGTH_SHORT).show(); stopTestPlayer(); }
        }}, 8000);
    }

    private void stopTestPlayer() {
        if (testPlayer != null) { testPlayer.setEventListener(null); testPlayer.stop(); testPlayer.release(); testPlayer = null; }
        if (testLibVLC != null) { testLibVLC.release(); testLibVLC = null; }
    }

    @Override protected void onDestroy() { if (scanner != null) { scanner.stop(); scanner = null; } stopTestPlayer(); super.onDestroy(); }

    private LinearLayout.LayoutParams rowLayoutParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(8));
        return params;
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private String sanitizedEndpoint(String value) {
        Uri uri = Uri.parse(value);
        String host = uri.getHost();
        if (host == null) return "RTSP adresi";
        StringBuilder result = new StringBuilder("rtsp://").append(host);
        if (uri.getPort() >= 0) result.append(':').append(uri.getPort());
        if (uri.getPath() != null) result.append(uri.getPath());
        return result.toString();
    }
}
