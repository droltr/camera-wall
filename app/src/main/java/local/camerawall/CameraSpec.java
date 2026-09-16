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

    Uri playbackUri(String go2rtcUrl) {
        String playbackUrl = url;
        // Use the locally configured go2rtc host for Hikvision restreams.
        // Camera/NVR addresses and credentials stay in local app data.
        if (playbackUrl.contains("/Streaming/Channels/") && go2rtcUrl != null && go2rtcUrl.length() > 0) {
            String path = Uri.parse(playbackUrl).getLastPathSegment();
            if (path != null && path.matches("[1-4]0[12]")) {
                Uri server = Uri.parse(go2rtcUrl);
                if (server.getHost() != null) {
                    return Uri.parse("rtsp://" + server.getHost() + ":8554/hik" + path.charAt(0) + "_tablet");
                }
            }
        }
        // The K012's old H.264 decoder is unreliable with the NVR's 960x1080
        // main stream. Use the recorder's low-bandwidth substream for playback.
        if (playbackUrl.contains("/Streaming/Channels/") && playbackUrl.endsWith("01")) {
            playbackUrl = playbackUrl.substring(0, playbackUrl.length() - 2) + "02";
        }
        Uri endpoint = Uri.parse(playbackUrl);
        if (username.length() == 0) return endpoint;

        String authority = endpoint.getEncodedAuthority();
        if (authority == null || authority.indexOf('@') >= 0) return endpoint;
        String userInfo = Uri.encode(username) + ":" + Uri.encode(password);
        return endpoint.buildUpon().encodedAuthority(userInfo + "@" + authority).build();
    }
}
