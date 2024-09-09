package com.google.android.exoplayer2.demo;

import android.os.Handler;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.Renderer;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.upstream.Allocator;
import com.google.android.exoplayer2.upstream.DefaultAllocator;
import com.google.android.exoplayer2.util.PriorityTaskManager;
import com.google.android.exoplayer2.util.Util;

/**
 * The default {@link LoadControl} implementation.
 */
public class CustomLoadControl implements LoadControl {

    /**
     * The default minimum duration of media that the player will attempt to ensure is buffered at all
     * times, in milliseconds.
     */
    public static final int DEFAULT_MIN_BUFFER_MS = 15000;

    /**
     * The default maximum duration of media that the player will attempt to buffer, in milliseconds.
     */
    public static final int DEFAULT_MAX_BUFFER_MS = 50000;

    /**
     * The default duration of media that must be buffered for playback to start or resume following a
     * user action such as a seek, in milliseconds.
     */
    public static final int DEFAULT_BUFFER_FOR_PLAYBACK_MS = 2500;

    /**
     * The default duration of media that must be buffered for playback to resume after a rebuffer,
     * in milliseconds. A rebuffer is defined to be caused by buffer depletion rather than a user
     * action.
     */
    public static final int DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS  = 5000;

    /**
     * The default target buffer size in bytes. When set to {@link C#LENGTH_UNSET}, the load control
     * automatically determines its target buffer size.
     */
    public static final int DEFAULT_TARGET_BUFFER_BYTES = C.LENGTH_UNSET;

    /** The default prioritization of buffer time constraints over size constraints. */
    public static final boolean DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS = true;

    public static int VIDEO_BUFFER_SCALE_UP_FACTOR = 4;

    public static final int LOADING_PRIORITY = 0;

    private final DefaultAllocator allocator;

    private final long minBufferUs;
    private final long maxBufferUs;
    private final long bufferForPlaybackUs;
    private final long bufferForPlaybackAfterRebufferUs;
    private final int targetBufferBytesOverwrite;
    private final boolean prioritizeTimeOverSizeThresholds;
    private final PriorityTaskManager priorityTaskManager;

    private int targetBufferSize;
    private boolean isBuffering;

    private EventListener bufferedDurationListener;
    private Handler eventHandler;

    private static final int ABOVE_HIGH_WATERMARK = 0;
    private static final int BETWEEN_WATERMARKS = 1;
    private static final int BELOW_LOW_WATERMARK = 2;


