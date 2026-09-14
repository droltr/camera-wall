package local.camerawall;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import org.videolan.libvlc.LibVLC;
import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    private static final int CAMERAS_PER_PAGE = 4;

    private final Handler handler = new Handler();
    private final CameraPlayerView[] tiles = new CameraPlayerView[CAMERAS_PER_PAGE];
    private LibVLC libVLC;
    private CameraRepository cameraRepository;
    private AppSettings appSettings;
    private List<CameraSpec> cameras = new ArrayList<>();
    private int currentPage;
    private final Runnable nextPage = new Runnable() {
        @Override public void run() {
            currentPage = (currentPage + 1) % pageCount();
            showPage(currentPage);
            schedulePageChange();
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
        if (appSettings.hardwareAcceleration()) options.add("--avcodec-hw=any");
        libVLC = new LibVLC(this, options);
        cameraRepository = new CameraRepository(this);
        cameras = cameraRepository.getCameras();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout wall = new LinearLayout(this);
        wall.setOrientation(LinearLayout.VERTICAL); wall.setBackgroundColor(Color.BLACK);
        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL);
            for (int column = 0; column < 2; column++) {
                int index = row * 2 + column;
                tiles[index] = new CameraPlayerView(this, libVLC, handler);
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
        root.addView(navigation, new LinearLayout.LayoutParams(-1, dp(52)));
        setContentView(root);
    }

    private int pageCount() { return Math.max(1, (cameras.size() + CAMERAS_PER_PAGE - 1) / CAMERAS_PER_PAGE); }
    private void showPage(int page) {
        for (int slot = 0; slot < CAMERAS_PER_PAGE; slot++) {
            int cameraIndex = page * CAMERAS_PER_PAGE + slot;
            CameraSpec camera = cameraIndex < cameras.size() ? cameras.get(cameraIndex) : null;
            tiles[slot].bind(camera == null ? null : camera.name, camera == null ? null : camera.playbackUri());
        }
    }
    private void schedulePageChange() {
        handler.removeCallbacks(nextPage);
        if (pageCount() > 1 && appSettings.autoPage()) handler.postDelayed(nextPage, appSettings.pageIntervalSeconds() * 1000L);
    }
    private void openCamera(int slot) {
        int cameraIndex = currentPage * CAMERAS_PER_PAGE + slot;
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
        showPage(currentPage);
        schedulePageChange();
    }
    @Override protected void onPause() { handler.removeCallbacks(nextPage); for (CameraPlayerView tile : tiles) if (tile != null) tile.stop(); super.onPause(); }
    @Override protected void onDestroy() { if (libVLC != null) { libVLC.release(); libVLC = null; } super.onDestroy(); }

}
