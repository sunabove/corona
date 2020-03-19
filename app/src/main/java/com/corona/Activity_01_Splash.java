package com.corona;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class Activity_01_Splash extends ComActivity {

    @Override
    public final int getLayoutId() {
        return R.layout.activity_01_splash;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.v(TAG, "onResume");

        this.moveToNextActivity();
    }

    private void moveToNextActivity() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                startActivity(new android.content.Intent(Activity_01_Splash.this, Activity_02_Map.class));
            }
        }, 1_200);
    }
}
