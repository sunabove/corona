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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class DbHelper extends SQLiteOpenHelper implements ComInterface {
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 31 ;
    public static final String DATABASE_NAME = "Corona.db";

    private static DbHelper dbHelper = null;
    // Gets the data repository in write mode
    public SQLiteDatabase wdb;
    public SQLiteDatabase rdb;

    private Proj projection = Proj.projection();

    public static DbHelper getLocationDbHelper(Context context ) {
        if (null == dbHelper) {
            dbHelper = new DbHelper(context);
        }
        return dbHelper;
    }

    private DbHelper(Context context) {
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
        ProjCoordinate coord = this.projection.convertToUtmK( latitude, longitude );

        values.put( "latitude", latitude );
        values.put( "longitude", longitude );

        values.put( "y", coord.y );
        values.put( "x", coord.x );

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

        long visit_tm = System.currentTimeMillis();

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
                values.put("notification", 0 );
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
    // whenCoronaDbReceived

    public long getCoronaListInfectedCount( int ... notifications ) {
        long cnt = 0 ;
        SQLiteDatabase db = this.rdb;

        int notiLen = null == notifications ? 9 : notifications.length ;

        String sql = "";
        sql += " SELECT IFNULL( COUNT( DISTINCT id), 0 ) AS cnt ";
        sql += " FROM corona ";
        sql += " WHERE deleted = 0 AND checked = 1 " ;
        sql += " AND notification IN (  ";

        for( int i = 0 ; i < notiLen ; i ++ ) {
            sql += 0 > i ? ", ?" : "?" ;
        }

        sql += " ) " ;

        String [] args = new String[ notiLen ] ;
        for( int i = 0 ; i < notiLen ; i ++ ) {
            args[ i ] = "" + notifications[ i ] ;
        }

        Cursor cursor = db.rawQuery(sql, args );

        while (cursor.moveToNext()) {
            cnt = cursor.getLong( 0 );
        }

        cursor.close();

        return cnt;
    }
    // -- getCoronaListInfectedCount

    public ArrayList<Corona> getCoronaListInfected( int ... notifications ) {
        SQLiteDatabase db = this.rdb;

        int notiLen = null == notifications ? 9 : notifications.length ;

        String sql = "" ;
        sql += " SELECT id, deleted, checked, notification, up_dt, place, patient, visit_fr, visit_to " ;
        sql += " , latitude, longitude " ;
        sql += " FROM corona " ;
        sql += " WHERE deleted = 0 AND checked = 1 ";
        sql += " AND notification IN (  ";

        for( int i = 0 ; i < notiLen ; i ++ ) {
            sql += 0 > i ? ", ?" : "?" ;
        }

        sql += " ) " ;
        sql += " ORDER BY notification, up_dt DESC, visit_fr, visit_to, place, patient " ;
        ;


        String [] args = new String[ notiLen ] ;
        for( int i = 0 ; i < notiLen ; i ++ ) {
            args[ i ] = "" + notifications[ i ] ;
        }
        
        Cursor cursor = db.rawQuery(sql, args);

        ArrayList<Corona> dataSet = new ArrayList<>();

        Corona corona;
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        while ( cursor.moveToNext()) {
            corona = new Corona();
            corona.id = cursor.getLong(cursor.getColumnIndex("id"));

            corona.deleted = cursor.getLong(cursor.getColumnIndex("deleted"));
            corona.checked = cursor.getLong(cursor.getColumnIndex("checked"));
            corona.notification = cursor.getLong(cursor.getColumnIndex("notification"));

            corona.up_dt = cursor.getLong(cursor.getColumnIndex("up_dt"));
            corona.place = cursor.getString(cursor.getColumnIndex("place"));
            corona.patient = cursor.getString(cursor.getColumnIndex("patient"));

            corona.visit_fr = cursor.getLong(cursor.getColumnIndex("visit_fr"));
            corona.visit_to = cursor.getLong(cursor.getColumnIndex("visit_to"));

            corona.latitude = cursor.getFloat(cursor.getColumnIndex("latitude"));
            corona.longitude = cursor.getFloat(cursor.getColumnIndex("longitude"));

            corona.infection = 1 == corona.checked ? "동선 겹침" : "" ;

            corona.title = String.format("[%d] %s / %s / %s", corona.id, corona.place, corona.patient , corona.infection );
            String snippet = String.format( "%s ~ %s", df.format( corona.visit_fr ) , df.format( corona.visit_to ) );
            corona.content = "동선 겹침 / 자가 격리 요망";

            corona.up_dt_str = df.format( corona.up_dt );

            corona.up_dt_str = df.format( corona.up_dt ) ;

            String info = String.format("corona marker deleted = %d, checked = %d, notification = %d, title = %s, snippet = %s, latitude = %f, longitude = %f, up_dt = %s",
                    corona.deleted, corona.checked, corona.notification, corona.title, snippet, corona.latitude, corona.longitude, corona.up_dt_str ) ;
            Log.d( TAG, info );

            dataSet.add( corona );
        }

        cursor.close();

        return dataSet ;
    }
    // getCoronaListInfected

    public void updateCoronaNotification( Corona corona , int notification ) {
        String table = "corona" ;
        String whereClause =  " id = ? ";

        ContentValues values = new ContentValues();
        values.put( "notification", notification );

        SQLiteDatabase db = this.wdb;

        String [] args = new String[] { "" + corona.id };

        int updCnt = db.update( table , values, whereClause, args );

        Log.d( TAG, "notification updCnt = " + updCnt );
    }
    // -- updateCoronaNotification


}