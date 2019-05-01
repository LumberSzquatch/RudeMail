package client;

import java.util.Base64;

public class B64Util {

    public B64Util(){

    }

    public static String encode(String request) {
        return Base64.getEncoder().encodeToString(request.getBytes());
    }
}
