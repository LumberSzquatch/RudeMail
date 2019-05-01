package client;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;

public class TcpReceiver implements Runnable {

    private TcpClient multithreadedClient;
    private Socket securedSocket;
    private InputStream inputStream;
    private String serverHostname;
    private int serverPort;

    public TcpReceiver(Socket securedSocket) throws IOException {
        this.securedSocket = securedSocket;
        this.inputStream = securedSocket.getInputStream();
    }

    public TcpReceiver(TcpClient multithreadedClient, String serverHostname, int serverPort) {
        this.multithreadedClient = multithreadedClient;
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        try {
            if (this.securedSocket != null) {
                while (securedSocket.isConnected()) {
                    String response =  Integer.toString(secureReadFromServer());
                    System.out.println(response);
                    if (response.startsWith("235")) {
                        TcpAgent.shouldEncodeData = false;
                    }
                    if (response.startsWith("330")) {
                        System.out.println("Registration process requires a connection refresh\n" +
                                "Refreshing connection to server...");
                        waitToRefreshConnection();
                        System.out.println("Connection Refreshed!\n" +
                                "To change your password; manually change it via .user_pass file " +
                                "(if not changed, subsequent logins after the registration process will require you to add 447 to what your temporary password was)");
                    }
                }
            } else {
                while (multithreadedClient.isConnected()) {
                    String response = (String) multithreadedClient.readFromServer();
                    System.out.println(response);
                    if (response.startsWith("235")) {
                        multithreadedClient.setEncodeData(false);
                    }
                    if (response.startsWith("330")) {
                        System.out.println("Registration process requires a connection refresh\n" +
                                "Refreshing connection to server...");
                        waitToRefreshConnection();
                        System.out.println("Connection Refreshed!\n" +
                                "To change your password; manually change it via .user_pass file " +
                                "(if not changed, subsequent logins after the registration process will require you to add 447 to what your temporary password was)");
                    }
                }
            }
        } catch (IOException ioe) {
            System.out.println("Disconnected from host");
            System.exit(0);
        } catch (InterruptedException e) {
            System.out.println("An unexpected error has occurred in the registration process");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private int secureReadFromServer() throws IOException {
        return 0;//inputStream.read();
    }

    private void waitToRefreshConnection() throws InterruptedException {
        boolean sleep = true;
        System.out.println("Connection will refresh after 5 seconds...");
        while (sleep) {
            Thread.sleep(5000);
            sleep = false;
        }
    }
}