package client;

import java.io.IOException;

public class TcpReceiver implements Runnable {

    private final TcpClient multithreadedClient;

    public TcpReceiver(TcpClient multithreadedClient) {
        this.multithreadedClient = multithreadedClient;
    }

    @Override
    public void run() {
        try {
            while (multithreadedClient.isConnected()) {
                String response = (String) multithreadedClient.readFromServer();
                if (response.startsWith("235")) {
                    multithreadedClient.setEncodeData(false);
                }
                System.out.println(response);
            }

        } catch (IOException ex) {
            System.out.println("Disconnected from host.");
            System.exit(0);
        }
    }
}