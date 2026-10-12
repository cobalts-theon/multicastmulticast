import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

// Bai tap net send: UDP cho mot may, multicast cho nhom va toan mang LAN.
public class Net {
    static final int PORT = 50000;
    static final String ALL = "239.10.0.1";

    public static void main(String[] args) {
        try {
            if (args.length >= 1 && args[0].equalsIgnoreCase("listen")) {
                listen(Arrays.copyOfRange(args, 1, args.length));
            } else if (args.length >= 3 && args[0].equalsIgnoreCase("send")) {
                String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                send(args[1], message);
            } else {
                System.out.println("Cach dung:");
                System.out.println("  net listen [group1 group2 ...]");
                System.out.println("  net send {IP | group | *} message");
                System.out.println("Vi du: net send lop1 Xin chao cac ban");
            }
        } catch (Exception e) {
            System.err.println("Loi: " + e.getMessage());
            System.exit(1);
        }
    }

    // Co the chon card mang bang bien moi truong NET_INTERFACE (vi du eth0).
    static NetworkInterface network() throws Exception {
        String name = System.getenv("NET_INTERFACE");
        if (name != null && !name.isEmpty()) {
            NetworkInterface card = NetworkInterface.getByName(name);
            if (card == null || !card.isUp() || !card.supportsMulticast()) {
                throw new Exception("Card mang khong hop le: " + name);
            }
            return card;
        }
        Enumeration<NetworkInterface> cards = NetworkInterface.getNetworkInterfaces();
        while (cards.hasMoreElements()) {
            NetworkInterface card = cards.nextElement();
            if (card.isUp() && !card.isLoopback() && card.supportsMulticast()) {
                Enumeration<InetAddress> ips = card.getInetAddresses();
                while (ips.hasMoreElements()) {
                    if (ips.nextElement() instanceof Inet4Address) return card;
                }
            }
        }
        throw new Exception("Khong tim thay card mang IPv4 ho tro multicast.");
    }

    // Cung ten nhom se tinh ra cung dia chi multicast, khong can may chu.
    static String groupAddress(String group) {
        int hash = group.hashCode();
        return "239.11." + ((hash >>> 8) & 255) + "." + (hash & 255);
    }

    static boolean isIp(String value) {
        return value.matches("[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+");
    }

    static void checkGroup(String group) throws Exception {
        if (!group.matches("[\\p{L}\\p{N}_-]+") || isIp(group)) {
            throw new Exception("Ten nhom chi gom chu, so, dau _ va -.");
        }
    }

    static void send(String target, String message) throws Exception {
        boolean multicast = !isIp(target);
        String address;
        String label;
        if (target.equals("*")) {
            address = ALL;
            label = "*";
        } else if (!multicast) {
            for (String part : target.split("\\.")) {
                if (Integer.parseInt(part) > 255) throw new Exception("IPv4 khong hop le.");
            }
            address = target;
            label = "IP";
        } else {
            checkGroup(target);
            address = groupAddress(target);
            label = target;
        }
        // Goi tin gom nhan dich + xuong dong + noi dung UTF-8.
        byte[] data = (label + "\n" + message).getBytes(StandardCharsets.UTF_8);
        if (data.length > 60000) throw new Exception("Tin nhan qua dai (toi da 60000 byte ca nhan dich).");
        try (MulticastSocket socket = new MulticastSocket()) {
            if (multicast) {
                socket.setNetworkInterface(network());
                socket.setTimeToLive(1); // Chi gui trong mang cuc bo.
            }
            socket.send(new DatagramPacket(data, data.length, InetAddress.getByName(address), PORT));
        }
        System.out.println("Da gui den " + target + ": " + message);
    }

    static void listen(String[] groups) throws Exception {
        Set<String> joined = new HashSet<>();
        for (String group : groups) {
            checkGroup(group);
            joined.add(group);
        }
        NetworkInterface card = network();
        try (MulticastSocket socket = new MulticastSocket(null)) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(PORT));
            socket.setNetworkInterface(card);
            socket.joinGroup(new InetSocketAddress(ALL, PORT), card);
            Set<String> addresses = new HashSet<>();
            for (String group : joined) {
                String address = groupAddress(group);
                if (addresses.add(address)) {
                    socket.joinGroup(new InetSocketAddress(address, PORT), card);
                }
            }
            System.out.println("Dang nhan tren " + card.getName() + ", UDP " + PORT);
            System.out.println("Nhom: " + joined + ". Nhan ca tin gui den IP va *. Ctrl+C de dung.");
            byte[] buffer = new byte[65507];
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String text = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                int separator = text.indexOf('\n');
                if (separator < 0) continue;
                String label = text.substring(0, separator);
                // Loc ten nhom vi hai ten nhom co the trung dia chi bam.
                if (label.equals("IP") || label.equals("*") || joined.contains(label)) {
                    System.out.println("[Tu " + packet.getAddress().getHostAddress() + " | " + label + "] "
                            + text.substring(separator + 1));
                }
            }
        }
    }
}
