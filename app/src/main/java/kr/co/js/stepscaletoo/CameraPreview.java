package kr.co.js.stepscaletoo;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    private boolean isFirstFrame = true;
    private SurfaceHolder holder;
    private Camera camera;
    private Interpreter interpreter;
    public Context context;

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        holder = getHolder();
        holder.addCallback(this);
        this.context = context;
    }



    private Pair<Integer, Float> getMaxIndexAndValue(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        return new Pair<>(maxIndex, maxValue);
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
                processFrame(data, previewSize.width, previewSize.height);
            }

        });
    }

    private void processFrame(byte[] data, int width, int height) {
        // TensorFlow 모델에 입력 데이터 전달
        ByteBuffer inputBuffer = preprocessInput(data, width, height);
        inputBuffer.order(ByteOrder.nativeOrder());

        int numClasses = 9;

        // 객체 분류 결과를 담을 버퍼
        float[][] output = new float[1][numClasses];

        // TensorFlow 모델 실행
        interpreter.run(inputBuffer, output);

        // 결과 처리
        Pair<Integer, Float> maxIndexAndValue = getMaxIndexAndValue(output[0]);
        int predictedIndex = maxIndexAndValue.first;
        float confidence = maxIndexAndValue.second;

        List<String> labels = loadLabels(context, "classes.txt");
        String predictedClass = labels.get(predictedIndex);

        Log.d("Object Recognition", "Predicted Class: " + predictedClass);
        Log.d("Object Recognition", "Confidence: " + confidence);
        Log.d("Object Recognition", "Confidence: " + output[0][0]);
    }

    private ByteBuffer preprocessInput(byte[] data, int width, int height) {
        int inputWidth = 224;// 입력 데이터의 가로 크기를 모델과 일치하도록 설정해야 합니다.
        int inputHeight = 224;// 입력 데이터의 세로 크기를 모델과 일치하도록 설정해야 합니다.

        // 중앙 부분을 중점으로 데이터를 잘라냅니다.
        int left = (width - inputWidth) / 2;
        int top = (height - inputHeight) / 2;
        int right = left + inputWidth;
        int bottom = top + inputHeight;

        // 잘라낸 데이터를 새로운 배열에 복사합니다.
        byte[] croppedData = new byte[inputWidth * inputHeight];
        for (int row = top; row < bottom; row++) {
            System.arraycopy(data, (row * width + left) * 3, croppedData, (row - top) * inputWidth, inputWidth);
        }

        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(inputWidth * inputHeight * 3 * 4);
        inputBuffer.order(ByteOrder.nativeOrder());

        // 잘라낸 데이터를 복사합니다.
        inputBuffer.put(croppedData);

        return inputBuffer;
    }


    private float normalizePixelValue(byte[] data) {
        // 입력 데이터를 정규화하는 작업을 수행
        float pixelValue = (float) (data[0]);

        return pixelValue;
    }

    private List<String> loadLabels(Context context, String labelsFileName) {
        List<String> labels = new ArrayList<>();
        BufferedReader reader = null;
        try {
            InputStream inputStream = context.getAssets().open(labelsFileName);
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                labels.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            // 예외 처리 코드 추가
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    // 예외 처리 코드 추가
                }
            }
        }
        return labels;
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

        // 모델 파일 로드
        try {
            MappedByteBuffer modelFile = loadModelFile(context, "model.tflite");

            // TensorFlow Interpreter 생성
            interpreter = new Interpreter(modelFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        camera.stopPreview();
        camera.release();

        // TensorFlow Interpreter 해제
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


