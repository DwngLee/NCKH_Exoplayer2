package com.google.android.exoplayer2.demo.services;

import android.os.Looper;
import android.util.Log;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.demo.models.SegmentInfo;
import com.google.android.exoplayer2.demo.models.StallingTime;
import com.google.android.exoplayer2.demo.models.VideoPlaybackData;

import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MediaSourceEventListener;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.DataSpec;

import java.io.IOException;
import java.util.logging.Handler;


public class VideoQualityTracker implements Player.EventListener, MediaSourceEventListener {
    private SimpleExoPlayer player;
    private long stallStartTime = -1;
    private VideoPlaybackData playbackData;

    public VideoQualityTracker(SimpleExoPlayer player) {
        this.player = player;
        player.addListener(this);
        this.playbackData = new VideoPlaybackData();
    }


    public void onPlaybackUpdate() {
        long currentPosition = player.getCurrentPosition();
        System.out.println("Playback position: " + currentPosition + "ms");
    }

    public VideoPlaybackData getPlaybackData() {
        return playbackData;
    }

    public void release() {
        player.removeListener(this);
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    //#TODO: tính stalling time này chỉ là tương đối. Nếu muốn chính xác thì tính dựa trên buffer và thời gian tải
    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        if (playbackState == Player.STATE_BUFFERING) { //Tìm thời điểm xảy ra rebuffering
            if (stallStartTime == -1) {
                stallStartTime = System.currentTimeMillis();
            }
        } else if (playbackState == Player.STATE_READY) {
            if (stallStartTime != -1) { //Khi hết rebuffering thì sẽ tính duration
                long stallDuration = System.currentTimeMillis() - stallStartTime;
                StallingTime stallingTime = new StallingTime(stallStartTime/1000, stallDuration/1000);
                playbackData.addStallingTime(stallingTime);
                System.out.println("Added stalling time: " + stallingTime);
                stallStartTime = -1;
            }
        }
    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    //gets called every time the video changes or "seeks" to the next in the playlist.
    @Override
    public void onPositionDiscontinuity(int reason) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) {
            System.out.println("Seek occurred. Resetting stall tracking.");
            stallStartTime = -1;
        }
    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    @Override
    public void onLoadStarted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs) {
        System.out.println("Segment started loading: Start time = " + mediaStartTimeMs);
    }

    @Override
    public void onLoadCompleted(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {
        long segmentDuration = mediaEndTimeMs - mediaStartTimeMs;
        Format format = player.getVideoFormat(); // This might not be accurate for the specific segment

        if (format != null) {
            SegmentInfo segmentInfo = new SegmentInfo(
                    format.bitrate,
                    format.codecs,
                    segmentDuration,
                    format.frameRate,
                    format.width + "x" + format.height,
                    mediaStartTimeMs
            );

            playbackData.addSegmentInfo(segmentInfo);
            System.out.println("Added segment info: " + segmentInfo);
        }
    }

    @Override
    public void onLoadCanceled(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded) {

    }

    @Override
    public void onLoadError(DataSpec dataSpec, int dataType, int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaStartTimeMs, long mediaEndTimeMs, long elapsedRealtimeMs, long loadDurationMs, long bytesLoaded, IOException error, boolean wasCanceled) {

    }

    @Override
    public void onUpstreamDiscarded(int trackType, long mediaStartTimeMs, long mediaEndTimeMs) {

    }

    @Override
    public void onDownstreamFormatChanged(int trackType, Format trackFormat, int trackSelectionReason, Object trackSelectionData, long mediaTimeMs) {

    }


}