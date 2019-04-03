package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

public class TcpAgent implements Runnable {

    private final TcpClient tcpClient;

    private final static String SMTP_HELO = "HELO";
    private final static String SMTP_MAIL_FROM = "MAIL FROM:";
    private final static String SMTP_MAIL_TO = "RCPT TO:";
    private final static String SMTP_DATA = "DATA";
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
            IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                    InetAddress.getLocalHost().getHostAddress(),
                    "Request connection/Started thread for client",
                    SMTP_SERVER_READY);
            communicateServerReady();
            Email email = new Email();
            String request;
            while (tcpClient.getSocket().isConnected()) {

                request = getClientRequest();
                if (request.equalsIgnoreCase(SMTP_HELO)) {
                    tcpClient.writeToClient("250 Hello");
                    IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                            InetAddress.getLocalHost().getHostAddress(),
                            SMTP_HELO,
                            "250 Hello");

                    request = (String) tcpClient.readFromClient();
                    if (requestMailFromInSequence(request)) {
                        email.setFromField(parseAddressFromRequest(request));
                        communicateOK();
                        IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                                InetAddress.getLocalHost().getHostAddress(),
                                SMTP_MAIL_FROM,
                                SMTP_OK);

                        request = getClientRequest();
                        if (requestMailToInSequence(request)) {
                            email.setRecipientField(parseRecipientFromRequest(request));
                            communicateOK();
                            IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                                    InetAddress.getLocalHost().getHostAddress(),
                                    SMTP_MAIL_TO,
                                    SMTP_OK);

                            request = getClientRequest();
                            if (requestDataInSequence(request)) {
                                tcpClient.writeToClient(SMTP_START_INPUT);
                                request = parseDataFromRequest();
                                email.setEmailData(
                                        request.substring(0, request.length() - 3)
                                );
                                email.setTimeStamp(new Date());
                                EmailDBMS.insert(email);
                                communicateOK();
                                IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                                        InetAddress.getLocalHost().getHostAddress(),
                                        SMTP_START_INPUT,
                                        SMTP_OK);
                            }
                        }
                    }
                }

                if (request.equalsIgnoreCase("QUIT")) {
                    tcpClient.writeToClient(SMTP_GOODBYE);
                    IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                            InetAddress.getLocalHost().getHostAddress(),
                            "QUIT",
                            SMTP_GOODBYE);
                    break;
                } else if (isRequestInvalid(request)) {
                    tcpClient.writeToClient(SMTP_INVALID + "Please start from 'HELO'");
                    IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                            InetAddress.getLocalHost().getHostAddress(),
                            request,
                            SMTP_INVALID);
                }
            }
        } catch (IOException ex) {
            try {
                IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                        InetAddress.getLocalHost().getHostAddress(),
                        "UNKNOWN",
                        "UNKNOWN");
            } catch (UnknownHostException e) {
                System.err.println("Unexpected error occurred. Terminating application");
                System.exit(1);
            }
        } finally {
            System.out.println("Connection to client: " + tcpClient.getSocket().getInetAddress().getHostAddress() + " has been lost\n" +
                    "Disconnecting and moving on");
            tcpClient.closeStreams();
        }
    }

    private void communicateServerReady() {
        tcpClient.writeToClient(SMTP_SERVER_READY + " (Prompted user input not implemented; Must use protocols to continue)");
    }

    private void communicateOK() {
        tcpClient.writeToClient("250 OK");
    }

    private String getClientRequest() throws IOException {
        return (String) tcpClient.readFromClient();
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
            emailBody += (String) tcpClient.readFromClient() + "\n";
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
