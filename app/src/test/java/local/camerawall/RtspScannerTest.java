package local.camerawall;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class RtspScannerTest {
    @Test public void rejectsMissingLoopbackAndLinkLocalAddresses() {
        assertFalse(RtspScanner.isUsableIpv4Address(0));
        assertFalse(RtspScanner.isUsableIpv4Address(ipv4(127, 0, 0, 1)));
        assertFalse(RtspScanner.isUsableIpv4Address(ipv4(169, 254, 1, 2)));
    }

    @Test public void acceptsAssignedPrivateAndPublicUnicastAddresses() {
        assertTrue(RtspScanner.isUsableIpv4Address(ipv4(192, 168, 1, 10)));
        assertTrue(RtspScanner.isUsableIpv4Address(ipv4(8, 8, 8, 8)));
    }

    private static int ipv4(int first, int second, int third, int fourth) {
        return first | (second << 8) | (third << 16) | (fourth << 24);
    }
}
