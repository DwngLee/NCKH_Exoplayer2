package com.google.android.exoplayer2.demo.custom_bandwidth_meter;

import android.os.Handler;
import android.util.Log;

import com.google.android.exoplayer2.demo.services.BaseBandwidthPredictionModel;
import com.google.android.exoplayer2.demo.services.Logger;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSpec;
import com.google.android.exoplayer2.upstream.TransferListener;
import com.google.android.exoplayer2.util.Assertions;
import com.google.android.exoplayer2.util.Clock;
import com.google.android.exoplayer2.util.SlidingPercentile;

import java.util.LinkedList;
import java.util.Queue;

public class PredictiveBandwidthMeter implements BandwidthMeter, TransferListener<Object> {
  /**
   * The default maximum weight for the sliding window.
   */
  public static final int DEFAULT_MAX_WEIGHT = 2000;

  private static final int ELAPSED_MILLIS_FOR_ESTIMATE = 2000;
  private static final int BYTES_TRANSFERRED_FOR_ESTIMATE = 512 * 1024;

  private final Handler eventHandler;
  private final EventListener eventListener;
  private final SlidingPercentile slidingPercentile;
  private final Clock clock;

  private int streamCount;
  private long sampleStartTimeMs;
  private long sampleBytesTransferred;

  private long totalElapsedTimeMs;
  private long totalBytesTransferred;
  private long bitrateEstimate;

  // model
  private BaseBandwidthPredictionModel model;

  // store latest bandwidth values
  private int queue_size;
  private Queue<Float> mbps_queue = new LinkedList<>();


  public PredictiveBandwidthMeter(BaseBandwidthPredictionModel model, int input_queue_size) {
    this(null, null, model, input_queue_size);
  }

  public PredictiveBandwidthMeter(Handler eventHandler, EventListener eventListener,
                                  BaseBandwidthPredictionModel model, int input_queue_size) {
    this(eventHandler, eventListener, DEFAULT_MAX_WEIGHT, model, input_queue_size);
  }

  public PredictiveBandwidthMeter(Handler eventHandler, EventListener eventListener, int maxWeight,
                                  BaseBandwidthPredictionModel model, int input_queue_size) {
    this(eventHandler, eventListener, maxWeight, Clock.DEFAULT, model, input_queue_size);
  }

  public PredictiveBandwidthMeter(Handler eventHandler, EventListener eventListener, int maxWeight,Clock clock,
                                  BaseBandwidthPredictionModel model, int input_queue_size) {
    this.eventHandler = eventHandler;
    this.eventListener = eventListener;
    this.slidingPercentile = new SlidingPercentile(maxWeight);
    this.clock = clock;
    this.bitrateEstimate = this.NO_ESTIMATE;
    this.model = model;
    this.queue_size = input_queue_size;
  }

  public float predictNextBandwidth() {
    // get input
    float[] input_array = new float[this.queue_size];
    int i = 0;
    for (Float f : this.mbps_queue)
      input_array[i++] = (f != null ? f : 0);

    // predict and return result
    return this.model.predict(input_array);
  }

  @Override
  public synchronized void onTransferStart(Object source, DataSpec dataSpec) {
    if (streamCount == 0) {
      sampleStartTimeMs = clock.elapsedRealtime();
    }
    streamCount++;
  }

  @Override
  public synchronized void onBytesTransferred(Object source, int bytes) {
    sampleBytesTransferred += bytes;
  }

  @Override
  public synchronized long getBitrateEstimate() {
    return bitrateEstimate;
  }


  @Override
  public synchronized void onTransferEnd(Object source) {
    Assertions.checkState(streamCount > 0);
    long nowMs = clock.elapsedRealtime();
    int sampleElapsedTimeMs = (int) (nowMs - sampleStartTimeMs);
    totalElapsedTimeMs += sampleElapsedTimeMs;
    totalBytesTransferred += sampleBytesTransferred;

    if (sampleElapsedTimeMs > 0) {

      float bitsPerSecond = sampleBytesTransferred * 8000.0f / sampleElapsedTimeMs;
      Log.e("datalog_model::", String.format("\t%s\t%s\t%s",sampleBytesTransferred, sampleElapsedTimeMs, bitsPerSecond));
      // MODEL - Get data
//      // remove oldest value(s) in queue
      while (mbps_queue.size() >= queue_size) {
        mbps_queue.remove();
      }
      // add new value to queue
      while (mbps_queue.size() < queue_size) {
        mbps_queue.add((float) bitsPerSecond / (1024*1024 * 100)); //Do model can chia 100 de chuan hoa
      }

      Log.e("data_inputmodel::", String.format("\t%s", bitsPerSecond / (1024 * 1024)));
//      Logger.logBitrateData((float) sampleBytesTransferred/ (1024*1024), (float) sampleElapsedTimeMs/ 1000, bitsPerSecond / (1024 * 1024 * 8));

      // EXO - Get data
      float bitsPerSecond_Exo = sampleBytesTransferred * 8000.0f / sampleElapsedTimeMs;
      slidingPercentile.addSample((int) Math.sqrt(sampleBytesTransferred), bitsPerSecond_Exo);

      // predict next value
      if ((totalElapsedTimeMs >= ELAPSED_MILLIS_FOR_ESTIMATE
              || totalBytesTransferred >= BYTES_TRANSFERRED_FOR_ESTIMATE) && mbps_queue.size() >= queue_size) {


        // EXO - predict
        float bitrateEstimateFloat = slidingPercentile.getPercentile(0.5f);
//         check if new value is valid
        if (Float.isNaN(bitrateEstimateFloat))
          bitrateEstimateFloat = 0.0f;
        long bitrateEstimate_Exo = (long)bitrateEstimateFloat;
//        this.bitrateEstimate = (long)bitrateEstimateFloat;

//        Log.e("bitratelogexo::", String.format("\t%s\t%s",bitsPerSecond_Exo, bitrateEstimate_Exo));

        // MODEL - predict and convert from mbps to bps
        long  bitratePred = (long) ((this.predictNextBandwidth())  * (8 * 1024 * 1024 * 100)); // Nhan voi 100 do ban dau chuan hoa k ve 100
        this.bitrateEstimate = bitratePred;
        Log.e("model_pred::", String.format("\t%s", bitratePred));
        Log.e("bitratelogtest::", String.format("\t%s,%s,%s",bitsPerSecond / (1024 * 1024), this.bitrateEstimate / (1024 * 1024), bitrateEstimate_Exo / (1024 * 1024)));

      }
    }

    notifyBandwidthSample(sampleElapsedTimeMs, sampleBytesTransferred, this.bitrateEstimate);
    if (--streamCount > 0) {
      sampleStartTimeMs = nowMs;
    }
    sampleBytesTransferred = 0;
  }

  public void notifyBandwidthSample(final int elapsedMs, final long bytes, final long bitrate) {
    if (eventHandler != null && eventListener != null) {
      eventHandler.post(new Runnable() {
        @Override
        public void run() {
          eventListener.onBandwidthSample(elapsedMs, bytes, bitrate);
          AdaptiveTrackSelection adaptiveTrackSelection = new AdaptiveTrackSelection();
          adaptiveTrackSelection.setDb(bitrate);
        }
      });
    }
  }
}
