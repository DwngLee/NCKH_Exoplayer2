/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.trackselection;



import android.util.Log;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.model.QoeLogger;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.chunk.MediaChunk;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A bandwidth based adaptive {@link TrackSelection}, whose selected track is updated to be the one
 * of highest quality given the current network conditions and the state of the buffer.
 */
public class AdaptiveTrackSelection extends BaseTrackSelection {

  /**
   * Factory for {@link AdaptiveTrackSelection} instances.
   */

  public static final class Factory implements TrackSelection.Factory {

    private  BandwidthMeter bandwidthMeter;
    private  int maxInitialBitrate;
    private  int minDurationForQualityIncreaseMs;
    private  int maxDurationForQualityDecreaseMs;
    private int minDurationToRetainAfterDiscardMs;
    private  float bandwidthFraction;
    private  float bufferedFractionToLiveEdgeForQualityIncrease;
    private  long minTimeBetweenBufferReevaluationMs;
    private  Clock clock;






    /**
     * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
     */




    public Factory(BandwidthMeter bandwidthMeter) {
      this(
          bandwidthMeter,
          DEFAULT_MAX_INITIAL_BITRATE,
          DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS,
          DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
          DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS,
          DEFAULT_BANDWIDTH_FRACTION,
          DEFAULT_BUFFERED_FRACTION_TO_LIVE_EDGE_FOR_QUALITY_INCREASE,
          DEFAULT_MIN_TIME_BETWEEN_BUFFER_REEVALUTATION_MS,
          Clock.DEFAULT);
    }

    /**
     * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
     * @param maxInitialBitrate The maximum bitrate in bits per second that should be assumed
     *     when a bandwidth estimate is unavailable.
     * @param minDurationForQualityIncreaseMs The minimum duration of buffered data required for
     *     the selected track to switch to one of higher quality.
     * @param maxDurationForQualityDecreaseMs The maximum duration of buffered data required for
     *     the selected track to switch to one of lower quality.
     * @param minDurationToRetainAfterDiscardMs When switching to a track of significantly higher
     *     quality, the selection may indicate that media already buffered at the lower quality can
     *     be discarded to speed up the switch. This is the minimum duration of media that must be
     *     retained at the lower quality.
     * @param bandwidthFraction The fraction of the available bandwidth that the selection should
     *     consider available for use. Setting to a value less than 1 is recommended to account
     *     for inaccuracies in the bandwidth estimator.
     */
    public Factory(BandwidthMeter bandwidthMeter, int maxInitialBitrate,
        int minDurationForQualityIncreaseMs, int maxDurationForQualityDecreaseMs,
        int minDurationToRetainAfterDiscardMs, float bandwidthFraction) {
      this(
          bandwidthMeter,
          maxInitialBitrate,
          minDurationForQualityIncreaseMs,
          maxDurationForQualityDecreaseMs,
          minDurationToRetainAfterDiscardMs,
          bandwidthFraction,
          DEFAULT_BUFFERED_FRACTION_TO_LIVE_EDGE_FOR_QUALITY_INCREASE,
          DEFAULT_MIN_TIME_BETWEEN_BUFFER_REEVALUTATION_MS,
          Clock.DEFAULT);
    }

