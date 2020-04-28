package com.example.maptest1;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class JoinTripActivity extends AppCompatActivity {

    private Button mJoinButton;
    private Button mCancelButton;
    private EditText mEdit;

    private final String TAG = "JoinTripActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_trip);

        mJoinButton = findViewById(R.id.join_submit);
        mCancelButton = findViewById(R.id.join_cancel);

        mJoinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEdit = findViewById(R.id.input_trip_id);
                openMapActivity(mEdit.getText().toString());
            }
        });

        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openStartActivity();
            }
        });
    }

    /**
     * Method to open start activity
     * Ends current join activity
     */
    private void openStartActivity() {
        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Method to open start activity
     * Ends current join activity
     */
    private void openMapActivity(String aTripID) {
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putExtra("tripID", aTripID);
        startActivity(intent);
        finish();
    }
}
