package local.camerawall;

import android.app.Activity;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

/** Reusable, lifecycle-explicit RTSP player for wall and fullscreen views. */
final class CameraPlayerView extends FrameLayout implements MediaPlayer.EventListener, SurfaceHolder.Callback {
    private static final long RECONNECT_INTERVAL_MS = 15000;

    private final Activity activity;
    private final LibVLC libVLC;
    private final Handler handler;
    private final SurfaceView surface;
    private final TextView status;
    private String cameraName;
    private Uri cameraUri;
    private MediaPlayer player;
    private final Runnable reconnect = new Runnable() {
        @Override public void run() { connect(); }
    };

    CameraPlayerView(Activity activity, LibVLC libVLC, Handler handler) {
        super(activity);
        this.activity = activity;
        this.libVLC = libVLC;
        this.handler = handler;
        setBackgroundColor(Color.rgb(12, 12, 12));

        surface = new SurfaceView(activity);
        addView(surface, new FrameLayout.LayoutParams(-1, -1));
        surface.getHolder().addCallback(this);

        status = new TextView(activity);
        status.setTextColor(Color.WHITE);
        status.setTextSize(15);
        status.setGravity(Gravity.CENTER);
        status.setBackgroundColor(0x66000000);
        addView(status, new FrameLayout.LayoutParams(-1, -1));
    }

    void bind(String name, Uri uri) {
        stop();
        cameraName = name;
        cameraUri = uri;
        if (cameraUri == null) {
            status.setText("");
            status.setVisibility(VISIBLE);
            return;
        }
        showConnecting();
        connectSoon(100);
    }

    void stop() {
        handler.removeCallbacks(reconnect);
        stopPlayerOnly();
    }

    private void connectSoon(long delay) {
        handler.removeCallbacks(reconnect);
        handler.postDelayed(reconnect, delay);
    }

    private void connect() {
        if (cameraUri == null || activity.isFinishing()) return;
        stopPlayerOnly();
        showConnecting();
        player = new MediaPlayer(libVLC);
        player.setEventListener(this);
        player.getVLCVout().setVideoView(surface);
        player.getVLCVout().attachViews();
        if (getWidth() > 0 && getHeight() > 0) {
            player.getVLCVout().setWindowSize(getWidth(), getHeight());
        }

        Media media = new Media(libVLC, cameraUri);
        media.addOption(":rtsp-tcp");
        media.addOption(":no-audio");
        media.addOption(":network-caching=800");
        player.setMedia(media);
        media.release();
        player.setAspectRatio(null);
        player.setScale(0);
        player.play();
    }

    private void showConnecting() {
        status.setText(cameraName + "\nConnecting…");
        status.setVisibility(VISIBLE);
    }

    private void failed() {
        if (cameraUri == null) return;
        stopPlayerOnly();
        status.setVisibility(VISIBLE);
        status.setText(cameraName + "\nNo connection — retrying");
        connectSoon(RECONNECT_INTERVAL_MS);
    }

    private void stopPlayerOnly() {
        if (player == null) return;
        player.setEventListener(null);
        player.stop();
        player.getVLCVout().detachViews();
        player.release();
        player = null;
    }

    @Override public void surfaceCreated(SurfaceHolder holder) { }

    @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (player != null) player.getVLCVout().setWindowSize(width, height);
    }

    @Override public void surfaceDestroyed(SurfaceHolder holder) { }

    @Override public void onEvent(final MediaPlayer.Event event) {
        handler.post(new Runnable() {
            @Override public void run() {
                if (event.type == MediaPlayer.Event.Playing && player != null) {
                    player.setAspectRatio(null);
                    player.setScale(0);
                    status.setVisibility(View.GONE);
                } else if (event.type == MediaPlayer.Event.EncounteredError
                    || event.type == MediaPlayer.Event.EndReached) {
                    failed();
                }
            }
        });
    }
}
