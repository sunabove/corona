package com.corona;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class CoronaListView extends ListView implements ComInterface {
    public CoronaDataAdapter adapter;
    public ArrayList<Corona> dataSet ;

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

            this.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        }

        ArrayList<Corona> dataSet = this.dataSet;

        dataSet.clear();

        DbHelper dbHelper = DbHelper.getLocationDbHelper(this.getContext()) ;

        ArrayList<Corona> list = dbHelper.getCoronaListInfected( 0, 1, 2 );

        dataSet.addAll( list );

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
