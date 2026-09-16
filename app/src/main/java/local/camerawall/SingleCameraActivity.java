package local.camerawall;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.videolan.libvlc.LibVLC;

import java.util.ArrayList;
import java.util.List;

public final class SingleCameraActivity extends Activity {
    static final String EXTRA_CAMERA_INDEX = "camera_index";
    private static final String STATE_ZOOM = "camera_zoom";

    private final Handler handler = new Handler();
    private LibVLC libVLC;
    private CameraPlayerView playerView;
    private CameraSpec camera;
    private AppSettings appSettings;
    private TextView zoomLabel;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();

        List<CameraSpec> cameras = new CameraRepository(this).getCameras();
        int cameraIndex = getIntent().getIntExtra(EXTRA_CAMERA_INDEX, -1);
        if (cameraIndex < 0 || cameraIndex >= cameras.size()) {
            finish();
            return;
        }
        camera = cameras.get(cameraIndex);

        appSettings = new AppSettings(this);
        ArrayList<String> options = new ArrayList<>();
        options.add("--no-audio");
        if (appSettings.rtspTcp()) options.add("--rtsp-tcp");
        options.add("--network-caching=" + appSettings.networkCacheMs());
        options.add(appSettings.hardwareAcceleration() ? "--avcodec-hw=any" : "--avcodec-hw=none");
        libVLC = new LibVLC(this, options);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        playerView = new CameraPlayerView(this, libVLC, handler, appSettings);
        playerView.setZoomEnabled(true, new CameraPlayerView.ZoomListener() {
            @Override public void onZoomChanged(float value) { updateZoomLabel(value); }
        });
        if (state != null) playerView.restoreZoom(state.getFloat(STATE_ZOOM, 1f));
        root.addView(playerView, new FrameLayout.LayoutParams(-1, -1));
        root.addView(createHeader(), headerLayoutParams());
        root.addView(createZoomControls(), zoomControlsLayoutParams());
        setContentView(root);
    }

    private View createZoomControls() {
        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setPadding(dp(6), dp(4), dp(6), dp(4));
        controls.setBackground(Ui.shape(0xE6111B2D, Ui.OUTLINE, dp(16)));

        Button minus = zoomButton("−", "Uzaklaştır");
        minus.setOnClickListener(view -> playerView.zoomOut());
        controls.addView(minus, new LinearLayout.LayoutParams(dp(52), dp(48)));

        zoomLabel = new TextView(this);
        zoomLabel.setTextColor(Ui.PRIMARY);
        zoomLabel.setTextSize(14);
        zoomLabel.setGravity(Gravity.CENTER);
        zoomLabel.setMinWidth(dp(62));
        controls.addView(zoomLabel, new LinearLayout.LayoutParams(-2, -1));
        updateZoomLabel(playerView.zoomFactor());

        Button plus = zoomButton("+", "Yakınlaştır");
        plus.setOnClickListener(view -> playerView.zoomIn());
        controls.addView(plus, new LinearLayout.LayoutParams(dp(52), dp(48)));

        Button reset = zoomButton("Sıfırla", "Yakınlaştırmayı sıfırla");
        reset.setOnClickListener(view -> playerView.resetZoom());
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(-2, dp(48));
        resetParams.leftMargin = dp(6);
        controls.addView(reset, resetParams);
        return controls;
    }

    private Button zoomButton(String label, String description) {
        Button button = new Button(this);
        button.setText(label);
        button.setContentDescription(description);
        Ui.button(button, false);
        return button;
    }

    private FrameLayout.LayoutParams zoomControlsLayoutParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-2, dp(60), Gravity.BOTTOM | Gravity.END);
        params.setMargins(dp(12), 0, dp(16), dp(16));
        return params;
    }

    private void updateZoomLabel(float value) {
        if (zoomLabel != null) zoomLabel.setText(String.format(java.util.Locale.ROOT, "%.1f×", value));
    }

    private View createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), 0, dp(16), 0);
        header.setBackgroundColor(0xE6111B2D);

        TextView back = new TextView(this);
        back.setText("‹  Geri");
        back.setTextColor(Ui.ACCENT);
        back.setTextSize(18);
        back.setGravity(Gravity.CENTER);
        back.setContentDescription("Kamera duvarına geri dön");
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { finish(); }
        });
        header.addView(back, new LinearLayout.LayoutParams(dp(120), -1));

        TextView title = new TextView(this);
        title.setText(camera.name);
        title.setTextColor(Ui.PRIMARY);
        title.setTextSize(18);
        title.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(title, new LinearLayout.LayoutParams(0, -1, 1f));
        return header;
    }

    private FrameLayout.LayoutParams headerLayoutParams() {
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, dp(48));
        params.gravity = Gravity.TOP;
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus);
        if (focus) hideSystemUi();
    }

    @Override protected void onResume() {
        super.onResume();
        if (playerView != null) {
            playerView.bind(camera.name, camera.playbackUri(appSettings.go2rtcUrl()), false);
        }
    }

    @Override protected void onPause() {
        if (playerView != null) playerView.stop();
        super.onPause();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (playerView != null) state.putFloat(STATE_ZOOM, playerView.zoomFactor());
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
        super.onDestroy();
    }
}
