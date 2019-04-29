package server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Random;

public class CredentialsManager {

    private static final String VAULT = "/.user_pass";
    private static final String ROOT_DIRECTORY = "db";
    private static final String CREDS_DELIMITER = "/";

    private static final int EXTRA_STEP_NUMBER = 447;

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
        String credentials = ROOT_DIRECTORY + VAULT;
        String encodedUser = b64Email + CREDS_DELIMITER + b64Password;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(credentials));
            String currentCredentials = reader.readLine();

            if (currentCredentials == null) {
                return false;
            }

            if (encodedUser.equals(currentCredentials)) {
                return true;
            }

            while (currentCredentials != null) {
                currentCredentials = reader.readLine();
                if (encodedUser.equals(currentCredentials)) {
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

    public static String generateTemporaryPassword() {
        char[] password = new char[5];
        for (int i = 0; i < password.length; i++) {
            password[i] = getRandomSingleDigit();
        }
        return modifyGeneratedNumber(new String(password));
    }

    private static char getRandomSingleDigit() {
        return Integer
                .toString(new Random()
                .nextInt(10))
                .charAt(0);
    }

    private static String modifyGeneratedNumber(String fiveDigitInteger) {
        // Adding 447 to generated value for extra layer of security (or something; sounds like extra steps)
        int modifiedDigits = Integer.parseInt(fiveDigitInteger) + EXTRA_STEP_NUMBER;
        return Integer.toString(modifiedDigits);
    }
}
