package local.camerawall;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

final class CameraSpec {
    final String name;
    final String url;
    final String username;
    final String password;

    CameraSpec(String name, String url, String username, String password) {
        this.name = name;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    static CameraSpec fromJson(JSONObject value) throws JSONException {
        return new CameraSpec(
            value.getString("name"),
            value.getString("url"),
            value.optString("username"),
            value.optString("password")
        );
    }

    JSONObject toJson() throws JSONException {
        JSONObject value = new JSONObject();
        value.put("name", name);
        value.put("url", url);
        value.put("username", username);
        value.put("password", password);
        return value;
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
