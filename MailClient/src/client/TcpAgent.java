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
        boolean hasServerConnection = true;
        try {
            TcpClient multithreadedClient = new TcpClient(serverHostname, serverPort);
            // New incoming clients will run on their own thread
            new Thread(new TcpReceiver(multithreadedClient)).start();
            Scanner scanner = new Scanner(System.in);
            while (hasServerConnection) {
                multithreadedClient.sendToServer(scanner.nextLine());
            }

        } catch (IOException ex) {
            System.err.println("Failed to establish connection with server");
            System.exit(1);
        }
    }

    public void mailPrompts(TcpClient multithreadedClient, Scanner scanner) {
        if(!multithreadedClient.saidHelo()) {
            System.out.println("Greeting the server...");
            multithreadedClient.setHelo(true);
            multithreadedClient.sendToServer("helo");
        }

        if (!multithreadedClient.isAuthorized()) {
            System.out.println("Must authenticate...");
            multithreadedClient.setAuth(true);
            multithreadedClient.sendToServer("auth");
        }

        if (!multithreadedClient.isMailFromSet()) {
            System.out.println("Provide the name of the person sending the mail:");
            String mailFrom = scanner.nextLine();
            if (mailFrom.equalsIgnoreCase("quit")) {
                multithreadedClient.sendToServer(mailFrom);
            }
            multithreadedClient.setMailFrom(true);
            multithreadedClient.sendToServer("mail from: " + mailFrom);
        }

        if (!multithreadedClient.isMailToSet()) {
            System.out.println("Provide the name of the recipient:");
            String rcptTo = scanner.nextLine();
            if (rcptTo.equalsIgnoreCase("quit")) {
                multithreadedClient.sendToServer(rcptTo);
            }
            multithreadedClient.setMailTo(true);
            multithreadedClient.sendToServer("rcpt to: " + rcptTo);
        }

        if (!multithreadedClient.isDataSet()) {
            multithreadedClient.sendToServer("data");
            multithreadedClient.setData(true);
            System.out.println("What would you like the content of the mail to be? (end your text content with a newline," +
                    " followed by a period, followed by an additional newline)" +
                    "\nEXAMPLE:\n" +
                    "\tContent\n\n.\n<End of content>");
            multithreadedClient.sendToServer(scanner.nextLine());
        }
        System.out.println("Mail has successfully been sent!\nAuthentication session expiring...");
        System.out.println("Do you wish to re-authenticate and send another mail message? (Y/n)");
        String userResponse = scanner.nextLine();
        if (userResponse.equalsIgnoreCase("n") || userResponse.equalsIgnoreCase("no")) {
            multithreadedClient.sendToServer("quit");
        }
        multithreadedClient.initializeCommandPrompts();
    }
}
