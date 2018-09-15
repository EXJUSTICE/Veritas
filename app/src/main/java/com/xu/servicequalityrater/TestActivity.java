package com.xu.servicequalityrater;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.xu.servicequalityrater.services.HealthPictureService2;

public class TestActivity extends AppCompatActivity {
    /*TODO this class exists only to test the HealthPictureService 2
    TODO when done, delete this and activity_test, and mod launcher in manifest
    TODO 0705 added in emotionrecycler and checkpermission()
    */
     Button launch;
    Button launchemotion;
    public static final int PERMISSIONS_MULTIPLE_REQUEST = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        if (Build.VERSION.SDK_INT >= 23 && !(checkSelfPermission("android.permission.CAMERA") == PackageManager.PERMISSION_GRANTED && checkSelfPermission("android.permission.READ_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED  && checkSelfPermission("android.permission.WRITE_EXTERNAL_STORAGE") == PackageManager.PERMISSION_GRANTED && checkSelfPermission("android.permission.READ_PHONE_STATE") == PackageManager.PERMISSION_GRANTED)) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.CAMERA", "android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE","android.permission.READ_PHONE_STATE"}, PERMISSIONS_MULTIPLE_REQUEST);
        }

        launchemotion = (Button)findViewById(R.id.launchemotion);
        launchemotion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent emoteintent= new Intent(TestActivity.this, EmotionRecyclerActivity.class);
                startActivity(emoteintent);
            }
        });

       launch=(Button)findViewById(R.id.launcher);


        launch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent servintent = new Intent(TestActivity.this, HealthPictureService2.class);
                startService(servintent);
            }
        });



    }
}
