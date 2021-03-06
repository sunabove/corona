package com.corona;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class Activity_010_Splash extends ComActivity {


    private int resumeCnt = 0;

    @Override
    public final int getLayoutId() {
        return R.layout.activity_010_splash;
    }

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        this.hideActionBar();

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
                Intent intent = new Intent( getApplicationContext(), Activity_020_Map.class) ;

                Intent prevIntent = getIntent() ;
                Bundle prevBundle = prevIntent.getExtras() ;
                if( null != prevBundle ) {
                    intent.putExtras( prevBundle );
                }

                startActivity( intent );
            }
        }, 1_000);
    }
}
