package com.google.android.exoplayer2.demo.services;

import android.content.Context;

import org.pytorch.IValue;
import org.pytorch.LiteModuleLoader;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BandwidthTorchRecurrentModel implements BaseBandwidthPredictionModel {
    Module torch_model;
    long []input_shape;

    public BandwidthTorchRecurrentModel(Context context, String model_file, long[] input_shape) {
        this.input_shape = input_shape;
        try {
            torch_model = LiteModuleLoader.load(assetFilePath(context, model_file));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public float predict(float[] input_array) {
        Tensor data_tensor = Tensor.fromBlob(input_array, this.input_shape);
        IValue pred_ivalue = torch_model.forward(IValue.from(data_tensor));
        float[] pred_arr = pred_ivalue.toTensor().getDataAsFloatArray();
        return pred_arr[0];
    }

    @Override
    public void close() {
        torch_model.destroy();
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }

        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        }
    }
}
