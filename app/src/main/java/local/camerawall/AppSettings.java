package local.camerawall;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONException;
import org.json.JSONObject;

/** Persisted application preferences with conservative playback defaults. */
final class AppSettings {
    static final int DEFAULT_PAGE_INTERVAL_SECONDS = 3;
    static final int DEFAULT_NETWORK_CACHE_MS = 800;
    private static final String PREFS = "app_settings";
    private static final String PAGE_INTERVAL = "page_interval_seconds";
    private static final String AUTO_PAGE = "auto_page";
    private static final String GROUP_SIZE = "group_size";
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
    int groupSize() { return prefs.getInt(GROUP_SIZE, 4) == 8 ? 8 : 4; }
    void saveGroupSize(int size) { prefs.edit().putInt(GROUP_SIZE, size == 8 ? 8 : 4).apply(); }
    int networkCacheMs() { return clamp(prefs.getInt(NETWORK_CACHE, DEFAULT_NETWORK_CACHE_MS), 100, 10000); }
    boolean rtspTcp() { return prefs.getBoolean(RTSP_TCP, true); }
    // ASUS K012's Intel OMX AVC decoder loses reference frames on the NVR
    // streams and produces grey/corrupted frames. Prefer VLC software decode.
    boolean hardwareAcceleration() { return false; }
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

    JSONObject backupSnapshot() throws JSONException {
        JSONObject value = new JSONObject();
        value.put("pageIntervalSeconds", pageIntervalSeconds());
        value.put("autoPage", autoPage());
        value.put("groupSize", groupSize());
        value.put("networkCacheMs", networkCacheMs());
        value.put("rtspTcp", rtspTcp());
        value.put("showLabels", showLabels());
        value.put("keepScreenOn", keepScreenOn());
        value.put("go2rtcUrl", go2rtcUrl());
        value.put("go2rtcUser", go2rtcUser());
        value.put("go2rtcPassword", go2rtcPassword());
        return value;
    }

    boolean restoreBackup(JSONObject value) {
        if (!isValidBackup(value)) return false;
        int interval = value.optInt("pageIntervalSeconds", -1);
        int group = value.optInt("groupSize", -1);
        int cache = value.optInt("networkCacheMs", -1);
        String server = value.optString("go2rtcUrl", null);
        String user = value.optString("go2rtcUser", null);
        String password = value.optString("go2rtcPassword", null);
        if (interval < 1 || interval > 3600 || (group != 4 && group != 8)
            || cache < 100 || cache > 10000 || server == null || server.length() > 2048
            || user == null || user.length() > 512 || password == null || password.length() > 512) return false;
        return prefs.edit().putInt(PAGE_INTERVAL, interval)
            .putBoolean(AUTO_PAGE, value.optBoolean("autoPage"))
            .putInt(GROUP_SIZE, group)
            .putInt(NETWORK_CACHE, cache)
            .putBoolean(RTSP_TCP, value.optBoolean("rtspTcp"))
            .putBoolean(HW_ACCEL, false)
            .putBoolean(SHOW_LABELS, value.optBoolean("showLabels"))
            .putBoolean(KEEP_SCREEN, value.optBoolean("keepScreenOn"))
            .putString(GO2RTC_URL, server)
            .putString(GO2RTC_USER, user)
            .putString(GO2RTC_PASSWORD, password)
            .commit();
    }

    boolean isValidBackup(JSONObject value) {
        if (value == null) return false;
        Object interval = value.opt("pageIntervalSeconds");
        Object auto = value.opt("autoPage");
        Object group = value.opt("groupSize");
        Object cache = value.opt("networkCacheMs");
        Object tcp = value.opt("rtspTcp");
        Object labels = value.opt("showLabels");
        Object keep = value.opt("keepScreenOn");
        Object server = value.opt("go2rtcUrl");
        Object user = value.opt("go2rtcUser");
        Object password = value.opt("go2rtcPassword");
        if (!(interval instanceof Number) || !(auto instanceof Boolean) || !(group instanceof Number)
            || !(cache instanceof Number) || !(tcp instanceof Boolean) || !(labels instanceof Boolean)
            || !(keep instanceof Boolean) || !(server instanceof String) || !(user instanceof String)
            || !(password instanceof String)) return false;
        int intervalValue = ((Number) interval).intValue();
        int groupValue = ((Number) group).intValue();
        int cacheValue = ((Number) cache).intValue();
        return ((Number) interval).doubleValue() == intervalValue
            && ((Number) group).doubleValue() == groupValue
            && ((Number) cache).doubleValue() == cacheValue
            && intervalValue >= 1 && intervalValue <= 3600 && (groupValue == 4 || groupValue == 8)
            && cacheValue >= 100 && cacheValue <= 10000
            && ((String) server).length() <= 2048 && ((String) user).length() <= 512
            && ((String) password).length() <= 512;
    }

    void reset() { prefs.edit().clear().apply(); }
    void saveGo2rtc(String url, String user, String password) { prefs.edit().putString(GO2RTC_URL, url.trim()).putString(GO2RTC_USER, user.trim()).putString(GO2RTC_PASSWORD, password).apply(); }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
