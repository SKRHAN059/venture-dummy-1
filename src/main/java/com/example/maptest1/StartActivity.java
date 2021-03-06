package com.example.maptest1;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;

public class StartActivity extends AppCompatActivity {
    private Button mCreateTripButton;
    private Button mJoinTripButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        mCreateTripButton = findViewById(R.id.create_trip);
        mJoinTripButton = findViewById(R.id.join_trip);

        mCreateTripButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("StartActivity", "\'Create Trip\' button clicked");
                openMapActivity();
            }
        });

        mJoinTripButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openJoinTripActivity();
            }
        });
    }

    /**
     * method to start map activity
     * ends current activity
     */
    public void openMapActivity() {
        Log.d("StartActivity", "opening map activity");
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
        finish();
    }

    public void openJoinTripActivity() {
        Log.d("StartActivity", "opening join trip activity");
        Intent intent = new Intent(this, JoinTripActivity.class);
        startActivity(intent);
        finish();
    }
}
