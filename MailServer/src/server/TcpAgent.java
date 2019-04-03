package server;

import java.io.IOException;

public class TcpAgent implements Runnable {

    private final TcpClient tcpClient;

    private final static String SMTP_HELO = "HELO";
    private final static String SMTP_MAIL_TO = "MAIL TO:";
    private final static String SMTP_DATA = "DATA:";
    private final static String SMTP_OK = "250 OK";
    private final static String SMTP_SERVER_READY = "220 Domain Server Ready";
    private final static String SMTP_GOODBYE = "221 Goodbye";
    private final static String SMTP_INVALID = "500 Invalid Command";

    public TcpAgent(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
    }

    @Override
    public void run() {
        try {
            // Thread for tcpClient setup successfully; notify server.
            System.out.println(
                    "Thread started for tcpClient:\n\t"
                            + "IP Address: "
                            + tcpClient.getSocket().getInetAddress().getHostAddress()
                            + " | Name: "
                            + tcpClient.getCustomName());

//                Mail mail = new Mail();

            // Begin SMTP service. Handle incoming data from tcpClient.
            tcpClient.writeObject(SMTP_SERVER_READY);

            String message;
            while (tcpClient.getSocket().isConnected()) {

                message = (String) tcpClient.readObject();
                if (message.equalsIgnoreCase("HELO")) {
                    // parse the sender's FQDN (use InetAddress) or IP.
                    // TODO this is not a requirement, ignore for now..
                    tcpClient.writeObject("250 Hello");

                    message = (String) tcpClient.readObject();
                    if (message.toUpperCase().startsWith("MAIL FROM:")) {
                        // parse the sender's email addr and reply.
//                        mail.setMailFrom(message.substring(10).trim());
                        tcpClient.writeObject("250 OK");

                        message = (String) tcpClient.readObject();
                        if (message.toUpperCase().startsWith("RCPT TO:")) {
                            // parse the recipient's addr and reply.
//                            mail.setRcptTo(message.substring(8).trim());
                            tcpClient.writeObject("250 OK");

                            message = (String) tcpClient.readObject();
                            if (message.equalsIgnoreCase("DATA")) {
                                tcpClient.writeObject("354 Send message content; end with <CRLF>.<CRLF>");

                                message = "";
                                do {
                                    message += (String) tcpClient.readObject() + "\n";
                                } while (!message.endsWith("\n.\n"));

                                // commit the data, stamp the time, and save.
//                                mail.setData(
//                                        message.substring(0, message.length() - 3)
//                                );
//                                mail.setTimeStamp(new Date());
//
//                                MailDBMS.save(mail);

                                tcpClient.writeObject("250 OK");
                            }
                        }
                    }
                }

                if (message.equalsIgnoreCase("QUIT")) {
                    tcpClient.writeObject("221 Goodbye");
                    break;
                } else if (!(message.equalsIgnoreCase("HELO")
                        || message.toUpperCase().startsWith("MAIL FROM:")
                        || message.toUpperCase().startsWith("RCPT TO:")
                        || message.equalsIgnoreCase("DATA")
                        || message.endsWith("\n.\n"))) {
                    tcpClient.writeObject("500 Invalid Command. Restart From HELO.");
                }
            }
        } catch (IOException ex) {
            // Ignore because it's likely that the tcpClient is lost.
        } finally {
            // Losing the tcpClient- perform cleanup.
            System.out.println(
                    "Lost tcpClient:\n\t"
                            + "IP Address: "
                            + tcpClient.getSocket().getInetAddress().getHostAddress()
                            + " | Name: "
                            + tcpClient.getCustomName()
                            + "\nPerforming cleanup..");
            tcpClient.closeStreams();
            System.out.println("Cleanup complete.");
        }
    }

    private boolean isRequestInvalid(String message) {
        return !(message.equalsIgnoreCase("HELO")
                || message.toUpperCase().startsWith("MAIL FROM:")
                || message.toUpperCase().startsWith("RCPT TO:")
                || message.equalsIgnoreCase("DATA")
                || message.endsWith("\n.\n"));
    }
}
