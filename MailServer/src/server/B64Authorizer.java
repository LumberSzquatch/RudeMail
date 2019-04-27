package server;

public class B64Authorizer {

    public B64Authorizer(){

    }

    public static String encode(String request) {
        return "";
    }

    public static String decode(String request) {
        return request;
    }

    public static boolean passwordValid(String b64Password) {
        return b64Password.equals("letmein");
    }
}
