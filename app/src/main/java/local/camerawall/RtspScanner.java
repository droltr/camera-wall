package local.camerawall;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Candidate-only scan: local /24, common RTSP ports, bounded workers and cancellation. */
final class RtspScanner {
    interface Listener { void onCandidate(String host, int port); void onFinished(); }
    private final ExecutorService pool = Executors.newFixedThreadPool(16);
    private final List<Future<?>> jobs = new ArrayList<>();
    private volatile boolean stopped;
    RtspScanner(Context context, Listener listener) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi == null ? null : wifi.getConnectionInfo(); int ip = info == null ? 0 : info.getIpAddress();
        String prefix = (ip & 255) + "." + ((ip >> 8) & 255) + "." + ((ip >> 16) & 255) + ".";
        int[] ports = {554, 8554, 10554};
        for (int host = 1; host < 255; host++) for (final int port : ports) { final String address = prefix + host; jobs.add(pool.submit(new Runnable() { @Override public void run() { if (stopped) return; try { Socket socket = new Socket(); socket.connect(new InetSocketAddress(address, port), 350); socket.close(); if (!stopped) listener.onCandidate(address, port); } catch (Exception ignored) { } }})); }
        pool.submit(new Runnable() { @Override public void run() { for (Future<?> job : jobs) try { job.get(); } catch (Exception ignored) { } listener.onFinished(); pool.shutdownNow(); }});
    }
    void stop() { stopped = true; for (Future<?> job : jobs) job.cancel(true); pool.shutdownNow(); }
}
