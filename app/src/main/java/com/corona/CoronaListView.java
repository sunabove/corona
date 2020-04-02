package com.corona;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.ListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class CoronaListView extends ListView implements ComInterface {
    public CoronaDataAdapter adapter;
    public ArrayList<Corona> dataSet = new ArrayList<>();

    public CoronaListView(Context context) {
        super(context);

        this.initData();
    }

    public CoronaListView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.initData();
    }

    public CoronaListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.initData();
    }

    public CoronaListView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.initData();
    }

    public void initData() {
        boolean test = false ;
        if( null == this.dataSet ) {
            this.dataSet = new ArrayList<>();
        }

        ArrayList<Corona> dataSet = this.dataSet;

        dataSet.clear();

        if( true ) {
            SQLiteDatabase db = DbHelper.getLocationDbHelper(this.getContext()).rdb;

            String sql = "" ;
            sql += " SELECT id, deleted, checked, notification, up_dt, place, patient, visit_fr, visit_to " ;
            sql += " , latitude, longitude " ;
            sql += " FROM corona " ;
            sql += " WHERE  1 = 1 " ;
            sql += " ORDER BY up_dt DESC, visit_fr, visit_to, place, patient " ;
            ;

            String[] args = { };
            Cursor cursor = db.rawQuery(sql, args);

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

                corona.up_dt_str = df.format( corona.up_dt ) ;

                String info = String.format("corona marker deleted = %d, checked = %d, notification = %d, title = %s, snippet = %s, latitude = %f, longitude = %f, up_dt = %s",
                        corona.deleted, corona.checked, corona.notification, corona.title, snippet, corona.latitude, corona.longitude, corona.up_dt_str ) ;
                Log.d( TAG, info );

                dataSet.add( corona );
            }

            cursor.close();
        }

        if( test ) {
            dataSet.add(new Corona("Apple Pie", "Android 1.0", 1, 1));
            dataSet.add(new Corona("Banana Bread", "Android 1.1", 1, 1));
            dataSet.add(new Corona("Cupcake", "Android 1.5", 1, 1));
            dataSet.add(new Corona("Donut", "Android 1.6", 1, 1));
            dataSet.add(new Corona("Eclair", "Android 2.0", 1, 1));
            dataSet.add(new Corona("Froyo", "Android 2.2", 1, 1));
            dataSet.add(new Corona("Gingerbread", "Android 2.3", 1, 1));
            dataSet.add(new Corona("Honeycomb", "Android 3.0", 1, 1));
            dataSet.add(new Corona("Ice Cream Sandwich", "Android 4.0", 1, 1));
            dataSet.add(new Corona("Jelly Bean", "Android 4.2", 1, 1));
            dataSet.add(new Corona("Kitkat", "Android 4.4", 1, 1));
            dataSet.add(new Corona("Lollipop", "Android 5.0", 1, 1));
            dataSet.add(new Corona("Marshmallow", "Android 6.0", 1, 1));
        }

        if( null == this.adapter ) {
            this.adapter = new CoronaDataAdapter(this.getContext(), dataSet);
            this.setAdapter(adapter);
        }
    }
}
