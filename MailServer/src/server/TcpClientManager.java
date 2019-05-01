package server;

import javax.net.ssl.SSLServerSocket;
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
            SSLServerSocket secureSocket;
            ServerSocket serverSocket;
            if (CHANNEL_SECURE) {
                SSLServerSocketFactory secureChannelFactory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
                secureSocket = (SSLServerSocket) secureChannelFactory.createServerSocket(TCP_PORT);
                String enabledSuites[] = { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256" };
                String protocols[] = {"TLSv1.2"};
                secureSocket.setEnabledCipherSuites(enabledSuites);
                secureSocket.setEnabledProtocols(protocols);
                System.out.println("TCP agent listening for clients through secure channel on port " + secureSocket.getLocalPort());
                while (SERVER_ACTIVE_LISTEN) {
                    TcpClient tcpClient = new TcpClient(secureSocket.accept(), CHANNEL_SECURE);
                    TcpAgent tcpAgent = new TcpAgent(tcpClient, CHANNEL_SECURE);
                    Thread clientThread = new Thread(tcpAgent);
                    clientThread.start();
                }
            } else {
                serverSocket = new ServerSocket(TCP_PORT);
                System.out.println("TCP agent listening for incoming clients on port " + serverSocket.getLocalPort() + "; Connection is unsecured!");
                while (SERVER_ACTIVE_LISTEN) {
                    TcpClient tcpClient = new TcpClient(serverSocket.accept(), CHANNEL_SECURE);
                    TcpAgent tcpAgent = new TcpAgent(tcpClient, CHANNEL_SECURE);
                    Thread clientThread = new Thread(tcpAgent);
                    clientThread.start();
                }
            }
        } catch (IOException ex) {
            System.err.println("Failed to setup server socket.");
            System.exit(1);
        }
    }
}

