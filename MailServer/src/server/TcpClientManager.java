package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

public class TcpClientManager implements Runnable {

    private final int TCP_PORT;
    private final boolean serverActiveListenForClients = true;

    public TcpClientManager(final int TCP_PORT) {
        this.TCP_PORT = TCP_PORT;
    }

    @Override
    public void run() {
        try {
            // init new client socket
            ServerSocket serverSocket = new ServerSocket(TCP_PORT);
            System.out.println("TCP agent listening for incoming clients on port " + serverSocket.getLocalPort());

            while (serverActiveListenForClients) {
                TcpClient tcpClient = new TcpClient(serverSocket.accept());
                TcpAgent tcpAgent = new TcpAgent(tcpClient);
                Thread clientThread = new Thread(tcpAgent);
                tcpAgent.setClientThread(clientThread);
                clientThread.start();
            }
        } catch (IOException ex) {
            System.err.println("Failed to setup server socket.");
            System.exit(1);
        }
    }
}

