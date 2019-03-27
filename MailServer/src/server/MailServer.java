package server;

import java.net.SocketException;

public class MailServer {

    public static void main(String[] args) throws SocketException {
        int port = -1;
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.out.println("Given Port must be valid");
        }
        UDPAgent udpAgent = new UDPAgent(port);
        new Thread(udpAgent).start();
    }
}
