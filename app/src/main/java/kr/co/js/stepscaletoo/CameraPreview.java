package kr.co.js.stepscaletoo;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
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
        initCamera();
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
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }

        // TensorFlow Interpreter closure
        if (interpreter != null) {
            interpreter.close();
        }
    }

    private void initCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
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
        String result = className;
        int color = Color.BLACK;
        if(result.equals("apple")){
            result = "사과";
            color = Color.rgb(255, 0, 0);
        }
        else if(result.equals("carrot")){
            result = "당근";
            color = Color.rgb(255, 165, 0);
        }
        else if(result.equals("potato")){
            result = "감자";
            color = Color.rgb(139, 69, 19);
        }
        else if(result.equals("banana")){
            result = "바나나";
            color = Color.YELLOW;
        }
        else if(result.equals("tomato")){
            result = "토마토";
            color = Color.RED;
        }
        else{
            result = "Unknown";
        }

        final String finalResult = result;
        final int finalColor = color;
        post(new Runnable() {
            @Override
            public void run() {
                resultTextView.setText(finalResult);
                resultTextView.setTextColor(finalColor);
            }
        });
    }

    private Pair<String, Float> findMaxClass(float[][] output) {
        int maxIndex = -1;
        float maxProbability = 0.0f;
        for (int i = 0; i < classes.size(); i++) {
            if (output[0][i] > maxProbability) {
                maxIndex = i;
                maxProbability = output[0][i];
            }
        }
        String className = classes.get(maxIndex);
        return new Pair<>(className, maxProbability);
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int width, int height) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) width / height;
        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = height;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;

            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return optimalSize;
    }
}
