package com.example.maptest1;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.text.HtmlCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.Marker;
import com.google.android.libraries.places.api.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Arrays;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    //Main activity variables
    private static GoogleMap mMap;
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private DataSnapshot mDataSnapshot;
    private Trip mTrip;
    private Car mCar;

    //Location variables
    private LatLng mCurrentLocation;
    private boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private ImageView mGpsIcon;
    private Place mPlace;
    private Handler mHandler = new Handler();
    private Runnable mRunnable;

    //Buttons
    private Button mBackButton;
    private Button mMapRouteButton;
    private Button mRouteCancelButton;
    private Marker mPlaceMarker;

    //Logging
    private final String TAG = "MapsActivity";

    //API key
    private final String mApiKey = "AIzaSyAaXzqfxCReWdAoHbHecNQDVqCedv1qChQ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Initialize the SDK
        Places.initialize(getApplicationContext(), mApiKey);

        // Initialize the AutocompleteSupportFragment.
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        autocompleteFragment.setPlaceFields(Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG));

        // Set up a PlaceSelectionListener to handle the response.
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                mPlace = place;
                locateAddress(place);
                mBackButton.setVisibility(View.GONE);
                mMapRouteButton.setVisibility(View.VISIBLE);
                mRouteCancelButton.setVisibility(View.VISIBLE);
                Log.i(TAG, "Place: " + place.getName() + ", " + place.getId());

                mMapRouteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //TODO: route polylines from current location to marker

                        //temporary call to populate trip into firebase
                        createTrip();
                    }
                });

                mRouteCancelButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mPlaceMarker.remove();
                        mMapRouteButton.setVisibility(View.GONE);
                        mRouteCancelButton.setVisibility(View.GONE);
                        mBackButton.setVisibility(View.VISIBLE);

                        TextView tripIDView = findViewById(R.id.trip_id);
                        tripIDView.setVisibility(View.GONE);
                        tripIDView.setText("Trip Key");
                    }
                });
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i(TAG, "An error occurred: " + status);
            }
        });

        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "onDataChange: " + dataSnapshot.toString());
                    mDataSnapshot = dataSnapshot;
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if (checkLocationServices()) {
            if (!mLocationPermissionGranted) {
                getLocationPermission();
            }
        }
    }

    /**
     * Obtains and sets up map fragment
     */
    public void initializeMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "This is a test", Toast.LENGTH_SHORT).show();
        mMap = googleMap;

        //Obtain and populate Trip Key on map layout
        TextView tripIDView = findViewById(R.id.trip_id);
        tripIDView.setVisibility(View.GONE);

        //Set button listener for returning to Start Activity
        mBackButton = findViewById(R.id.back_button);
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openStartActivity();
            }
        });

        //Set route button and visibility for map route button
        mMapRouteButton = findViewById(R.id.map_route);
        mMapRouteButton.setVisibility(View.GONE);

        //Set route cancel button and visibility for cancel button
        mRouteCancelButton = findViewById(R.id.cancel_map_route);
        mRouteCancelButton.setVisibility(View.GONE);

        //Set GPS fixed widget to move camera to current location
        mGpsIcon = findViewById(R.id.ic_gps_icon);

        if (mLocationPermissionGranted) {
            getDeviceLocation();
            mMap.setMyLocationEnabled(true);

            // Set listener for GPS icon to obtain current location
            mGpsIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    getDeviceLocation();
                }
            });
        }

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(17.5707, -3.9962);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }

    private void getDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        if (mLocationPermissionGranted) {
            Task location = mFusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        Location currentLocation = (Location) task.getResult();
                        if (mMap != null && currentLocation!= null) {
                            Log.d(TAG, "Obtaining and mapping to current location");
                            mCurrentLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLocation, 16f));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);

                            //If join trip activity
                            String tripID = getIntent().getStringExtra("tripID");
                            if (tripID != null) {
                                Log.d(TAG, "Calling join trip");
                                joinTrip(tripID);
                            }
                        }
                    }
                }
            });
        }
    }

    /**
     * Method to open start activity
     * Ends current map activity
     */
    private void openStartActivity() {
        Intent intent = new Intent(this, StartActivity.class);
        startActivity(intent);
        finish();
    }


    /**
     * Determines if map service is enabled
     * @return true if map content is enabled, false otherwise
     */
    private boolean checkLocationServices(){
        if(checkGoogleServices() && isMapsEnabled()){
            return true;
        }
        return false;
    }

    private void buildAlertMessageNoGps() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setMessage("VENTURE requires GPS to be enabled.\nWould you like to enable it?").setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent, 1);
                    }
                });
        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    /**
     * Checks if application has permission after enabling location services.
     * If the application does not have permission, calls method to ask user
     * for permission to allow the application to access device's location
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1: {
                if(!mLocationPermissionGranted){
                    getLocationPermission();
                }
            }
        }

    }

    /**
     * checks if GPS is enabled
     * @return true if GPS is enabled, false otherwise
     */
    private boolean isMapsEnabled(){
        final LocationManager locationManager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }

    /**
     * Obtains the device's location permissions. Request is handled by
     * onRequestPermissionsResult callback.
     */
    private void getLocationPermission() {
        String[] permissions  = new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                initializeMap();
            } else {
                ActivityCompat.requestPermissions(this, permissions, 1);
            }
        } else {
            ActivityCompat.requestPermissions(this, permissions, 1);
        }
    }

    /**
     * Determines if Google Play services is working and can make map requests
     * @return returns true if user has Google Play services enabled, false otherwise
     */
    public boolean checkGoogleServices(){
        int available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MapsActivity.this);

        if(available == ConnectionResult.SUCCESS){
            return true;
        }
        Toast.makeText(this, "You can't make map requests", Toast.LENGTH_SHORT).show();
        return false;
    }

    /**
     * Sets mLocationPermissionGranted to true based on permission request results
     * @param requestCode specified code to request permissions
     * @param permissions list of permissions in Manifest
     * @param grantResults list of request results
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            Log.d("MapActivity", "Unable to grant location permissions to application");
                            return;
                        }
                    }
                    mLocationPermissionGranted = true;
                    initializeMap();
                }
            }
        }
    }

    /**
     * Function to call function to mark location on map and initialize marker member variable
     * @param place argument to mark on map
     */
    public void locateAddress(Place place) {
        mPlaceMarker = markLocationOnMap(place.getLatLng(), place.getAddress());
    }

    /**
     * Function to mark specified location on map
     * @param latLng coordinates to create marker at
     * @param name name of marker
     * @return Marker object to assign to member variable Marker
     */
    public Marker markLocationOnMap(LatLng latLng, String name) {
        MarkerOptions markerOptions = new MarkerOptions().position(latLng).title(name);
        Marker marker = mMap.addMarker(markerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latLng.latitude, latLng.longitude), 16f));
        return marker;
    }

    /**
     * Function to create trip
     * Creates new trip object and populates trip and car info into firebase
     */
    private void createTrip() {
        //Create Trip and Car objects for Trip
        mTrip = new Trip(mPlace.getLatLng(), mPlace.getAddress());
        mCar = new Car(65, mCurrentLocation, mTrip.getTripID());
        mTrip.addCar(mCar);

        //Text View for trip ID
        TextView tripID = findViewById(R.id.trip_id);
        String htmlTripID = "<br /><font color=#2B97EB>"+mTrip.getTripID()+"</font>";
        tripID.append(HtmlCompat.fromHtml(htmlTripID, HtmlCompat.FROM_HTML_MODE_LEGACY));
        tripID.setVisibility(View.VISIBLE);

        //Add trip info to firebase
        mDatabase.child("trips").child(mTrip.getTripID()).child("address").setValue(mTrip.getDestAddress());
        mDatabase.child("trips").child(mTrip.getTripID()).child("id").setValue(mTrip.getTripID());
        mDatabase.child("trips").child(mTrip.getTripID()).child("latitude").setValue(mTrip.getTripLat());
        mDatabase.child("trips").child(mTrip.getTripID()).child("longitude").setValue(mTrip.getTripLong());

        //Add car info to trip in firebase
        mDatabase.child("trips").child(mTrip.getTripID()).child("cars").child(mCar.getCarName()).child("latitude").setValue(mCar.getCarLat());
        mDatabase.child("trips").child(mTrip.getTripID()).child("cars").child(mCar.getCarName()).child("longitude").setValue(mCar.getCarLong());

        startPartyLocationsRunnable();
    }

    /**
     * Runnable to request party (car) locations every 4 seconds
     */
    private void startPartyLocationsRunnable(){
        Log.d(TAG, "startUserLocationsRunnable: starting runnable for retrieving updated locations.");
        mHandler.postDelayed(mRunnable = new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(mRunnable, 4000);
                updatePartyLocations();
            }
        }, 4000);
    }

    /**
     * Function to create new car to add to trip in firebase
     * @param tripID specified trip id to add new car to
     */
    private void joinTrip(String tripID) {
        if (mDataSnapshot != null) {
            Log.d(TAG, "Joining trip: " + mDataSnapshot.toString());

            DataSnapshot dataSnapshot = mDataSnapshot.child("trips").child(tripID);
            if (dataSnapshot.getValue() == null) {
                //TODO: handle invalid trip ID entry from user
                Log.d(TAG, "joinTrip: Exiting: Handle trip IDs not in Firebase");
                System.exit(0);
            }

            getPartyLocations(tripID);

            //iterate through all cars in trip to determine new car ID
            int newCarID = 0;
            for (int i = 0; i < mTrip.getCars().size(); i++) {
                if (mTrip.getCars().get(i).getCarID() > newCarID) {
                    newCarID = mTrip.getCars().get(i).getCarID();
                }
            }
            newCarID++;
            Car newCar = new Car(newCarID, mCurrentLocation, tripID);
            mTrip.addCar(newCar);

            //Add car info to trip in firebase respectively
            Log.d(TAG, "Joining trip: " + newCar.getCarName());
            mDatabase.child("trips").child(tripID).child("cars").child(newCar.getCarName()).child("latitude").setValue(mCurrentLocation.latitude);
            mDatabase.child("trips").child(tripID).child("cars").child(newCar.getCarName()).child("longitude").setValue(mCurrentLocation.longitude);
        }
    }

    /**
     * Function to put update car coordinates into Trip member variable
     * Obtains data snapshot from firebase and creates new Trip object
     * @param tripID
     */
    private void getPartyLocations(String tripID) {
        Log.d(TAG, "getPartyLocations: obtaining updated locations from firebase.");

        DataSnapshot dataSnapshot = mDataSnapshot.child("trips").child(tripID);
        Trip trip = new Trip(new LatLng((Double)dataSnapshot.child("latitude").getValue(), (Double)dataSnapshot.child("longitude").getValue()),
                (String)dataSnapshot.child("address").getValue(), tripID);

        //Add cars from data snapshot to trip object
        for (DataSnapshot ds : dataSnapshot.child("cars").getChildren()) {
            Log.d(TAG, "getPartyLocations: Joining trip (car info): " + ds.toString());

            trip.addCar(new Car((int)ds.getKey().charAt(0), new LatLng(((Map<String, Double>)ds.getValue()).get("latitude"),
                    ((Map<String, Double>)ds.getValue()).get("latitude")), tripID));
        }
        mTrip = trip;


    }

    /**
     * Function to update all car locations in Map
     */
    private void updatePartyLocations() {
        //TODO: update all cars in Map using Trip member--use after calling getPartyLocations()
    }
}
