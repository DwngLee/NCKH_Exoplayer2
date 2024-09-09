package com.google.android.exoplayer2.demo.models;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Logger {
    private static final String FILE_NAME = "bitrate_data.csv";
    private static File logFile;

    public static void init(File directory) {
        logFile = new File(directory, FILE_NAME);
    }

    public static void logBitrateData(float sampleBytesTransferredMB, float sampleElapsedTimeSeconds, float bitsPerSecondMbps) {
        String message = String.format("%f,%f,%f", sampleBytesTransferredMB, sampleElapsedTimeSeconds, bitsPerSecondMbps);
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
