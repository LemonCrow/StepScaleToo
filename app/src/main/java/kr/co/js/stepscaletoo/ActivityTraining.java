package kr.co.js.stepscaletoo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;

public class ActivityTraining extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);

        // SharedFragment 인스턴스 생성
        SharedFragment sharedFragment = new SharedFragment();

        // 프래그먼트 매니저 가져오기
        FragmentManager fragmentManager = getSupportFragmentManager();

        // 프래그먼트 트랜잭션 시작
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // 프래그먼트를 추가하고 커밋
        fragmentTransaction.add(R.id.bottom_navigation, sharedFragment);
        fragmentTransaction.commit();
    }
}