package kr.co.js.stepscaletoo;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
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
import java.util.List;

public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
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



    private int getMaxIndex(float[] array) {
        int maxIndex = 0;
        float maxValue = array[0];
        for (int i = 1; i < array.length; i++) {
            if (array[i] > maxValue) {
                maxValue = array[i];
                maxIndex = i;
            }
        }
        return maxIndex;
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
        ByteBuffer inputBuffer = ByteBuffer.allocateDirect(width * height * 3); // 가정: RGB 이미지
        inputBuffer.order(ByteOrder.nativeOrder());
        inputBuffer.put(data);
        inputBuffer.rewind();

        int numClasses = 9;
        // 객체 분류 결과를 담을 버퍼
        float[][] output = new float[1][numClasses];
        // 가정: numClasses는 분류할 객체의 클래스 개수

        // TensorFlow 모델 실행
        interpreter.run(inputBuffer, output);

        // 결과 처리
        List<String> labels = loadLabels(context, "classes.txt");
        String predictedClass = labels.get(getMaxIndex(output[0]));

        // 처리된 결과를 사용하거나 반환할 수 있습니다.
        // 예를 들어, 객체 인식 결과를 로그로 출력하거나 UI에 표시할 수 있습니다.
        Log.d("Object Recognition", "Predicted Class: " + predictedClass);
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


