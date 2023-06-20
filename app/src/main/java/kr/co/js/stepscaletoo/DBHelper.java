package kr.co.js.stepscaletoo;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public DBHelper(Context context) {
        super(context, "StepScaleTooDB", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create a new table for records
        db.execSQL("CREATE TABLE records (id INTEGER PRIMARY KEY AUTOINCREMENT, date INTEGER, time INTEGER, distance REAL, calories REAL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS records");
        onCreate(db);
    }
}
