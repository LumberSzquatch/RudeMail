package server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashAuthorizer {

    public HashAuthorizer(){

    }

    public static String generateHashedPassword(String toHash) throws NoSuchAlgorithmException {
        String generatedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(toHash.getBytes());
            StringBuilder hashBuilder = new StringBuilder();
            for(int i = 0; i < hashBytes.length; i++) {
                hashBuilder.append(Integer.toString(getHexValue(hashBytes[i]), 16).substring(1));
            }
            generatedPassword = hashBuilder.toString();
        }
        catch (NoSuchAlgorithmException e) {
            System.err.println("Unexpected error while hashing temporary password");
            throw e;
        }
        return generatedPassword;
    }

    public static String retrieveHashedPassword(String hashedPassword) throws NoSuchAlgorithmException {
        String generatedPassword = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(hashedPassword.getBytes());
            StringBuilder sb = new StringBuilder();
            for(int i=0; i< bytes.length ;i++) {
                sb.append(Integer.toString(getHexValue(bytes[i]), 16).substring(1));
            }
            generatedPassword = sb.toString();
        }
        catch (NoSuchAlgorithmException e) {
            System.err.println("Unexpected error while retrieving hashed password");
            throw e;
        }
        System.out.println(generatedPassword);
        return generatedPassword;
    }

    private static int getHexValue(byte hashByte) {
        return (hashByte & 0xff) + 0x100;
    }

}
