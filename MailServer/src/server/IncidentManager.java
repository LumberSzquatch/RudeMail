package server;

import java.io.*;
import java.util.Date;

public class IncidentManager {

    private static final String LOG_FILE = "/.server_log";

    public IncidentManager() {
        File logFolder = new File("logs");
        if (!logFolder.exists()) {
            logFolder.mkdir();
        }

        File log = new File("logs" + LOG_FILE);
        if (!log.exists()) {
            try {
                log.createNewFile();
            } catch (IOException e) {
                System.err.println("Failed to create log file");
            }
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
        if (!log.exists()) {
            try {
                log.createNewFile();
            } catch (IOException e) {
                System.err.println("Failed to create log file");
            }
        }

        try (FileWriter fw = new FileWriter(log, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter printWriter = new PrintWriter(bw))
        {
            printWriter.println(new Date() + ": "
                    + fromIp
                    + " "
                    + toIp
                    + " "
                    + protocolCommand
                    + " "
                    + messageCode);
        } catch (IOException e) {
            System.err.println("Failed to write to log file");
        }
    }
}
