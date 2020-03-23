package com.corona;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.provider.BaseColumns;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class FeedReaderDbHelper extends SQLiteOpenHelper implements ComInterface {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 10 ;
    public static final String DATABASE_NAME = "FeedReader.db";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS GPS " ;

    public FeedReaderDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        String sql = "";
        sql = "CREATE TABLE gps( " +
                " id INTEGER PRIMARY KEY AUTOINCREMENT , "
                + " yyyy INTEGER, mm INTEGER, dd INTEGER, hh INTEGER, mi INTEGER, ss INTEGER, millis INTEGER "
                + " longitude REAL, latitude REAL " +
                " )"
                ;
        db.execSQL( sql );
        db.execSQL ( "CREATE INDEX date_idx ON gps( yyyy DESC, mm DESC, dd DESC, hh DESC, mi DESC, ss DESC, milllis DESC ) " );
    }
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL( "DROP TABLE IF EXISTS GPS " );
        db.execSQL( "DROP INDEX date_idx" ) ;
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static void createGpsDb(Context context) {
        FeedReaderDbHelper dbHelper = new FeedReaderDbHelper( context );
        // Gets the data repository in write mode
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put( "longitude", 112.02 );
        values.put( "latitude", 222.34 );

        LocalDateTime now = LocalDateTime.now();
        int yyyy = now.getYear();
        int mm = now.getMonthValue();
        int dd = now.getDayOfMonth();
        int hh = now.getHour();
        int mi = now.getMinute();
        int ss = now.getSecond();
        int millis = now.get(ChronoField.MILLI_OF_SECOND);

        values.put( "yyyy", yyyy );
        values.put( "mm", mm );
        values.put( "dd", dd );
        values.put( "hh", hh );
        values.put( "mi", mi );
        values.put( "ss", ss );
        values.put( "millis", millis );

        // Insert the new row, returning the primary key value of the new row
        long newRowId = db.insert( "gps", null, values);

        db.close();
    }

    public static void readGpsDb(Context context) {
        FeedReaderDbHelper dbHelper = new FeedReaderDbHelper( context );
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String sql = "SELECT id, upd, longitude, latitude FROM gps ORDER BY UPD DESC";
        String [] args = { };
        Cursor cursor = db.rawQuery( sql, args );

        while(cursor.moveToNext()) {
            long id = cursor.getLong( cursor.getColumnIndex( "id") );
            double longitude = cursor.getFloat( cursor.getColumnIndex( "longitude") );
            double latitude = cursor.getFloat( cursor.getColumnIndex( "latitude") );

            long yyyy = cursor.getLong( cursor.getColumnIndex( "yyyy") );
            long mm = cursor.getLong( cursor.getColumnIndex( "mm") );
            long dd = cursor.getLong( cursor.getColumnIndex( "dd") );
            long hh = cursor.getLong( cursor.getColumnIndex( "hh") );
            long mi = cursor.getLong( cursor.getColumnIndex( "mi") );
            long ss = cursor.getLong( cursor.getColumnIndex( "ss") );
            long millis = cursor.getLong( cursor.getColumnIndex( "millis") );

            String dateTime = "%04d-%02d-%02d %02d:%02d:%02d:%02d %d";
            dateTime = String.format( dateTime, yyyy, mm, dd, hh, mi, ss, millis );

            String info = "id = %d, lon = %f, lat = %f, upd = %s ";
            info = String.format( info, id, longitude, latitude, dateTime ) ;
            Log.d( TAG, info );
        }
        cursor.close();

        db.close();
    }
}