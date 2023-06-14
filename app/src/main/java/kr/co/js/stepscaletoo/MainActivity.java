package kr.co.js.stepscaletoo;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;


import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private CameraPreview cameraPreview;
    private LinearLayout textViewContainer;
    private Button tagBtn;
    private TextView  resultTextView;
    private int textViewId;
    private String text;
    private int[] isMake = {0, 0, 0, 0, 0};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Context context = getApplicationContext();



        resultTextView = findViewById(R.id.resultTextView);
        tagBtn = findViewById(R.id.tagBtn);
        cameraPreview = findViewById(R.id.cameraPreview);
        textViewContainer = findViewById(R.id.textViewContainer);


        cameraPreview.setResultTextView(resultTextView);
        tagBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tagTextView(context);
            }
        });


    }


    private void tagTextView(Context context){
        LinearLayout tagLinear = findViewById(R.id.tagLinear);
        int color = resultTextView.getCurrentTextColor();
        TextView textView = new TextView(this);
        boolean isCap = true;

        if(color == Color.rgb(255, 0, 0) && isMake[0] != 1){ //사과
            textViewId = 0;
            text = "사과";
            isMake[0] = 1;
        }
        else if(color == Color.rgb(255, 165, 0) && isMake[1] != 1){ //당근
            textViewId = 1;
            text = "당근";
            isMake[1] = 1;
        }
        else if(color == Color.rgb(139, 69, 19) && isMake[2] != 1){ //감자
            textViewId = 2;
            text = "감자";
            isMake[2] = 1;
        }
        else if(color == Color.rgb(220, 20, 60) && isMake[3] != 1){ //딸기
            textViewId = 3;
            text = "딸기";
            isMake[3] = 1;
        }
        else if(color ==  Color.rgb(255, 99, 71) && isMake[4] != 1){ //토마토
            textViewId = 4;
            text = "토마토";
            isMake[4] = 1;
        }
        else{ //기본
            isCap = false;
            Toast.makeText(context, "객체가 없습니다.", Toast.LENGTH_LONG);
        }
        if (isCap) {
            textView.setId(textViewId);
            textView.setText("#" + text);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

            textView.setLayoutParams(layoutParams);
            textView.setTextColor(color);

            textView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tagLinear.removeView(textView);
                    int num = textView.getId();
                    isMake[num] = 0;
                }
            });

            tagLinear.addView(textView);
        }
    }


}