    /**
     * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
     * @param maxInitialBitrate The maximum bitrate in bits per second that should be assumed when a
     *     bandwidth estimate is unavailable.
     * @param minDurationForQualityIncreaseMs The minimum duration of buffered data required for the
     *     selected track to switch to one of higher quality.
     * @param maxDurationForQualityDecreaseMs The maximum duration of buffered data required for the
     *     selected track to switch to one of lower quality.
     * @param minDurationToRetainAfterDiscardMs When switching to a track of significantly higher
     *     quality, the selection may indicate that media already buffered at the lower quality can
     *     be discarded to speed up the switch. This is the minimum duration of media that must be
     *     retained at the lower quality.
     * @param bandwidthFraction The fraction of the available bandwidth that the selection should
     *     consider available for use. Setting to a value less than 1 is recommended to account for
     *     inaccuracies in the bandwidth estimator.
     * @param bufferedFractionToLiveEdgeForQualityIncrease For live streaming, the fraction of the
     *     duration from current playback position to the live edge that has to be buffered before
     *     the selected track can be switched to one of higher quality. This parameter is only
     *     applied when the playback position is closer to the live edge than {@code
     *     minDurationForQualityIncreaseMs}, which would otherwise prevent switching to a higher
     *     quality from happening.
     * @param minTimeBetweenBufferReevaluationMs The track selection may periodically reevaluate its
     *     buffer and discard some chunks of lower quality to improve the playback quality if
     *     network conditions have changed. This is the minimum duration between 2 consecutive
     *     buffer reevaluation calls.
     * @param clock A {@link Clock}.
     */
    public Factory(
        BandwidthMeter bandwidthMeter,
        int maxInitialBitrate,
        int minDurationForQualityIncreaseMs,
        int maxDurationForQualityDecreaseMs,
        int minDurationToRetainAfterDiscardMs,
        float bandwidthFraction,
        float bufferedFractionToLiveEdgeForQualityIncrease,
        long minTimeBetweenBufferReevaluationMs,
        Clock clock) {
      this.bandwidthMeter = bandwidthMeter;
      this.maxInitialBitrate = maxInitialBitrate;
      this.minDurationForQualityIncreaseMs = minDurationForQualityIncreaseMs;
      this.maxDurationForQualityDecreaseMs = maxDurationForQualityDecreaseMs;
      this.minDurationToRetainAfterDiscardMs = minDurationToRetainAfterDiscardMs;
      this.bandwidthFraction = bandwidthFraction;
      this.bufferedFractionToLiveEdgeForQualityIncrease =
          bufferedFractionToLiveEdgeForQualityIncrease;
      this.minTimeBetweenBufferReevaluationMs = minTimeBetweenBufferReevaluationMs;
      this.clock = clock;

    }

    @Override
    public AdaptiveTrackSelection createTrackSelection(TrackGroup group, int... tracks) {
      return new AdaptiveTrackSelection(
          group,
          tracks,
          bandwidthMeter,
          maxInitialBitrate,
          minDurationForQualityIncreaseMs,
          maxDurationForQualityDecreaseMs,
          minDurationToRetainAfterDiscardMs,
          bandwidthFraction,
          bufferedFractionToLiveEdgeForQualityIncrease,
          minTimeBetweenBufferReevaluationMs,
          clock);
    }

  }

  public static final int DEFAULT_MAX_INITIAL_BITRATE = 800000;
  public static final int DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS = 10000;
  public static final int DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS = 25000;
  public static final int DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS = 25000;
  public static final float DEFAULT_BANDWIDTH_FRACTION = 0.75f;
  public static final float DEFAULT_BUFFERED_FRACTION_TO_LIVE_EDGE_FOR_QUALITY_INCREASE = 0.75f;
  public static final long DEFAULT_MIN_TIME_BETWEEN_BUFFER_REEVALUTATION_MS = 2000;

  private  BandwidthMeter bandwidthMeter;
  private  int maxInitialBitrate;
  private  long minDurationForQualityIncreaseUs;
  private  long maxDurationForQualityDecreaseUs;
  private  long minDurationToRetainAfterDiscardUs;
  private  float bandwidthFraction;
  private  float bufferedFractionToLiveEdgeForQualityIncrease;
  private  long minTimeBetweenBufferReevaluationMs;
  private  Clock clock;

  private float playbackSpeed;
  private int selectedIndex;
  private int reason;
  private long lastBufferEvaluationMs;

  private  long bitrateNet=0;

  private double  sumQoE = 0;

  //Tien: danh sach quan ly Bufferduration
  ArrayList<Long> bufferList = new ArrayList<Long>();





