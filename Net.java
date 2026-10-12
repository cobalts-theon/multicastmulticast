import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

// Bai tap net send: UDP cho mot may, multicast cho nhom va toan mang LAN.
public class Net {
    static final int PORT = 50000; // Nhan tin gui rieng qua IP.
    static final int GROUP_PORT = 50001; // Nhan multicast, tach khoi unicast.
    static final String ALL = "239.10.0.1";

    public static void main(String[] args) {
        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("listen")) {
                listen(args.length == 0 ? new String[0] : Arrays.copyOfRange(args, 1, args.length));
            } else if (args.length >= 3 && args[0].equalsIgnoreCase("send")) {
                send(args[1], String.join(" ", Arrays.copyOfRange(args, 2, args.length)));
            } else {
                System.out.println("java -cp build Net listen [nhom ...]");
                System.out.println("java -cp build Net send {IP | group | *} message");
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
        if (group.equals("IP") || !group.matches("[\\p{L}\\p{N}_-]+") || isIp(group)) {
            throw new Exception("Ten nhom chi gom chu, so, dau _ va -.");
        }
    }

    static void send(String target, String message) throws Exception {
        boolean multicast = !isIp(target);
        String address = target;
        String label = "IP";
        if (multicast) {
            if (!target.equals("*")) checkGroup(target);
            address = target.equals("*") ? ALL : groupAddress(target);
            label = target;
        } else {
            for (String part : target.split("\\.")) {
                if (Integer.parseInt(part) > 255) throw new Exception("IPv4 khong hop le.");
            }
        }
        // Goi tin gom nhan dich + xuong dong + noi dung UTF-8.
        byte[] data = (label + "\n" + message).getBytes(StandardCharsets.UTF_8);
        if (data.length > 60000) throw new Exception("Tin nhan qua dai (toi da 60000 byte ca nhan dich).");
        try (MulticastSocket socket = new MulticastSocket()) {
            if (multicast) {
                socket.setNetworkInterface(network());
                socket.setTimeToLive(1); // Chi gui trong mang cuc bo.
            }
            socket.send(new DatagramPacket(data, data.length, InetAddress.getByName(address), multicast ? GROUP_PORT : PORT));
        }
        System.out.println("Da gui den " + target + ": " + message);
    }

