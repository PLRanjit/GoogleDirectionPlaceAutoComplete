package com.examples.googleapis;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.app.ActionBar.LayoutParams;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.itshareplus.googlemapdemo.GPSTracker.GPSTracker;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import android.support.design.widget.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import Modules.DirectionFinder;
import Modules.DirectionFinderListener;
import Modules.Route;

import android.os.Bundle;

import static com.examples.googleapis.AppConfig.*;

public class MapsActivity extends AbsRuntimePermission implements
        OnMapReadyCallback,
        DirectionFinderListener,
        LocationListener,
        android.location.LocationListener {              //AppCompatActivity

    private GoogleMap mMap;
    private LocationManager locationManager;
    LinearLayout mainCoordinatorLayout;
    private Button btnFindPath;
    private AutoCompleteTextView etOrigin;
    private AutoCompleteTextView etDestination;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;
    private Spinner spinner;
    private ArrayAdapter<String> arrayAdapter;
    private static String selectedItem;
    private SlidingUpPanelLayout mPlane;
    private static String TAG = "MapsActivity";
    private List<LatLng> latlng_list;
    private ListView listView;
    private Geocoder gc;
    private FloatingActionButton direct;
    private static Location l;
    private static List<String> s = Arrays.asList("a");
    private static final String LOG_TAG = "Google Places Autocompl";
    private static final String PLACES_API_BASE = "https://maps.googleapis.com/maps/api/place";
    private static final String TYPE_AUTOCOMPLETE = "/autocomplete";
    private static final String OUT_JSON = "/json";

    private static final String API_KEY = "AIzaSyBHaBZN94wLAP8Xo1PFNyc1pR079F1YRK4";

    private boolean flag = false;
    private static final int REQUEST_PERMISSION = 10;

    private boolean loadnear = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestAppPermissions(new String[]{
                        Manifest.permission.INTERNET,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_NETWORK_STATE},
                R.string.msg, REQUEST_PERMISSION);

        if (!isGooglePlayServicesAvailable()) {
            finish();
        }

        if (!isInternetAvailable()) {
            Toast.makeText(this, "Please! Connect to internet", Toast.LENGTH_LONG).show();
        }


        setContentView(R.layout.activity_maps);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mainCoordinatorLayout = (LinearLayout) findViewById(R.id.mainCoordinatorLayout);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showLocationSettings();
        }

        init();
        try {
            setListView();
        } catch (Exception e) {
            Toast.makeText(this, "Slow net", Toast.LENGTH_SHORT).show();
        }

        panelListener();

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!isInternetAvailable()) {
            Toast.makeText(this, "Please! Connect to internet", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isInternetAvailable()) {
            Toast.makeText(this, "Please! Connect to internet", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode) {
        //Do anything when permisson granted
        //Toast.makeText(getApplicationContext(), "Permission granted", Toast.LENGTH_LONG).show();

    }

    private void sendRequest() {
        String origin = etOrigin.getText().toString();
        String destination = etDestination.getText().toString();
        if (origin.isEmpty()) {
            Toast.makeText(this, "Please enter origin address!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (destination.isEmpty()) {
            Toast.makeText(this, "Please enter destination address!", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            new DirectionFinder(this, origin, destination).execute();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getBaseContext(), "Grant location and Storage permission", Toast.LENGTH_LONG).show();
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        // mMap.getUiSettings().setZoomControlsEnabled(true);

        showCurrentLocation();
    }


    @Override
    public void onDirectionFinderStart() {
        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Finding direction..!", true);

        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if (polylinePaths != null) {
            for (Polyline polyline : polylinePaths) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        for (Route route : routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));
            ((TextView) findViewById(R.id.tvDuration)).setText(route.duration.text);
            ((TextView) findViewById(R.id.tvDistance)).setText(route.distance.text);

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(route.startLocation);
            builder.include(route.endLocation);
            try {
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(),
                        (int) (getResources().getDisplayMetrics().widthPixels * 0.10)));
            } catch (Exception e) {
                Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            }

            originMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.start_l))
                    .title(route.startAddress)
                    .position(route.startLocation)));
            destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.end_l))
                    .title(route.endAddress)
                    .position(route.endLocation)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(10);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {

        Toast.makeText(getBaseContext(),"on location changed",Toast.LENGTH_LONG);
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        LatLng latLng = new LatLng(latitude, longitude);

        String ll = "Latitude:" + latitude + ", Longitude:" + longitude;
        if(loadnear){
            loadNearByPlaces(latitude, longitude);
            loadnear = false;
        }

        l = location;
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private void init() {
        s = new ArrayList<String>();
        latlng_list = new ArrayList<LatLng>();

        direct = (FloatingActionButton) findViewById(R.id.direct);

        spinner = (Spinner) findViewById(R.id.placeSpinner);
        mPlane = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        listView = (ListView) findViewById(R.id.list_view);
        btnFindPath = (Button) findViewById(R.id.btnFindPath);
        etOrigin = (AutoCompleteTextView) findViewById(R.id.etOrigin);
        etDestination = (AutoCompleteTextView) findViewById(R.id.etDestination);

        etOrigin.setVisibility(View.GONE);
        etDestination.setVisibility(View.GONE);
        btnFindPath.setVisibility(View.GONE);

        arrayAdapter = new ArrayAdapter<String>
                (MapsActivity.this, android.R.layout.simple_list_item_1, getResources().getStringArray(R.array.names));
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(arrayAdapter);

        btnFindPath.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMap.clear();
                invisible();
                sendRequest();
            }
        });

        direct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                invisible();
            }
        });

        etDestination.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.list_item));
        etDestination.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                String str = (String) parent.getItemAtPosition(position);
                Toast.makeText(getBaseContext(), str, Toast.LENGTH_SHORT).show();
            }
        });

        etOrigin.setAdapter(new GooglePlacesAutocompleteAdapter(this, R.layout.list_item));
        etOrigin.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View view, int position, long id) {
                String str = (String) parent.getItemAtPosition(position);
                Toast.makeText(getBaseContext(), str, Toast.LENGTH_SHORT).show();
            }
        });

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                                              @Override
                                              public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                                  selectedItem = parent.getItemAtPosition(position).toString();
                                                  showCurrentLocation();
                                                  // Toast.makeText(getBaseContext(), selectedItem, Toast.LENGTH_SHORT).show();
                                              }

                                              @Override
                                              public void onNothingSelected(AdapterView<?> parent) {

                                              }
                                          }
        );
    }

    private void invisible() {
        if (etOrigin.getVisibility() == View.GONE && etDestination.getVisibility() == View.GONE && btnFindPath.getVisibility() == View.GONE) {

            etOrigin.setText("");
            etDestination.setText("");

            etOrigin.setVisibility(View.VISIBLE);
            etDestination.setVisibility(View.VISIBLE);
            btnFindPath.setVisibility(View.VISIBLE);
        } else {
            etOrigin.setVisibility(View.GONE);
            etDestination.setVisibility(View.GONE);
            btnFindPath.setVisibility(View.GONE);
        }
    }

    private void setListView() {
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, list());
        listView.setAdapter(arrayAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                s = new ArrayList<String>();
                etDestination.setText(parent.getItemAtPosition(position).toString().replaceAll("\n", ""));
                try {
                    getCity(l);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mPlane.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);

                if (mPlane != null &&
                        (mPlane.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED || mPlane.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
                    mPlane.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                    flag = true;
                } else {
                    flag = false;
                }
                if (flag) {
                    mMap.clear();
                    sendRequest();
                }
            }
        });
    }

    private void panelListener() {
        mPlane.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {

            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {

            }
        });

        mPlane.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPlane.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });
    }

    private List<String> list() {
        return s;
    }


    @Override
    public void onBackPressed() {
        if (mPlane != null &&
                (mPlane.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED || mPlane.getPanelState() == SlidingUpPanelLayout.PanelState.ANCHORED)) {
            mPlane.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        } else {
            super.onBackPressed();
        }
    }

    private void showCurrentLocation() {

        Toast.makeText(getBaseContext(),"show current location",Toast.LENGTH_LONG);
        loadnear = true;
        Criteria criteria = new Criteria();
        String bestProvider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = getLocation();
        s = new ArrayList<String>();
        if (location != null) {
            onLocationChanged(location);
        }
        locationManager.requestLocationUpdates(bestProvider, MIN_TIME_BW_UPDATES,
                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
    }

    private void showLocationSettings() {
        Snackbar snackbar = Snackbar
                .make(mainCoordinatorLayout, "Location Error: GPS Disabled!",
                        Snackbar.LENGTH_LONG)
                .setAction("Enable", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                });
        snackbar.setActionTextColor(Color.RED);
        snackbar.setDuration(Snackbar.LENGTH_INDEFINITE);

        View sbView = snackbar.getView();
        TextView textView = (TextView) sbView
                .findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);

        snackbar.show();
    }

    private void loadNearByPlaces(double latitude, double longitude) {
        //YOU Can change this type at your own will, e.g hospital, cafe, restaurant.... and see how it all works
        //String type = "grocery_or_supermarket";
        String type = selectedItem;
        if (String.valueOf(selectedItem).equals("monuments")) {
            type = "museum|amusement_park|aquarium|art_gallery|hindu_temple|mosque";
        }
        StringBuilder googlePlacesUrl =
                new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlacesUrl.append("location=").append(latitude).append(",").append(longitude);
        googlePlacesUrl.append("&radius=").append(PROXIMITY_RADIUS);
        googlePlacesUrl.append("&types=").append(type);
        googlePlacesUrl.append("&sensor=true");
        googlePlacesUrl.append("&key=" + GOOGLE_BROWSER_API_KEY);

        JsonObjectRequest request = new JsonObjectRequest(googlePlacesUrl.toString(),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject result) {

                        Log.i(TAG, "onResponse: Result= " + result.toString());
                        parseLocationResult(result);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "onErrorResponse: Error= " + error);
                        Log.e(TAG, "onErrorResponse: Error= " + error.getMessage());
                    }
                });

        AppController.getInstance().addToRequestQueue(request);     //android:name="com.itshareplus.googlemapdemo.AppController"
    }

    private void parseLocationResult(JSONObject result) {

        String id, place_id, placeName = null, reference, icon, vicinity = null;
        double latitude, longitude;

        try {
            JSONArray jsonArray = result.getJSONArray("results");

            if (result.getString(STATUS).equalsIgnoreCase(OK)) {

                mMap.clear();

                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject place = jsonArray.getJSONObject(i);

                    id = place.getString(SUPERMARKET_ID);
                    place_id = place.getString(PLACE_ID);
                    if (!place.isNull(NAME)) {
                        placeName = place.getString(NAME);
                    }
                    if (!place.isNull(VICINITY)) {
                        vicinity = place.getString(VICINITY);
                    }
                    latitude = place.getJSONObject(GEOMETRY).getJSONObject(LOCATION)
                            .getDouble(LATITUDE);
                    longitude = place.getJSONObject(GEOMETRY).getJSONObject(LOCATION)
                            .getDouble(LONGITUDE);
                    reference = place.getString(REFERENCE);
                    icon = place.getString(ICON);

                    MarkerOptions markerOptions = new MarkerOptions();
                    LatLng latLng = new LatLng(latitude, longitude);
                    markerOptions.position(latLng);
                    markerOptions.title(placeName + " : " + vicinity);
                    s.add("\n" + placeName + ",\n" + vicinity + "\n");
                    mMap.addMarker(markerOptions);
                    latlng_list.add(latLng);

                }
                try {
                    setListView();
                } catch (Exception e) {
                    Toast.makeText(this, "Slow net", Toast.LENGTH_SHORT).show();
                }
                Toast.makeText(getBaseContext(), selectedItem + "!",
                        Toast.LENGTH_SHORT).show();
                try {
                    getMarkerZoom();
                } catch (Exception e) {
                }
                s = new ArrayList<String>();
                latlng_list.clear();
            } else if (result.getString(STATUS).equalsIgnoreCase(ZERO_RESULTS)) {
                Toast.makeText(getBaseContext(), "No " + selectedItem + " found in 5KM radius!!!",
                        Toast.LENGTH_LONG).show();
            }

        } catch (JSONException e) {

            e.printStackTrace();
            Log.e(TAG, "parseLocationResult: Error=" + e.getMessage());
        }
    }

    private void getMarkerZoom() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng marker : latlng_list) {
            builder.include(marker);
        }
        LatLngBounds bounds = builder.build();
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, (int) (getResources().getDisplayMetrics().widthPixels * 0.10)));
    }


    public static ArrayList autocomplete(String input) {
        ArrayList resultList = null;

        HttpURLConnection conn = null;
        StringBuilder jsonResults = new StringBuilder();
        try {
            StringBuilder sb = new StringBuilder(PLACES_API_BASE + TYPE_AUTOCOMPLETE + OUT_JSON);
            sb.append("?key=" + API_KEY);
            sb.append("&components=country:in");
            sb.append("&input=" + URLEncoder.encode(input, "utf8"));

            URL url = new URL(sb.toString());
            conn = (HttpURLConnection) url.openConnection();
            InputStreamReader in = new InputStreamReader(conn.getInputStream());

            // Load the results into a StringBuilder
            int read;
            char[] buff = new char[1024];
            while ((read = in.read(buff)) != -1) {
                jsonResults.append(buff, 0, read);
            }
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, "Error processing Places API URL", e);
            return resultList;
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error connecting to Places API", e);
            return resultList;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        try {
            // Create a JSON object hierarchy from the results
            JSONObject jsonObj = new JSONObject(jsonResults.toString());
            JSONArray predsJsonArray = jsonObj.getJSONArray("predictions");

            // Extract the Place descriptions from the results
            resultList = new ArrayList(predsJsonArray.length());
            for (int i = 0; i < predsJsonArray.length(); i++) {
                System.out.println(predsJsonArray.getJSONObject(i).getString("description"));
                System.out.println("============================================================");
                resultList.add(predsJsonArray.getJSONObject(i).getString("description"));
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Cannot process JSON results", e);
        }

        return resultList;
    }

    class GooglePlacesAutocompleteAdapter extends ArrayAdapter implements Filterable {
        private ArrayList resultList;

        public GooglePlacesAutocompleteAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        @Override
        public int getCount() {
            return resultList.size();
        }

        @Override
        public String getItem(int index) {
            return resultList.get(index).toString();
        }

        @Override
        public Filter getFilter() {
            Filter filter = new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    FilterResults filterResults = new FilterResults();
                    if (constraint != null) {
                        // Retrieve the autocomplete results.
                        resultList = autocomplete(constraint.toString());

                        // Assign the data to the FilterResults
                        filterResults.values = resultList;
                        filterResults.count = resultList.size();
                    }
                    return filterResults;
                }

                @Override
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    if (results != null && results.count > 0) {
                        notifyDataSetChanged();
                    } else {
                        notifyDataSetInvalidated();
                    }
                }
            };
            return filter;
        }
    }

    private void getCity(Location location) throws IOException {
        gc = new Geocoder(getBaseContext(), Locale.getDefault());
        List<Address> addressList = gc.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        Address address = addressList.get(0);

        ArrayList<String> addressFragments = new ArrayList<String>();

        for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
            addressFragments.add(address.getAddressLine(i));
        }
        String s = addressFragments.toString().trim();
        etOrigin.setText(s.substring(1, s.length() - 1));
    }

    private boolean isInternetAvailable() {
        boolean a = false;
        ConnectivityManager cm = (ConnectivityManager) getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                // connected to wifi
                // Toast.makeText(getBaseContext(), activeNetwork.getTypeName(), Toast.LENGTH_SHORT).show();
                a = true;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                // connected to the mobile provider's data plan
                // Toast.makeText(getBaseContext(), activeNetwork.getTypeName(), Toast.LENGTH_SHORT).show();
                a = true;
            }
        } else {
            a = false;
            // not connected to the internet
        }
        return a;
    }

    boolean isGPSEnabled = false;

    // flag for network status
    boolean isNetworkEnabled = false;

    // flag for get location status
    boolean canGetLocation = false;

    Location location = null; // location
    double latitude; // latitude
    double longitude; // longitude

    public Location getLocation() {
        try {
            locationManager = (LocationManager) getBaseContext().getSystemService(LOCATION_SERVICE);

            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if (!isGPSEnabled && !isNetworkEnabled) {
                // no network provider is enabled
            } else {
                this.canGetLocation = true;
                if (isNetworkEnabled) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return null;
                    }
                    locationManager.requestLocationUpdates(
                            LocationManager.NETWORK_PROVIDER,
                            MIN_TIME_BW_UPDATES,
                            MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                    Log.d("Network", "Network Enabled");
                    if (locationManager != null) {
                        location = locationManager
                                .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                        }
                    }
                }
                // if GPS Enabled get lat/long using GPS Services
                if (isGPSEnabled) {
                    if (location == null) {
                        locationManager.requestLocationUpdates(
                                LocationManager.GPS_PROVIDER,
                                MIN_TIME_BW_UPDATES,
                                MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        Log.d("GPS", "GPS Enabled");
                        if (locationManager != null) {
                            location = locationManager
                                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null) {
                                latitude = location.getLatitude();
                                longitude = location.getLongitude();
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }
}