  /**
   * @param group The {@link TrackGroup}.
   * @param tracks The indices of the selected tracks within the {@link TrackGroup}. Must not be
   *     empty. May be in any order.
   * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
   */

  public AdaptiveTrackSelection(){};

  public AdaptiveTrackSelection(TrackGroup group, int[] tracks,
      BandwidthMeter bandwidthMeter) {
    this(
        group,
        tracks,
        bandwidthMeter,
        DEFAULT_MAX_INITIAL_BITRATE,
        DEFAULT_MIN_DURATION_FOR_QUALITY_INCREASE_MS,
        DEFAULT_MAX_DURATION_FOR_QUALITY_DECREASE_MS,
        DEFAULT_MIN_DURATION_TO_RETAIN_AFTER_DISCARD_MS,
        DEFAULT_BANDWIDTH_FRACTION,
        DEFAULT_BUFFERED_FRACTION_TO_LIVE_EDGE_FOR_QUALITY_INCREASE,
        DEFAULT_MIN_TIME_BETWEEN_BUFFER_REEVALUTATION_MS,
        Clock.DEFAULT);
  }

  /**
   * @param group The {@link TrackGroup}.
   * @param tracks The indices of the selected tracks within the {@link TrackGroup}. Must not be
   *     empty. May be in any order.
   * @param bandwidthMeter Provides an estimate of the currently available bandwidth.
   * @param maxInitialBitrate The maximum bitrate in bits per second that should be assumed when a
   *     bandwidth estimate is unavailable.
   * @param minDurationForQualityIncreaseMs The minimum duration of buffered data required for the
   *     selected track to switch to one of higher quality.
   * @param maxDurationForQualityDecreaseMs The maximum duration of buffered data required for the
   *     selected track to switch to one of lower quality.
   * @param minDurationToRetainAfterDiscardMs When switching to a track of significantly higher
   *     quality, the selection may indicate that media already buffered at the lower quality can be
   *     discarded to speed up the switch. This is the minimum duration of media that must be
   *     retained at the lower quality.
   * @param bandwidthFraction The fraction of the available bandwidth that the selection should
   *     consider available for use. Setting to a value less than 1 is recommended to account for
   *     inaccuracies in the bandwidth estimator.
   * @param bufferedFractionToLiveEdgeForQualityIncrease For live streaming, the fraction of the
   *     duration from current playback position to the live edge that has to be buffered before the
   *     selected track can be switched to one of higher quality. This parameter is only applied
   *     when the playback position is closer to the live edge than {@code
   *     minDurationForQualityIncreaseMs}, which would otherwise prevent switching to a higher
   *     quality from happening.
   * @param minTimeBetweenBufferReevaluationMs The track selection may periodically reevaluate its
   *     buffer and discard some chunks of lower quality to improve the playback quality if network
   *     condition has changed. This is the minimum duration between 2 consecutive buffer
   *     reevaluation calls.
   */


  public AdaptiveTrackSelection(
      TrackGroup group,
      int[] tracks,
      BandwidthMeter bandwidthMeter,
      int maxInitialBitrate,
      long minDurationForQualityIncreaseMs,
      long maxDurationForQualityDecreaseMs,
      long minDurationToRetainAfterDiscardMs,
      float bandwidthFraction,
      float bufferedFractionToLiveEdgeForQualityIncrease,
      long minTimeBetweenBufferReevaluationMs,
      Clock clock) {
    super(group, tracks);
    this.bandwidthMeter = bandwidthMeter;
    this.maxInitialBitrate = maxInitialBitrate;
    this.minDurationForQualityIncreaseUs = minDurationForQualityIncreaseMs * 1000L;
    this.maxDurationForQualityDecreaseUs = maxDurationForQualityDecreaseMs * 1000L;
    this.minDurationToRetainAfterDiscardUs = minDurationToRetainAfterDiscardMs * 1000L;
    this.bandwidthFraction = bandwidthFraction; //Tỷ lệ chính xác của hàm predict băng thông
    this.bufferedFractionToLiveEdgeForQualityIncrease =
        bufferedFractionToLiveEdgeForQualityIncrease;
    this.minTimeBetweenBufferReevaluationMs = minTimeBetweenBufferReevaluationMs;
    this.clock = clock;
    playbackSpeed = 1f;
    selectedIndex = determineIdealSelectedIndex(Long.MIN_VALUE);
    reason = C.SELECTION_REASON_INITIAL;
    lastBufferEvaluationMs = C.TIME_UNSET;
    QoeLogger.init(new File("/data/user/0/com.google.android.exoplayer2.demo/files"));
  }

