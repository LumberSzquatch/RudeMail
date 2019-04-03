package server;

import java.io.IOException;
import java.util.Date;

public class TcpAgent implements Runnable {

    private final TcpClient tcpClient;

    private final static String SMTP_HELO = "HELO";
    private final static String SMTP_MAIL_FROM = "MAIL FROM:";
    private final static String SMTP_MAIL_TO = "RCPT TO:";
    private final static String SMTP_DATA = "DATA:";
    private final static String SMTP_OK = "250 OK";
    private final static String SMTP_START_INPUT = "354 Start mail input; end with <CRLF>.<CRLF>";
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
            communicateServerReady();
            Email email = new Email();
            String request;
            while (tcpClient.getSocket().isConnected()) {

                request = getClientRequest();
                if (request.equalsIgnoreCase(SMTP_HELO)) {
                    tcpClient.writeObject("250 Hello");

                    request = (String) tcpClient.readObject();
                    if (requestMailFromInSequence(request)) {
                        email.setFromField(parseAddressFromRequest(request));
                        communicateOK();

                        request = getClientRequest();
                        if (requestMailToInSequence(request)) {
                            email.setRecipientField(parseRecipientFromRequest(request));
                            communicateOK();

                            request = getClientRequest();
                            if (requestDataInSequence(request)) {
                                tcpClient.writeObject(SMTP_START_INPUT);
                                request = parseDataFromRequest();
                                email.setEmailData(
                                        request.substring(0, request.length() - 3)
                                );
                                email.setTimeStamp(new Date());
                                EmailDBMS.insert(email);
                                communicateOK();
                            }
                        }
                    }
                }

                if (request.equalsIgnoreCase("QUIT")) {
                    tcpClient.writeObject(SMTP_GOODBYE);
                    break;
                } else if (isRequestInvalid(request)) {
                    tcpClient.writeObject(SMTP_INVALID + "Please start from 'HELO'");
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

    private void communicateServerReady() {
        tcpClient.writeObject(SMTP_SERVER_READY);
    }

    private void communicateOK() {
        tcpClient.writeObject("250 OK");
    }

    private String getClientRequest() throws IOException {
        return (String) tcpClient.readObject();
    }

    private String parseAddressFromRequest(String request) {
        return request.substring(10).trim();
    }

    private String parseRecipientFromRequest(String request) {
        return request.substring(8).trim();
    }

    private String parseDataFromRequest() throws IOException {
        String emailBody = "";
        do {
            emailBody += (String) tcpClient.readObject() + "\n";
        } while (!emailBody.endsWith("\n.\n"));
        return emailBody;
    }

    private boolean requestMailFromInSequence(String request) {
        return request.toUpperCase().startsWith(SMTP_MAIL_FROM);
    }

    private boolean requestMailToInSequence(String request) {
        return request.toUpperCase().startsWith(SMTP_MAIL_TO);
    }

    private boolean requestDataInSequence(String request) {
        return request.equalsIgnoreCase(SMTP_DATA);
    }

    private boolean isRequestInvalid(String request) {
        return !(request.equalsIgnoreCase(SMTP_HELO)
                || request.toUpperCase().startsWith(SMTP_MAIL_FROM)
                || request.toUpperCase().startsWith(SMTP_MAIL_TO)
                || request.equalsIgnoreCase(SMTP_DATA)
                || request.endsWith("\n.\n"));
    }
}
