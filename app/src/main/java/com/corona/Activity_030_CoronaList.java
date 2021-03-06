package com.corona;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class Activity_030_CoronaList extends ComActivity {

    private CoronaListView coronaListView;
    private TextView status;

    @Override
    public int getLayoutId() {
        return R.layout.activity_030_corona_list;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.status = this.findViewById(R.id.status );
        this.coronaListView = this.findViewById(R.id.coronaListView);

        /*
        this.coronaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Corona corona = coronaListView.dataSet.get(position);

                coronaListView.setSelection( position );

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
        */

    }

    @Override
    protected void onResume() {
        super.onResume();

        this.coronaListView.initData();

        int count = this.coronaListView.dataSet.size() ;

        String info = "";
        if( 1 > count ) {
            info = "조회할 데이터가 없습니다." ;
        } else {
            info = String.format("%d건의 데이터가 조회되었습니다.",  count);
        }

        this.status.setText( info );

        Intent intent = getIntent();

        if( null != intent ) {
            this.whenNewIntentReceived( intent );
        }
    }

    private void whenNewIntentReceived(Intent intent) {
        Bundle bundle = intent.getExtras();
        if( null != bundle ) {
            Corona corona = (Corona) bundle.getSerializable(corona_from_notification_click );
            if ( null != corona ) {
                Log.d(TAG, "corona_from_notification_click corona id = " + corona.id);

                this.coronaListView.adapter.selCoronaId = corona.id ;
            }
        }
    }
}
