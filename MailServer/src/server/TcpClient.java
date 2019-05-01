package server;

import java.io.*;
import java.net.Socket;

public class TcpClient {

    private Socket socket;
    private boolean usesSecureChannel;

    private String customName;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    private BufferedInputStream securedInputStream;
    private BufferedOutputStream securedOutputStream;

    public TcpClient() {
    }

    public TcpClient(Socket socket, boolean usesSecureChannel) throws IOException {
        this.socket = socket;
        this.usesSecureChannel = usesSecureChannel;
        setupStreams();
        customName = socket.getInetAddress().getHostAddress();
    }

    public void setCustomName(String customName) {
        this.customName = customName;
    }

    public void setInputStream(ObjectInputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setOutputStream(ObjectOutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getCustomName() {
        return customName;
    }

    public ObjectInputStream getInputStream() {
        return inputStream;
    }

    public ObjectOutputStream getOutputStream() {
        return outputStream;
    }

    public Socket getSocket() {
        return socket;
    }

    public Object readFromClient() throws IOException {
        try {
            return inputStream.readObject();
        } catch (ClassNotFoundException ex) {
            System.err.println(
                    "Failed to read object from client.");
        }
        return new Object();
    }

    public void writeToClient(String out) {
        try {
            if (usesSecureChannel) {
                securedOutputStream.write(out.getBytes());
                securedOutputStream.flush();
            } else {
                outputStream.writeObject(out);
            }
        } catch (IOException ex) {
            System.err.println(
                    "Failed to write object to client.");
        }
    }

    public void closeStreams() {
        try {
            inputStream.close();
            outputStream.close();
            this.getSocket().close();
        } catch (IOException ex) {
            System.err.println("Failed to close streams.");
            ex.printStackTrace();
        }
    }

    private void setupStreams() {
        try {
            if (usesSecureChannel) {
                securedInputStream = new BufferedInputStream(getSocket().getInputStream());
                securedOutputStream = new BufferedOutputStream(getSocket().getOutputStream());
            } else {
                inputStream = new ObjectInputStream(getSocket().getInputStream());
                outputStream = new ObjectOutputStream(getSocket().getOutputStream());
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Failed to setup streams.");
        }
    }
}

