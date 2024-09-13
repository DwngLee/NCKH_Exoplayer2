package com.google.android.exoplayer2.demo.services;

public interface BaseBandwidthPredictionModel {
  /**
   * Predict the next bitrate value
   * @param input_array: array of past bitrate values
   * @return the next bitrate value
   */
  float predict(float[] input_array);

  /**
   * Unload model
   */
  void close();
}
