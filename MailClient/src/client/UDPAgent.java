package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class UDPAgent implements Runnable {

    private BufferedReader userInput;
    private DatagramSocket datagramSocket;
    private InetAddress clientIP;
    private String serverHostname;
    private int serverPort;
    private byte[] inputBuffer;
    private byte[] outputBuffer;
    private boolean socketOpen;

    private static final String QUIT = "QUIT";

    // Timeout values for DatagramSocket; Server has 10 sec. to respond
    //     after response set back to 0 (which indicates infinite timeout)
    private static final int REQUEST_TIMEOUT = 10000;
    private static final int INFINITE_TIMEOUT = 0;

    private String validatedUsername;
    private Downloader downloader;

    public UDPAgent(String serverHostname, int serverPort) throws SocketException, UnknownHostException {
        datagramSocket = new DatagramSocket();
        clientIP = InetAddress.getByName(serverHostname);
        this.serverHostname = serverHostname;
        this.serverPort = serverPort;
        inputBuffer = new byte[2048];
        outputBuffer = new byte[2048];
        socketOpen = false;
        downloader = new Downloader();
    }

    @Override
    public void run() {
        setSocketOpen();
        initializeInputReader();
        String requestString;
        String responseString;

        System.out.println("Connected to " + this.serverHostname + ". Ready to retrieve (enter 'QUIT' at anytime to exit)...\n" +
                "Please log in before we continue...");
        System.out.println("AUTH: Do auth login here; can set file location based on username (i.e. db/username/*.mail)\n" +
                "Continuing as cheese...");

        validatedUsername = "cheese"; // todo: make this get set based on how server responds
        downloader.initReceiverFolder(validatedUsername);

        while (this.socketOpen) {
            try {
                // Tasks:
                // 1) Authenticate user
                //       a) server should respond accordingly
                //              - failed auth re-requests creds?
                //              - only after validation does system ask user for input
                // 3) Validate input and have server respond accordingly
                // 4) On valid input make stuff all emails into one file (fuck it)
                // 5) Then just exit because it's not worth it to figure the logic for them to keep requesting (not even in the requirements)
                System.out.println("How many email would you like to receive? (requesting for more email than you have will fetch all email): ");

                String userResponse = this.userInput.readLine();
                requestString = constructHttpGetRequest(userResponse);
                closeIfUserQuit(requestString);
                sendServerRequest(requestString);

                responseString = receiveServerResponse();
                // todo: check that the response string is a successful GET; if so download to user folder
                if (true) {
                    downloader.downloadToFolder(responseString);
                }
                System.out.println(responseString); // todo: good candidate for logging
                flushBuffers();

            } catch (Exception e) {
                if (!(e instanceof SocketTimeoutException)) {
                    errorInducedShutdown();
                }
                System.out.println("WARNING! Your request to the server incurred a timeout. " +
                        "Ensure the server is running and try again (or enter 'QUIT' to exit)");
            }
        }
    }

    /*
     *
     * Since authentication tells us what the receiving user is and the user says how many emails they want
     * after being prompted by the application, we can construct or HTTP request and send it to the server
     */
    private String constructHttpGetRequest(String count) {
        return "GET db/" + this.validatedUsername + "/ HTTP/1.1\n" +
                "Host:" + this.serverHostname + "\n" +
                "Count:" + count;
    }

    /*
     * Captures user entered input and converts it a byte array and stores the result in the output buffer;
     * .send() is then called on the socket given a packet created from the buffer;
     * Is dependent on convertToBytes(String) and outboundPacketFrom(byte[]) methods
     */
    private void sendServerRequest(String request) throws IOException {
        this.outputBuffer = convertToBytes(request);
        this.datagramSocket.send(outboundPacketFrom(outputBuffer));
    }

    /**
     * Retrieves the response packet from the server, sets it's data field using receive
     * and then returns the response string to display to the user;
     * Is dependent on inboundPacketFrom(byte[]) and convertToString(byte[]) methods
     */
    private String receiveServerResponse() throws IOException {
        DatagramPacket serverResponsePacket = inboundPacketFrom(this.inputBuffer);

        // Add timeout property to the datagram; Reset back
        this.datagramSocket.setSoTimeout(REQUEST_TIMEOUT);
        this.datagramSocket.receive(serverResponsePacket);
        this.datagramSocket.setSoTimeout(INFINITE_TIMEOUT);
        return convertToString(serverResponsePacket.getData());
    }

    // Serves up a DatagramPacket given a byte[] for outgoing client requests
    private DatagramPacket outboundPacketFrom(byte[] request) {
        return new DatagramPacket(request, request.length, this.clientIP, this.serverPort);
    }

    // Serves up a DatagramPacket given a byte[] for incoming server responses
    private DatagramPacket inboundPacketFrom(byte[] response) {
        return new DatagramPacket(response, response.length);
    }

    // Converts a String to a byte array; used to set the data of client packets
    private byte[] convertToBytes(String message) {
        return message.getBytes();
    }

    // Converts a byte array to a String; used translate server response
    private String convertToString(byte[] buffer) {
        return new String(buffer);
    }

    // Simply instantiating a new BufferReader for user input
    private void initializeInputReader() {
        this.userInput = new BufferedReader(new InputStreamReader(System.in));
    }

    // User input loop control; set to false when users inputs 'bye'
    private void setSocketOpen() {
        this.socketOpen = true;
    }

    /**
     * Used to empty the input/output buffers after request/response is sent/received
     * Is called at the end of every loop iteration
     */
    private void flushBuffers() {
        this.inputBuffer = new byte[2048];
        this.outputBuffer = new byte[2048];
    }

    private void closeIfUserQuit(String requestString) {
        if (requestString.equalsIgnoreCase(QUIT)) {
            closeSocket();
            System.out.println("Goodbye");
            System.exit(0);
        }
    }

    private void errorInducedShutdown(){
        System.err.println("ERROR! Unexpectedly failed to communicate input/output with server\nClosing connection...");
        closeSocket();
        System.out.println("Goodbye");
        System.exit(-1);
    }

    //Breaks input loop and kills the socket after input of 'bye'
    private void closeSocket() {
        this.socketOpen = false;
        this.datagramSocket.close();
    }

}
