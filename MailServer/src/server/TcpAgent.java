package server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

public class TcpAgent implements Runnable {

    private final TcpClient tcpClient;
    private Thread clientThread;

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
    private final static String HASH_USERNAME_CHALLENGE = "334 f86ef3eb8e32629d3d04eb94e64252f1";
    private final static String PASSWORD_CHALLENGE = "334 cGFzc3dvcmQ6";
    private final static String HASH_PASSWORD_CHALLENGE = "334 748867e22058f7e83b64404f987dd8f9";
    private final static String AUTH_SUCCESS = "235 AUTH Success";
    private final static String AUTH_REGISTER = "330 Must Register\nTemporary Password: ";
    private final static String EMAIL_DOMAIN = "@cs447.edu";

    private boolean usesSecureChannel;
    private boolean usesHashAuth;
    private boolean isNewUser;
    private String clientEmail;
    private String encodedEmailString;
    private String encodedPasswordString;
    private String temporaryPassword;

    private String clientIP;

    public TcpAgent(TcpClient tcpClient, boolean usesSecureChannel, boolean usesHashAuth) {
        this.tcpClient = tcpClient;
        this.usesSecureChannel = usesSecureChannel;
        this.usesHashAuth = usesHashAuth;
        this.clientIP = tcpClient.getCustomName();
        this.isNewUser = false;
        this.clientThread = null;
    }

    public void setClientThread(Thread thread) {
        this.clientThread = thread;
    }

    @Override
    public void run() {
        System.out.println("Client " + tcpClient.getCustomName() + " has established connection");
        try {
            IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                    InetAddress.getLocalHost().getHostAddress(),
                    "Request connection/Started thread for client",
                    SMTP_SERVER_READY);
            communicateServerReady();
            Email email = new Email();
            String request;
            while (tcpClient.getSocket().isConnected()) {

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
                    if (usesHashAuth) {
                        tcpClient.writeToClient(HASH_USERNAME_CHALLENGE);
                        IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                                InetAddress.getLocalHost().getHostAddress(),
                                AUTH,
                                HASH_USERNAME_CHALLENGE);

                        request = HashAuthorizer.retrieveHashedPassword(getClientRequest());
                        System.out.println(request);
                    } else {
                        tcpClient.writeToClient(USERNAME_CHALLENGE);
                        IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                                InetAddress.getLocalHost().getHostAddress(),
                                AUTH,
                                USERNAME_CHALLENGE);

                        request = B64Util.decode(getClientRequest());
                    }
                    while (!request.contains(EMAIL_DOMAIN)) {
                        terminateIfClientQuit(request);
                        if (clientSocketClosed()) {
                            break;
                        }
                        tcpClient.writeToClient(INVALID_CREDENTIALS + "; Not a valid email");
                        if (usesHashAuth) {
                            request = HashAuthorizer.retrieveHashedPassword(getClientRequest());
                        } else {
                            request = B64Util.decode(getClientRequest());
                        }
                    }

                    if (clientSocketClosed()) {
                        break;
                    }
                    this.clientEmail = request;
                    if (usesHashAuth) {
                        this.encodedEmailString = HashAuthorizer.generateHashedPassword(this.clientEmail);
                    } else {
                        this.encodedEmailString = B64Util.encode(this.clientEmail);
                    }
                    // Start registration process if user is not already registered in .user_pass
                    if (!CredentialsManager.isRegisteredUser(encodedEmailString)) {
                        System.out.println("Waiting for connection refresh from client" + tcpClient.getCustomName() + " ...");
                        this.isNewUser = true;
                        this.temporaryPassword = CredentialsManager.generateTemporaryPassword();
                        if (usesHashAuth) {
                            CredentialsManager.writeHashedUserToMasterFile(this.encodedEmailString, temporaryPassword);
                            tcpClient.writeToClient(AUTH_REGISTER + temporaryPassword);
                        } else {
                            CredentialsManager.writeUserToMasterFile(this.encodedEmailString, temporaryPassword);
                            tcpClient.writeToClient(AUTH_REGISTER + temporaryPassword);
                        }
                        System.out.println("Connection refreshed for client " + tcpClient.getCustomName());
                    }

                    if (clientSocketClosed()) {
                        break;
                    }

                    // AUTH PASSWORD Challenge - Lock client in until valid password is given or requested QUIT
                    int loginAttempts = 4;
                    if (usesHashAuth) {
                        tcpClient.writeToClient(HASH_PASSWORD_CHALLENGE);
                        IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                                InetAddress.getLocalHost().getHostAddress(),
                                AUTH,
                                HASH_PASSWORD_CHALLENGE);
                        request = HashAuthorizer.retrieveHashedPassword(getClientRequest());
                    } else {
                        tcpClient.writeToClient(PASSWORD_CHALLENGE);
                        IncidentManager.log(tcpClient.getSocket().getInetAddress().getHostAddress(),
                                InetAddress.getLocalHost().getHostAddress(),
                                AUTH,
                                PASSWORD_CHALLENGE);
                        request = B64Util.decode(getClientRequest());
                    }
                    if (isNewUser) {
                        request = CredentialsManager.getSalt(request);
                        isNewUser = false;
                    }
                    if (usesHashAuth) {
                        this.encodedPasswordString = HashAuthorizer.generateHashedPassword(request);
                    } else {
                        this.encodedPasswordString = B64Util.encode(request);
                    }

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
                        if (usesHashAuth) {
                            request = HashAuthorizer.retrieveHashedPassword(getClientRequest());
                            this.encodedPasswordString = HashAuthorizer.generateHashedPassword(request);
                        } else {
                            request = B64Util.decode(getClientRequest());
                            this.encodedPasswordString = B64Util.encode(request);
                        }
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
                    if (ex instanceof IOException) {
                        // todo: watch out, this maybe might happen if we sleep the thread (maybe, idk? just a gut feeling)
                        System.out.println("Client " + clientIP + " connection was not able to be maintained\n" +
                                "Closing socket connection and terminating thread for client...");
                        terminateClientConnection();
                        return;
                    }
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

    private void terminateIfClientQuit(String request) throws UnknownHostException {
        if (clientQuit(request)) {
            bidFarewell();
            terminateClientConnection();
        }
    }

    private void terminateClientConnection() {
        System.out.println("Client " + tcpClient.getSocket().getInetAddress().getHostAddress() + " has disconnected...");
        tcpClient.closeStreams();
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
                    ex.toString());
        } catch (UnknownHostException e) {
            System.err.println("Unexpected error occurred. Terminating application");
            System.exit(1);
        }
    }
}
