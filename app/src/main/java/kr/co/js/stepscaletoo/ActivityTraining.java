package kr.co.js.stepscaletoo;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ActivityTraining extends AppCompatActivity {

    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean isTracking = false; // 위치 추적 중인지 나타내는 플래그
    private ImageButton startStopButton; // 시작/정지 버튼
    private TextView textViewTime, textViewDistance, textViewSpeed, textViewCalories;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private long startTime; // 시작 시간 (밀리초)
    private double totalDistance; // 총 이동 거리 (미터)
    private Location lastLocation; // 마지막 위치
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable updateRunnable;
    private DBHelper dbHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        dbHelper = new DBHelper(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        textViewTime = findViewById(R.id.textViewTime);
        textViewDistance = findViewById(R.id.textViewDistance);
        textViewSpeed = findViewById(R.id.textViewSpeed);
        textViewCalories = findViewById(R.id.textViewCalories);
        startStopButton = findViewById(R.id.startStopButton);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (lastLocation != null) {
                    // 새로운 위치와 마지막 위치 사이의 거리 계산
                    float distance = location.distanceTo(lastLocation);
                    totalDistance += distance; // 총 이동 거리 업데이트
                }
                lastLocation = location; // 마지막 위치 업데이트
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        startStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTracking) { // 이미 추적 중이면
                    stopTracking(); // 추적 중지
                } else { // 추적 중이 아니면
                    startTracking(); // 추적 시작
                }
            }
        });


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

    private void saveRecord() {
        long elapsedTime = System.currentTimeMillis() - startTime; // 경과 시간 계산
        double caloriesBurned = 3.3 * 70 * elapsedTime / 3600000.0; // 칼로리 소모량 계산 (걷기 기준, 체중 70kg)

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        SQLiteStatement stmt = db.compileStatement("INSERT INTO records (date, time, distance, calories) VALUES (?, ?, ?, ?)");

        long currentTimeMillis = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String dateString = sdf.format(new Date(currentTimeMillis));

        stmt.bindString(1, dateString);
        stmt.bindLong(2, elapsedTime);
        stmt.bindDouble(3, totalDistance);
        stmt.bindDouble(4, caloriesBurned);
        stmt.executeInsert();
    }

    private void startTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 위치 업데이트 시작
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
            startTime = System.currentTimeMillis(); // 시작 시간 기록
            totalDistance = 0.0; // 총 이동 거리 초기화
            lastLocation = null; // 마지막 위치 초기화
            startStopButton.setImageResource(R.drawable.stop_image); // 이미지 버튼 이미지 변경
            isTracking = true; // 추적 중으로 상태 변경

            // Runnable 객체 생성
            updateRunnable = new Runnable() {
                @Override
                public void run() {
                    long elapsedTime = System.currentTimeMillis() - startTime; // 경과 시간 계산
                    textViewTime.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d",
                            elapsedTime / 3600000, elapsedTime % 3600000 / 60000, elapsedTime % 60000 / 1000));
                    textViewDistance.setText(String.format(Locale.getDefault(), "%.2f km", totalDistance / 1000.0));
                    textViewSpeed.setText(String.format(Locale.getDefault(), "%.2f km/h", totalDistance / 1000.0 / elapsedTime * 3600000.0));
                    double caloriesBurned = 3.3 * 70 * elapsedTime / 3600000.0; // 칼로리 소모량 계산 (걷기 기준, 체중 70kg)
                    textViewCalories.setText(String.format(Locale.getDefault(), "%.2f kcal", caloriesBurned));

                    // Runnable 객체를 다시 1초 후에 실행
                    handler.postDelayed(this, 1000);
                }
            };

            // Runnable 객체를 1초 후에 실행
            handler.postDelayed(updateRunnable, 1000);
        }
    }

    private void stopTracking() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 위치 업데이트 중지
            locationManager.removeUpdates(locationListener);
            startStopButton.setImageResource(R.drawable.start_image); // 이미지 버튼 이미지 변경
            if (isTracking) {
                saveRecord();
                addTestData();
            }
            isTracking = false; // 추적 중지로 상태 변경

            // Runnable 객체가 실행되지 않도록 취소
            handler.removeCallbacks(updateRunnable);
        }
    }

    private void addTestData() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // 일년 전 데이터 추가
        ContentValues values1 = new ContentValues();
        values1.put("time", 10000);
        values1.put("distance", 1000);
        values1.put("calories", 50);
        values1.put("date", getPastDate(365));  // 1년 전
        db.insert("records", null, values1);

        // 6개월 전 데이터 추가
        ContentValues values2 = new ContentValues();
        values2.put("time", 20000);
        values2.put("distance", 2000);
        values2.put("calories", 100);
        values2.put("date", getPastDate(180));  // 6개월 전
        db.insert("records", null, values2);

        // 한 달 전 데이터 추가
        ContentValues values3 = new ContentValues();
        values3.put("time", 30000);
        values3.put("distance", 3000);
        values3.put("calories", 150);
        values3.put("date", getPastDate(30));  // 한 달 전
        db.insert("records", null, values3);
    }

    // 과거 날짜를 반환하는 함수
    private String getPastDate(int daysAgo) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -daysAgo);
        Date pastDate = calendar.getTime();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return format.format(pastDate);
    }
}
