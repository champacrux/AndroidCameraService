package com.acruxtek.cameratest;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onTakePicture(View view) {
        startService(new Intent(this, CameraService.class));
    }

    public void onStopService(View view) {
        stopService(new Intent(this, CameraService.class));
    }
}
