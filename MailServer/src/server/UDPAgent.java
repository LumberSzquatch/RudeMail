package server;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class UDPAgent implements Runnable {

    private DatagramSocket datagramSocket;
    private boolean isRunning;

    //TODO: needs two args TCP Port and UDP port
    public UDPAgent(int port) throws SocketException {
        datagramSocket = new DatagramSocket(port);
//        service = new ResponseService();
        isRunning = false;
    }

    @Override
    public void run() {
        spinUp();
        System.out.println("Server ready to receive...");
        while (this.isRunning) {
            try {
                byte[] inputBuffer = new byte[2048];

                DatagramPacket clientRequestPacket = inboundPacketFrom(inputBuffer);
                String requestString = receiveClientRequest(clientRequestPacket);
                System.out.println("Recieved request: " + requestString);
                String serverResponse = "I'm alive"; //service.getResponse(requestString, clientRequestPacket.getAddress());
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
