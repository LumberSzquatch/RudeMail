package client;

// Class to create download folder for emails
public class Downloader {

    private String userFolder;

    public Downloader(){
    }

    public void initReceiverFolder(String user) {
        this.userFolder = user;
        // create a download folder named after user's username
        // all successfull HTTP request will put emails here as a .txt file
    }

    public void downloadToFolder(String response){

    }
}
