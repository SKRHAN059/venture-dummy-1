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
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
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
import com.google.maps.DirectionsApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.internal.PolylineEncoding;
import com.google.maps.model.DirectionsLeg;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static android.graphics.BlendMode.HUE;

public class MapsActivity extends FragmentActivity implements
        OnMapReadyCallback,
        GoogleMap.OnMarkerClickListener {

    //Main activity variables
    private static GoogleMap mMap;
    private DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    private DataSnapshot mDataSnapshot;
    private Trip mTrip;
    private Car mCar;
    private String mTripID;
    private boolean mEndTrip = false;

    //Location variables
    private LatLng mCurrentLocation;
    private final Object mLocationLock = new Object();
    private boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private ImageView mGpsIcon;
    private Place mPlace;
    private Handler mHandler = new Handler();
    private Runnable mRunnable;
    private Marker mPlaceMarker;

    //Direction variables
    private GeoApiContext mGeoApiContext = null;
    private List<PolyPair> mPolylineData = new ArrayList<>();
    private PolyPair mTripPolylineData;
    private String mMarkerClickedTitle;

    //Buttons
    private Button mBackButton;
    private Button mMapRouteButton;
    private Button mStartTripButton;
    private Button mRouteCancelButton;
    private Button mEndTripButton;

    //Logging
    private final String TAG = "MapsActivity";

    //API key
    private final String mApiKey = "AIzaSyAaXzqfxCReWdAoHbHecNQDVqCedv1qChQ";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: creating mapsActivity");
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: creating mapsActivity");
        setContentView(R.layout.activity_maps);

        // Initialize the SDK
        Log.d(TAG, "onCreate: initializing SDK");
        Places.initialize(getApplicationContext(), mApiKey);

        // Initialize the AutocompleteSupportFragment.
        Log.d(TAG, "onCreate: initializing AutoCompleteSupportFragment");
        AutocompleteSupportFragment autocompleteFragment = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.autocomplete_fragment);

        // Specify the types of place data to return.
        Log.d(TAG, "onCreate: specifying place field types");
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
                    // if all car instances removed from database, remove trip from database
                    if (mEndTrip) {
                        if (!mDataSnapshot.child("trips").child(mTripID).child("cars").exists()) {
                            Log.d(TAG, "onDataChange: all cars are gone, remove trip instance from firebase");
                            mDatabase.child("trips").child(mTripID).setValue(null);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        Log.d(TAG, "onCreate: permissions and location");
        if (checkLocationServices()) {
            if (!mLocationPermissionGranted) {
                getLocationPermission();
            }
        }

        Log.d(TAG, "NEED TO CALL JOIN TRIP");
    }

    /**
     * Obtains and sets up map fragment
     */
    public void initializeMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (mGeoApiContext == null) {
            mGeoApiContext = new GeoApiContext.Builder().apiKey(mApiKey).build();
        }
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

        //Initialize start trip button and set visibility
        mStartTripButton = findViewById(R.id.start_trip);
        mStartTripButton.setVisibility(View.GONE);

        //Initialize end trip button and set visibility
        mEndTripButton = findViewById(R.id.end_trip);
        mEndTripButton.setVisibility(View.GONE);

        //Set GPS fixed widget to move camera to current location
        mGpsIcon = findViewById(R.id.ic_gps_icon);

        if (mLocationPermissionGranted) {
            getDeviceLocation();
            mMap.setMyLocationEnabled(true);

            // Set listener for GPS icon to obtain current location
            mGpsIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    updateDeviceLocation();
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLocation, 16f));
                }
            });
        }

        //button listener to populate routes onto map
        mMapRouteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getPlaceDirections(mPlaceMarker);
                mMapRouteButton.setVisibility(View.GONE);
                mStartTripButton.setVisibility(View.VISIBLE);
            }
        });

        // button listener to restart map activity
        mRouteCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mPlaceMarker.remove();
                mMapRouteButton.setVisibility(View.GONE);
                mRouteCancelButton.setVisibility(View.GONE);
                mStartTripButton.setVisibility(View.GONE);
                mBackButton.setVisibility(View.VISIBLE);

                TextView tripIDView = findViewById(R.id.trip_id);
                tripIDView.setVisibility(View.GONE);
                tripIDView.setText("Trip Key");

                for (PolyPair p : mPolylineData) {
                    p.getPolyline().remove();
                }

                mPolylineData.clear();
            }
        });

        //button listener to start trip with selected polyline
        mStartTripButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStartTripButton.setVisibility(View.GONE);
                mRouteCancelButton.setVisibility(View.GONE);
                mEndTripButton.setVisibility(View.VISIBLE);

                for (PolyPair p : mPolylineData) {
                    if (!p.getPolyline().getId().equals(mTripPolylineData.getPolyline().getId())) {
                        p.getPolyline().remove();
                    }
                }

                createTrip();
            }
        });

        mEndTripButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "endTripButton: button clicked");
                mHandler.removeCallbacks(mRunnable);
                mEndTripButton.setVisibility(View.GONE);
                mBackButton.setVisibility(View.VISIBLE);
                mPlaceMarker.remove();
                try {
                    endTrip();
                } catch (InterruptedException e) {
                    Log.d(TAG, "endTripButton: " + e.toString());
                }
            }
        });

        //set marker click listener
        mMap.setOnMarkerClickListener(this);

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
                            Log.d(TAG, "getDeviceLocation: Obtaining and mapping to current location");
                            mCurrentLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                            if (getIntent().getStringExtra("tripID") != null) {
                                Log.d(TAG, "getDeviceLocation: calling joinTrip()");
                                joinTrip(getIntent().getStringExtra("tripID"));
                                mBackButton.setVisibility(View.GONE);
                                mEndTripButton.setVisibility(View.VISIBLE);
                            }
                            Log.d(TAG, "getDeviceLocation: current location found");

                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mCurrentLocation, 16f));
                        }
                    }
                }
            });
        }
    }

    private void updateDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        if (mLocationPermissionGranted) {
            Task location = mFusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Location currentLocation = (Location) task.getResult();
                    Log.d(TAG, "updateDeviceLocation: Obtaining and mapping to current location");
                    mCurrentLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
                    if (mCar != null) {
                        Log.d(TAG, "updateDeviceLocation: Updating current car coordinates");
                        mCar.setLat(mCurrentLocation.latitude);
                        mCar.setLong(mCurrentLocation.longitude);
                    }
                    Log.d(TAG, "updateDeviceLocation: current location found");
                    mMap.getUiSettings().setMyLocationButtonEnabled(false);

                    //Add car info to trip in firebase
                    if (mTrip != null &&  mCar != null) {
                        Log.d(TAG, "updateDeviceLocation: writing car info to database: " + mCar.getCarName());
                        mDatabase.child("trips").child(mTrip.getTripID()).child("cars")
                                .child(mCar.getCarName()).child("latitude").setValue(mCar.getCarLat());
                        mDatabase.child("trips").child(mTrip.getTripID()).child("cars")
                                .child(mCar.getCarName()).child("longitude").setValue(mCar.getCarLong());
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
        mPlaceMarker.setTitle(place.getAddress());
        mPlaceMarker.showInfoWindow();

        //Set default marker clicked
        mMarkerClickedTitle = place.getAddress();
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

        //Initialize default marker title to destination address
        mMarkerClickedTitle = mTrip.getDestAddress();

        startTripRunnable();
    }

    private void endTrip() throws InterruptedException {
        mEndTrip = true;
        Log.d(TAG, "endTrip(): ending trip");
        TimeUnit.MILLISECONDS.sleep(250); //set 250ms delay for runnable to end

        //remove car instance from firebase
        mDatabase.child("trips").child(mTrip.getTripID()).child("cars")
                .child(mCar.getCarName()).child("latitude").setValue(null);
        mDatabase.child("trips").child(mTrip.getTripID()).child("cars")
                .child(mCar.getCarName()).child("longitude").setValue(null);

        //remove current polyline data
        for (PolyPair p : mPolylineData) {
            p.getPolyline().remove();
        }
        mPolylineData.clear();

        //remove party's polyline data
        try {
            for (Car car : mTrip.getCars()) {
                for (PolyPair p : car.getPolyineData()) {
                    p.getPolyline().remove();
                }
                car.getPolyineData().clear();
                car.getCarPolylineData().getPolyline().remove();
            }
        } catch (Exception e) {
            Log.d(TAG, "endTrip: not yet defined: " + e.getMessage());
        }

        //remove party's markers
        removeCarMarkers();

        mTripID = mTrip.getTripID();
        mTrip = null;

        //Remove text view for trip ID
        TextView tripIDView = findViewById(R.id.trip_id);
        tripIDView.setText("");
        tripIDView.setVisibility(View.GONE);

//        if (getIntent().getStringExtra("tripID") != null) {
//            Intent intent = new Intent(this, StartActivity.class);
//            startActivity(intent);
//            finish();
//        }

        //TODO: end current trip
        //remove instance of car from trip in firebase
        //if all cars are removed from trip in firebase, then remove trip instance from firebase
    }

    /**
     * Runnable to request party (car) locations every 4 seconds
     */
    private void startTripRunnable(){
        Log.d(TAG, "startUserLocationsRunnable: starting runnable for retrieving updated locations.");
        mHandler.postDelayed(mRunnable = new Runnable() {
            @Override
            public void run() {
                mHandler.postDelayed(mRunnable, 4000);
                Log.d(TAG, "startTripRunnable: calling updateDeviceLocation");
                updateDeviceLocation();
                Log.d(TAG, "startTripRunnable: calling getPlaceDirections(mPlaceMarker)");
                getPlaceDirections(mPlaceMarker);

                removeCarPolylines();
                getPartyLocations(mTrip.getTripID());
                updatePartyLocations();
                for (Car car : mTrip.getCars()) {
                    if (car.getCarID() != mCar.getCarID())
                        getCarDirections(mPlaceMarker, car);
                }
            }
        }, 4000);
    }

    /**
     * Function to create new car to add to trip in firebase
     * @param tripID specified trip id to add new car to
     */
    private void joinTrip(String tripID) {
        if (mDataSnapshot != null) {
            //Initialize trip object with info from firebase
            DataSnapshot dataSnapshot = mDataSnapshot.child("trips").child(tripID);
            Log.d(TAG, "joinTrip: " + dataSnapshot.toString());

            //Check for invalid tripID from user
            if (dataSnapshot.getValue() == null) {
                //TODO: handle invalid trip ID entry from user
                Log.d(TAG, "joinTrip: Exiting: Handle trip IDs not in Firebase");
                System.exit(0);
            }

            String address = (String) dataSnapshot.child("address").getValue();
            String id = (String) dataSnapshot.child("id").getValue();
            double lat = (Double) dataSnapshot.child("latitude").getValue();
            double lng = (Double) dataSnapshot.child("longitude").getValue();
            LatLng tripLatLng = new LatLng(lat, lng);
            mTrip = new Trip(tripLatLng, address, id);

            mPlaceMarker = markLocationOnMap(tripLatLng, mTrip.getDestAddress());
            mPlaceMarker.setTitle(mTrip.getDestAddress());
            mPlaceMarker.showInfoWindow();

            //calculate directions and draw polyline
            getPlaceDirections(mPlaceMarker);

            //add all car instances from firebase into trip object
            getPartyLocations(tripID);

            //iterate through all cars in trip to determine new car ID
            int newCarID = 65;
            for (int i = 0; i < mTrip.getCars().size(); i++) {
                if (mTrip.getCars().get(i).getCarID() == newCarID) {
                    newCarID++;
                } else {
                    break;
                }
            }
            mCar = new Car(newCarID, mCurrentLocation, tripID);
            mTrip.addCar(mCar);


            //Add car info to trip in firebase respectively
            Log.d(TAG, "Joining trip: " + mCar.getCarName());
            mDatabase.child("trips").child(tripID).child("cars").child(mCar.getCarName()).child("latitude").setValue(mCurrentLocation.latitude);
            mDatabase.child("trips").child(tripID).child("cars").child(mCar.getCarName()).child("longitude").setValue(mCurrentLocation.longitude);

            //Initialize default marker title to destination address
            mMarkerClickedTitle = mTrip.getDestAddress();

            //Initiate runnable to update polylines and car information
            startTripRunnable();
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

        removeCarMarkers();
        mTrip.clearCars();
        //Add cars from data snapshot to trip object
        Log.d(TAG, "getPartyLocations: adding car info from database to trip object");
        for (DataSnapshot ds : dataSnapshot.child("cars").getChildren()) {
            Log.d(TAG, "getPartyLocations: Joining trip (car info): " + ds.toString());
            mTrip.addCar(new Car((int) ds.getKey().charAt(0), new LatLng(((Map<String, Double>) ds.getValue()).get("latitude"),
                    ((Map<String, Double>) ds.getValue()).get("longitude")), tripID));
        }
    }

    /**
     * Function to update all car locations in Map
     */
    private void updatePartyLocations() {
        for (Car car : mTrip.getCars()) {
            if (car.getCarID() == mCar.getCarID())
                continue;
//            Log.d(TAG, "updatePartyLocations: Lat:" + car.getCarLat() + ", Long:" + car.getCarLong());
            MarkerOptions markerOptions = new MarkerOptions()
                    .position(car.getLatLng())
                    .title(car.getCarName());
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
            car.setMarker(mMap.addMarker(markerOptions));
        }
    }

    private void getPlaceDirections(Marker marker){
        Log.d(TAG, "getPlaceDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude);
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

        directions.alternatives(true);
        directions.origin(new com.google.maps.model.LatLng(
                mCurrentLocation.latitude,
                mCurrentLocation.longitude));

        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                //add polyline to map
                drawPolylines(result);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage() );
            }
        });
    }

    private void drawPolylines(final DirectionsResult result){
        //post onto main thread to update map
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                float min = Float.MAX_VALUE;
                for(DirectionsRoute route: result.routes){
                    List<com.google.maps.model.LatLng> path = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> paths = new ArrayList<>();
                    Log.d(TAG, "drawPolylines: getting polyline route");
                    for(com.google.maps.model.LatLng latLng: path) {
                        paths.add(new LatLng(latLng.lat, latLng.lng));
                    }

                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(paths));
                    polyline.setWidth(10);
                    polyline.setVisible(false);

                    mPolylineData.add(new PolyPair(polyline, route.legs[0]));

                    if (route.legs[0].duration.inSeconds < min) {
                        min = route.legs[0].duration.inSeconds;
                        drawPolylineHelper(polyline);
                        if (mTrip == null) {
                            zoomOnPolylines(polyline.getPoints());
                        }
                    }
                }
                mPlaceMarker.setSnippet("Duration: " + mTripPolylineData.getDirectionsLeg().duration);
            }
        });
    }

    public void drawPolylineHelper(Polyline polyline) {
        for (PolyPair p : mPolylineData) {
            if (p.getPolyline().getId().equals(polyline.getId())) {
                //highlight selected trip option
                p.getPolyline().setVisible(true);
                if (mMarkerClickedTitle.length() > 1) {
                    p.getPolyline().setColor(Color.BLUE);
                    p.getPolyline().setZIndex(1);
                } else {
                    p.getPolyline().setColor(Color.GREEN);
                    p.getPolyline().setZIndex(0);
                }

                try {
                    mTripPolylineData.getPolyline().setPoints(p.getPolyline().getPoints());
                    mTripPolylineData.setDirectionsLeg(p.getDirectionsLeg());
                    Log.d(TAG, "drawPolylineHelper: setting points for existing polyline");
                } catch (Exception e) {
                    mTripPolylineData = p;
                }

                //update place marker with trip option details
                mPlaceMarker.setSnippet("Duration: " + p.getDirectionsLeg().duration);
            } else {
                p.getPolyline().remove();
            }
        }
    }

    public void zoomOnPolylines(List<LatLng> aLatLngList) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : aLatLngList)
            builder.include(latLng);
        int padding = 170;
        LatLngBounds bounds = builder.build();

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding), 300, null);
    }

    private void getCarDirections(Marker marker, Car car){
        Log.d(TAG, "getCarDirections: calculating directions.");

        com.google.maps.model.LatLng destination = new com.google.maps.model.LatLng(
                marker.getPosition().latitude,
                marker.getPosition().longitude);
        DirectionsApiRequest directions = new DirectionsApiRequest(mGeoApiContext);

        directions.alternatives(true);
        directions.origin(new com.google.maps.model.LatLng(
                car.getCarLat(),
                car.getCarLong()));

        directions.destination(destination).setCallback(new PendingResult.Callback<DirectionsResult>() {
            @Override
            public void onResult(DirectionsResult result) {
                //add polyline to map
                drawCarPolyline(result, car);
            }

            @Override
            public void onFailure(Throwable e) {
                Log.e(TAG, "calculateDirections: Failed to get directions: " + e.getMessage() );
            }
        });
    }

    private void drawCarPolyline(final DirectionsResult result, Car car) {
        //post onto main thread to update map
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                float min = Float.MAX_VALUE;
                for(DirectionsRoute route: result.routes){
                    List<com.google.maps.model.LatLng> path = PolylineEncoding.decode(route.overviewPolyline.getEncodedPath());

                    List<LatLng> paths = new ArrayList<>();
                    Log.d(TAG, "drawCarPolyline: getting polyline route");
                    for(com.google.maps.model.LatLng latLng: path) {
                        paths.add(new LatLng(latLng.lat, latLng.lng));
                    }

                    Polyline polyline = mMap.addPolyline(new PolylineOptions().addAll(paths));
                    polyline.setWidth(10);
                    polyline.setVisible(false);

                    car.getPolyineData().add(new PolyPair(polyline, route.legs[0]));

                    if (route.legs[0].duration.inSeconds < min) {
                        min = route.legs[0].duration.inSeconds;
                        drawCarPolylinesHelper(polyline, car);
                    }
                }
                for (PolyPair p : car.getPolyineData()) {
                    if (!p.getPolyline().getId().equals(car.getCarPolylineData().getPolyline().getId())) {
                        p.getPolyline().remove();
                    }
                }
                showMarkerSnippet();
            }
        });
    }

    public void drawCarPolylinesHelper(Polyline polyline, Car car) {
        for (PolyPair p : car.getPolyineData()) {
            if (p.getPolyline().getId().equals(polyline.getId())) {
                //highlight selected trip option
                p.getPolyline().setVisible(true);
                p.getPolyline().setColor(Color.GREEN);
                p.getPolyline().setZIndex(0);

                try {
                    car.getCarPolylineData().getPolyline().setPoints(p.getPolyline().getPoints());
                    car.getCarPolylineData().setDirectionsLeg(p.getDirectionsLeg());
                    Log.d(TAG, "drawCarPolylinesHelper: setting points for existing polyline");
                } catch (Exception e) {
                    car.setCarPolylineData(p);
                }

                //update place marker with trip option details
                if (car.getMarker() != null)
                    car.getMarker().setSnippet("Duration: " + p.getDirectionsLeg().duration);
            } else {
                p.getPolyline().remove();
            }
        }
    }

    private void removeCarMarkers() {
        if (mTrip.getCars() != null) {
            for (Car car : mTrip.getCars()) {
                if (car.getMarker() != null)
                    car.getMarker().remove();
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        mMarkerClickedTitle = marker.getTitle();
        return false;
    }

    private void showMarkerSnippet() {
        if (mMarkerClickedTitle != null) {
            try {
                if (mMarkerClickedTitle.length() > 1) {
                    mPlaceMarker.setSnippet("Duration: " + mTripPolylineData.getDirectionsLeg().duration);
                    mPlaceMarker.showInfoWindow();
                    mTripPolylineData.getPolyline().setZIndex(1);
                    mTripPolylineData.getPolyline().setColor(Color.BLUE);
                    for (Car p : mTrip.getCars()) {
                        if (p.getCarID() != mCar.getCarID()) {
                            p.getCarPolylineData().getPolyline().setZIndex(0);
                            p.getCarPolylineData().getPolyline().setColor(Color.GREEN);
                        }
                    }
                } else {
                    mTripPolylineData.getPolyline().setZIndex(0);
                    mTripPolylineData.getPolyline().setColor(Color.GREEN);
                    for (Car p : mTrip.getCars()) {
                        if (p.getCarName().equals(mMarkerClickedTitle)) {

                            p.getMarker().setSnippet("Duration: " + p.getCarPolylineData().getDirectionsLeg().duration);
                            p.getMarker().showInfoWindow();
                            p.getCarPolylineData().getPolyline().setZIndex(1);
                            p.getCarPolylineData().getPolyline().setColor(Color.BLUE);

                        } else {
                            p.getCarPolylineData().getPolyline().setZIndex(0);
                            p.getCarPolylineData().getPolyline().setColor(Color.GREEN);
                        }
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, "updatePartyLocations: not yet defined: " + e.getMessage());
            }
        }
    }

    private void removeCarPolylines() {
        for (Car car : mTrip.getCars()) {
            try {
                car.getPolyineData().clear();
                car.getCarPolylineData().getPolyline().remove();
            } catch (Exception e) {
                Log.d(TAG, "removeCarPolylines: not yet defined: " + e.getMessage());
            }
        }
    }
}
