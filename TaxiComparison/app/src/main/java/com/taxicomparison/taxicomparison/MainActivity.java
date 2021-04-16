package com.taxicomparison.taxicomparison;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Address;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.RectangularBounds;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private FusedLocationProviderClient fusedLocationClient;
    private Location curLocation;
    private EditText edtTxtDestAddress;
    private EditText edtTxtDepAddress;
    private ListView listViewResult;
    private ProgressBar progressBar;
    private static Context context;
    private Place depAddress;
    private Place destAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        initializeFields();
        initializeLocation();
    }

    private void initializeFields() {
        listViewResult = findViewById(R.id.listViewResult);
        progressBar = findViewById(R.id.progressBar);
        initializeDepartureAddress();
        initializeDestinationAddress();
    }

    private void initializeDepartureAddress() {
        Places.initialize(context, "AIzaSyDq6GUrEFzocsgbecJWTOpecf7tJcycNCo", new Locale("RU"));
        edtTxtDepAddress = findViewById(R.id.departureAddress);
        edtTxtDepAddress.setFocusable(false);

        edtTxtDepAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG);
                Intent intent =
                        new Autocomplete.IntentBuilder((AutocompleteActivityMode.FULLSCREEN), fieldList)
                                .setCountries(Arrays.asList("RU"))
                                .build(context);
                startActivityForResult(intent, 100);
            }
        });
    }

    private void initializeDestinationAddress() {
        edtTxtDestAddress = findViewById(R.id.destinationAddress);
        edtTxtDestAddress.setFocusable(false);

        edtTxtDestAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<Place.Field> fieldList = Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG);
                Intent intent =
                        new Autocomplete.IntentBuilder((AutocompleteActivityMode.FULLSCREEN), fieldList)
                                .setCountries(Arrays.asList("RU"))
                                //.setLocationBias(RectangularBounds.newInstance(
                                //        new LatLng(curLocation.getLatitude() - 1,
                                //                curLocation.getLongitude() - 1),
                                //        new LatLng(curLocation.getLatitude() + 1,
                                //                curLocation.getLongitude() + 1)))
                                .build(context);
                startActivityForResult(intent, 101);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                depAddress = Autocomplete.getPlaceFromIntent(data);
                edtTxtDepAddress.setText(getAddressFromPlace(depAddress));
                if (destAddress != null) {
                    clearResultListView();
                    progressBar.setVisibility(View.VISIBLE);
                    makeHttpRequest();
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Toast.makeText(context, "Произошла ошибка, попробуйте снова", Toast.LENGTH_SHORT).show();
            }
        }
        if (requestCode == 101) {
            if (resultCode == RESULT_OK) {
                destAddress = Autocomplete.getPlaceFromIntent(data);
                edtTxtDestAddress.setText(getAddressFromPlace(destAddress));
                if (depAddress != null) {
                    clearResultListView();
                    progressBar.setVisibility(View.VISIBLE);
                    makeHttpRequest();
                }
            } else if (resultCode == AutocompleteActivity.RESULT_ERROR) {
                Toast.makeText(context, "Произошла ошибка, попробуйте снова", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void makeHttpRequest() {
        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        String url = buildRequestUrl();
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "Произошла ошибка, попробуйте снова", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE);
                    }
                });
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    final ArrayList<String> resp = parseResponse(URLDecoder.decode(response.body().string(), "utf-8"));
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ArrayAdapter<String> resultAdapter = new ArrayAdapter<String>(
                                    context,
                                    R.layout.text_view_layout,
                                    resp
                            );
                            listViewResult.setAdapter(resultAdapter);
                            progressBar.setVisibility(View.GONE);
                        }
                    });
                }
            }
        });
    }

    private String buildRequestUrl() {
        String result = "";
        System.out.println(getCityFromPlace(depAddress));
        System.out.println(getAddressFromPlace(depAddress));
        System.out.println(getAddressFromPlace(destAddress));
        try {
            result = "https://20210415t110208-dot-taxiapi-310121.oa.r.appspot.com/"
                    + "?city=" + URLEncoder.encode(getCityFromPlace(depAddress), "utf-8")
                    + "?addres1=" + URLEncoder.encode(getAddressFromPlace(depAddress), "utf-8")
                    + "?address2=" + URLEncoder.encode(getAddressFromPlace(destAddress), "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    private ArrayList<String> parseResponse(String response) {
        response = response.replaceAll(":", " - ");
        String[] result = response.split(";");
        return new ArrayList<String>(Arrays.asList(result));
    }


    private void clearResultListView() {
        if (listViewResult.getCount() != 0) {
            listViewResult.setAdapter(null);
        }
    }

    private String getCityFromPlace(Place place) {
        String result = "";
        Geocoder gcd = new Geocoder(context, new Locale("RU"));
        try {
            List<Address> addresses = gcd.getFromLocation(place.getLatLng().latitude,
                    place.getLatLng().longitude, 1);
            if (addresses.size() != 0) {
                result = addresses.get(0).getLocality();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private String getAddressFromPlace(Place place) {
        String result = "";
        Geocoder gcd = new Geocoder(context, new Locale("RU"));
        try {
            List<Address> addresses = gcd.getFromLocation(place.getLatLng().latitude,
                    place.getLatLng().longitude, 1);
            if (addresses.size() != 0) {
                result = addresses.get(0).getThoroughfare() + " " + addresses.get(0).getSubThoroughfare();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void initializeLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            curLocation = location;
                        }
                    }
                });
    }
}