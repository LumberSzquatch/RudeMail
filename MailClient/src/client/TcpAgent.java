package client;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;

public class TcpAgent implements Runnable {

    private final static String GREET_SERVER = "HELO";
    private final static String SMTP_MAIL_FROM = "MAIL FROM: ";
    private final static String SMTP_MAIL_TO = "RCPT TO: ";
    private final static String AUTH_REQUEST = "AUTH";

    private String serverHostname;
    private int serverPort;
    private boolean usesSecureChannel;

    public static boolean saidHelo = false;
    public static boolean shouldEncodeData = false;

    public TcpAgent(String serverHostname, int serverPort, boolean usesSecureChannel) {
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
        this.usesSecureChannel = usesSecureChannel;
    }

    @Override
    public void run() {
        boolean hasServerConnection = true;
        try {
            TcpClient multithreadedClient;
            SSLSocket secureSocket = null;
            if (usesSecureChannel) {
                // Set up new SSLSocketFactory and get a secured Socket
                //     use the secured socket instead of TcpClient
                SSLSocketFactory secureChannelFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
                secureSocket = (SSLSocket) secureChannelFactory.createSocket(serverHostname, serverPort);
                String enabledSuites[] = { "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256" };
                String protocols[] = {"TLSv1.2"};
                secureSocket.setEnabledCipherSuites(enabledSuites);
                secureSocket.setEnabledProtocols(protocols);
                secureSocket.startHandshake();
                new Thread(new TcpReceiver(secureSocket)).start();
                Scanner scanner = new Scanner(System.in);

                while (hasServerConnection) {
                    String request = scanner.nextLine();
                    if (request.equalsIgnoreCase(GREET_SERVER)) {
                        TcpAgent.saidHelo = true;
                    }
                    if (TcpAgent.shouldEncodeData) {
//                        secureSendToServer(streamToServer, B64Util.encode(request));
                    } else {
//                        secureSendToServer(streamToServer, request);
                        if (request.equalsIgnoreCase(AUTH_REQUEST) && TcpAgent.saidHelo) {
                            TcpAgent.shouldEncodeData = true;
                            TcpAgent.saidHelo = false;
                        }
                    }
                }
            } else {
                multithreadedClient = new TcpClient(serverHostname, serverPort);
                // New incoming clients will run on their own thread
                new Thread(new TcpReceiver(multithreadedClient, serverHostname, serverPort)).start();
                Scanner scanner = new Scanner(System.in);
                while (hasServerConnection) {
                    String request = scanner.nextLine();
                    if (request.equalsIgnoreCase(GREET_SERVER)) {
                        multithreadedClient.setHelo(true);
                    }
                    if (multithreadedClient.shouldDataBeEncoded()) {
                        multithreadedClient.sendToServer(B64Util.encode(request));
                    } else {
                        multithreadedClient.sendToServer(request);
                        if (request.equalsIgnoreCase(AUTH_REQUEST) && multithreadedClient.saidHelo()) {
                            multithreadedClient.setEncodeData(true);
                            multithreadedClient.setHelo(false);
                        }
                    }
                }
            }

        } catch (IOException ex) {
            System.err.println("Failed to establish connection with server");
            ex.printStackTrace();
            System.exit(1);
        }
    }

    public void secureSendToServer(ObjectOutputStream secureStream, String request) {
//        try {
//            secureStream.writeObject(request);
//        } catch (IOException ex) {
//            System.err.println(
//                    "Failed to write data to output stream");
//        }
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
