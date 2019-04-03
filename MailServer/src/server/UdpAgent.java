package server;

import java.io.IOException;
import java.net.*;

public class UdpAgent implements Runnable {

    private DatagramSocket datagramSocket;
    private HttpValidator httpValidator;
    private boolean isRunning;

    public UdpAgent(int port) throws SocketException {
        datagramSocket = new DatagramSocket(port);
        httpValidator = new HttpValidator();
        isRunning = false;
    }

    @Override
    public void run() {
        spinUp();
        System.out.println("UDP agent ready to receive");
        while (this.isRunning) {
            try {

                System.out.println("cheese validated as user");
                String user = "cheese";
                byte[] inputBuffer = new byte[2048];

                DatagramPacket clientRequestPacket = inboundPacketFrom(inputBuffer);
                String requestString = receiveClientRequest(clientRequestPacket);

                System.out.println("Received request: " + requestString); // todo: perferct canidate for logging

                String serverResponse = this.httpValidator.validatedResponse(requestString);
                sendServerResponse(
                        convertToBytes(serverResponse),
                        getClientIP(clientRequestPacket),
                        getClientPort(clientRequestPacket)
                );
            } catch (IOException e) {
                System.err.println("Server encountered an unexpected due to an input/output error:");
                e.printStackTrace();
            }
        }
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