    /**
     * Constructs a new instance, using the {@code DEFAULT_*} constants defined in this class.
     */
    public CustomLoadControl(EventListener listener, Handler handler) {
        this(new DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE));
        bufferedDurationListener = listener;
        eventHandler = handler;
    }


    /**
     * Constructs a new instance, using the {@code DEFAULT_*} constants defined in this class.
     *
     * @param allocator The {@link DefaultAllocator} used by the loader.
     */




    public CustomLoadControl(DefaultAllocator allocator) {
        this(
                allocator,
                DEFAULT_MIN_BUFFER_MS,
                DEFAULT_MAX_BUFFER_MS,
                DEFAULT_BUFFER_FOR_PLAYBACK_MS,
                DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS,
                DEFAULT_TARGET_BUFFER_BYTES,
                DEFAULT_PRIORITIZE_TIME_OVER_SIZE_THRESHOLDS);
    }

    /**
     * Constructs a new instance.
     *
     * @param allocator The {@link DefaultAllocator} used by the loader.
     * @param minBufferMs The minimum duration of media that the player will attempt to ensure is
     *     buffered at all times, in milliseconds.
     * @param maxBufferMs The maximum duration of media that the player will attempt buffer, in
     *     milliseconds.
     * @param bufferForPlaybackMs The duration of media that must be buffered for playback to start or
     *     resume following a user action such as a seek, in milliseconds.
     * @param bufferForPlaybackAfterRebufferMs The default duration of media that must be buffered for
     *     playback to resume after a rebuffer, in milliseconds. A rebuffer is defined to be caused by
     *     buffer depletion rather than a user action.
     * @param targetBufferBytes The target buffer size in bytes. If set to {@link C#LENGTH_UNSET}, the
     *     target buffer size will be calculated using {@link #calculateTargetBufferSize(Renderer[],
     *     TrackSelectionArray)}.
     * @param prioritizeTimeOverSizeThresholds Whether the load control prioritizes buffer time
     */
    public CustomLoadControl(
            DefaultAllocator allocator,
            int minBufferMs,
            int maxBufferMs,
            int bufferForPlaybackMs,
            int bufferForPlaybackAfterRebufferMs,
            int targetBufferBytes,
            boolean prioritizeTimeOverSizeThresholds) {
        this(
                allocator,
                minBufferMs,
                maxBufferMs,
                bufferForPlaybackMs,
                bufferForPlaybackAfterRebufferMs,
                targetBufferBytes,
                prioritizeTimeOverSizeThresholds,
                null);
    }

    /**
     * Constructs a new instance.
     *
     * @param allocator The {@link DefaultAllocator} used by the loader.
     * @param minBufferMs The minimum duration of media that the player will attempt to ensure is
     *     buffered at all times, in milliseconds.
     * @param maxBufferMs The maximum duration of media that the player will attempt buffer, in
     *     milliseconds.
     * @param bufferForPlaybackMs The duration of media that must be buffered for playback to start or
     *     resume following a user action such as a seek, in milliseconds.
     * @param bufferForPlaybackAfterRebufferMs The default duration of media that must be buffered for
     *     playback to resume after a rebuffer, in milliseconds. A rebuffer is defined to be caused by
     *     buffer depletion rather than a user action.
     * @param targetBufferBytes The target buffer size in bytes. If set to {@link C#LENGTH_UNSET}, the
     *     target buffer size will be calculated using {@link #calculateTargetBufferSize(Renderer[],
     *     TrackSelectionArray)}.
     * @param prioritizeTimeOverSizeThresholds Whether the load control prioritizes buffer time
     *     constraints over buffer size constraints.
     * @param priorityTaskManager If not null, registers itself as a task with priority {@link
     *     C#PRIORITY_PLAYBACK} during loading periods, and unregisters itself during draining
     */
    public CustomLoadControl(
            DefaultAllocator allocator,
            int minBufferMs,
            int maxBufferMs,
            int bufferForPlaybackMs,
            int bufferForPlaybackAfterRebufferMs,
            int targetBufferBytes,
            boolean prioritizeTimeOverSizeThresholds,
            PriorityTaskManager priorityTaskManager) {
        this.allocator = allocator;
        minBufferUs = minBufferMs * VIDEO_BUFFER_SCALE_UP_FACTOR * 1000L;
        maxBufferUs = maxBufferMs * VIDEO_BUFFER_SCALE_UP_FACTOR * 1000L;
        bufferForPlaybackUs = bufferForPlaybackMs * 1000L;
        bufferForPlaybackAfterRebufferUs = bufferForPlaybackAfterRebufferMs * 1000L;
        targetBufferBytesOverwrite = targetBufferBytes;
        this.prioritizeTimeOverSizeThresholds = prioritizeTimeOverSizeThresholds;
        this.priorityTaskManager = priorityTaskManager;
    }

    @Override
    public void onPrepared() {
        reset(false);
    }

    @Override
    public void onTracksSelected(Renderer[] renderers, TrackGroupArray trackGroups,
                                 TrackSelectionArray trackSelections) {
        targetBufferSize =
                targetBufferBytesOverwrite == C.LENGTH_UNSET
                        ? calculateTargetBufferSize(renderers, trackSelections)
                        : targetBufferBytesOverwrite;
        allocator.setTargetBufferSize(targetBufferSize);
    }

    @Override
    public void onStopped() {
        reset(true);
    }

    @Override
    public void onReleased() {
        reset(true);
    }

    @Override
    public Allocator getAllocator() {
        return allocator;
    }

    @Override
    public long getBackBufferDurationUs() {
        return 0;
    }

    @Override
    public boolean retainBackBufferFromKeyframe() {
        return false;
    }

    @Override
    public boolean shouldContinueLoading(final long bufferedDurationUs, float playbackSpeed) {

    int bufferTimeState = getBufferTimeState(bufferedDurationUs);
    boolean targetBufferSizeReached = allocator.getTotalBytesAllocated() >= targetBufferSize;
    boolean wasBuffering = isBuffering;
    if (prioritizeTimeOverSizeThresholds){
        isBuffering = bufferTimeState == BELOW_LOW_WATERMARK
                || (bufferTimeState == BETWEEN_WATERMARKS
                /*
                 * commented below line to achieve drip-feeding method for better caching. once you are below maxBufferUs, do fetch immediately.
                 * Added by Tran Tien Anh
                 */
                /* && isBuffering */
                && !targetBufferSizeReached);
    }

    if (priorityTaskManager != null && isBuffering != wasBuffering) {
      if (isBuffering) {
        priorityTaskManager.add(LOADING_PRIORITY);
      } else {
        priorityTaskManager.remove(LOADING_PRIORITY);
      }
    }
      if (null != bufferedDurationListener && null != eventHandler)
          eventHandler.post(new Runnable() {
              @Override
              public void run() {
                  bufferedDurationListener.onBufferedDurationSample(bufferedDurationUs);
              }
          });
    //Log.e("DLC","current buff Dur: "+bufferedDurationUs+",max buff:" + maxBufferUs +" shouldContinueLoading: "+isBuffering);
    return isBuffering;

    }

    //@Override
