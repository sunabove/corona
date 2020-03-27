package com.corona;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;

public class LocationDbHelper extends SQLiteOpenHelper implements ComInterface {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 17;
    public static final String DATABASE_NAME = "Corona.db";

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
        sql = "CREATE TABLE gps( ";
        sql += " id INTEGER PRIMARY KEY AUTOINCREMENT ";
        sql += " , yyyy INTEGER, mm INTEGER, dd INTEGER, hh INTEGER, mi INTEGER, ss INTEGER, zz INTEGER ";
        sql += " , up_dt INTEGR ";
        sql += " , latitude REAL, longitude REAL" ;
        sql += " )" ;

        db.execSQL(sql);
        db.execSQL("CREATE INDEX gps_idx_01 ON gps( yyyy DESC, mm DESC, dd DESC, hh DESC, mi DESC, ss DESC, zz DESC ) ");
        db.execSQL("CREATE INDEX gps_idx_02 ON gps( up_dt DESC ) ");

        sql = "CREATE TABLE corona( ";
        sql += "   id INTEGER PRIMARY KEY ";
        sql += " , deleted INT2 ";
        sql += " , up_dt INTEGER ";
        sql += " , place VARCHAR(500), patient VARCHAR(500) ";
        sql += " , visit_fr INTEGER, visit_to INTEGER ";
        sql += " , latitude REAL, longitude REAL" ;
        sql += ")";

        db.execSQL( sql );
        db.execSQL("CREATE INDEX corona_idx_01 ON corona( deleted, up_dt ) ");
        db.execSQL("CREATE INDEX corona_idx_02 ON corona( visit_fr, visit_to ) ");
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over
        db.execSQL("DROP INDEX IF EXISTS gps_idx_01");
        db.execSQL("DROP INDEX IF EXISTS gps_idx_02");
        db.execSQL("DROP TABLE IF EXISTS gps ");

        db.execSQL("DROP INDEX IF EXISTS corona_idx_01");
        db.execSQL("DROP INDEX IF EXISTS corona_idx_02");
        db.execSQL("DROP TABLE IF EXISTS corona ");

        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public static void insertGpsLog(Context context, Location location) {
        LocationDbHelper dbHelper = LocationDbHelper.getLocationDbHelper(context);

        // Create a new map of values, where column names are the keys
        ContentValues values = new ContentValues();
        values.put("latitude", location.getLatitude());
        values.put("longitude", location.getLongitude());

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

        long up_dt = now.getTimeInMillis() ;

        values.put( "up_dt", up_dt );

        // Insert the new row, returning the primary key value of the new row
        SQLiteDatabase db = dbHelper.wdb;

        long newRowId = db.insert("gps", null, values);
    }

    public long getCoronaMaxUpDt() {
        String sql = " SELECT MAX( up_dt ) AS max_up_dt FROM corona " ;

        String[] args = { };
        SQLiteDatabase db = this.rdb;
        Cursor cursor = db.rawQuery(sql, args);

        while (cursor.moveToNext()) {
            long max_up_dt = cursor.getLong( 0 );
            return max_up_dt ;
        }

        cursor.close();

        return 0;
    }

    public void whenCoronaDbReceived(JSONArray response) throws Exception {
        SQLiteDatabase db = this.wdb;

        JSONObject obj ;
        for( int i = 0 ; i < response.length() ; i ++ ) {
            try {
                obj = response.getJSONObject( i );
                long id = obj.getLong( "id" );
                Object deleted = obj.get( "deleted" );
                long upDt = obj.getLong( "upDt" );
                String place = obj.getString( "place" );
                String patient = obj.getString( "patient" );
                JSONObject geom = obj.getJSONObject( "geom" );
                long visitFr = obj.getLong( "visitFr" );
                long visitTo = obj.getLong( "visitTo" );
                double latitude = geom.getDouble( "lat" );
                double longitude = geom.getDouble( "lon" );

                String info = "[%d] upDt: %s, id=%s, place = %s, patient, %s, deleted = %s, latitude = %s, longitude = %s, visitFr = %s, visitTo = %s" ;
                info = String.format( info, i, "" + id, "" + upDt, place, patient, "" + deleted, latitude, longitude, "" + visitFr, "" + visitTo );

                // Create a new map of values, where column names are the keys
                ContentValues values = new ContentValues();
                values.put("id", id );
                values.put("deleted", "1".equals("" + deleted) ? 1 : 0 );
                values.put("up_dt", upDt );
                values.put("place", place );
                values.put("patient", patient );
                values.put("visit_fr", visitFr );
                values.put("visit_to", visitTo );
                values.put("latitude", latitude );
                values.put("longitude", longitude );

                long newRowId = db.replace("corona", null, values);

                Log.d(TAG, info );

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}