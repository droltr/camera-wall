package local.camerawall;

import android.app.Activity;
import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Uses a loopback-only disposable go2rtc response; no camera or LAN endpoint is contacted. */
@RunWith(AndroidJUnit4.class)
public final class Go2rtcSyncInstrumentedTest {
    private static final String STREAMS_JSON = "{"
        + "\"frontdoor_alias\":{\"producers\":[{\"url\":\"RTSP://camera-a.invalid:554/live\"}]},"
        + "\"frontdoor\":{\"producers\":[{\"url\":\"rtsp://camera-a.invalid:554/live\"}]},"
        + "\"office\":{\"producers\":[{\"url\":\"rtsp://camera-b.invalid:554/live\"}]},"
        + "\"offline\":{\"producers\":[]}"
        + "}";

    @Test public void syncReplacesLocalListAndDeduplicatesProducerAliases() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final CameraRepository repository = new CameraRepository(context);
        final List<CameraSpec> originalCameras = repository.getCameras();
        final AppSettings settings = new AppSettings(context);
        final String originalUrl = settings.go2rtcUrl();
        final String originalUser = settings.go2rtcUser();
        final String originalPassword = settings.go2rtcPassword();
        final ServerSocket server = new ServerSocket(0, 1, InetAddress.getByName("localhost"));
        server.setSoTimeout(10000);
        final String temporaryUrl = "http://localhost:" + server.getLocalPort();
        final Thread serverThread = new Thread(new Runnable() {
            @Override public void run() {
                try (Socket socket = server.accept()) {
                    BufferedReader request = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = request.readLine()) != null && line.length() > 0) { }
                    byte[] body = STREAMS_JSON.getBytes(StandardCharsets.UTF_8);
                    OutputStream output = socket.getOutputStream();
                    output.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: "
                        + body.length + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                    output.write(body);
                    output.flush();
                } catch (Exception ignored) { }
            }
        });
        CameraListActivity activity = null;
        try {
            serverThread.start();
            settings.saveGo2rtc(temporaryUrl, "", "");
            activity = (CameraListActivity) launch(CameraListActivity.class);
            final CameraListActivity currentActivity = activity;
            InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override public void run() { currentActivity.syncGo2rtcStreams(settings, temporaryUrl); }
            });

            long deadline = System.currentTimeMillis() + 12000;
            List<CameraSpec> synced = repository.getCameras();
            while (System.currentTimeMillis() < deadline && synced.size() != 3) {
                Thread.sleep(100);
                synced = repository.getCameras();
            }
            assertEquals("the server list replaces old local cameras and removes one alias", 3, synced.size());
            HashSet<String> names = new HashSet<>();
            HashSet<String> urls = new HashSet<>();
            for (CameraSpec camera : synced) {
                names.add(camera.name);
                urls.add(camera.url);
                assertEquals("synchronized go2rtc entries do not copy upstream credentials", "", camera.username);
                assertEquals("synchronized go2rtc entries do not copy upstream credentials", "", camera.password);
            }
            assertEquals(3, names.size());
            assertEquals(3, urls.size());
            assertTrue(names.containsAll(Arrays.asList("frontdoor", "office", "offline")));
        } finally {
            repository.replaceAll(originalCameras);
            settings.saveGo2rtc(originalUrl, originalUser, originalPassword);
            server.close();
            serverThread.join(2000);
            final CameraListActivity currentActivity = activity;
            if (currentActivity != null) InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
                @Override public void run() { if (!currentActivity.isFinishing()) currentActivity.finish(); }
            });
        }
    }

    private Activity launch(Class<? extends Activity> screen) {
        android.content.Intent intent = new android.content.Intent(
            InstrumentationRegistry.getInstrumentation().getTargetContext(), screen);
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
        return InstrumentationRegistry.getInstrumentation().startActivitySync(intent);
    }
}
