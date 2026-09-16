package local.camerawall;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class RtspCredentialsTest {
    @Test public void extractsEncodedCredentialsAndKeepsPathAndQuery() {
        RtspCredentials.Values result = RtspCredentials.normalize(
            "rtsp://cam%40operator:p%3Ass+word@camera.invalid:554/live/ch1?transport=tcp", "", "");
        assertEquals("rtsp://camera.invalid:554/live/ch1?transport=tcp", result.url);
        assertEquals("cam@operator", result.username);
        assertEquals("p:ss+word", result.password);
    }

    @Test public void keepsExplicitCredentialsAndRemovesEmbeddedOnes() {
        RtspCredentials.Values result = RtspCredentials.normalize(
            "rtsp://old:embedded@camera.invalid/stream", "new", "separate");
        assertEquals("rtsp://camera.invalid/stream", result.url);
        assertEquals("new", result.username);
        assertEquals("separate", result.password);
    }

    @Test public void leavesCredentialFreeRtspUrlUnchanged() {
        String url = "rtsp://camera.invalid:554/live/ch1?token=not-a-real-secret";
        RtspCredentials.Values result = RtspCredentials.normalize(url, "user", "pass");
        assertEquals(url, result.url);
        assertEquals("user", result.username);
        assertEquals("pass", result.password);
    }
}
