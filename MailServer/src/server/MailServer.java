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
    private static final int MAX_PORT = 65535;
    private static final String SECURED_FLAG = "S";
    private static final String MD5_FLAG = "md5";

    public static void main(String[] args) throws SocketException {

        if (args.length < 4) {
            System.err.println("Invalid argument length; Expected 4 but was given " + args.length + "; see below for valid arguments\n" +
                    "Expected input: \"java -jar MailServer.jar <tcp-listen-port> <udp-listen-port> <ssl-tls-flag>\"\n" +
                    "\ttcp-listen-port: a valid port number for clients sending mail\n" +
                    "\tudp-listen-port: a valid port number for a client receiving mail\n" +
                    "\t<ssl-tls-flag>: a single character; either 'S' for secure channeled server or 'u' for an unsecured server" +
                    "\t\t-- If answer cannot be understood channel will opt for secure channel by default\n" +
                    "\t<hash-auth-flag>: flag to indicate Base64 or Hash MD5 authentication (B64 will be used if answer not understood)\n" +
                    "\t\tFor Base64: feed b64 to parameter\n" +
                    "\t\tFor Hash MD5: feed md5 to parameter\n" +
                    "TCP and UDP listen ports must be an integer within the range [1, 65535] and cannot equal each other");
            System.exit(1);
        }

        int forTCP = -1;
        int forUDP = -1;
        try {
            forTCP = Integer.parseInt(args[0]);
            forUDP = Integer.parseInt(args[1]);
        } catch (NumberFormatException nfe) {
            System.err.println("TCP and UDP listen ports must be an integer within the range [1, 65535] and cannot equal each other\n" +
                    "Please provide valid port numbers to run the application...");
            System.exit(1);
        }
        checkPortValidity(forTCP, forUDP);

        String secureChannelFlag = args[2];
        boolean isChannelSecure = SECURED_FLAG.equalsIgnoreCase(secureChannelFlag);

        String hashFlag = args[3];
        boolean useHashAuth = MD5_FLAG.equalsIgnoreCase(hashFlag);

        initializeDBMS();
        initializeSIM();
        initializeCM();
        spinUpTCPAgent(forTCP, isChannelSecure, useHashAuth);
        spinUpUDPAgent(forUDP);
    }

    private static void checkPortValidity(int tcpPort, int udpPort) {
        if (isPortInvalid(tcpPort)) {
            System.err.println("Use a valid TCP listen port if you want the application to run");
            System.exit(1);
        }

        if (isPortInvalid(udpPort)) {
            System.err.println("Use a valid UDP listen port if you want the application to run");
            System.exit(1);
        }

        if (portsAreEqual(tcpPort, udpPort)) {
            System.err.println("Invalid arguments; tcp-listen-port and udp-listen-port CANNOT use the same port number!\n" +
                    "Use unique TCP and UDP port numbers within the range [1, 65535] if you want the application to run");
            System.exit(1);
        }
    }

    private static boolean isPortInvalid(int port) {
        if (port < MIN_PORT || port > MAX_PORT) {
            System.err.println("The port " + port + " is invalid; valid port numbers range from 1 to 65535");
            return true;
        }
        return false;
    }

    private static boolean portsAreEqual(int port, int portToCompare) {
        return port == portToCompare;
    }

    private static void spinUpTCPAgent(int port, boolean isChannelSecure, boolean useHashAuth) {
        new Thread(new TcpClientManager(port, isChannelSecure, useHashAuth)).start();
    }

    private static void spinUpUDPAgent(int port) throws SocketException {
        new Thread(new UdpAgent(port)).start();
    }

    private static void initializeDBMS() {
        EmailDBMS.initializeDB();
    }

    private static void initializeSIM() {
        IncidentManager.initializeSIM();
    }

    private static void initializeCM() {
        CredentialsManager.initializeCM();
    }
}
