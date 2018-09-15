package com.xu.servicequalityrater;

/**
 * Created by Omistaja on 14/04/2017.
 */

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
        import android.content.Intent;
        import android.os.Bundle;
        import android.support.v7.app.AppCompatActivity;

public class Splash extends AppCompatActivity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        new Thread() {
            public void run() {
                try {
                    this.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Splash.this.startActivity(new Intent(Splash.this, CameraActivity.class));
                    Splash.this.finish();
                    Splash.this.overridePendingTransition(0, R.anim.custom_fadeout);
                }
            }
        }.start();
    }
}
