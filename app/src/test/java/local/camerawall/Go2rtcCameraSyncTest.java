package local.camerawall;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public final class Go2rtcCameraSyncTest {
    @Test public void aliasesForSameProducerKeepOneStableShortestName() {
        Map<String, List<String>> sources = new LinkedHashMap<>();
        sources.put("frontdoor_tablet", Arrays.asList("RTSP://CAMERA.local:554/live"));
        sources.put("front", Arrays.asList("rtsp://camera.local:554/live"));
        sources.put("office", Arrays.asList("rtsp://camera.local:554/office"));

        assertEquals(Arrays.asList("front", "office"), Go2rtcCameraSync.canonicalNames(sources));
    }

    @Test public void differentSourcesRemainAndNamesAreReturnedInStableOrder() {
        Map<String, List<String>> sources = new LinkedHashMap<>();
        sources.put("zeta", Arrays.asList("rtsp://camera.local/z"));
        sources.put("alpha", Arrays.asList("rtsp://camera.local/a"));

        assertEquals(Arrays.asList("alpha", "zeta"), Go2rtcCameraSync.canonicalNames(sources));
    }

    @Test public void streamsWithoutProducerDetailsArePreservedSeparately() {
        Map<String, List<String>> sources = new LinkedHashMap<>();
        sources.put("unavailable_one", new ArrayList<String>());
        sources.put("unavailable_two", null);
        sources.put("camera", Arrays.asList(" ", null));

        assertEquals(Arrays.asList("camera", "unavailable_one", "unavailable_two"),
            Go2rtcCameraSync.canonicalNames(sources));
    }

    @Test public void multipleProducersAreDeduplicatedAsAnUnorderedSourceSet() {
        Map<String, List<String>> sources = new LinkedHashMap<>();
        sources.put("alias", Arrays.asList("rtsp://camera.local/a", "rtsp://camera.local/b"));
        sources.put("preferred", Arrays.asList("rtsp://camera.local/b", "rtsp://camera.local/a"));

        assertEquals(Arrays.asList("alias"), Go2rtcCameraSync.canonicalNames(sources));
    }
}
