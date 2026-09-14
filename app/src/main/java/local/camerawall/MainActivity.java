package local.camerawall;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;

import org.videolan.libvlc.LibVLC;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public final class MainActivity extends Activity {
    private static final int CAMERAS_PER_PAGE = 4;
    private static final long PAGE_INTERVAL_MS = 3000;

    private static final CameraSpec[] CAMERAS = loadCameras();

    private final Handler handler = new Handler();
    private final CameraPlayerView[] tiles = new CameraPlayerView[CAMERAS_PER_PAGE];
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
                tiles[index] = new CameraPlayerView(this, libVLC, handler);
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
                cameras.add(new CameraSpec(
                    value.getString("name"),
                    value.getString("url"),
                    value.optString("username"),
                    value.optString("password")
                ));
            }
        } catch (JSONException error) {
            throw new IllegalStateException("Invalid generated camera configuration", error);
        }
        return cameras.toArray(new CameraSpec[cameras.size()]);
    }
    private void showPage(int page) {
        for (int slot = 0; slot < CAMERAS_PER_PAGE; slot++) {
            int cameraIndex = page * CAMERAS_PER_PAGE + slot;
            CameraSpec camera = cameraIndex < CAMERAS.length ? CAMERAS[cameraIndex] : null;
            tiles[slot].bind(camera == null ? null : camera.name, camera == null ? null : camera.playbackUri());
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
    @Override protected void onPause() { handler.removeCallbacks(nextPage); for (CameraPlayerView tile : tiles) if (tile != null) tile.stop(); super.onPause(); }
    @Override protected void onDestroy() { if (libVLC != null) { libVLC.release(); libVLC = null; } super.onDestroy(); }

    private static final class CameraSpec {
        final String name, url, username, password;
        CameraSpec(String name, String url, String username, String password) {
            this.name = name;
            this.url = url;
            this.username = username;
            this.password = password;
        }

        Uri playbackUri() {
            Uri endpoint = Uri.parse(url);
            if (username.length() == 0) return endpoint;

            String authority = endpoint.getEncodedAuthority();
            if (authority == null || authority.indexOf('@') >= 0) return endpoint;
            String userInfo = Uri.encode(username) + ":" + Uri.encode(password);
            return endpoint.buildUpon().encodedAuthority(userInfo + "@" + authority).build();
        }
    }

}
