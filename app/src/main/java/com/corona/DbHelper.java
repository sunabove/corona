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
    public static final int DATABASE_VERSION = 52 ;
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
        sql += " , latitude REAL, longitude REAL" ;
        sql += " , y REAL, x REAL" ;
        sql += ")";

        db.execSQL( sql );
        db.execSQL( "CREATE INDEX corona_idx_01 ON corona( deleted, checked, notification, up_dt ) ");
        db.execSQL( "CREATE INDEX corona_idx_02 ON corona( deleted, checked, visit_fr, visit_to, y, x ) ");

        sql = "CREATE TABLE gps( ";
        sql += " id INTEGER PRIMARY KEY AUTOINCREMENT ";
        sql += " , yyyy INTEGER, mm INTEGER, dd INTEGER, hh INTEGER, mi INTEGER, ss INTEGER, zz INTEGER ";
        sql += " , visit_tm INTEGR ";
        sql += " , latitude REAL, longitude REAL, y REAL, x REAL" ;
        sql += " , py REAL, px REAL, pdistum REAL" ;
        sql += " , corona_id INTEGER ";
        sql += " , FOREIGN KEY( corona_id ) REFERENCES corona( id ) ";
        sql += " )" ;

        db.execSQL(sql);
        db.execSQL("CREATE INDEX gps_idx_01 ON gps( yyyy DESC, mm DESC, dd DESC, hh DESC, mi DESC, ss DESC, zz DESC ) ");
        db.execSQL("CREATE INDEX gps_idx_02 ON gps( visit_tm, y, x, py, px ) ");

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

    private long getCoronaCheckedCnt() {
        long checkedCnt = 0 ;

        SQLiteDatabase db = this.rdb;

        String sql = " SELECT COUNT( id ) FROM corona ";
        sql += " WHERE checked = 1 ";
        String [] args = {};

        Cursor cursor = db.rawQuery( sql, args );
        while( cursor.moveToNext() ) {
            checkedCnt = cursor.getLong( 0 );
        }
        cursor.close();

        return checkedCnt ;
    }

    public void checkCoronaInfection() {
        // Insert the new row, returning the primary key value of the new row

        long prevCheckedCnt = this.getCoronaCheckedCnt() ;
        Log.d( TAG, "prevCheckedCnt = " + prevCheckedCnt );

        String table = "corona";

        ContentValues values = new ContentValues();

        String checked_tm = "" + System.currentTimeMillis() ;

        values.put( "checked" , 1 );
        values.put( "notification" , 0 );
        values.put( "checked_tm" , checked_tm );

        String whereClause = "";
        whereClause += " 1 = 1 ";
        whereClause += " AND checked = 0 ";
        whereClause += " AND deleted = 0 ";
        whereClause += " AND id IN ( ";
        whereClause += "    SELECT DISTINCT c.id FROM corona c , gps g ";
        whereClause += "    WHERE c.deleted = 0 ";
        whereClause += "    AND c.checked = 0 ";
        whereClause += "    AND g.visit_tm BETWEEN c.visit_fr AND c.visit_to";
        whereClause += "    AND ( ";
        whereClause += "          ( ";
        whereClause += "            ABS( g.y - c.y ) < 31 AND ABS( g.x - c.x ) < 31 ";
        whereClause += "            AND ( (g.y - c.y)*(g.y -c.y) + (g.x - c.x)*(g.x - c.x) ) < 901 ";
        whereClause += "          ) OR ( ";
        whereClause += "            g.pdistum > 0 ";
        whereClause += "            AND ABS( (g.py - c.y)*(g.x - c.x) - (g.px - c.x)*(g.y - c.y) ) < g.pdistum ";
        whereClause += "          ) ";
        whereClause += "        ) ";
        whereClause += " ) ";

        SQLiteDatabase db = this.wdb;
        String[] args = { };

        long then = System.currentTimeMillis() ;
        int updCnt = db.update(table, values, whereClause, args);
        long now = System.currentTimeMillis() ;

        Log.d( TAG, String.format("checked corona infection. updCnt = %d, queryTime = %d", updCnt, (now -then) ) );

        long currCheckedCnt = this.getCoronaCheckedCnt() ;
        Log.d( TAG, "currCheckCnt = " + currCheckedCnt );
    }

    private ProjCoordinate prevCoord ;
    public void insertGpsLog( Location location) {
        ContentValues values = new ContentValues();
        double latitude = location.getLatitude();;
        double longitude = location.getLongitude();;
        ProjCoordinate coord = this.projection.convertToUtmK( latitude, longitude );

        values.put( "latitude", latitude );
        values.put( "longitude", longitude );

        values.put( "y", coord.y );
        values.put( "x", coord.x );

        double py = coord.y ;
        double px = coord.x ;
        double pdistum = 0 ;

        if( null != this.prevCoord ) {
            py = prevCoord.y ;
            px = prevCoord.x ;
            pdistum = 30*Math.sqrt( (py - coord.y)*(py - coord.y) + (px - coord.x)*(px - coord.x) ) + 1 ;
        }

        values.put( "py", py );
        values.put( "px", px );
        values.put( "pdistum", pdistum );

        String info = "gps latitude = %f, longitude = %f, y = %f, x = %f" ;
        info = String.format(info,  latitude, longitude, coord.y, coord.x );

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

        this.prevCoord = coord ;
    }

    public long getCoronaMaxUpDt() {
        long maxUpdt = 0 ;

        try {
            String sql = " SELECT IFNULL( MAX( up_dt ) , 0 ) AS max_up_dt FROM corona ";

            String[] args = {};
            SQLiteDatabase db = this.rdb;
            Cursor cursor = db.rawQuery(sql, args);

            while (cursor.moveToNext()) {
                maxUpdt = cursor.getLong(0);
            }

            cursor.close();
        } catch( Exception e ) {
            e.printStackTrace();
        }

        return maxUpdt ;
    }

    public void whenCoronaDbReceived(JSONArray response) throws Exception {
        SQLiteDatabase db = this.wdb;

        SimpleDateFormat df = ComInterface.yyyyMMdd_HHmmSS ;

        JSONObject obj ;
        for( int i = 0 ; i < response.length() ; i ++ ) {
            try {
                obj = response.getJSONObject( i );

                long id = obj.getLong( "id" );
                long deleted = obj.getLong( "deleted" );
                long upDt = obj.getLong( "upDt" );
                String place = obj.getString( "place" );
                String patient = obj.getString( "patient" );
                JSONObject geom = obj.getJSONObject( "geom" );
                long visitFr = obj.getLong( "visitFr" );
                long visitTo = obj.getLong( "visitTo" );
                double latitude = geom.getDouble( "lat" );
                double longitude = geom.getDouble( "lon" );

                ProjCoordinate projCoord = this.projection.convertToUtmK( latitude, longitude );

                String info = "[%d] id=%s, upDt: %s, place = %s, patient, %s, deleted = %s, latitude = %s, longitude = %s, y = %f, x = %f, visitFr = %s, visitTo = %s" ;
                info = String.format( info, i, "" + id, df.format(upDt), place, patient, "" + deleted, latitude, longitude, projCoord.y, projCoord.x, df.format( visitFr ), df.format( visitTo ) );

                // Create a new map of values, where column names are the keys
                ContentValues values = new ContentValues();
                values.put("id", id );
                values.put("deleted", deleted );
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

        final int notiLen = null == notifications ? 0 : notifications.length ;

        String sql = "";
        sql += " SELECT IFNULL( COUNT( DISTINCT id), 0 ) AS cnt ";
        sql += " FROM corona ";
        sql += " WHERE deleted = 0 AND checked = 1 " ;
        sql += " AND notification IN (  ";

        for( int i = 0 ; i < notiLen ; i ++ ) {
            sql += 0 < i ? ", ?" : "?" ;
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

        int notiLen = null == notifications ? 0 : notifications.length ;

        String sql = "" ;
        sql += " SELECT id, deleted, checked, notification, up_dt, place, patient, visit_fr, visit_to " ;
        sql += " , latitude, longitude " ;
        sql += " FROM corona " ;
        sql += " WHERE deleted = 0 AND checked = 1 ";
        sql += " AND notification IN (  ";

        for( int i = 0 ; i < notiLen ; i ++ ) {
            sql += 0 < i ? ", ?" : "?" ;
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

            String title = corona.getTitle();
            String snippet = String.format( "%s ~ %s", df.format( corona.visit_fr ) , df.format( corona.visit_to ) );
            corona.content = "동선 겹침 / 자가 격리 요망";

            String info = String.format("corona marker deleted = %d, checked = %d, notification = %d, title = %s, snippet = %s, latitude = %f, longitude = %f, up_dt = %s",
                    corona.deleted, corona.checked, corona.notification, title, snippet, corona.latitude, corona.longitude, df.format( corona.up_dt ) ) ;
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