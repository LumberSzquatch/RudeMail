package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Date;

public class TcpClientManager implements Runnable {

    private final int TCP_PORT;

    public TcpClientManager(final int TCP_PORT) {
        this.TCP_PORT = TCP_PORT;
    }

    @Override
    public void run() {
        try {
            // Setup the welcoming socket.
            ServerSocket serverSocket = new ServerSocket(TCP_PORT);

            // Notify the service has been setup successfully.
            System.out.println(
                    "Starting TCP service at: " + new Date() + '\n'
                            + "TCP service connected to port: "
                            + serverSocket.getLocalPort());

            // Listen for new clients; put each new client on its own thread.
            while (true) {
                TcpClient tcpClient = new TcpClient(serverSocket.accept());
                System.out.println("TcpClient detected. Attempting to setup thread.");
                new Thread(new TcpAgent(tcpClient)).start();
            }
        } catch (IOException ex) {
            System.err.println("Failed to setup server socket.");
            System.exit(1);
        }
    }
}

