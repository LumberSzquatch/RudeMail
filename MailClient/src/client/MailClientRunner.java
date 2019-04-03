package client;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;

public class MailClientRunner {

    // Allowable Port ranges; the range [1, 1023] is not guaranteed
    private static final int MIN_PORT = 1;
    private static final int  MAX_PORT = 65535;
    private static final String QUIT = "QUIT";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Invalid argument length; Expected 2 but was given " + args.length + "; see below for valid arguments\n" +
                    "Expected input: \"java -jar MailClientRunner.jar <server-hostname> <server-port>\"\n" +
                    "Where server-hostname is a valid hostname of a server, and server-port is an integer within the range [1, 65535]");
            System.exit(1);
        }

        String serverHostname = args[0];
        int serverPort = parsePortNumber(args[1]);

        if (!isPortValid(serverPort)) {
            System.out.println("Use a valid port number if you want to the application to run");
            System.exit(1);
        }

        System.out.println("Application running (enter 'QUIT' at any time to exit)...");

        Scanner scanner = new Scanner(System.in);
        do {
            System.out.print("Will you be sending or receiving today? ('s' / 'r'): ");
            String response = scanner.next();
            if (responseIsSend(response)) {
                startTCPSenderAgent(serverHostname, serverPort);
                System.exit(0);
            }

            if (responseIsReceive(response)) {
                startUDPReceiverAgent(serverHostname, serverPort);
                System.exit(0);
            }

            if (response.equalsIgnoreCase(QUIT)) {
                System.out.println("Goodbye");
                System.exit(0);
            }
            System.out.println("Answer could not be understood\n");
        } while (true);
    }

    private static void startUDPReceiverAgent(String host, int port) {
        try {
            UdpAgent udpAgent = new UdpAgent(host, port);
            udpAgent.run();
        } catch (UnknownHostException | SocketException e) {
            connectionNotEstablishedOutput(host, port);
            System.exit(1);
        }
    }

    private static void startTCPSenderAgent(String host, int port) {
        TcpAgent tcpAgent = new TcpAgent(host, port);
        tcpAgent.run();
    }

    private static boolean isPortValid(int port) {
        if (port >= MIN_PORT && port <= MAX_PORT) {
            return true;
        }
        System.err.println("Invalid port; valid port numbers range from 1 to 65535");
        return false;
    }

    private static int parsePortNumber(String portString) {
        int port;
        try {
            port = Integer.parseInt(portString);
        } catch (Exception e) {
            port = -1;
        }
        return port;
    }

    private static boolean responseIsSend(String response){
        return response.equalsIgnoreCase("sending")
                || response.equalsIgnoreCase("send")
                || response.equalsIgnoreCase("s");
    }

    private static boolean responseIsReceive(String response){
        return response.equalsIgnoreCase("receiving")
                || response.equalsIgnoreCase("receive")
                || response.equalsIgnoreCase("r");
    }

    private static void connectionNotEstablishedOutput(String host, int port) {
        System.err.println("\nA connection to the host " + host + " on port " + port + " could not be established");
        System.err.println("What can you do about it?\n" +
                "     + Ensure the server application is running\n" +
                "     + Ensure the hostname and port number you are connecting to is correct and try again\n" +
                "     + Check that the port you provided is the port the intended service is running on, i.e. port for receiving vs. port for sending\n" +
                "     + ¯\\_(-.-)_/¯");
    }
}
