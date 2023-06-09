package kr.co.js.stepscaletoo;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.TextView;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private boolean isFirstFrame = true;
    private SurfaceHolder holder;
    private Camera camera;
    private Interpreter interpreter;
    private List<String> classes;
    private Context context;
    private TextView resultTextView;

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        this.context = context;
    }

    public void setResultTextView(TextView textView) {
        this.resultTextView = textView;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();

        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        camera.setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                // 카메라 프레임 데이터 처리
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                Bitmap bitmap = convertToBitmap(data, previewSize.width, previewSize.height);
                processImage(bitmap);
            }
        });
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (holder.getSurface() == null) {
            return;
        }

        try {
            camera.stopPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            camera.setPreviewDisplay(holder);

            Camera.Parameters parameters = camera.getParameters();
            List<Camera.Size> supportedSizes = parameters.getSupportedPreviewSizes();
            Camera.Size optimalSize = getOptimalPreviewSize(supportedSizes, width, height);
            parameters.setPreviewSize(optimalSize.width, optimalSize.height);
            parameters.setRotation(90);
            camera.setParameters(parameters);

            camera.setDisplayOrientation(90);

            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Load the model file
        try {
            MappedByteBuffer modelFile = loadModelFile(context, "model.tflite");

            // TensorFlow Interpreter creation
            interpreter = new Interpreter(modelFile);

            // Load the classes from classes.txt
            classes = loadClasses(context, "classes.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();

        // TensorFlow Interpreter closure
        if (interpreter != null) {
            interpreter.close();
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelFileName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelFileName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadClasses(Context context, String fileName) {
        List<String> classes = new ArrayList<>();
        try {
            InputStream inputStream = context.getAssets().open(fileName);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                classes.add(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return classes;
    }

    private Bitmap convertToBitmap(byte[] data, int width, int height) {
        YuvImage yuvImage = new YuvImage(data, ImageFormat.NV21, width, height, null);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), 80, stream);
        byte[] jpegData = stream.toByteArray();
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
    }

    private void processImage(Bitmap bitmap) {
        // Resize the bitmap to match the model input size
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);

        // Convert the bitmap to a ByteBuffer
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(224 * 224 * 3 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());
        int[] pixels = new int[224 * 224];
        resizedBitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224);
        for (int pixel : pixels) {
            float r = (float) ((pixel >> 16) & 0xFF);
            float g = (float) ((pixel >> 8) & 0xFF);
            float b = (float) (pixel & 0xFF);
            inputBuffer.putFloat(r);
            inputBuffer.putFloat(g);
            inputBuffer.putFloat(b);
        }

        // Run the inference
        float[][] output = new float[1][classes.size()];
        interpreter.run(inputBuffer, output);

        // Find the predicted class and its probability
        Pair<String, Float> prediction = findMaxClass(output);
        String className = prediction.first;
        float probability = prediction.second;

        // Display the result
        String result = "Class: " + className + "\nProbability: " + probability +
                "\noutput[0][0]" + output[0][0] +
                "\noutput[0][1]" + output[0][1] +
                "\noutput[0][2]" + output[0][2] +
                "\noutput[0][3]" + output[0][3] +
                "\noutput[0][4]" + output[0][4];

        resultTextView.setText(result);
    }

    private Pair<String, Float> findMaxClass(float[][] array) {
        int maxIndex = 0;
        float maxValue = array[0][0];
        for (int col = 1; col < array[0].length; col++) {
            if (array[0][col] > maxValue) {
                maxValue = array[0][col];
                maxIndex = col;
            }
        }
        String className = classes.get(maxIndex);
        float probability = maxValue;
        return new Pair<>(className, probability);
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) width / height;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;

            double diff = Math.abs(size.height - height);
            if (diff < minDiff) {
                optimalSize = size;
                minDiff = diff;
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                double diff = Math.abs(size.height - height);
                if (diff < minDiff) {
                    optimalSize = size;
                    minDiff = diff;
                }
            }
        }

        return optimalSize;
    }
}

