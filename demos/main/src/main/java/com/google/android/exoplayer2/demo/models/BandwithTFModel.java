package com.google.android.exoplayer2.demo.models;

import android.content.res.AssetFileDescriptor;
import android.content.Context;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class BandwithTFModel implements BaseBandwidthPredictionModel {
    private Interpreter tflite;
    private Context context;
    private String modelFile;
    private int inputLength;

    public BandwithTFModel(Context context, String modelFile, int inputLength) {
        this.context = context;
        this.modelFile = modelFile;
        this.inputLength = inputLength;
        try {
            tflite = new Interpreter(loadModelFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFile);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    @Override
    public float predict(float[] input_array) {
        if (tflite == null) {
            return 0;
        }

        if (input_array.length != inputLength) {
            throw new IllegalArgumentException("Input array length does not match the expected input length");
        }

        float[][] input = new float[1][inputLength];
        System.arraycopy(input_array, 0, input[0], 0, inputLength);

        float[][] output = new float[1][1];
        tflite.run(input, output);
        return output[0][0];
    }

    @Override
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
    }
}