package server;

import java.io.*;
import java.util.Random;

public class CredentialsManager {

    private static final String VAULT = "/.user_pass";
    private static final String ROOT_DIRECTORY = "db";
    private static final String CREDS_DELIMITER = "/";

    private static final int EXTRA_STEP_NUMBER = 447;

    private static File masterFile;

    public CredentialsManager() {
        File directory = new File(ROOT_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdir();
        }

        File credentials = new File(ROOT_DIRECTORY + VAULT);
        CredentialsManager.masterFile = credentials;
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
                if (currentCredentials == null) {
                    return false;
                }
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

    public static void writeUserToMasterFile(String b64EncodedEmail, String unmodifiedPassword) throws IOException {

        String userCredentials = b64EncodedEmail + CREDS_DELIMITER + B64Util.encode(modifyGeneratedNumber(unmodifiedPassword));

        try (FileWriter fw = new FileWriter(masterFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter printWriter = new PrintWriter(bw))
        {
            printWriter.println(userCredentials);
        } catch (IOException e) {
            System.err.println("Failed to write to log file");
        }
    }

    public static String generateTemporaryPassword() {
        char[] password = new char[5];
        for (int i = 0; i < password.length; i++) {
            password[i] = getRandomSingleDigit();
        }
        return new String(password);
    }

    private static char getRandomSingleDigit() {
        return Integer
                .toString(new Random()
                .nextInt(10))
                .charAt(0);
    }

    public static String modifyGeneratedNumber(String fiveDigitInteger) {
        // Adding 447 to generated value for extra layer of security (or something; sounds like extra steps)
        int modifiedDigits = Integer.parseInt(fiveDigitInteger) + EXTRA_STEP_NUMBER;
        return Integer.toString(modifiedDigits);
    }
}
