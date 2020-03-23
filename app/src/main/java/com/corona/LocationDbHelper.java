package com.corona;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import java.util.Calendar;

public class LocationDbHelper extends SQLiteOpenHelper implements ComInterface {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 13;
    public static final String DATABASE_NAME = "Location.db";

    private static LocationDbHelper dbHelper = null;
    // Gets the data repository in write mode
    public SQLiteDatabase wdb;
    public SQLiteDatabase rdb;

    public static LocationDbHelper getLocationDbHelper( Context context ) {
        if (null == dbHelper) {
            dbHelper = new LocationDbHelper(context);
        }
        return dbHelper;
    }

    private LocationDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        wdb = this.getWritableDatabase();
        rdb = this.getReadableDatabase();
    }

    public void onCreate(SQLiteDatabase db) {
        String sql = "";
        sql = "CREATE TABLE gps( " +
                " id INTEGER PRIMARY KEY AUTOINCREMENT , "
                + " yyyy INTEGER, mm INTEGER, dd INTEGER, hh INTEGER, mi INTEGER, ss INTEGER, zz INTEGER, "
                + " longitude REAL, latitude REAL " +
                " )"
        ;
        db.execSQL(sql);
        db.execSQL("CREATE INDEX date_idx ON gps( yyyy DESC, mm DESC, dd DESC, hh DESC, mi DESC, ss DESC, zz DESC ) ");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL("DROP TABLE IF EXISTS GPS ");
        db.execSQL("DROP INDEX IF EXISTS date_idx");
        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public static void insertGpsLog(Context context, Location location) {
        LocationDbHelper dbHelper = LocationDbHelper.getLocationDbHelper(context);

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put("longitude", location.getLatitude());
        values.put("latitude", location.getLongitude());

        Calendar now = Calendar.getInstance();

        int yyyy = now.get(Calendar.YEAR);
        int mm = now.get(Calendar.MONTH) + 1; // Note: zero based!
        int dd = now.get(Calendar.DAY_OF_MONTH);
        int hh = now.get(Calendar.HOUR_OF_DAY);
        int mi = now.get(Calendar.MINUTE);
        int ss = now.get(Calendar.SECOND);
        int zz = now.get(Calendar.MILLISECOND);

        values.put("yyyy", yyyy);
        values.put("mm", mm);
        values.put("dd", dd);
        values.put("hh", hh);
        values.put("mi", mi);
        values.put("ss", ss);
        values.put("zz", zz);

        // Insert the new row, returning the primary key value of the new row
        SQLiteDatabase db = dbHelper.wdb;

        long newRowId = db.insert("gps", null, values);
    }
}