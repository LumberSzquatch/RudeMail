package client;

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

    private static int getHexValue(byte hashByte) {
        return (hashByte & 0xff) + 0x100;
    }
}
