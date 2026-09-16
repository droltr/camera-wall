package local.camerawall;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Selects one stable go2rtc stream name for each configured producer source. */
final class Go2rtcCameraSync {
    private static final Comparator<String> NAME_ORDER = new Comparator<String>() {
        @Override public int compare(String left, String right) {
            int length = Integer.compare(left.length(), right.length());
            if (length != 0) return length;
            int insensitive = left.compareToIgnoreCase(right);
            return insensitive != 0 ? insensitive : left.compareTo(right);
        }
    };

    private Go2rtcCameraSync() { }

    static List<String> canonicalNames(Map<String, ? extends List<String>> sourcesByName) {
        Map<String, String> selectedBySource = new HashMap<>();
        for (Map.Entry<String, ? extends List<String>> entry : sourcesByName.entrySet()) {
            String name = entry.getKey();
            if (name == null || name.length() == 0) continue;

            List<String> sources = normalizeSources(entry.getValue());
            String identity = sources.isEmpty()
                ? "stream:" + name
                : "source:" + join(sources);
            String current = selectedBySource.get(identity);
            if (current == null || NAME_ORDER.compare(name, current) < 0) {
                selectedBySource.put(identity, name);
            }
        }

        ArrayList<String> names = new ArrayList<>(selectedBySource.values());
        Collections.sort(names, new Comparator<String>() {
            @Override public int compare(String left, String right) {
                int insensitive = left.compareToIgnoreCase(right);
                return insensitive != 0 ? insensitive : left.compareTo(right);
            }
        });
        return names;
    }

    private static List<String> normalizeSources(List<String> sources) {
        ArrayList<String> normalized = new ArrayList<>();
        if (sources != null) for (String source : sources) {
            if (source == null || source.trim().length() == 0) continue;
            String value = normalizeSource(source.trim());
            if (!normalized.contains(value)) normalized.add(value);
        }
        Collections.sort(normalized);
        return normalized;
    }

    private static String normalizeSource(String source) {
        try {
            URI uri = new URI(source);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) return source;
            StringBuilder value = new StringBuilder(scheme.toLowerCase(Locale.ROOT)).append("://");
            if (uri.getRawUserInfo() != null) value.append(uri.getRawUserInfo()).append('@');
            if (host.indexOf(':') >= 0) value.append('[').append(host.toLowerCase(Locale.ROOT)).append(']');
            else value.append(host.toLowerCase(Locale.ROOT));
            if (uri.getPort() >= 0) value.append(':').append(uri.getPort());
            if (uri.getRawPath() != null) value.append(uri.getRawPath());
            if (uri.getRawQuery() != null) value.append('?').append(uri.getRawQuery());
            if (uri.getRawFragment() != null) value.append('#').append(uri.getRawFragment());
            return value.toString();
        } catch (URISyntaxException ignored) {
            return source;
        }
    }

    private static String join(List<String> values) {
        StringBuilder joined = new StringBuilder();
        for (String value : values) {
            if (joined.length() > 0) joined.append('\u001f');
            joined.append(value);
        }
        return joined.toString();
    }
}
