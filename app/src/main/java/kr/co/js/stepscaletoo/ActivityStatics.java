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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

public class ActivityStatics extends AppCompatActivity {

    private DBHelper dbHelper;
    private TextView textViewDaily, textViewMonthly, textViewYearly;

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

        dbHelper = new DBHelper(this);
        textViewDaily = findViewById(R.id.textViewDaily);
        textViewMonthly = findViewById(R.id.textViewMonthly);
        textViewYearly = findViewById(R.id.textViewYearly);
        loadStatistics();
    }

    private void loadStatistics() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor;

        cursor = db.rawQuery("SELECT SUM(time), SUM(distance), SUM(calories) FROM records WHERE date >= datetime('now', '-1 day')", null);
        if (cursor.moveToFirst()) {
            long time = cursor.getLong(0);
            String formattedTime = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(time),
                    TimeUnit.MILLISECONDS.toMinutes(time) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1));
            double distance = cursor.getDouble(1) / 1000;
            String formattedDistance = String.format("%.2f", distance);
            long calories = Math.round(cursor.getDouble(2));
            textViewDaily.setText("Daily: Time = " + formattedTime + ", Distance = " + formattedDistance + "km, Calories = " + calories + "kcal");
        }
        cursor.close();

        cursor = db.rawQuery("SELECT SUM(time), SUM(distance), SUM(calories) FROM records WHERE date >= datetime('now', '-1 month')", null);
        if (cursor.moveToFirst()) {
            long time = cursor.getLong(0);
            String formattedTime = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(time),
                    TimeUnit.MILLISECONDS.toMinutes(time) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1));
            double distance = cursor.getDouble(1) / 1000;
            String formattedDistance = String.format("%.2f", distance);
            long calories = Math.round(cursor.getDouble(2));
            textViewMonthly.setText("Monthly: Time = " + formattedTime + ", Distance = " + formattedDistance + "km, Calories = " + calories + "kcal");
        }
        cursor.close();

        cursor = db.rawQuery("SELECT SUM(time), SUM(distance), SUM(calories) FROM records WHERE date >= datetime('now', '-1 year')", null);
        if (cursor.moveToFirst()) {
            long time = cursor.getLong(0);
            String formattedTime = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(time),
                    TimeUnit.MILLISECONDS.toMinutes(time) % TimeUnit.HOURS.toMinutes(1),
                    TimeUnit.MILLISECONDS.toSeconds(time) % TimeUnit.MINUTES.toSeconds(1));
            double distance = cursor.getDouble(1) / 1000;
            String formattedDistance = String.format("%.2f", distance);
            long calories = Math.round(cursor.getDouble(2));
            textViewYearly.setText("Yearly: Time = " + formattedTime + ", Distance = " + formattedDistance + "km, Calories = " + calories + "kcal");
        }
        cursor.close();
    }

}
