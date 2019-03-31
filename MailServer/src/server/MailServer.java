package server;

import java.net.SocketException;

public class MailServer {
    /*
    TODO:
    Sample directory structure:
    db/username/001.mail
    When the mail is "read", move it to a read folder as to keep count accurate, i.e.
    db/username/read/001.mail
    all unread mail will just be directly under user folder for simplicity
    */

    // Allowable Port ranges; the range [1, 1023] is not guaranteed
    private static final int MIN_PORT = 1;
    private static final int  MAX_PORT = 65535;
    public static void main(String[] args) throws SocketException {

//        //TODO: needs two args TCP Port and UDP port
//        UDPAgent udpAgent = new UDPAgent(port);
//        new Thread(udpAgent).start();


        if (args.length < 2) {
            System.out.println("Invalid argument length; Expected 2 but was given " + args.length + "; see below for valid arguments\n" +
                    "Expected input: \"java -jar MailServer.jar <tcp-listen-port> (<udp-listen-port>)\"\n" +
                    "Where tcp-listen-port is a valid port number for clients sending mail and " +
                    "udp-listen-port is a valid port number for a client receiving mail\n" +
                    "The tcp and upd ports must be an integer within the range [1, 65535] and cannot equal each other");
            System.exit(1);
        }

        int forTCP = Integer.parseInt(args[0]);
        int forUDP = Integer.parseInt(args[1]);
        if (!isPortValid(forTCP)) {
            System.out.println("Use a valid port number if you want to the application to run");
            System.exit(1);
        }


        if (!isPortValid(forUDP)) {
            System.out.println("Use a valid port number if you want to the application to run");
            System.exit(1);
        }

        if (portsAreEqual(forTCP, forUDP)) {
            System.err.println("Invalid arguments; tcp-listen-port and udp-listen-port CANNOT use the same port number!\n" +
                    "Use unique TCP and UDP port numbers within the range [1, 65535] if you want the application to run");
            System.exit(1);
        }

        // Start the Database service.
//        MailDBMS.init();

        // Start the TCP service.
//        new Thread(new TcpService(TCP_PORT)).start();

        // Start the UDP service.
//        new Thread(new UdpService(UDP_PORT)).start();

    }

    private static boolean isPortValid(int port) {
        if (port >= MIN_PORT && port <= MAX_PORT) {
            return true;
        }
        System.out.println("Invalid port; valid port numbers range from 1 to 65535");
        return false;
    }

    private static boolean portsAreEqual(int port, int portToCompare) {
        return port == portToCompare;
    }
}
