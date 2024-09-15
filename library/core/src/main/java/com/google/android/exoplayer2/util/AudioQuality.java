package com.google.android.exoplayer2.util;

public class AudioQuality {
    private double bitrate;
    private String codec;
    private double duration;
    private double start;

    public AudioQuality(double bitrate, String codec, double duration, double start) {
        this.bitrate = bitrate;
        this.codec = codec;
        this.duration = duration;
        this.start = start;
    }

    public double getBitrate() {
        return bitrate;
    }

    public void setBitrate(double bitrate) {
        this.bitrate = bitrate;
    }

    public String getCodec() {
        return codec;
    }

    public void setCodec(String codec) {
        this.codec = codec;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public double getStart() {
        return start;
    }

    public void setStart(double start) {
        this.start = start;
    }
}
