package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class CredentialsManager {

    private static final String VAULT = "/.user_pass";
    public static final String ROOT_DIRECTORY = "db";

    public CredentialsManager() {
        File directory = new File(ROOT_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdir();
        }

        File credentials = new File(ROOT_DIRECTORY + VAULT);
        if (!credentials.exists()) {
            try {
                credentials.createNewFile();
            } catch (IOException e) {
                System.err.println("Failed to create log file");
            }
        }
    }

    public static void initializeCM() {
        CredentialsManager cman = new CredentialsManager();
    }

    public static boolean isRegisteredUser(String encodedEmail) {
        String credentials = ROOT_DIRECTORY + VAULT;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(credentials));
            String currentCredentials = reader.readLine();

            if (currentCredentials == null) {
                return false;
            }

            if (currentCredentials.contains(encodedEmail)) {
                return true;
            }

            while (currentCredentials != null) {
                currentCredentials = reader.readLine();
                if (currentCredentials.contains(encodedEmail)) {
                    return true;
                }
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("Failed to parse master credentials file");
            e.printStackTrace();
        }
        return false;
    }

    public static boolean passwordValid(String b64Email, String b64Password) {
        // todo: parse .user_pass file for email + password and return true if exists
        return b64Password.equals("letmein");
    }
}
