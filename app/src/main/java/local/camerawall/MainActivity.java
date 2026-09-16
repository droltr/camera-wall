package local.camerawall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import org.videolan.libvlc.LibVLC;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int MAX_CAMERAS_PER_PAGE = 8;
    private static final String STATE_CURRENT_PAGE = "current_camera_page";

    private final Handler handler = new Handler();
    private final CameraPlayerView[] tiles = new CameraPlayerView[MAX_CAMERAS_PER_PAGE];
    private LibVLC libVLC;
    private CameraRepository cameraRepository;
    private AppSettings appSettings;
    private TextView cameraCount;
    private List<CameraSpec> cameras = new ArrayList<>();
    private int currentPage;
    private final Runnable nextPage = new Runnable() {
        @Override public void run() {
            changePage(1);
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        appSettings = new AppSettings(this);
        if (appSettings.keepScreenOn()) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();
        ArrayList<String> options = new ArrayList<>();
        options.add("--no-audio"); if (appSettings.rtspTcp()) options.add("--rtsp-tcp");
        options.add("--network-caching=" + appSettings.networkCacheMs());
        options.add(appSettings.hardwareAcceleration() ? "--avcodec-hw=any" : "--avcodec-hw=none");
        libVLC = new LibVLC(this, options);
        cameraRepository = new CameraRepository(this);
        cameras = cameraRepository.getCameras();
        if (state != null) currentPage = state.getInt(STATE_CURRENT_PAGE, 0);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BACKGROUND);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(dp(18), dp(8), dp(18), dp(4));
        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_camera_wall);
        header.addView(logo, new LinearLayout.LayoutParams(dp(42), dp(42)));
        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setPadding(dp(10), 0, 0, 0);
        TextView title = new TextView(this);
        title.setText("Camera Wall");
        title.setTextColor(Ui.PRIMARY);
        title.setTextSize(19);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        TextView subtitle = new TextView(this);
        subtitle.setText("Güvenlik kamera paneli");
        subtitle.setTextColor(Ui.SECONDARY);
        subtitle.setTextSize(12);
        brand.addView(title);
        brand.addView(subtitle);
        header.addView(brand, new LinearLayout.LayoutParams(0, -2, 1f));
        cameraCount = new TextView(this);
        cameraCount.setText(cameras.size() + " kamera");
        cameraCount.setTextColor(Ui.ACCENT);
        cameraCount.setTextSize(13);
        header.addView(cameraCount);
        root.addView(header, new LinearLayout.LayoutParams(-1, dp(58)));

        LinearLayout layoutPicker = new LinearLayout(this);
        layoutPicker.setGravity(android.view.Gravity.CENTER_VERTICAL);
        layoutPicker.setPadding(dp(14), dp(1), dp(14), dp(3));
        TextView layoutLabel = new TextView(this);
        layoutLabel.setText("Kamera görünümü");
        layoutLabel.setTextColor(Ui.SECONDARY);
        layoutLabel.setTextSize(13);
        layoutPicker.addView(layoutLabel, new LinearLayout.LayoutParams(dp(126), dp(40)));
        final Spinner layoutChoice = new Spinner(this, Spinner.MODE_DROPDOWN);
        ArrayAdapter<String> layoutAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, new String[]{"4 kamera · 2×2", "8 kamera · 4×2"});
        layoutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        layoutChoice.setAdapter(layoutAdapter);
        layoutChoice.setContentDescription("Kamera görünümü sayısı");
        layoutChoice.setSelection(appSettings.groupSize() == 4 ? 0 : 1);
        layoutPicker.addView(layoutChoice, new LinearLayout.LayoutParams(0, dp(42), 1f));
        layoutChoice.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selectedSize = position == 0 ? 4 : 8;
                if (selectedSize != appSettings.groupSize()) {
                    appSettings.saveGroupSize(selectedSize);
                    currentPage = 0;
                    recreate();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });
        root.addView(layoutPicker);

        SwipeableLayout wall = new SwipeableLayout(this, new SwipeableLayout.Listener() {
            @Override public void onSwipe(boolean nextPage) {
                changePage(nextPage ? 1 : -1);
            }
        });
        wall.setOrientation(LinearLayout.VERTICAL); wall.setBackgroundColor(Ui.BACKGROUND);
        boolean portrait = getResources().getConfiguration().orientation
            == android.content.res.Configuration.ORIENTATION_PORTRAIT;
        int columns = appSettings.groupSize() == 4 || portrait ? 2 : 4;
        int rows = appSettings.groupSize() / columns;
        for (int row = 0; row < rows; row++) {
            LinearLayout line = new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL);
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                tiles[index] = new CameraPlayerView(this, libVLC, handler, appSettings);
                final int slot = index;
                tiles[index].setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View view) { openCamera(slot); }
                });
                line.addView(tiles[index], horizontalWeight());
            }
            wall.addView(line, verticalWeight());
        }
        root.addView(wall, new LinearLayout.LayoutParams(-1, 0, 1f));
        BottomNavigationBar navigation = new BottomNavigationBar(this, BottomNavigationBar.Destination.HOME);
        navigation.setListener(new BottomNavigationBar.Listener() {
            @Override public void onDestinationSelected(BottomNavigationBar.Destination destination) {
                if (destination == BottomNavigationBar.Destination.CAMERAS) {
                    startActivity(new Intent(MainActivity.this, CameraListActivity.class));
                } else if (destination == BottomNavigationBar.Destination.SETTINGS) {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                }
            }
        });
        root.addView(navigation, new LinearLayout.LayoutParams(-1, dp(BottomNavigationBar.HEIGHT_DP)));
        setContentView(root);
    }

    private int camerasPerPage() { return appSettings.groupSize(); }
    private int pageCount() { return Math.max(1, (cameras.size() + camerasPerPage() - 1) / camerasPerPage()); }
    private void changePage(int direction) {
        if (pageCount() <= 1) return;
        currentPage = (currentPage + direction + pageCount()) % pageCount();
        showPage(currentPage);
        updateCameraCount();
        schedulePageChange();
    }
    private void updateCameraCount() {
        cameraCount.setText(cameras.size() + " kamera  ·  " + (currentPage + 1) + "/" + pageCount());
    }
    private void showPage(int page) {
        for (int slot = 0; slot < camerasPerPage(); slot++) {
            int cameraIndex = page * camerasPerPage() + slot;
            CameraSpec camera = cameraIndex < cameras.size() ? cameras.get(cameraIndex) : null;
            tiles[slot].bind(camera == null ? null : camera.name, camera == null ? null : camera.playbackUri(appSettings.go2rtcUrl()), appSettings.showLabels());
        }
    }
    private void schedulePageChange() {
        handler.removeCallbacks(nextPage);
        if (pageCount() > 1 && appSettings.autoPage()) handler.postDelayed(nextPage, appSettings.pageIntervalSeconds() * 1000L);
    }
    private void openCamera(int slot) {
        int cameraIndex = currentPage * camerasPerPage() + slot;
        if (cameraIndex >= cameras.size()) return;
        Intent intent = new Intent(this, SingleCameraActivity.class);
        intent.putExtra(SingleCameraActivity.EXTRA_CAMERA_INDEX, cameraIndex);
        startActivity(intent);
    }
    private LinearLayout.LayoutParams horizontalWeight() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1f); p.setMargins(1,1,1,1); return p; }
    private LinearLayout.LayoutParams verticalWeight() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 0, 1f); p.setMargins(1,1,1,1); return p; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void hideSystemUi() { getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE); }
    @Override public void onWindowFocusChanged(boolean focus) { super.onWindowFocusChanged(focus); if (focus) hideSystemUi(); }
    @Override protected void onResume() {
        super.onResume();
        cameras = cameraRepository.getCameras();
        currentPage = Math.min(currentPage, pageCount() - 1);
        updateCameraCount();
        showPage(currentPage);
        schedulePageChange();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt(STATE_CURRENT_PAGE, currentPage);
        super.onSaveInstanceState(state);
    }
    @Override protected void onPause() { handler.removeCallbacks(nextPage); for (CameraPlayerView tile : tiles) if (tile != null) tile.stop(); super.onPause(); }
    @Override protected void onDestroy() { if (libVLC != null) { libVLC.release(); libVLC = null; } super.onDestroy(); }

}