    // Scanner doc lenh lien tuc; luong rieng nhan tin trong luc cho nguoi dung go.
    static void listen(String[] groups) throws Exception {
        // Hai luong cung dung danh sach nhom: nhap lenh them, nhan tin doc.
        Set<String> joined = ConcurrentHashMap.newKeySet();
        NetworkInterface card = network();
        Queue<String> inbox = new ConcurrentLinkedQueue<>();
        try (DatagramSocket direct = new DatagramSocket(new InetSocketAddress("0.0.0.0", PORT));
             MulticastSocket socket = new MulticastSocket(null);
             Scanner scanner = new Scanner(System.in)) {
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress("0.0.0.0", GROUP_PORT));
            socket.setNetworkInterface(card);
            socket.joinGroup(new InetSocketAddress(ALL, GROUP_PORT), card);
            for (String group : groups) changeGroup(socket, card, joined, group, true);
            System.out.println("Dang nghe tren " + card.getName() + ", nhom: " + joined);
            // In IP that de nguoi khac biet dia chi gui tin rieng.
            for (InetAddress ip : Collections.list(card.getInetAddresses())) {
                if (ip instanceof Inet4Address) System.out.println("IP cua ban: " + ip.getHostAddress());
            }
            Thread receiver = startReceiver(socket, joined, inbox);
            Thread directReceiver = startReceiver(direct, joined, inbox);
            System.out.println("Nhap: net send {IP | group | *} message");
            System.out.println("Nhom: net listen group1 group2 | net outgroup group1 group2");
            System.out.println("Vi du: net send group1 Xin chao. Go exit de thoat.");
            System.out.println("Nhan Enter de xem tin den. Tin khong chen vao dong dang go.");
            while (true) {
                showMessages(inbox);
                System.out.print("> ");
                if (!scanner.hasNextLine()) break;
                String line = scanner.nextLine().trim();
                showMessages(inbox);
                if (line.equalsIgnoreCase("exit")) break;
                if (line.isEmpty()) continue;
                try {
                    command(line, socket, card, joined);
                } catch (Exception e) {
                    // Sai lenh hoac gui loi thi bao loi va van cho nhap tiep.
                    System.err.println("Loi: " + e.getMessage());
                }
            }
            // Dong socket de danh thuc luong dang cho receive().
            socket.close();
            direct.close();
            receiver.join();
            directReceiver.join();
            showMessages(inbox);
        }
        System.out.println("Da thoat.");
    }

    // Tach xu ly lenh khoi vong Scanner de de doc.
    static void command(String line, MulticastSocket socket, NetworkInterface card,
                        Set<String> joined) throws Exception {
        String[] parts = line.split("\\s+", 4);
        if (parts.length < 3 || !parts[0].equalsIgnoreCase("net")) {
            throw new Exception("Dung: net listen nhom | net outgroup nhom | net send dich message");
        }
        switch (parts[1].toLowerCase(Locale.ROOT)) {
            case "send":
                if (parts.length != 4) throw new Exception("Thieu noi dung tin nhan.");
                send(parts[2], parts[3]);
                break;
            case "listen":
            case "outgroup":
                String[] groups = line.split("\\s+");
                for (int i = 2; i < groups.length; i++) checkGroup(groups[i]);
                for (int i = 2; i < groups.length; i++) {
                    changeGroup(socket, card, joined, groups[i], parts[1].equalsIgnoreCase("listen"));
                }
                System.out.println("Nhom dang nghe: " + joined);
                break;
            default:
                throw new Exception("Chi dung listen, outgroup hoac send. Go exit de thoat.");
        }
    }

    // Tham gia/roi nhom. Hai ten nhom trung dia chi van duoc xu ly dung.
    static void changeGroup(MulticastSocket socket, NetworkInterface card,
                            Set<String> joined, String group, boolean join) throws Exception {
        checkGroup(group);
        if (joined.contains(group) == join) return; // Khong tham gia/roi lai cung nhom.
        String address = groupAddress(group);
        boolean shared = false;
        for (String other : joined) {
            if (!other.equals(group) && groupAddress(other).equals(address)) shared = true;
        }
        if (!shared) {
            InetSocketAddress destination = new InetSocketAddress(address, GROUP_PORT);
            if (join) socket.joinGroup(destination, card);
            else socket.leaveGroup(destination, card);
        }
        if (join) joined.add(group);
        else joined.remove(group);
    }

    // Luong nhan chi luu tin; chi luong Scanner in de khong chen dong dang go.
    static Thread startReceiver(DatagramSocket socket, Set<String> joined, Queue<String> inbox) {
        Thread thread = new Thread(() -> {
            try {
                receive(socket, joined, inbox);
            } catch (Exception e) {
                if (!socket.isClosed()) inbox.add("Loi nhan tin: " + e.getMessage());
            }
        });
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    static void showMessages(Queue<String> inbox) {
        String message;
        while ((message = inbox.poll()) != null) System.out.println(message);
    }

    static void receive(DatagramSocket socket, Set<String> joined, Queue<String> inbox) throws Exception {
        byte[] buffer = new byte[65507];
        while (!socket.isClosed()) {
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            socket.receive(packet);
            String text = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
            int separator = text.indexOf('\n');
            if (separator < 0) continue;
            String label = text.substring(0, separator);
            // Loc ten nhom vi hai ten nhom co the trung dia chi bam.
            if (label.equals("IP") || label.equals("*") || joined.contains(label)) {
                inbox.add("[Tu " + packet.getAddress().getHostAddress() + " | " + label + "] "
                        + text.substring(separator + 1));
            }
        }
    }
}
