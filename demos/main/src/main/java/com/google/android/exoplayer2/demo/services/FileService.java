package com.google.android.exoplayer2.demo.services;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class FileService {
    public static void saveJsonToFile(Context context, String jsonString, String fileName) {
        try {
            File dir = new File(context.getFilesDir(), "playback_data");
            Log.e("File dir:::", dir.getPath());
            if (!dir.exists()) {
                dir.mkdir();
            }

            File file = new File(dir, fileName);
            FileWriter writer = new FileWriter(file);
            writer.write(jsonString);
            writer.close();
            System.out.println("JSON data saved to " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String generateFileName() {
        String timestamp = LocalDateTime.now().toString();
        return "EXO_MODEL_playback_data_" + timestamp + ".json";
    }
}
