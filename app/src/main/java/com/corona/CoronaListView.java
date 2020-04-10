package com.corona;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.android.material.snackbar.Snackbar;

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

    private Activity getActivity(View view) {
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }

    private void initView() {
        final View view = this ;

        this.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        this.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Corona corona = dataSet.get(position);

                Corona c = corona;
                SimpleDateFormat df = ComInterface.MMdd_HHmm ;
                String info = String.format("동선 겹침: %s, 위도 %.4f, 경도 %.4f", df.format(c.visit_fr), c.latitude, c.longitude ) ;
                info += "\n잠시후 지도로 이동합니다.";

                Snackbar snackbar = Snackbar.make(view, info, Snackbar.LENGTH_SHORT );
                snackbar.setAction("No action", null);

                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        Activity activity = getActivity( view ) ;

                        Intent intent = new Intent();
                        Bundle bundle = new Bundle();
                        bundle.putSerializable( "corona", corona ) ;
                        intent.putExtras(bundle);
                        activity.setResult( INTENT_RESULT_CORONA_SELECTED, intent );

                        activity.finish();//finishing activity
                    }

                    @Override
                    public void onShown(Snackbar snackbar) {
                        DbHelper dbHelper = DbHelper.getLocationDbHelper(CoronaListView.this.getContext());
                        dbHelper.updateCoronaNotification( corona, 2 );
                    }
                });

                snackbar.show();
            }
        });
    }

    public void initData() {
        boolean test = false ;

        if( null == this.dataSet ) {
            this.dataSet = new ArrayList<>();

            this.initView();
        }

        ArrayList<Corona> dataSet = this.dataSet;

        dataSet.clear();

        DbHelper dbHelper = DbHelper.getLocationDbHelper(this.getContext()) ;

        ArrayList<Corona> list = dbHelper.getCoronaListInfected( 1, 2 );

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
