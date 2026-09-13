package local.camerawall;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public final class MainActivity extends Activity {
    private static final int CAMERAS_PER_PAGE = 4;
    private static final long PAGE_INTERVAL_MS = 3000;

    private static final CameraSpec[] CAMERAS = loadCameras();

    private final Handler handler = new Handler();
    private final CameraTile[] tiles = new CameraTile[CAMERAS_PER_PAGE];
    private LibVLC libVLC;
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
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        hideSystemUi();
        ArrayList<String> options = new ArrayList<>();
        options.add("--no-audio"); options.add("--rtsp-tcp");
        options.add("--network-caching=800"); options.add("--avcodec-hw=any");
        libVLC = new LibVLC(this, options);

        LinearLayout wall = new LinearLayout(this);
        wall.setOrientation(LinearLayout.VERTICAL); wall.setBackgroundColor(Color.BLACK);
        for (int row = 0; row < 2; row++) {
            LinearLayout line = new LinearLayout(this); line.setOrientation(LinearLayout.HORIZONTAL);
            for (int column = 0; column < 2; column++) {
                int index = row * 2 + column;
                tiles[index] = new CameraTile();
                line.addView(tiles[index], horizontalWeight());
            }
            wall.addView(line, verticalWeight());
        }
        setContentView(wall);
    }

    private static int pageCount() { return Math.max(1, (CAMERAS.length + CAMERAS_PER_PAGE - 1) / CAMERAS_PER_PAGE); }
    private static CameraSpec[] loadCameras() {
        ArrayList<CameraSpec> cameras = new ArrayList<>();
        try {
            JSONArray values = new JSONArray(BuildConfig.CAMERAS_JSON);
            for (int index = 0; index < values.length(); index++) {
                JSONObject value = values.getJSONObject(index);
                cameras.add(new CameraSpec(value.getString("name"), value.getString("url")));
            }
        } catch (JSONException error) {
            throw new IllegalStateException("Invalid generated camera configuration", error);
        }
        return cameras.toArray(new CameraSpec[cameras.size()]);
    }
    private void showPage(int page) {
        for (int slot = 0; slot < CAMERAS_PER_PAGE; slot++) {
            int cameraIndex = page * CAMERAS_PER_PAGE + slot;
            tiles[slot].bind(cameraIndex < CAMERAS.length ? CAMERAS[cameraIndex] : null);
        }
    }
    private void schedulePageChange() {
        handler.removeCallbacks(nextPage);
        if (pageCount() > 1) handler.postDelayed(nextPage, PAGE_INTERVAL_MS);
    }
    private LinearLayout.LayoutParams horizontalWeight() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -1, 1f); p.setMargins(1,1,1,1); return p; }
    private LinearLayout.LayoutParams verticalWeight() { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, 0, 1f); p.setMargins(1,1,1,1); return p; }
    private void hideSystemUi() { getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE); }
    @Override public void onWindowFocusChanged(boolean focus) { super.onWindowFocusChanged(focus); if (focus) hideSystemUi(); }
    @Override protected void onResume() { super.onResume(); showPage(currentPage); schedulePageChange(); }
    @Override protected void onPause() { handler.removeCallbacks(nextPage); for (CameraTile tile : tiles) if (tile != null) tile.stop(); super.onPause(); }
    @Override protected void onDestroy() { if (libVLC != null) { libVLC.release(); libVLC = null; } super.onDestroy(); }

    private static final class CameraSpec {
        final String name, url;
        CameraSpec(String name, String url) { this.name = name; this.url = url; }
    }

    private final class CameraTile extends FrameLayout implements MediaPlayer.EventListener, SurfaceHolder.Callback {
        private final SurfaceView surface; private final TextView status;
        private CameraSpec camera; private MediaPlayer player;
        private final Runnable reconnect = new Runnable() { @Override public void run() { connect(); } };
        CameraTile() {
            super(MainActivity.this); setBackgroundColor(Color.rgb(12,12,12));
            surface = new SurfaceView(MainActivity.this); addView(surface, new FrameLayout.LayoutParams(-1,-1));
            surface.getHolder().addCallback(this);
            status = new TextView(MainActivity.this); status.setTextColor(Color.WHITE); status.setTextSize(15); status.setGravity(Gravity.CENTER); status.setBackgroundColor(0x66000000); addView(status, new FrameLayout.LayoutParams(-1,-1));
        }
        void bind(CameraSpec value) {
            stop(); camera = value;
            if (camera == null) { status.setText(""); status.setVisibility(VISIBLE); return; }
            status.setText(camera.name + "\nConnecting…"); status.setVisibility(VISIBLE); connectSoon(100);
        }
        void connectSoon(long delay) { handler.removeCallbacks(reconnect); handler.postDelayed(reconnect, delay); }
        void connect() {
            if (camera == null || isFinishing() || libVLC == null) return;
            stopPlayerOnly(); status.setVisibility(VISIBLE); status.setText(camera.name + "\nConnecting…");
            player = new MediaPlayer(libVLC); player.setEventListener(this); player.getVLCVout().setVideoView(surface); player.getVLCVout().attachViews();
            if (getWidth() > 0 && getHeight() > 0) player.getVLCVout().setWindowSize(getWidth(), getHeight());
            Media media = new Media(libVLC, android.net.Uri.parse(camera.url)); media.addOption(":rtsp-tcp"); media.addOption(":no-audio"); media.addOption(":network-caching=800"); player.setMedia(media); media.release();
            player.setAspectRatio(null); player.setScale(0); player.play();
        }
        @Override public void surfaceCreated(SurfaceHolder holder) { }
        @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) { if (player != null) player.getVLCVout().setWindowSize(width, height); }
        @Override public void surfaceDestroyed(SurfaceHolder holder) { }
        @Override public void onEvent(final MediaPlayer.Event event) { handler.post(new Runnable() { @Override public void run() { if (event.type == MediaPlayer.Event.Playing && player != null) { player.setAspectRatio(null); player.setScale(0); status.setVisibility(GONE); } else if (event.type == MediaPlayer.Event.EncounteredError || event.type == MediaPlayer.Event.EndReached) failed(); }}); }
        private void failed() { if (camera == null) return; stopPlayerOnly(); status.setVisibility(VISIBLE); status.setText(camera.name + "\nNo connection — retrying"); connectSoon(15000); }
        void stop() { handler.removeCallbacks(reconnect); stopPlayerOnly(); }
        private void stopPlayerOnly() { if (player != null) { player.setEventListener(null); player.stop(); player.getVLCVout().detachViews(); player.release(); player = null; } }
    }
}
