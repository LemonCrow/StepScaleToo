package kr.co.js.stepscaletoo;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.Tensor;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {

    private CameraPreview cameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find the resultTextView from the layout
        TextView resultTextView = findViewById(R.id.resultTextView);

        // Find the cameraPreview from the layout
        cameraPreview = findViewById(R.id.cameraPreview);

        // Set the resultTextView to the cameraPreview
        cameraPreview.setResultTextView(resultTextView);
    }

    // Rest of the MainActivity code
}
