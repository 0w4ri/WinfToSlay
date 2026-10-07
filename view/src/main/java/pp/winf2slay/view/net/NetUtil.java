package pp.winf2slay.view.net;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Hilfsfunktionen rund um Adressen und Ports.
 */
public final class NetUtil {

    private NetUtil() {
        // Utility-Klasse
    }

    private static final long CACHE_MILLIS = 30_000;
    private static final String[] VIRTUAL_HINTS = {"virtual", "vethernet", "vmware", "vbox", "hyper-v", "wsl",
                                                   "docker", "loopback", "pseudo", "tap", "tun"};
    private static List<String> cached = List.of();
    private static long cachedAt;

    /**
     * Liefert die IPv4-Adressen dieses Rechners, die beste zuerst: Adressen in
     * Heimnetzen (192.168.…, 10.…) von echten Netzwerkkarten vor solchen von
     * virtuellen Adaptern (WSL, Docker, VirtualBox …). Das Ergebnis wird kurz
     * zwischengespeichert, weil die Abfrage unter Windows spürbar dauern kann.
     *
     * @return Adressen für Mitspieler (kann leer sein)
     */
    public static synchronized List<String> localAddresses() {
        long now = System.currentTimeMillis();
        if (now - cachedAt < CACHE_MILLIS && !cached.isEmpty()) return cached;
        List<String[]> found = new ArrayList<>(); // {Adresse, Rang}
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                boolean virtual = looksVirtual(ni);
                for (InetAddress address : Collections.list(ni.getInetAddresses())) {
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()
                        && !address.isLinkLocalAddress()) {
                        found.add(new String[]{address.getHostAddress(), String.valueOf(rank(address, virtual))});
                    }
                }
            }
        }
        catch (SocketException e) {
            // keine Adressen ermittelbar
        }
        found.sort((x, y) -> Integer.compare(Integer.parseInt(x[1]), Integer.parseInt(y[1])));
        List<String> result = new ArrayList<>();
        for (String[] f : found) {
            if (!result.contains(f[0])) result.add(f[0]);
        }
        cached = List.copyOf(result);
        cachedAt = now;
        return cached;
    }

    private static boolean looksVirtual(NetworkInterface ni) {
        String name = (ni.getName() + " " + ni.getDisplayName()).toLowerCase();
        for (String hint : VIRTUAL_HINTS) {
            if (name.contains(hint)) return true;
        }
        return false;
    }

    private static int rank(InetAddress address, boolean virtual) {
        byte[] b = address.getAddress();
        int first = b[0] & 0xff;
        int second = b[1] & 0xff;
        int r;
        if (first == 192 && second == 168) r = 0;
        else if (first == 10) r = 1;
        else if (first == 172 && second >= 16 && second <= 31) r = 2;
        else r = 3;
        return virtual ? r + 10 : r;
    }

    /**
     * @return Ports, die für ein Solospiel ausprobiert werden
     */
    public static List<Integer> candidatePorts() {
        List<Integer> ports = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < 12; i++) ports.add(42000 + random.nextInt(9000));
        return ports;
    }

    /**
     * @param text Eingabe
     * @return Port oder −1 bei ungültiger Eingabe
     */
    public static int parsePort(String text) {
        try {
            int port = Integer.parseInt(text.trim());
            return port > 0 && port < 65536 ? port : -1;
        }
        catch (NumberFormatException e) {
            return -1;
        }
    }
}
