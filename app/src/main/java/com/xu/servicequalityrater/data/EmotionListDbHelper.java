package com.xu.servicequalityrater.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.util.Log;

import com.xu.servicequalityrater.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EmotionListDbHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "emotionSQLTable.db";
   public static final String DATABASE_TABLE_NAME ="emotionlist";

    private static final int DATABASE_VERSION = 1;

    public EmotionListDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    //0905 added time http://stackoverflow.com/questions/41596841/android-sql-datetime-current-timestamp
    //TODO 2105 solved problem with dateTime, left out a bracket!
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        sqLiteDatabase.execSQL("CREATE TABLE "+ DATABASE_TABLE_NAME+"(_id INTEGER PRIMARY KEY AUTOINCREMENT, anger DOUBLE NOT NULL, contempt DOUBLE NOT NULL, disgust DOUBLE NOT NULL, fear DOUBLE NOT NULL, happiness DOUBLE NOT NULL, neutral DOUBLE NOT NULL, sadness DOUBLE NOT NULL, surprise DOUBLE NOT NULL, datetime DEFAULT (datetime('now','localtime')));");

    }

    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS emotionlist");
        onCreate(sqLiteDatabase);
    }


}