  public double newQoeFormular(
          long oldIndexBitrate, // bitrate cua doan chunk
          long newIndexBitrate, // bitrate du doan
          long bufferedDurationUs, // bo dem theo giay
          long bitrateEstimate, // bitrate cua mang
          long timeAChunk // thoi gian cua mot doan chunk
  ){

    double M = 1;
    double T = 3000;

    double differentBitrate = Math.abs(newIndexBitrate - oldIndexBitrate); // do khac biet ve bitrate

    double timeRebuffering = timeAChunk * oldIndexBitrate/bitrateEstimate - bufferedDurationUs;// thoi gian nap lai bo dem

    if(timeRebuffering < 0){ // neu gia tri nho hon 0 tuc la khong bi rebuffering
      timeRebuffering = 0;
    }


    double QoE = oldIndexBitrate - M * differentBitrate - T * timeRebuffering;

    Log.i("QoE","QoE: " + QoE);


    this.sumQoE +=QoE;

    return QoE;
  }

  @Override
  public void enable() {
    lastBufferEvaluationMs = C.TIME_UNSET;
  }

  @Override
  public void onPlaybackSpeed(float playbackSpeed) {
    this.playbackSpeed = playbackSpeed;
  }

  public void setDb(long db){
    Log.e("khuatduc0002::",String.valueOf(db));
    this.bitrateNet=db;
  }

  @Override
  public void updateSelectedTrack(long playbackPositionUs, long bufferedDurationUs,
      long availableDurationUs) {

    Log.e("availableDurationUs000::", String.valueOf(minDurationForQualityIncreaseUs(availableDurationUs)));
    long nowMs = clock.elapsedRealtime();
    // Stash the current selection, then make a new one.
    int currentSelectedIndex = selectedIndex;
    selectedIndex = determineIdealSelectedIndex(nowMs);
    //Tien: su dung ham nay de lay thong tin ve buffer
//    selectedIndex = determineIdealSelectedIndex_org(nowMs,currentSelectedIndex,bufferedDurationUs);
    //QoE tham khao
//    selectedIndex = determineIdealSelectedIndex_refQoE(nowMs,currentSelectedIndex,bufferedDurationUs);
    //QoE de xuat:
    //selectedIndex = determineIdealSelectedIndex_prop(nowMs,currentSelectedIndex,bufferedDurationUs);

    if (selectedIndex == currentSelectedIndex) {
      return;
    }

    if (!isBlacklisted(currentSelectedIndex, nowMs)) {

      // Revert back to the current selection if conditions are not suitable for switching.
      Format currentFormat = getFormat(currentSelectedIndex);
      Format selectedFormat = getFormat(selectedIndex);

      if (selectedFormat.bitrate > currentFormat.bitrate
          && bufferedDurationUs < minDurationForQualityIncreaseUs(availableDurationUs)) {

        // The selected track is a higher quality, but we have insufficient buffer to safely switch
        // up. Defer switching up for now.
        selectedIndex = currentSelectedIndex;
      } else if (selectedFormat.bitrate < currentFormat.bitrate
          && bufferedDurationUs >= maxDurationForQualityDecreaseUs) {

        // The selected track is a lower quality, but we have sufficient buffer to defer switching
        // down for now.
        selectedIndex = currentSelectedIndex;
        //System.out.println("nhanh");
      }
    }
    // If we adapted, update the trigger.
    if (selectedIndex != currentSelectedIndex) {

      reason = C.SELECTION_REASON_ADAPTIVE;
    }
  }
//co dung
  @Override
  public int getSelectedIndex() {
    System.out.println("nhanh14::");
    return selectedIndex;
  }

