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

public class ActivityStatics extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statics);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.menu_item_page2);  // BottomNavigationView에서 Page 2를 선택한 상태로 만듭니다.
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_item_page1:
                        startActivity(new Intent(ActivityStatics.this, ActivityTraining.class));
                        finish();
                        break;
                    case R.id.menu_item_main:
                        startActivity(new Intent(ActivityStatics.this, MainActivity.class));
                        finish();
                        break;
                    case R.id.menu_item_page2:
                        break;
                }
                return true;
            }
        });
    }
}
