package server;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

public class Email {

    private Date timeStamp;
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
    private String fromField;
    private String recipientField;
    private String emailData;

    public Email() {
        timeStamp = new Date();
        fromField = "";
        recipientField = "";
        emailData = "";
    }

    public Date getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getFromField() {
        return fromField;
    }

    public void setFromField(String fromField) {
        this.fromField = fromField;
    }

    public String getRecipientField() {
        return recipientField.split("@")[0];
    }

    public void setRecipientField(String recipientField) {
        this.recipientField = recipientField;
    }

    public String getEmailData() {
        return emailData;
    }

    public void setEmailData(String emailData) {
        this.emailData = emailData;
    }

    public void setFieldsFromRawFile(Scanner fileScanner) {
        try {
            // todo: substrings may end up not being necessary depending on TCP adjustments
            setTimeStamp(DATE_FORMAT.parse(fileScanner.nextLine().substring(6)));
            setFromField(fileScanner.nextLine().substring(6));
            setRecipientField(fileScanner.nextLine().substring(4));

            StringBuilder data = new StringBuilder();
            while (fileScanner.hasNextLine()) {
                data.append(fileScanner.nextLine());
                data.append("\n");
            }
            setEmailData(data.toString());
        } catch (ParseException ex) {
            System.err.println("Unexpectedly unable to parse date out of email");
        }
    }

    @Override
    public String toString() {
        return "Date: " + timeStamp + "\n"
                + "From: " + fromField + "\n"
                + "To: " + recipientField + "\n"
                + emailData + "\n";
    }
}
