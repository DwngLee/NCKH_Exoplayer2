package com.google.android.exoplayer2.model;

import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class QoeLogger {
    private static final String FILE_NAME = "qoe_data.csv";
    private static File logFile;

    public static void init(File directory) {
        logFile = new File(directory, FILE_NAME);
    }

    public static void logBitrateData(float lowestBitrateNonBlacklistedIndex, float bufferedDurationUs, float newQoe) {
        String message = String.format("%f,%f,%f", lowestBitrateNonBlacklistedIndex, bufferedDurationUs, newQoe);
        logToFile(message);
    }

    private static void logToFile(String message) {
        if (logFile == null) {
            Log.e("DataLogger", "Log file not initialized. Call init() first.");
            return;
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
            out.println(message);
        } catch (IOException e) {
            Log.e("DataLogger", "Error writing to log file", e);
        }
    }
}
