package client;

import java.io.IOException;

public class TcpReceiver implements Runnable {

    private static final String SMTP_HELO = "250 Hello";
    private static final String SMTP_OK = "250 OK";

    private final TcpClient multithreadedClient;

    public TcpReceiver(TcpClient multithreadedClient) {
        this.multithreadedClient = multithreadedClient;
    }

    @Override
    public void run() {
        try {
            while (multithreadedClient.isConnected()) {
                System.out.println((String) multithreadedClient.readObject());
                String serverResponse = (String) multithreadedClient.readObject();
                if (serverResponse.equals(SMTP_HELO)) {
                    System.out.println("Authenticate user");
                    String user = "cheese@cs447.edu";
                    System.out.println("Greetings " + user + ", please indicate who the email is from...");
                }
                if ((!multithreadedClient.isMailFromSet() && !multithreadedClient.isMailToSet() && !multithreadedClient.isDataSet()) && serverResponse.equals(SMTP_OK)) {
                    multithreadedClient.setMailFrom(true);
                    System.out.println("Please indicate who the email should be sent to...");
                }
                if ((!multithreadedClient.isMailToSet() && multithreadedClient.isMailFromSet()) && serverResponse.equals(SMTP_OK)) {
                    multithreadedClient.setMailTo(true);
                    System.out.println("Please provide the email message to be sent...");
                }
                if ((!multithreadedClient.isDataSet() && multithreadedClient.isMailToSet()) && serverResponse.equals(SMTP_OK)) {
                    multithreadedClient.setData(true);
                    System.out.println("Email sent!");
                }
                if (serverResponse.equals(SMTP_HELO) && multithreadedClient.isDataSet()) {
                    System.out.println("Authenticate user");
                    String user = "cheese@cs447.edu";
                    System.out.println("Greetings " + user + ", please indicate who the email is from...");
                }
            }
        } catch (IOException ex) {
            System.out.println("Disconnected from host.");
            System.exit(0);
        }
    }
}