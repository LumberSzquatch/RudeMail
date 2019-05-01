package server;

import javax.net.ssl.SSLServerSocketFactory;
import java.io.IOException;
import java.net.ServerSocket;

public class TcpClientManager implements Runnable {

    private final int TCP_PORT;
    private final boolean SERVER_ACTIVE_LISTEN = true;
    private final boolean CHANNEL_SECURE;

    public TcpClientManager(final int TCP_PORT) {
        this.TCP_PORT = TCP_PORT;
        this.CHANNEL_SECURE = false;
    }

    public TcpClientManager(final int TCP_PORT, final boolean useSecureChannel) {
        this.TCP_PORT = TCP_PORT;
        this.CHANNEL_SECURE = useSecureChannel;
    }

    @Override
    public void run() {
        try {
            // client socket that will either be secured or unsecure
            ServerSocket serverSocket;
            if (CHANNEL_SECURE) {
                SSLServerSocketFactory secureChannelFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
                serverSocket = secureChannelFactory.createServerSocket(TCP_PORT);
                System.out.println("TCP agent listening for clients through secure channel on port " + serverSocket.getLocalPort());
            } else {
                serverSocket = new ServerSocket(TCP_PORT);
                System.out.println("TCP agent listening for incoming clients on port " + serverSocket.getLocalPort() + "; Connection is unsecured!");
            }

            while (SERVER_ACTIVE_LISTEN) {
                TcpClient tcpClient = new TcpClient(serverSocket.accept());
                TcpAgent tcpAgent = new TcpAgent(tcpClient, CHANNEL_SECURE);
                Thread clientThread = new Thread(tcpAgent);
                clientThread.start();
            }
        } catch (IOException ex) {
            System.err.println("Failed to setup server socket.");
            System.exit(1);
        }
    }
}

