package com.google.android.exoplayer2.demo.models;

public class StallingTime {
    private long startTime;
    private long duration;

    public StallingTime(long startTime, long duration) {
        this.startTime = startTime;
        this.duration = duration;
    }

    // Getters and setters
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    @Override
    public String toString() {
        return "StallingTime{startTime=" + startTime + ", duration=" + duration + "}";
    }
}
