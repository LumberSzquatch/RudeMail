package client;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

// Class to create download folder for emails
public class Downloader {

    private static String userFolder;

    private Downloader(String user) {
        userFolder = user;
        // create a download folder named after user's username
        // all successfull HTTP request will put emails here as a .txt file
        File directory = new File(user);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    public static void initReceiverFolder(String user) {
        Downloader downloader = new Downloader(user);
    }

    public static void downloadToFolder(String response) {
        int downloadCountSuffix = 1;

        File downloadFolder = new File(userFolder);
        if (!downloadFolder.exists()) {
            downloadFolder.mkdir();
        } else {
            downloadCountSuffix += downloadFolder.list().length;
        }

        File download = new File(userFolder + "/download" + downloadCountSuffix + ".txt");

        try (PrintWriter printWriter = new PrintWriter(download)) {
            printWriter.println(response);
            printWriter.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Failed to download content .txt file");
        }
    }
}
