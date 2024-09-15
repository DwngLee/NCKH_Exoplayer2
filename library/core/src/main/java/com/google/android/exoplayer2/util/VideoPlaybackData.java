package com.google.android.exoplayer2.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class VideoPlaybackData {
    private VideoSegments I13;
    private StallingTimeInfo I23;
    private AudioSegments I11;

    public VideoPlaybackData(VideoSegments i13, StallingTimeInfo i23, AudioSegments i11) {
        I13 = i13;
        I23 = i23;
        I11 = i11;
    }

    public AudioSegments getI11() {
        return I11;
    }

    public void setI11(AudioSegments i11) {
        I11 = i11;
    }

    public VideoSegments getI13() {
        return I13;
    }

    public void setI13(VideoSegments i13) {
        I13 = i13;
    }

    public StallingTimeInfo getI23() {
        return I23;
    }

    public void setI23(StallingTimeInfo i23) {
        I23 = i23;
    }

    public VideoPlaybackData(VideoSegments i13, StallingTimeInfo i23) {
        I13 = i13;
        I23 = i23;
    }

    public String toJson(){
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
