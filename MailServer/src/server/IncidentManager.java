package server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Date;

public class IncidentManager {

    private static final String LOG_FILE = "/.server_log";

    public IncidentManager() {
        File directory = new File("logs");
        if (!directory.exists()) {
            directory.mkdir();
        }
    }

    public static void initializeSIM() {
        IncidentManager sim = new IncidentManager();
    }

    public static void log(String fromIp, String toIp, String protocolCommand, String messageCode) {

        File logFolder = new File("logs");
        if (!logFolder.exists()) {
            logFolder.mkdir();
        }

        File log = new File("logs" + LOG_FILE);

        try (PrintWriter printWriter = new PrintWriter(log)) {
            printWriter.append(new Date() + ": "
                    + fromIp
                    + " "
                    + toIp
                    + " "
                    + protocolCommand
                    + " "
                    + messageCode);
            printWriter.close();
        } catch (FileNotFoundException ex) {
            System.err.println("Failed to write to log file");
        }
    }
}
