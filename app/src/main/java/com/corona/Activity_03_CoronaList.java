package com.corona;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class Activity_03_CoronaList extends ComActivity {

    @Override
    public int getLayoutId() {
        return R.layout.activity_03__corona_list ;
    }

    CoronaListView coronaListView;
    private static CoronaDataAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        coronaListView = this.findViewById(R.id.coronaListView);

        ArrayList<CoronaModel> dataSet = new ArrayList<>();
        dataSet.add(new CoronaModel("Apple Pie", "Android 1.0", "1","September 23, 2008"));
        dataSet.add(new CoronaModel("Banana Bread", "Android 1.1", "2","February 9, 2009"));
        dataSet.add(new CoronaModel("Cupcake", "Android 1.5", "3","April 27, 2009"));
        dataSet.add(new CoronaModel("Donut","Android 1.6","4","September 15, 2009"));
        dataSet.add(new CoronaModel("Eclair", "Android 2.0", "5","October 26, 2009"));
        dataSet.add(new CoronaModel("Froyo", "Android 2.2", "8","May 20, 2010"));
        dataSet.add(new CoronaModel("Gingerbread", "Android 2.3", "9","December 6, 2010"));
        dataSet.add(new CoronaModel("Honeycomb","Android 3.0","11","February 22, 2011"));
        dataSet.add(new CoronaModel("Ice Cream Sandwich", "Android 4.0", "14","October 18, 2011"));
        dataSet.add(new CoronaModel("Jelly Bean", "Android 4.2", "16","July 9, 2012"));
        dataSet.add(new CoronaModel("Kitkat", "Android 4.4", "19","October 31, 2013"));
        dataSet.add(new CoronaModel("Lollipop","Android 5.0","21","November 12, 2014"));
        dataSet.add(new CoronaModel("Marshmallow", "Android 6.0", "23","October 5, 2015"));

        adapter= new CoronaDataAdapter( getApplicationContext(), dataSet );

        coronaListView.setAdapter(adapter);

        coronaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                CoronaModel coronaModel = adapter.dataSet.get(position);

                Snackbar.make(view, coronaModel.getName()+"\n"+ coronaModel.getType()+" API: "+ coronaModel.getVersion_number(), Snackbar.LENGTH_LONG)
                        .setAction("No action", null).show();
            }
        });

    }
}
