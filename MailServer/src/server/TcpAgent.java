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

    private final static String AUTH = "AUTH";
    private final static String INVALID_CREDENTIALS = "535 Invalid Credentials";
    private final static String USERNAME_CHALLENGE = "334 dXNlcm5hbWU6";
    private final static String PASSWORD_CHALLENGE = "334 cGFzc3dvcmQ6";
    private final static String AUTH_SUCCESS = "235 AUTH Success";
    private final static String AUTH_REGISTER = "330 Must Register\nTemporary Password: ";
    private final static String EMAIL_DOMAIN = "@cs447.edu";

    private boolean isNewUser;
    private boolean clientHasConnection;
    private String clientEmail;
    private String encodedEmailString;
    private String encodedPasswordString;
    private String temporaryPassword;

    public TcpAgent(TcpClient tcpClient) {
        this.tcpClient = tcpClient;
        this.isNewUser = false;
    }

    @Override
    public void run() {
        System.out.println("Client " + tcpClient.getSocket().getInetAddress().getHostAddress() + " has established connection");
        try {
            IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                    InetAddress.getLocalHost().getHostAddress(),
                    "Request connection/Started thread for client",
                    SMTP_SERVER_READY);
            communicateServerReady();
            Email email = new Email();
            String request;
            while (tcpClient.getSocket().isConnected()) {
                this.clientHasConnection = true;
                try {
                    // HELO Sequence - Keep Client locked in until HELO or QUIT is requested
                    request = getClientRequest();
                    while (!request.equalsIgnoreCase(SMTP_HELO)) {
                        terminateIfClientQuit(request);
                        if (clientSocketClosed()) {
                            break;
                        }
                        tcpClient.writeToClient(SMTP_INVALID + "; I won't help you until you say HELO");
                        request = getClientRequest();
                        System.out.println(request);
                    }

                    if (clientSocketClosed()) {
                        break;
                    }
                    greetClient();

                    // AUTH Sequence - Keep client locked in until AUTH or QUIT is requested
                    request = getClientRequest();
                    while (!request.equalsIgnoreCase(AUTH)) {
                        terminateIfClientQuit(request);
                        if (clientSocketClosed()) {
                            break;
                        }
                        tcpClient.writeToClient(SMTP_INVALID + "; I must AUTHorize you before we continue");
                        request = getClientRequest();
                    }

                    if (clientSocketClosed()) {
                        break;
                    }

                    // AUTH USER Challenge - Lock client in until valid email/username is given or requested QUIT
                    tcpClient.writeToClient(USERNAME_CHALLENGE);
                    IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                            InetAddress.getLocalHost().getHostAddress(),
                            AUTH,
                            USERNAME_CHALLENGE);

                    request = B64Util.decode(getClientRequest());
                    while (!request.contains(EMAIL_DOMAIN)) {
                        terminateIfClientQuit(request);
                        if (clientSocketClosed()) {
                            break;
                        }
                        tcpClient.writeToClient(INVALID_CREDENTIALS + "; Not a valid email");
                        request = B64Util.decode(getClientRequest());
                    }

                    if (clientSocketClosed()) {
                        break;
                    }
                    this.clientEmail = request;
                    this.encodedEmailString = B64Util.encode(this.clientEmail);

                    // Start registration process if user is not already registered in .user_pass
                    if (!CredentialsManager.isRegisteredUser(encodedEmailString)) {
                        this.isNewUser = true; // this might end up being irrelevant
                        this.temporaryPassword = CredentialsManager.generateTemporaryPassword();
                        // TODO://////////////////////////////////////////////////
                        // if email not in .user_pass file => isNewUser = true
                        // start new sequence/while loop of registering new user
                        // TODO://////////////////////////////////////////////////

                    }

                    if (clientSocketClosed()) {
                        break;
                    }

                    // AUTH PASSWORD Challenge - Lock client in until valid password is given or requested QUIT
                    int loginAttempts = 4;
                    tcpClient.writeToClient(PASSWORD_CHALLENGE);
                    IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                            InetAddress.getLocalHost().getHostAddress(),
                            AUTH,
                            PASSWORD_CHALLENGE);
                    request = B64Util.decode(getClientRequest());
                    this.encodedPasswordString = B64Util.encode(request);

                    while (!CredentialsManager.passwordValid(this.encodedEmailString, this.encodedPasswordString)) {
                        terminateIfClientQuit(request);
                        loginAttempts--;
                        // User will be forcibly disconnected after a total of 4 failed login attempts
                        if (loginAttempts == 0 && !clientSocketClosed()) {
                            tcpClient.writeToClient(INVALID_CREDENTIALS + "; Maximum login attempts exceeded, connection terminating...");
                            bidFarewell();
                            terminateClientConnection();
                        }
                        if (clientSocketClosed()) {
                            break;
                        }
                        tcpClient.writeToClient(INVALID_CREDENTIALS + "; Password does not match what is on file ("
                                + loginAttempts + " attempts remaining before termination)");
                        request = B64Util.decode(getClientRequest());
                        this.encodedPasswordString = B64Util.encode(request);
                    }

                    if (clientSocketClosed()) {
                        break;
                    }

                    tcpClient.writeToClient(AUTH_SUCCESS + "; You may now continue with mail commands");
                    IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                            InetAddress.getLocalHost().getHostAddress(),
                            AUTH,
                            AUTH_SUCCESS);

                    // MAIL FROM - Keep client locked in until they either request proper MAIL FROM or QUIT
                    request = getClientRequest();
                    while (!validMailFrom(request)) {
                        terminateIfClientQuit(request);
                        if (clientSocketClosed()) {
                            break;
                        }
                        tcpClient.writeToClient(SMTP_INVALID + "; You must specify 'MAIL FROM: yourEmail@cs447.edu'");
                        request = getClientRequest();
                    }

                    if (clientSocketClosed()) {
                        break;
                    }

                    email.setFromField(parseAddressFromRequest(request));
                    communicateOK();
                    IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                            InetAddress.getLocalHost().getHostAddress(),
                            SMTP_MAIL_FROM,
                            SMTP_OK);

                    // RCPT TO - Keep client locked in until they either request proper RCPT TO or QUIT
                    request = getClientRequest();
                    while (!validMailTo(request)) {
                        terminateIfClientQuit(request);
                        if (clientSocketClosed()) {
                            break;
                        }
                        tcpClient.writeToClient(SMTP_INVALID + "; You must specify 'RCPT TO: recipient@cs447.edu'");
                        request = getClientRequest();
                    }

                    if (clientSocketClosed()) {
                        break;
                    }

                    email.setRecipientField(parseRecipientFromRequest(request));
                    communicateOK();
                    IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                            InetAddress.getLocalHost().getHostAddress(),
                            SMTP_MAIL_TO,
                            SMTP_OK);

                    // DATA- Keep client locked in until they either request proper DATA or QUIT
                    request = getClientRequest();
                    while (!validData(request)) {
                        terminateIfClientQuit(request);
                        if (clientSocketClosed()) {
                            break;
                        }
                        tcpClient.writeToClient(SMTP_INVALID + "; I need to know what DATA you are sending");
                        request = getClientRequest();
                    }

                    if (clientSocketClosed()) {
                        break;
                    }

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
                    tcpClient.writeToClient("Mail successfully sent!\nRevoking authorized privileges, invoke HELO to send another message");
                } catch (Exception ex) {
                    logUnknownError(ex);
                }
            }
        } catch (IOException ex) {
            logUnknownError(ex);
        }
    }

    private void communicateServerReady() {
        tcpClient.writeToClient(SMTP_SERVER_READY + "\n(Prompted user input not implemented; Must use protocols manually to continue)");
    }

    private void greetClient() throws UnknownHostException {
        tcpClient.writeToClient("250 Hello");
        IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                InetAddress.getLocalHost().getHostAddress(),
                SMTP_HELO,
                "250 Hello");
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

    private boolean clientQuit(String request) {
        return request.equalsIgnoreCase("QUIT");
    }

    private void bidFarewell() throws UnknownHostException {
        tcpClient.writeToClient(SMTP_GOODBYE);
        IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                InetAddress.getLocalHost().getHostAddress(),
                "QUIT",
                SMTP_GOODBYE);
    }

    private void terminateClientConnection() {
        System.out.println("Client " + tcpClient.getSocket().getInetAddress().getHostAddress() + " has disconnected...");
        tcpClient.closeStreams();
        this.clientHasConnection = false;
    }

    private void terminateIfClientQuit(String request) throws UnknownHostException {
        if (clientQuit(request)) {
            bidFarewell();
            terminateClientConnection();
        }
    }

    private boolean clientSocketClosed() {
        return tcpClient.getSocket().isClosed();
    }

    private boolean validMailFrom(String request) {
        //todo: check for matching client email really should be done client side
        return request.toUpperCase().startsWith(SMTP_MAIL_FROM) && request.contains(this.clientEmail);
    }

    private boolean validMailTo(String request) {
        return request.toUpperCase().startsWith(SMTP_MAIL_TO) && request.contains(EMAIL_DOMAIN);
    }

    private boolean validData(String request) {
        return request.equalsIgnoreCase(SMTP_DATA);
    }

    private boolean isRequestInvalid(String request) {
        return !(request.equalsIgnoreCase(SMTP_HELO)
                || request.toUpperCase().startsWith(SMTP_MAIL_FROM)
                || request.toUpperCase().startsWith(SMTP_MAIL_TO)
                || request.equalsIgnoreCase(SMTP_DATA)
                || request.endsWith("\n.\n"));
    }

    private void logUnknownError(Exception ex) {
        try {
            IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                    InetAddress.getLocalHost().getHostAddress(),
                    "EXCEPTION OCCURRED",
                    ex.getMessage() + "\n" + ex.getLocalizedMessage() + "\n" + ex.toString());
        } catch (UnknownHostException e) {
            System.err.println("Unexpected error occurred. Terminating application");
            System.exit(1);
        }
    }
}
