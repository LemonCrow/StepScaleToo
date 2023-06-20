package kr.co.js.stepscaletoo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ActivityTraining extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.menu_item_page1);  // BottomNavigationView에서 Page 1을 선택한 상태로 만듭니다.
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_item_page1:
                        break;
                    case R.id.menu_item_main:
                        startActivity(new Intent(ActivityTraining.this, MainActivity.class));
                        finish();
                        break;
                    case R.id.menu_item_page2:
                        startActivity(new Intent(ActivityTraining.this, ActivityStatics.class));
                        finish();
                        break;
                }
                return true;
            }
        });
    }
}