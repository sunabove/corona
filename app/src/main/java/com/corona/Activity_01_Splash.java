package com.corona;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Window;

public class Activity_01_Splash extends ComActivity {


    private int resumeCnt = 0;

    @Override
    public final int getLayoutId() {
        return R.layout.activity_01_splash;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        getSupportActionBar().hide();

        this.hideActionBar();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            startForegroundService(new Intent(this, LocationService.class));
        else
            startService(new Intent(this, LocationService.class));

        this.moveToNextActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.v(TAG, "onResume");

        this.resumeCnt ++ ;

        if( 1 < resumeCnt ) {
            this.finish();
        }
    }

    private void moveToNextActivity() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                startActivity(new android.content.Intent(Activity_01_Splash.this, Activity_02_Map.class));
            }
        }, 1_000);
    }
}
