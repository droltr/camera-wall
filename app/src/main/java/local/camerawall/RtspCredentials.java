package local.camerawall;

import java.net.URLDecoder;
import java.util.Locale;

/** Separates optional RTSP user information from the visible stream URL. */
final class RtspCredentials {
    static final class Values {
        final String url;
        final String username;
        final String password;

        Values(String url, String username, String password) {
            this.url = url;
            this.username = username;
            this.password = password;
        }
    }

    private RtspCredentials() { }

    static Values normalize(String url, String username, String password) {
        if (url == null) throw new IllegalArgumentException("RTSP URL is required");
        String safeUsername = username == null ? "" : username.trim();
        String safePassword = password == null ? "" : password;
        try {
            int schemeEnd = url.indexOf("://");
            if (schemeEnd < 0) return new Values(url, safeUsername, safePassword);
            if (!"rtsp".equals(url.substring(0, schemeEnd).toLowerCase(Locale.ROOT))) {
                return new Values(url, safeUsername, safePassword);
            }
            int authorityStart = schemeEnd + 3;
            int authorityEnd = url.length();
            for (char delimiter : new char[] {'/', '?', '#'}) {
                int index = url.indexOf(delimiter, authorityStart);
                if (index >= 0 && index < authorityEnd) authorityEnd = index;
            }
            String rawAuthority = url.substring(authorityStart, authorityEnd);
            int at = rawAuthority.lastIndexOf('@');
            if (at < 0) return new Values(url, safeUsername, safePassword);

            String userInfo = rawAuthority.substring(0, at);
            int separator = userInfo.indexOf(':');
            String embeddedUsername = decode(separator < 0 ? userInfo : userInfo.substring(0, separator));
            String embeddedPassword = separator < 0 ? "" : decode(userInfo.substring(separator + 1));
            if (safeUsername.isEmpty() && safePassword.isEmpty()) {
                safeUsername = embeddedUsername;
                safePassword = embeddedPassword;
            }

            String safeUrl = url.substring(0, authorityStart) + rawAuthority.substring(at + 1)
                + url.substring(authorityEnd);
            return new Values(safeUrl, safeUsername, safePassword);
        } catch (Exception error) {
            if (error instanceof IllegalArgumentException) throw (IllegalArgumentException) error;
            throw new IllegalArgumentException("Invalid RTSP URL", error);
        }
    }

    private static String decode(String value) throws Exception {
        // URLDecoder treats '+' as a space; in URI user information it is literal.
        String encoded = value.replace("+", "%2B").replaceAll("%(?![0-9A-Fa-f]{2})", "%25");
        return URLDecoder.decode(encoded, "UTF-8");
    }
}