//    public boolean shouldContinueLoading(final long bufferedDurationUs, float playbackSpeed) {
//        int bufferTimeState = getBufferTimeState(bufferedDurationUs);
//
//        boolean targetBufferSizeReached = allocator.getTotalBytesAllocated() >= targetBufferSize;
//        boolean wasBuffering = isBuffering;
//        if (prioritizeTimeOverSizeThresholds) {
//            isBuffering =
//                    bufferTimeState == BELOW_LOW_WATERMARK || (bufferTimeState == BETWEEN_WATERMARKS && isBuffering && !targetBufferSizeReached);
//        } else {
//            isBuffering =
//                    !targetBufferSizeReached && (bufferTimeState == BELOW_LOW_WATERMARK || (bufferTimeState == BETWEEN_WATERMARKS && isBuffering));
//        }
//        if (priorityTaskManager != null && isBuffering != wasBuffering) {
//            if (isBuffering) {
//                priorityTaskManager.add(C.PRIORITY_PLAYBACK);
//            } else {
//                priorityTaskManager.remove(C.PRIORITY_PLAYBACK);
//            }
//        }
//        //Add by Tran Tien Anh
//        if (null != bufferedDurationListener && null != eventHandler)
//            eventHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    bufferedDurationListener.onBufferedDurationSample(bufferedDurationUs);
//                }
//            });
//        //Log.e("DLC","current buff Dur: "+bufferedDurationUs+",max buff:" + maxBufferUs +" shouldContinueLoading: "+isBuffering);
//
//        return isBuffering;
//    }





    @Override
    public boolean shouldStartPlayback(
            long bufferedDurationUs, float playbackSpeed, boolean rebuffering) {
        bufferedDurationUs = Util.getPlayoutDurationForMediaDuration(bufferedDurationUs, playbackSpeed);
        long minBufferDurationUs = rebuffering ? bufferForPlaybackAfterRebufferUs : bufferForPlaybackUs;
        return minBufferDurationUs <= 0
                || bufferedDurationUs >= minBufferDurationUs
                || (!prioritizeTimeOverSizeThresholds
                && allocator.getTotalBytesAllocated() >= targetBufferSize);
    }

    /**
     * Calculate target buffer size in bytes based on the selected tracks. The player will try not to
     * exceed this target buffer. Only used when {@code targetBufferBytes} is {@link C#LENGTH_UNSET}.
     *
     * @param renderers The renderers for which the track were selected.
     * @param trackSelectionArray The selected tracks.
     * @return The target buffer size in bytes.
     */
    protected int calculateTargetBufferSize(
            Renderer[] renderers, TrackSelectionArray trackSelectionArray) {
        int targetBufferSize = 0;
        for (int i = 0; i < renderers.length; i++) {
            if (trackSelectionArray.get(i) != null) {
                targetBufferSize += Util.getDefaultBufferSize(renderers[i].getTrackType());
                if (renderers[i].getTrackType() == C.TRACK_TYPE_VIDEO)
                    targetBufferSize *= VIDEO_BUFFER_SCALE_UP_FACTOR;
            }
        }
        return targetBufferSize;
    }

    private int getBufferTimeState(long bufferedDurationUs) {
        return bufferedDurationUs > maxBufferUs ? ABOVE_HIGH_WATERMARK
                : (bufferedDurationUs < minBufferUs ? BELOW_LOW_WATERMARK : BETWEEN_WATERMARKS);
    }
    private void reset(boolean resetAllocator) {
        targetBufferSize = 0;
        if (priorityTaskManager != null && isBuffering) {
            priorityTaskManager.remove(C.PRIORITY_PLAYBACK);
        }
        isBuffering = false;
        if (resetAllocator) {
            allocator.reset();
        }
    }
    //Add by Tran Tien Anh
    public interface EventListener {
        void onBufferedDurationSample(long bufferedDurationUs);
    }

}
