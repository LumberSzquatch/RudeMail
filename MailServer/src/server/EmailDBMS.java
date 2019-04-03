package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class EmailDBMS {

    public static final String ROOT_DIRECTORY = "db";

    private EmailDBMS() {
        File directory = new File(ROOT_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    public static void initializeDB() {
        // instantiate a new static instance of this class
        // and make the root DB dir if it does not yet exist
        EmailDBMS db = new EmailDBMS();
    }

    public static void insert(final Email email) {
        int emailFileCountPrefix = 1;

        File recipientInbox = new File(ROOT_DIRECTORY + "/" + email.getRecipientField());
        if (!recipientInbox.exists()) {
            recipientInbox.mkdir();
        } else {
            emailFileCountPrefix += recipientInbox.list().length;
        }

        File dbEmail = constructFileForDB(recipientInbox, emailFileCountPrefix);
        executeQuery(dbEmail, email);
    }

    public static List<Email> select(String requestedDirectory, int requestedCount) throws FileNotFoundException, NullPointerException {
        ArrayList<Email> queryResult = new ArrayList<>();
        File queriedDirectory = new File(requestedDirectory);

        int queryResultLength = queriedDirectory.list().length;
        if (requestedCount > queryResultLength) {
            requestedCount = queryResultLength;
        }

        for (Integer i = 1; i <= requestedCount; i++) {
            Email email = new Email();
            try (Scanner scanner = new Scanner(
                    new File(queriedDirectory.getPath()
                            + "/"
                            + i.toString()
                            + ".email"))) {
                email.setFieldsFromRawFile(scanner);
            }
            queryResult.add(email);
        }
        return queryResult;
    }

    private static void executeQuery(File rawEmail, final Email email){
        try (PrintWriter printWriter = new PrintWriter(rawEmail)) {
            printWriter.println(email.toString());
            printWriter.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Failed to insert email into the database.");
        }
    }

    private static File constructFileForDB(File inbox, int inboxCount) {
       return new File(inbox.getPath() + "/" + inboxCount + ".email");
    }

}