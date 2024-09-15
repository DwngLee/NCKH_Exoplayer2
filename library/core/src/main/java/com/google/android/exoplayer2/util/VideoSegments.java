package com.google.android.exoplayer2.util;

import java.util.List;

public class VideoSegments {
    private List<VideoQuality> segments;
    private int streamId;

    // Constructor
    public VideoSegments(List<VideoQuality> segments, int streamId) {
        this.segments = segments;
        this.streamId = streamId;
    }

    // Getters và Setters
    public List<VideoQuality> getVideoQualityList() {
        return segments;
    }

    public void setVideoQualityList(List<VideoQuality> videoQualities) {
        this.segments = videoQualities;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }
}