  @Override
  public int getSelectionReason() {
    return reason;
  }

  @Override
  public Object getSelectionData() {
    return null;
  }

  @Override
  public int evaluateQueueSize(long playbackPositionUs, List<? extends MediaChunk> queue) {
    long nowMs = clock.elapsedRealtime();
    if (lastBufferEvaluationMs != C.TIME_UNSET
        && nowMs - lastBufferEvaluationMs < minTimeBetweenBufferReevaluationMs) {
      return queue.size();
    }
    lastBufferEvaluationMs = nowMs;
    if (queue.isEmpty()) {
      return 0;
    }

    int queueSize = queue.size();
    MediaChunk lastChunk = queue.get(queueSize - 1);
    long playoutBufferedDurationBeforeLastChunkUs =
        Util.getPlayoutDurationForMediaDuration(
            lastChunk.startTimeUs - playbackPositionUs, playbackSpeed);
    if (playoutBufferedDurationBeforeLastChunkUs < minDurationToRetainAfterDiscardUs) {
      return queueSize;
    }
    int idealSelectedIndex = determineIdealSelectedIndex(nowMs);

    Format idealFormat = getFormat(idealSelectedIndex);
    // If the chunks contain video, discard from the first SD chunk beyond
    // minDurationToRetainAfterDiscardUs whose resolution and bitrate are both lower than the ideal
    // track.
    for (int i = 0; i < queueSize; i++) {
      MediaChunk chunk = queue.get(i);
      Format format = chunk.trackFormat;
      long mediaDurationBeforeThisChunkUs = chunk.startTimeUs - playbackPositionUs;
      long playoutDurationBeforeThisChunkUs =
          Util.getPlayoutDurationForMediaDuration(mediaDurationBeforeThisChunkUs, playbackSpeed);
      if (playoutDurationBeforeThisChunkUs >= minDurationToRetainAfterDiscardUs
          && format.bitrate < idealFormat.bitrate
          && format.height != Format.NO_VALUE && format.height < 720
          && format.width != Format.NO_VALUE && format.width < 1280
          && format.height < idealFormat.height) {
        return i;
      }
    }
//    System.out.println();
    return queueSize;
  }



  /**
   * Computes the ideal selected index ignoring buffer health.
   *
   * @param nowMs The current time in the timebase of {@link Clock#elapsedRealtime()}, or {@link
   *     Long#MIN_VALUE} to ignore blacklisting.
   */


  private int determineIdealSelectedIndex(long nowMs) {
    long bitrateEstimate = bandwidthMeter.getBitrateEstimate();

    long effectiveBitrate = bitrateEstimate == BandwidthMeter.NO_ESTIMATE
        ? maxInitialBitrate : (long) (bitrateEstimate * bandwidthFraction);
    int lowestBitrateNonBlacklistedIndex = 0;
      //Log.e("length:::",String.valueOf(length));
    for (int i = 0; i < length; i++) {
      if (nowMs == Long.MIN_VALUE || !isBlacklisted(i, nowMs)) {
        Format format = getFormat(i);
       // System.out.println("format::"+format);
        if (Math.round(format.bitrate * playbackSpeed) <= effectiveBitrate) {
          Log.e("bitrateEstimate0\t ",String.valueOf(i));
          return i;
        } else {
          lowestBitrateNonBlacklistedIndex = i;
        }
      }
    }
    Log.e("bitrateEstimate0\t", String.valueOf(lowestBitrateNonBlacklistedIndex));
    return lowestBitrateNonBlacklistedIndex;
  }

