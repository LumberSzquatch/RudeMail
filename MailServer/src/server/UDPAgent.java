package server;

import java.io.IOException;
import java.net.*;
import java.util.Date;

public class UDPAgent implements Runnable {

    private DatagramSocket datagramSocket;
    private boolean isRunning;

    private static final String HTTP_VERSION = "HTTP/1.1";
    private static final byte[] HTTP_200 = "200 OK".getBytes();
    private static final byte[] HTTP_400 = "400 Bad Request".getBytes();
    private static final byte[] HTTP_404 = "404 Not Found".getBytes();

    //TODO: needs two args TCP Port and UDP port
    public UDPAgent(int port) throws SocketException {
        datagramSocket = new DatagramSocket(port);
//        service = new ResponseService();
        isRunning = false;
    }

    @Override
    public void run() {
        spinUp();
        System.out.println("UDP agent ready to receive");
        while (this.isRunning) {
            try {
                byte[] inputBuffer = new byte[2048];

                DatagramPacket clientRequestPacket = inboundPacketFrom(inputBuffer);
                String requestString = receiveClientRequest(clientRequestPacket);
                System.out.println("Received request: " + requestString);
                String serverResponse = testResponse(); //service.getResponse(requestString, clientRequestPacket.getAddress());
                sendServerResponse(
                        convertToBytes(serverResponse),
                        getClientIP(clientRequestPacket),
                        getClientPort(clientRequestPacket)
                );
            } catch (IOException e) {
                System.out.println("Server encountered an unexpected error:");
                e.printStackTrace();
            }
        }
    }

    private String testResponse() throws UnknownHostException {
        String responseHeader
                = HTTP_VERSION + " " + new String(HTTP_200) + "\n"
                + "Server: " + InetAddress.getLocalHost() + "\n"
                + "Last-Modified: " + new Date() + "\n"
                + "Count: " + 1 + "\n"
                + "Content-Type: text/plain" + "\n"
                + "Message: " + 1 + "\n\n";
        String response = "Hey look at this you stupid bitch,\n This pretty much what the user should see, don't fuck it up\n" +
                "Yours Truly,\nSome Mother fucker";
        return responseHeader + response;
    }

    private String receiveClientRequest(DatagramPacket clientRequest) throws IOException {
        this.datagramSocket.receive(clientRequest);
        return convertToString(clientRequest.getData());
    }

    private void sendServerResponse(byte[] outputBuffer, InetAddress clientIP, int clientPort) throws IOException {
        datagramSocket.send(outboundPacketFrom(outputBuffer, clientIP, clientPort));
    }

    // Serves up a DatagramPacket given a byte[] for incoming client requests
    private DatagramPacket inboundPacketFrom(byte[] request) {
        return new DatagramPacket(request, request.length);
    }

    // Serves up a DatagramPacket given a byte[] for outgoing server responses
    private DatagramPacket outboundPacketFrom(byte[] response, InetAddress clientIP, int clientPort) {
        return new DatagramPacket(response, response.length, clientIP, clientPort);
    }

    // Retrieves the client's IP address from the request packet
    private InetAddress getClientIP(DatagramPacket clientPacket){
        return clientPacket.getAddress();
    }

    // Retrieves the client's port from the request packet
    private int getClientPort(DatagramPacket clientPacket) {
        return clientPacket.getPort();
    }

    // Converts a String to a byte array; used to set the data of client packets
    private byte[] convertToBytes(String message) {
        return message.getBytes();
    }

    // Converts a byte array to a String; used translate server response
    private String convertToString(byte[] buffer) {
        return new String(buffer);
    }

    // Server instance control
    private void spinUp() {
        isRunning = true;
    }
}
