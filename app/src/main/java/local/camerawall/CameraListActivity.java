package local.camerawall;

import android.app.AlertDialog;
import android.graphics.Color;
import android.net.Uri;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

public final class CameraListActivity extends BaseSectionActivity {
    @Override String sectionTitle() { return "Kameralar"; }
    @Override BottomNavigationBar.Destination destination() { return BottomNavigationBar.Destination.CAMERAS; }

    @Override View createContentView() {
        List<CameraSpec> cameras = new CameraRepository(this).getCameras();
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(24), dp(16), dp(24), dp(16));

        TextView heading = text("Kameralar", 24, Color.WHITE);
        heading.setPadding(0, 0, 0, dp(12));
        content.addView(heading, new LinearLayout.LayoutParams(-1, -2));

        Button add = new Button(this);
        add.setText("+ Kamera ekle");
        add.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showAddDialog(); }
        });
        content.addView(add, rowLayoutParams());

        if (cameras.isEmpty()) {
            TextView empty = text("Henüz kamera eklenmedi.", 18, Color.LTGRAY);
            empty.setGravity(Gravity.CENTER);
            content.addView(empty, new LinearLayout.LayoutParams(-1, 0, 1f));
        } else {
            for (int index = 0; index < cameras.size(); index++) {
                content.addView(cameraRow(index, cameras.get(index)), rowLayoutParams());
            }
        }

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.BLACK);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        return scroll;
    }

    private void showAddDialog() { showCameraDialog(-1, null); }

    private void showCameraDialog(final int editIndex, CameraSpec existing) {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(24), dp(8), dp(24), 0);
        final EditText name = field("Kamera adı");
        final EditText url = field("RTSP adresi (rtsp://…)");
        final EditText username = field("Kullanıcı adı (isteğe bağlı)");
        final EditText password = field("Parola (isteğe bağlı)");
        if (existing != null) {
            name.setText(existing.name); url.setText(existing.url);
            username.setText(existing.username); password.setText(existing.password);
        }
        password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        form.addView(name); form.addView(url); form.addView(username); form.addView(password);

        final AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle(editIndex < 0 ? "Kamera ekle" : "Kamerayı düzenle")
            .setView(form)
            .setNegativeButton("İptal", null)
            .setPositiveButton("Kaydet", null)
            .create();
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
                        CameraSpec camera = new CameraSpec(cameraName, cameraUrl,
                            username.getText().toString().trim(), password.getText().toString());
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
        row.setBackgroundColor(Color.rgb(30, 30, 30));

        row.addView(text(position + ".  " + camera.name, 19, Color.WHITE));
        TextView endpoint = text(sanitizedEndpoint(camera.url), 14, Color.rgb(185, 185, 185));
        endpoint.setPadding(0, dp(4), 0, 0);
        row.addView(endpoint);
        TextView state = text("Kayıtlı", 13, Color.rgb(105, 205, 135));
        state.setPadding(0, dp(4), 0, 0);
        row.addView(state);
        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button edit = new Button(this); edit.setText("Düzenle");
        edit.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showCameraDialog(position, camera); }
        });
        Button remove = new Button(this); remove.setText("Sil");
        remove.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { confirmDelete(position, camera.name); }
        });
        actions.addView(edit, new LinearLayout.LayoutParams(0, -2, 1f));
        actions.addView(remove, new LinearLayout.LayoutParams(0, -2, 1f));
        row.addView(actions);
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
