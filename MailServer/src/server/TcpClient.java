package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TcpClient {

    private Socket socket;

    private String customName;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;

    public TcpClient() {
    }

    public TcpClient(Socket socket) throws IOException {
        this.socket = socket;
        setupStreams();
        customName = socket.getInetAddress().getHostAddress();
    }

    public TcpClient(Socket socket, String customName) throws IOException {
        this(socket);
        this.customName = customName;
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

    public void writeToClient(Object o) {
        try {
            outputStream.writeObject(o);
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
            inputStream = new ObjectInputStream(getSocket().getInputStream());
            outputStream = new ObjectOutputStream(getSocket().getOutputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Failed to setup streams.");
        }
    }
}

