package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class UDPAgent implements Runnable {

    private BufferedReader userInput;
    private DatagramSocket datagramSocket;
    private InetAddress clientIP;
    private int serverPort;
    private byte[] inputBuffer;
    private byte[] outputBuffer;
    private boolean socketOpen;

    private static final String FAREWELL = "BYE";

    public UDPAgent(String serverName, int serverPort) throws SocketException, UnknownHostException {
        datagramSocket = new DatagramSocket();
        clientIP = InetAddress.getByName(serverName);
        this.serverPort = serverPort;
        inputBuffer = new byte[2048];
        outputBuffer = new byte[2048];
        socketOpen = false;
    }

    @Override
    public void run() {
        setSocketOpen();
        initializeInputReader();
        System.out.println("Server ready for requests...");
        String requestString;
        String responseString;
        while (this.socketOpen) {
            try {
                requestString = this.userInput.readLine();
                sendServerRequest(requestString);

                responseString = receiveServerResponse();
                System.out.println(responseString);

                if (requestString.toUpperCase().contains(FAREWELL) /*&& serverBidsFarewell(responseString)*/) {
                    closeSocket();
                }
                flushBuffers();
            } catch (IOException ioe) {
                System.out.println("ERROR! Unexpectedly failed to communicate with server\nClosing connection...");
                closeSocket();
            }
        }
    }

    /*
     * Captures user entered input and converts it a byte array and stores the result in the output buffer;
     * .send() is then called on the socket given a packet created from the buffer;
     * Is dependent on convertToBytes(String) and outboundPacketFrom(byte[]) methods
     */
    private void sendServerRequest(String input) throws IOException {
        this.outputBuffer = convertToBytes(input);
        this.datagramSocket.send(outboundPacketFrom(outputBuffer));
    }

    /**
     * Retrieves the response packet from the server, sets it's data field using receive
     * and then returns the response string to display to the user;
     * Is dependent on inboundPacketFrom(byte[]) and convertToString(byte[]) methods
     */
    private String receiveServerResponse() throws IOException {
        DatagramPacket serverResponsePacket = inboundPacketFrom(this.inputBuffer);
        this.datagramSocket.receive(serverResponsePacket);
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

    private boolean serverBidsFarewell(String response) {
        return response.contains(FAREWELL);
    }

    //Breaks input loop and kills the socket after input of 'bye'
    private void closeSocket() {
        this.socketOpen = false;
        this.datagramSocket.close();
    }

}
