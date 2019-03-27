package client;

import java.net.SocketException;
import java.net.UnknownHostException;

public class MailClient {

    public static void main(String[] args) throws SocketException, UnknownHostException {
        if (args.length < 2) {
            System.out.println("Usage: java -jar mailClient.jar [server-name] [server-port]");
        }

        String serverName = args[0];
        int port = -1;
        try {
            port = Integer.parseInt(args[1]);
        } catch (Exception e) {
            System.out.println("Given Port must be valid");
        }
        UDPAgent udpAgent = new UDPAgent(serverName, port);
        new Thread(udpAgent).start();
    }
}
