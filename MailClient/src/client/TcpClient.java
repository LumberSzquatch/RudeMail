package client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class TcpClient extends Socket {

    private String hostName;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private String clientUsername;

    private boolean setMailFrom;
    private boolean setMailTo;
    private boolean setData;

    public TcpClient() {
        super();
        hostName = "";
        clientUsername = "";
    }

    public TcpClient(String host, int port) throws IOException {
        super(host, port);
        hostName = this.getInetAddress().getHostAddress();
        initializeCommandPrompts();
        initializeIOStreams();
    }

    public Object readFromServer() throws IOException {
        try {
            return inputStream.readObject();
        } catch (ClassNotFoundException ex) {
            System.err.println(
                    "Failed to retrieve data from input stream");
        }
        return new Object();
    }

    public void sendToServer(Object o) {
        try {
            outputStream.writeObject(o);
        } catch (IOException ex) {
            System.err.println(
                    "Failed to write data to output stream");
        }
    }

    public void setClientUsername(String clientUsername) {
        this.clientUsername = clientUsername;
    }

    public void closeStreams() {
        try {
            outputStream.close();
            inputStream.close();
        } catch (IOException ex) {
            System.err.println(
                    "Failed to close streams.");
        }
    }

    private void initializeCommandPrompts() {
        setMailFrom = false;
        setMailTo = false;
        setData = false;
    }

    private void initializeIOStreams() {
        try {
            outputStream = new ObjectOutputStream(this.getOutputStream());
            inputStream = new ObjectInputStream(this.getInputStream());
        } catch (IOException ex) {
            ex.printStackTrace();
            System.err.println("Failed to setup streams.");
        }
    }

    public boolean isMailFromSet() {
        return setMailFrom;
    }

    public boolean isMailToSet() {
        return setMailTo;
    }

    public boolean isDataSet() {
        return setData;
    }

    public void setMailFrom(boolean setMailFrom) {
        this.setMailFrom = setMailFrom;
    }

    public void setMailTo(boolean setMailTo) {
        this.setMailTo = setMailTo;
    }

    public void setData(boolean setData) {
        this.setData = setData;
    }
}