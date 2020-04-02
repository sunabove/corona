package com.corona;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.google.android.material.snackbar.Snackbar;

public class Activity_03_CoronaList extends ComActivity {

    private CoronaListView coronaListView;

    @Override
    public int getLayoutId() {
        return R.layout.activity_03_corona_list;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        coronaListView = this.findViewById(R.id.coronaListView);

        coronaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                Corona coronaModel = coronaListView.dataSet.get(position);

                Snackbar.make(view, coronaModel.place+"\n"+ coronaModel.patient+" API: "+ coronaModel.visit_fr, Snackbar.LENGTH_LONG)
                        .setAction("No action", null).show();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        this.coronaListView.initData();
    }
}
