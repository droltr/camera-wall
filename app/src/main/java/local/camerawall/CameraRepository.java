package local.camerawall;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/** Owns ordered camera persistence and the build-time recovery fallback. */
final class CameraRepository {
    private static final String PREFERENCES_NAME = "camera_wall_cameras";
    private static final String KEY_CAMERAS = "cameras_json_v1";

    private final SharedPreferences preferences;
    private final List<CameraSpec> fallbackCameras;

    CameraRepository(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        fallbackCameras = parse(BuildConfig.CAMERAS_JSON);
        if (!preferences.contains(KEY_CAMERAS)) save(fallbackCameras);
    }

    List<CameraSpec> getCameras() {
        if (!preferences.contains(KEY_CAMERAS)) return copyOf(fallbackCameras);
        try {
            return parseStrict(preferences.getString(KEY_CAMERAS, "[]"));
        } catch (IllegalArgumentException error) {
            return copyOf(fallbackCameras);
        }
    }

    boolean replaceAll(List<CameraSpec> cameras) {
        return save(cameras);
    }

    boolean add(CameraSpec camera) {
        List<CameraSpec> cameras = getCameras();
        cameras.add(camera);
        return save(cameras);
    }

    boolean update(int index, CameraSpec camera) {
        List<CameraSpec> cameras = getCameras();
        if (index < 0 || index >= cameras.size()) return false;
        cameras.set(index, camera);
        return save(cameras);
    }

    boolean delete(int index) {
        List<CameraSpec> cameras = getCameras();
        if (index < 0 || index >= cameras.size()) return false;
        cameras.remove(index);
        return save(cameras);
    }

    boolean move(int index, int delta) {
        List<CameraSpec> cameras = getCameras();
        int target = index + delta;
        if (index < 0 || target < 0 || index >= cameras.size() || target >= cameras.size()) return false;
        CameraSpec camera = cameras.remove(index);
        cameras.add(target, camera);
        return save(cameras);
    }

    private boolean save(List<CameraSpec> cameras) {
        JSONArray values = new JSONArray();
        try {
            for (CameraSpec camera : cameras) values.put(camera.toJson());
        } catch (JSONException error) {
            return false;
        }
        return preferences.edit().putString(KEY_CAMERAS, values.toString()).commit();
    }

    private static List<CameraSpec> parse(String json) {
        try {
            return parseStrict(json);
        } catch (IllegalArgumentException error) {
            return new ArrayList<>();
        }
    }

    private static List<CameraSpec> parseStrict(String json) {
        ArrayList<CameraSpec> cameras = new ArrayList<>();
        try {
            JSONArray values = new JSONArray(json);
            for (int index = 0; index < values.length(); index++) {
                cameras.add(CameraSpec.fromJson(values.getJSONObject(index)));
            }
        } catch (JSONException error) {
            throw new IllegalArgumentException("Invalid camera data", error);
        }
        return cameras;
    }

    private static List<CameraSpec> copyOf(List<CameraSpec> cameras) {
        return new ArrayList<>(cameras);
    }
}
