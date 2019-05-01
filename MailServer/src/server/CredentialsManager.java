package server;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class CredentialsManager {

    private static final String VAULT = "/.user_pass";
    private static final String HASH_VAULT = "/.hash_pass";
    private static final String ROOT_DIRECTORY = "db";
    private static final String CREDS_DELIMITER = "/";

    private static final int SALT = 447;

    private static File masterFile;
    private static File masterHashFile;
    private static boolean hashedCreds;

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

        File hashCredentials = new File(ROOT_DIRECTORY + HASH_VAULT);
        CredentialsManager.masterHashFile = credentials;
        if (!hashCredentials.exists()) {
            try {
                hashCredentials.createNewFile();
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
        if (hashedCreds) {
            credentials = ROOT_DIRECTORY + HASH_VAULT;
        }
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

    public static boolean passwordValid(String secureEmail, String securePassword) {
        String credentials = ROOT_DIRECTORY + VAULT;
        String encodedUser = secureEmail + CREDS_DELIMITER + securePassword;
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

    public static boolean hashPasswordValid(String hashedEmail, String hashedPassword) {
        String credentials = ROOT_DIRECTORY + HASH_VAULT;
        String encodedUser = hashedEmail + CREDS_DELIMITER + hashedEmail;
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

        String userCredentials = b64EncodedEmail + CREDS_DELIMITER + B64Util.encode(getSalt(unmodifiedPassword));

        try (FileWriter fw = new FileWriter(masterFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter printWriter = new PrintWriter(bw))
        {
            printWriter.println(userCredentials);
        } catch (IOException e) {
            System.err.println("Failed to write to log file");
        }
    }

    public static void writeHashedUserToMasterFile(String hashedEmail, String unmodifiedPassword) throws IOException, NoSuchAlgorithmException {

        String userCredentials = hashedEmail + CREDS_DELIMITER + HashAuthorizer.generateHashedPassword(getSalt(unmodifiedPassword));

        try (FileWriter fw = new FileWriter(masterHashFile, true);
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

    public static String getSalt(String fiveDigitInteger) {
        // Adding 447 to generated value for extra layer of security (the salt)
        int modifiedDigits = Integer.parseInt(fiveDigitInteger) + SALT;
        return Integer.toString(modifiedDigits);
    }
}
