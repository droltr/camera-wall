package local.camerawall;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OnvifDiscovery {
    private static final String PROBE = "<?xml version=\"1.0\"?><e:Envelope xmlns:e=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:w=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:d=\"http://schemas.xmlsoap.org/ws/2005/04/discovery\" xmlns:dn=\"http://www.onvif.org/ver10/network/wsdl\"><e:Header><w:MessageID>uuid:camera-wall-probe</w:MessageID><w:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To><w:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action></e:Header><e:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe></e:Body></e:Envelope>";
    private static final Pattern ADDRESS = Pattern.compile("https?://[^\\s<\\\"]+");
    private OnvifDiscovery() { }
    static Set<String> probe(int timeoutMs) throws Exception {
        Set<String> results = new LinkedHashSet<>(); byte[] data = PROBE.getBytes("UTF-8");
        DatagramSocket socket = new DatagramSocket(); socket.setSoTimeout(timeoutMs);
        DatagramPacket request = new DatagramPacket(data, data.length, InetAddress.getByName("239.255.255.250"), 3702); socket.send(request);
        byte[] buffer = new byte[8192]; long end = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < end) { try { DatagramPacket response = new DatagramPacket(buffer, buffer.length); socket.receive(response); String body = new String(response.getData(), 0, response.getLength(), "UTF-8"); Matcher matcher = ADDRESS.matcher(body); while (matcher.find()) results.add(matcher.group()); } catch (java.net.SocketTimeoutException ignored) { break; } }
        socket.close(); return results;
    }
}