  //Tien: Sua ham tren de lay thong tin ve buffer
  private int determineIdealSelectedIndex_org(long nowMs,int previousSelectedIndex,long bufferedDurationUs) {
    long bitrateEstimate = bandwidthMeter.getBitrateEstimate();

    long effectiveBitrate = bitrateEstimate == BandwidthMeter.NO_ESTIMATE
            ? maxInitialBitrate : (long) (bitrateEstimate * bandwidthFraction);

    int lowestBitrateNonBlacklistedIndex = 0;
    double M = 4.3;
    Format currentFormat = getFormat(previousSelectedIndex);
    //Log.e("length:::",String.valueOf(length));
    for (int i = 0; i < length; i++) {
      if (nowMs == Long.MIN_VALUE || !isBlacklisted(i, nowMs)) {
        Format format = getFormat(i);
        if (Math.round(format.bitrate * playbackSpeed) <= effectiveBitrate) {
          Format newFormat = getFormat(i);
          double T = newFormat.bitrate * 2 / effectiveBitrate - bufferedDurationUs / 1000000;
          double QoE = newFormat.bitrate / 1000 - M * T * 100 - Math.abs(newFormat.bitrate / 1000 - currentFormat.bitrate / 1000);
          double newQoe = newQoeFormular(
                  currentFormat.bitrate,
                  newFormat.bitrate,
                  bufferedDurationUs/1000000,
                  bitrateEstimate,
                  2);
          Log.e("qoe value::", "\t" + currentFormat.bitrate + "\t" +  newFormat.bitrate + "\t" + (bufferedDurationUs/1000000) + "\t" + Math.round(QoE));
          return i;
        }
        else {
          lowestBitrateNonBlacklistedIndex = i;
        }
      }
    }
    Format newFormat = getFormat(lowestBitrateNonBlacklistedIndex);
    double T = newFormat.bitrate * 2 / effectiveBitrate - bufferedDurationUs / 1000000;
    double QoE = newFormat.bitrate / 1000 - M * T * 100 - Math.abs(newFormat.bitrate / 1000 - currentFormat.bitrate / 1000);
    double newQoe = newQoeFormular(
            currentFormat.bitrate,
            newFormat.bitrate,
            bufferedDurationUs/1000000,
            bitrateEstimate,
            2);
    Log.e("qoe value::", "\t" + currentFormat.bitrate + "\t" +  newFormat.bitrate + "\t" + (bufferedDurationUs/1000000) + "\t" + Math.round(QoE));
    return lowestBitrateNonBlacklistedIndex;
  }

  private int determineIdealSelectedIndex_refQoE(long nowMs,int previousSelectedIndex,long bufferedDurationUs) {
    bufferList.add(bufferedDurationUs);


    long bitrateEstimate = bandwidthMeter.getBitrateEstimate();

    long effectiveBitrate = bitrateEstimate == BandwidthMeter.NO_ESTIMATE
            ? maxInitialBitrate : (long) (bitrateEstimate * bandwidthFraction);

    int lowestBitrateNonBlacklistedIndex = 0;

    double M = 4.3;
    double QoEmax=0;
    int indexSelected=0;
    Format currentFormat = getFormat(previousSelectedIndex);

    for (int i = 0; i < length; i++) {
      if(bufferList.size()>2){
        if(bufferedDurationUs<bufferList.get(bufferList.size()-2)){
          M=M*0.5;
        }
        else
          M=M*2;
      }

      if (nowMs == Long.MIN_VALUE || !isBlacklisted(i, nowMs)) {
        Format format = getFormat(i);
        // System.out.println("format::"+format);
        if (Math.round(format.bitrate * playbackSpeed) <= effectiveBitrate) {
          Format fm = getFormat(i);
          double T = fm.bitrate * 2 / effectiveBitrate - bufferedDurationUs / 1000000;
          double QoE = fm.bitrate / 1000 - M * T * 100 - Math.abs(fm.bitrate / 1000 - currentFormat.bitrate / 1000);
          if(QoE>QoEmax) {
            QoEmax = QoE;
            indexSelected=i;
          }
          Format newFormat = getFormat(indexSelected);
          Log.e("qoe value::", "\t" + currentFormat.bitrate + "\t" +  newFormat.bitrate + "\t" + (bufferedDurationUs/1000000) + "\t" + Math.round(QoE));
          return indexSelected;
        } else {
          lowestBitrateNonBlacklistedIndex = i;
        }
      }
    }
    Format fm = getFormat(lowestBitrateNonBlacklistedIndex);
    double T = fm.bitrate * 2 / effectiveBitrate - bufferedDurationUs / 1000000;
    double QoE = fm.bitrate / 1000 - M * T * 100 - Math.abs(fm.bitrate / 1000 - currentFormat.bitrate / 1000);
    Log.e("qoe value::", "\t" + currentFormat.bitrate + "\t" +  fm.bitrate + "\t" + (bufferedDurationUs/1000000) + "\t" + Math.round(QoE));
    return lowestBitrateNonBlacklistedIndex;
  }

