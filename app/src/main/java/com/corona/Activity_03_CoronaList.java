package com.corona;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;

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
                Corona corona = coronaListView.dataSet.get(position);

                String info = coronaListView.adapter.getSnackbarInfo( corona );

                Snackbar snackbar = Snackbar.make(view, info, Snackbar.LENGTH_LONG);
                snackbar.setAction("No action", null);
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                    }

                    @Override
                    public void onShown(Snackbar snackbar) {
                    }
                });
                snackbar.show();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        this.coronaListView.initData();
    }
}
