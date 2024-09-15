package com.google.android.exoplayer2.util;

import java.util.List;

public class AudioSegments {
    private List<AudioQuality> segments;
    private int streamId;

    public List<AudioQuality> getSegments() {
        return segments;
    }

    public void setSegments(List<AudioQuality> segments) {
        this.segments = segments;
    }

    public int getStreamId() {
        return streamId;
    }

    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }

    public AudioSegments(List<AudioQuality> segments, int streamId) {
        this.segments = segments;
        this.streamId = streamId;
    }
}
