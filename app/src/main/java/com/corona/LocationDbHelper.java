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
import org.locationtech.proj4j.ProjCoordinate;

import java.util.Calendar;

public class LocationDbHelper extends SQLiteOpenHelper implements ComInterface {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 30 ;
    public static final String DATABASE_NAME = "Corona.db";

    private static LocationDbHelper dbHelper = null;
    // Gets the data repository in write mode
    public SQLiteDatabase wdb;
    public SQLiteDatabase rdb;

    private Proj projection = Proj.projection();

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

        sql = "CREATE TABLE corona( ";
        sql += "   id INTEGER PRIMARY KEY ";
        sql += " , deleted INT2 NOT NULL DEFAULT 0 ";
        sql += " , checked INT2 NOT NULL DEFAULT 0 ";
        sql += " , checked_tm INTEGER ";
        sql += " , notification INT2 NOT NULL DEFAULT 0 ";
        sql += " , up_dt INTEGER ";
        sql += " , place VARCHAR(500), patient VARCHAR(500) ";
        sql += " , visit_fr INTEGER, visit_to INTEGER ";
        sql += " , latitude REAL, longitude REAL, y REAL, x REAL" ;
        sql += ")";

        db.execSQL( sql );
        db.execSQL( "CREATE INDEX corona_idx_01 ON corona( deleted, checked, notification, up_dt ) ");
        db.execSQL( "CREATE INDEX corona_idx_02 ON corona( deleted, checked, visit_fr, visit_to, y, x ) ");

        sql = "CREATE TABLE gps( ";
        sql += " id INTEGER PRIMARY KEY AUTOINCREMENT ";
        sql += " , yyyy INTEGER, mm INTEGER, dd INTEGER, hh INTEGER, mi INTEGER, ss INTEGER, zz INTEGER ";
        sql += " , visit_tm INTEGR ";
        sql += " , latitude REAL, longitude REAL, y REAL, x REAL" ;
        sql += " , corona_id INTEGER ";
        sql += " , FOREIGN KEY( corona_id ) REFERENCES corona( id ) ";
        sql += " )" ;

        db.execSQL(sql);
        db.execSQL("CREATE INDEX gps_idx_01 ON gps( yyyy DESC, mm DESC, dd DESC, hh DESC, mi DESC, ss DESC, zz DESC ) ");
        db.execSQL("CREATE INDEX gps_idx_02 ON gps( visit_tm, y, x ) ");

    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // This database is only a cache for online data, so its upgrade policy is
        // to simply to discard the data and start over

        db.execSQL("DROP INDEX IF EXISTS corona_idx_01");
        db.execSQL("DROP INDEX IF EXISTS corona_idx_02");
        db.execSQL("DROP TABLE IF EXISTS corona ");

        db.execSQL("DROP INDEX IF EXISTS gps_idx_01");
        db.execSQL("DROP INDEX IF EXISTS gps_idx_02");
        db.execSQL("DROP TABLE IF EXISTS gps ");

        onCreate(db);
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void checkCoronaInfection() {
        // Insert the new row, returning the primary key value of the new row

        if( true ) {
            long prevCheckedCnt = 0 ;

            String sql = " SELECT COUNT( id ) FROM corona ";
            sql += " WHERE checked = 1 ";
            String [] args = {};

            SQLiteDatabase db = this.wdb;

            Cursor cursor = db.rawQuery( sql, args );
            while( cursor.moveToNext() ) {
                prevCheckedCnt = cursor.getLong( 0 );
            }
            cursor.close();

            Log.d( TAG, "prevCheckedCnt = " + prevCheckedCnt );
        }

        if( true ) {
            final long min_dist = 31;
            String checked_tm   = "" + System.currentTimeMillis() ;
            String visit_gap    = "" + ( 120 * 60 * 1_000 );
            String dist_gap     = "" + min_dist;
            String distum_gap   = "" + (min_dist*min_dist);

            String table = "corona";

            String whereClause = "";
            whereClause += " checked = 0 AND deleted = 0 ";
            whereClause += " AND id IN ( ";
            whereClause += "    SELECT DISTINCT c.id FROM corona c , gps g ";
            whereClause += "    WHERE c.deleted = 0 AND c.checked = 0 ";
            whereClause += "    AND g.visit_tm BETWEEN c.visit_fr - ? AND c.visit_to + ? ";
            whereClause += "    AND ABS( g.y - c.y ) < ? AND ABS( g.x - c.x ) < ? ";
            whereClause += "    AND (g.y - c.y)*(g.y -c.y) + (g.x - c.x)*(g.x - c.x) < ? ";
            whereClause += " ) ";

            ContentValues values = new ContentValues();
            values.put( "checked" , 1 );
            values.put( "checked_tm" , checked_tm );

            SQLiteDatabase db = this.wdb;
            String[] args = { visit_gap, visit_gap, dist_gap, dist_gap, distum_gap };

            int updCnt = db.update(table, values, whereClause, args);

            Log.d( TAG, "checked corona infection. updCnt = " + updCnt );
        }

        if( true ) {
            long currCheckedCnt = 0 ;

            String sql = " SELECT COUNT( id ) FROM corona ";
            sql += " WHERE checked = 1 ";
            String [] args = {};

            SQLiteDatabase db = this.wdb;

            Cursor cursor = db.rawQuery( sql, args );
            while( cursor.moveToNext() ) {
                currCheckedCnt = cursor.getLong( 0 );
            }
            cursor.close();

            Log.d( TAG, "currCheckCnt = " + currCheckedCnt );
        }
    }

    public void insertGpsLog( Location location) {
        ContentValues values = new ContentValues();
        double latitude = location.getLatitude();;
        double longitude = location.getLongitude();;
        ProjCoordinate projCoord = this.projection.convertToUtmK( latitude, longitude );

        values.put( "latitude", latitude );
        values.put( "longitude", longitude );

        values.put( "y", projCoord.y );
        values.put( "x", projCoord.x );

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

        long visit_tm = location.getTime() ;

        values.put( "visit_tm", visit_tm );

        // Insert the new row, returning the primary key value of the new row
        SQLiteDatabase db = this.wdb;

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

                ProjCoordinate projCoord = this.projection.convertToUtmK( latitude, longitude );

                String info = "[%d] upDt: %s, id=%s, place = %s, patient, %s, deleted = %s, latitude = %s, longitude = %s, visitFr = %s, visitTo = %s" ;
                info = String.format( info, i, "" + id, "" + upDt, place, patient, "" + deleted, latitude, longitude, "" + visitFr, "" + visitTo );

                // Create a new map of values, where column names are the keys
                ContentValues values = new ContentValues();
                values.put("id", id );
                values.put("deleted", "1".equals("" + deleted) ? 1 : 0 );
                values.put("up_dt", upDt );
                values.put("checked", 0 );
                values.put("checked_tm", 0 );
                values.put("place", place );
                values.put("patient", patient );
                values.put("visit_fr", visitFr );
                values.put("visit_to", visitTo );
                values.put("latitude", latitude );
                values.put("longitude", longitude );
                values.put("y", projCoord.y );
                values.put("x", projCoord.x );

                long newRowId = db.replace("corona", null, values);

                Log.d(TAG, info );

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}