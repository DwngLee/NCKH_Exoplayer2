package com.google.android.exoplayer2.demo.models;

import java.util.ArrayList;
import java.util.List;

public class VideoPlaybackData {
    private List<StallingTime> stallingTimes;
    private List<SegmentInfo> segmentInfos;

    public VideoPlaybackData() {
        this.stallingTimes = new ArrayList<>();
        this.segmentInfos = new ArrayList<>();
    }

    public void addStallingTime(StallingTime stallingTime) {
        stallingTimes.add(stallingTime);
    }

    public void addSegmentInfo(SegmentInfo segmentInfo) {
        segmentInfos.add(segmentInfo);
    }

    public List<StallingTime> getStallingTimes() {
        return stallingTimes;
    }

    public List<SegmentInfo> getSegmentInfos() {
        return segmentInfos;
    }

    @Override
    public String toString() {
        return "VideoPlaybackData{stallingTimes=" + stallingTimes + ", segmentInfos=" + segmentInfos + "}";
    }
}
