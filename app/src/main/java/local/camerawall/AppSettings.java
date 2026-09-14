package local.camerawall;

import android.content.Context;
import android.content.SharedPreferences;

/** Persisted application preferences with conservative playback defaults. */
final class AppSettings {
    static final int DEFAULT_PAGE_INTERVAL_SECONDS = 3;
    static final int DEFAULT_NETWORK_CACHE_MS = 800;
    private static final String PREFS = "app_settings";
    private static final String PAGE_INTERVAL = "page_interval_seconds";
    private static final String AUTO_PAGE = "auto_page";
    private static final String NETWORK_CACHE = "network_cache_ms";
    private static final String RTSP_TCP = "rtsp_tcp";
    private static final String HW_ACCEL = "hardware_acceleration";
    private static final String SHOW_LABELS = "show_labels";
    private static final String KEEP_SCREEN = "keep_screen_on";
    private static final String GO2RTC_URL = "go2rtc_url";
    private static final String GO2RTC_USER = "go2rtc_user";
    private static final String GO2RTC_PASSWORD = "go2rtc_password";

    private final SharedPreferences prefs;
    AppSettings(Context context) { prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE); }
    int pageIntervalSeconds() { return clamp(prefs.getInt(PAGE_INTERVAL, DEFAULT_PAGE_INTERVAL_SECONDS), 1, 3600); }
    boolean autoPage() { return prefs.getBoolean(AUTO_PAGE, true); }
    int networkCacheMs() { return clamp(prefs.getInt(NETWORK_CACHE, DEFAULT_NETWORK_CACHE_MS), 100, 10000); }
    boolean rtspTcp() { return prefs.getBoolean(RTSP_TCP, true); }
    boolean hardwareAcceleration() { return prefs.getBoolean(HW_ACCEL, true); }
    boolean showLabels() { return prefs.getBoolean(SHOW_LABELS, true); }
    boolean keepScreenOn() { return prefs.getBoolean(KEEP_SCREEN, true); }
    String go2rtcUrl() { return prefs.getString(GO2RTC_URL, ""); }
    String go2rtcUser() { return prefs.getString(GO2RTC_USER, ""); }
    String go2rtcPassword() { return prefs.getString(GO2RTC_PASSWORD, ""); }
    void save(int pageSeconds, boolean auto, int cacheMs, boolean tcp, boolean hw, boolean labels, boolean keep) {
        prefs.edit().putInt(PAGE_INTERVAL, clamp(pageSeconds, 1, 3600)).putBoolean(AUTO_PAGE, auto)
            .putInt(NETWORK_CACHE, clamp(cacheMs, 100, 10000)).putBoolean(RTSP_TCP, tcp)
            .putBoolean(HW_ACCEL, hw).putBoolean(SHOW_LABELS, labels).putBoolean(KEEP_SCREEN, keep).apply();
    }
    void reset() { prefs.edit().clear().apply(); }
    void saveGo2rtc(String url, String user, String password) { prefs.edit().putString(GO2RTC_URL, url.trim()).putString(GO2RTC_USER, user.trim()).putString(GO2RTC_PASSWORD, password).apply(); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
