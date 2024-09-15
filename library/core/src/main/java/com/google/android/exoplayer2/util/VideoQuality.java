package com.google.android.exoplayer2.util;

public class VideoQuality {
    private int bitrate;
    private String codec;
    private long duration;
    private float fps;
    private String resolution;
    private long start;


    public VideoQuality(int bitrate, String codec, long duration, float fps, String resolution, long start) {
        this.bitrate = bitrate;
        this.codec = codec;
        this.duration = duration;
        this.fps = fps;
        this.resolution = resolution;
        this.start = start;
    }

    // Getters and setters
    public int getBitrate() { return bitrate; }
    public void setBitrate(int bitrate) { this.bitrate = bitrate; }
    public String getCodec() { return codec; }
    public void setCodec(String codec) { this.codec = codec; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    public float getFps() { return fps; }
    public void setFps(float fps) { this.fps = fps; }
    public String getResolution() { return resolution; }
    public void setResolution(String resolution) { this.resolution = resolution; }
    public long getStart() { return start; }
    public void setStart(long start) { this.start = start; }


    @Override
    public String toString() {
        return "SegmentInfo{bitrate=" + bitrate + ", codec='" + codec + "', duration=" + duration +
                ", fps=" + fps + ", resolution='" + resolution + "', start=" + start + "}";
    }
}
