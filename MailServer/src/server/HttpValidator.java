package server;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

public class HttpValidator {

    private static final String HTTP_200 = "200 OK";
    private static final String HTTP_400 = "400 Bad Request";
    private static final String HTTP_404 = "404 Not Found";
    private static final String HTTP_VERSION = "HTTP/1.1";

    private static final int GET_LINE = 0;
    private static final int HOST_LINE = 1;
    private static final int COUNT_LINE = 2;
    private static final int GET_REQUEST_LENGTH = 3;
    private static final int GET_LOCATION = 1;
    private static final int COUNT_VALUE = 1;

    private String getLocation;
    private int countValue;

    public HttpValidator(){
        getLocation = "";
        countValue = -1;
    }

    public String validatedResponse(String request) throws UnknownHostException {
        String responseCode = determineResponseCode(request);
        if (responseCode.equals(HTTP_200)) {
            StringBuilder responseBody = new StringBuilder();
            try {
                for (Email email : EmailDBMS.select(getLocation, countValue)) {
                    responseBody.append(email.toString());
                }
            } catch (FileNotFoundException | NullPointerException ex) {
                return HTTP_404;
            }
            return okResponse(responseBody.toString());
        }
        return HTTP_400;
    }

    private String determineResponseCode(String request) {

        String[] getRequest = request.split("\n");

        // GET Request not in proper format
        if (getRequest.length < GET_REQUEST_LENGTH) {
            return HTTP_400;
        }

        getLocation = EmailDBMS.ROOT_DIRECTORY + "/" + getRequest[GET_LINE].split("/")[GET_LOCATION];

        try {
            countValue = Integer.parseInt(getRequest[COUNT_LINE].split(":")[COUNT_VALUE]);
            if (countValue < 1) {
                return HTTP_400;
            }
        } catch (NumberFormatException nfe) {
            return HTTP_400;
        }
        return HTTP_200;
    }

    private String okResponse(String responseBody) throws UnknownHostException {
        return getResponseHeader() + responseBody;
    }

    private String getResponseHeader() throws UnknownHostException {
        return HTTP_VERSION + " " + HTTP_200 + "\n"
                + "Server: " + InetAddress.getLocalHost() + "\n"
                + "Last-Modified: " + new Date() + "\n"
                + "Count: " + countValue + "\n"
                + "Content-Type: text/plain" + "\n"
                + "Message: " + countValue + "\n\n";
    }

}
