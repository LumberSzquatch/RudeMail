package client;

import java.io.IOException;
import java.util.Scanner;

public class TcpAgent implements Runnable {

    private final static String GREET_SERVER = "HELO";
    private final static String SMTP_MAIL_FROM = "MAIL FROM: ";
    private final static String SMTP_MAIL_TO = "RCPT TO: ";

    private String serverHostname;
    private int serverPort;

    public TcpAgent(String serverHostname, int serverPort) {
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
    }

    @Override
    public void run() {
        try {
            TcpClient multithreadedClient = new TcpClient(serverHostname, serverPort);
            new Thread(new TcpReceiver(multithreadedClient)).start();
            multithreadedClient.writeObject(GREET_SERVER);
            Scanner scanner = new Scanner(System.in);
            while (true) {
                if (!multithreadedClient.isMailFromSet()) {
                    multithreadedClient.writeObject(SMTP_MAIL_FROM + scanner.nextLine());
                }
                if (!multithreadedClient.isMailToSet()) {
                    multithreadedClient.writeObject(SMTP_MAIL_TO + scanner.nextLine());
                }
                if (!multithreadedClient.isDataSet()) {
                    System.out.println("MADE IT");
                    System.exit(0);
                }
                multithreadedClient.writeObject(scanner.nextLine());
            }

        } catch (IOException ex) {
            System.err.println("Failed to connect to host.");
            System.exit(1);
        }
    }
}