  private int determineIdealSelectedIndex_prop(long nowMs,int previousSelectedIndex,long bufferedDurationUs) {
    long bitrateEstimate = bandwidthMeter.getBitrateEstimate();

    long effectiveBitrate = bitrateEstimate == BandwidthMeter.NO_ESTIMATE
            ? maxInitialBitrate : (long) (bitrateEstimate * bandwidthFraction);

    int lowestBitrateNonBlacklistedIndex = 0;
    double M = 4.3;
    double QoEmax=0;
    int indexSelected=0;
    Format currentFormat = getFormat(previousSelectedIndex);
    for (int i = 0; i < length; i++) {
      if (nowMs == Long.MIN_VALUE || !isBlacklisted(i, nowMs)) {
        Format format = getFormat(i);
        // System.out.println("format::"+format);
        if (Math.round(format.bitrate * playbackSpeed) <= effectiveBitrate) {
          Format fm = getFormat(i);
          double T = fm.bitrate * 2 / effectiveBitrate - bufferedDurationUs / 1000000;
          double QoE = fm.bitrate / 1000 - M * T * 100 - Math.abs(fm.bitrate / 1000 - currentFormat.bitrate / 1000);
          if(QoE>QoEmax) {
            QoEmax = QoE;
            indexSelected=i;
          }
          Log.e("lowestBitrateAllowed", "\t" + String.valueOf(indexSelected) + "\t" + (bufferedDurationUs) + "\t" + Math.round(QoE));
          //Log.e("bitrateEstimate0\t ",String.valueOf(i));
          return indexSelected;
        } else {
          lowestBitrateNonBlacklistedIndex = i;
        }
      }
    }
    Format fm = getFormat(lowestBitrateNonBlacklistedIndex);
    double T = fm.bitrate * 2 / effectiveBitrate - bufferedDurationUs / 1000000;
    double QoE = fm.bitrate / 1000 - M * T * 100 - Math.abs(fm.bitrate / 1000 - currentFormat.bitrate / 1000);
    Log.e("lowestBitrateAllowed", "\t" + lowestBitrateNonBlacklistedIndex + "\t" + (bufferedDurationUs) + "\t" + Math.round(QoE));
    return lowestBitrateNonBlacklistedIndex;
  }


  private long minDurationForQualityIncreaseUs(long availableDurationUs) {
    boolean isAvailableDurationTooShort = availableDurationUs != C.TIME_UNSET
        && availableDurationUs <= minDurationForQualityIncreaseUs;
    return isAvailableDurationTooShort
        ? (long) (availableDurationUs * bufferedFractionToLiveEdgeForQualityIncrease)
        : minDurationForQualityIncreaseUs;
  }

}
