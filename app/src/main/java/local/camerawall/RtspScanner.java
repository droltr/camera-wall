package local.camerawall;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.SupplicantState;
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
    static boolean hasUsableWifiAddress(Context context) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi == null || !wifi.isWifiEnabled()) return false;
        WifiInfo info = wifi.getConnectionInfo();
        return info != null && info.getNetworkId() >= 0
            && info.getSupplicantState() == SupplicantState.COMPLETED
            && isUsableIpv4Address(info.getIpAddress());
    }

    static boolean isUsableIpv4Address(int ip) {
        if (ip == 0) return false;
        int first = ip & 255;
        int second = (ip >> 8) & 255;
        return first != 0 && first != 127 && first < 224
            && !(first == 169 && second == 254);
    }

    RtspScanner(Context context, Listener listener) {
        WifiManager wifi = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi == null ? null : wifi.getConnectionInfo(); int ip = info == null ? 0 : info.getIpAddress();
        if (wifi == null || !wifi.isWifiEnabled() || info == null || info.getNetworkId() < 0
                || info.getSupplicantState() != SupplicantState.COMPLETED || !isUsableIpv4Address(ip)) {
            pool.shutdownNow();
            listener.onFinished();
            return;
        }
        String prefix = (ip & 255) + "." + ((ip >> 8) & 255) + "." + ((ip >> 16) & 255) + ".";
        int[] ports = {554, 8554, 10554};
        for (int host = 1; host < 255; host++) for (final int port : ports) { final String address = prefix + host; jobs.add(pool.submit(new Runnable() { @Override public void run() { if (stopped) return; try { Socket socket = new Socket(); socket.connect(new InetSocketAddress(address, port), 350); socket.close(); if (!stopped) listener.onCandidate(address, port); } catch (Exception ignored) { } }})); }
        pool.submit(new Runnable() { @Override public void run() {
            try {
                for (Future<?> job : jobs) try { job.get(); } catch (Exception ignored) { }
                if (!stopped) listener.onFinished();
            } finally {
                pool.shutdownNow();
            }
        }});
    }
    void stop() {
        stopped = true;
        for (Future<?> job : jobs) job.cancel(true);
        pool.shutdownNow();
    }

    boolean isStopped() { return stopped; }
}
