package com.taxicomparison.taxicomparison;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.location.Geocoder;
import android.location.Address;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;

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
    private ListView listViewResult;
    private ProgressBar progressBar;
    private  static Context context;
    Place depAddress;
    Place destAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        initializeFields();

    }

    private void initializeFields() {
        listViewResult = findViewById(R.id.listViewResult);
        progressBar = findViewById(R.id.progressBar);
        initializeDepartureAddress();
        initializeDestinationAddress();
    }

    private void initializeDepartureAddress() {
        Places.initialize(context, "AIzaSyDq6GUrEFzocsgbecJWTOpecf7tJcycNCo", new Locale("RU"));
        PlacesClient placesClient = Places.createClient(this);
        AutocompleteSupportFragment fragDepAddress = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.departureAddress);

        fragDepAddress.setTypeFilter(TypeFilter.ADDRESS);
        fragDepAddress.setCountries("Ru");
        fragDepAddress.setPlaceFields(Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG));
        fragDepAddress.setHint("Откуда");
        fragDepAddress.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                depAddress = place;
                fragDepAddress.setText(getAddressFromPlace(depAddress));
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });
    }

    private void initializeDestinationAddress() {
        AutocompleteSupportFragment fragDestAddress = (AutocompleteSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.destinationAddress);

        fragDestAddress.setTypeFilter(TypeFilter.ADDRESS);
        fragDestAddress.setCountries("Ru");
        fragDestAddress.setPlaceFields(Arrays.asList(Place.Field.ADDRESS, Place.Field.LAT_LNG));
        fragDestAddress.setHint("Куда");
        fragDestAddress.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(@NonNull Place place) {
                destAddress = place;
                fragDestAddress.setText(getAddressFromPlace(destAddress));
                clearResultListView();
                progressBar.setVisibility(View.VISIBLE);
                makeHttpRequest();
            }

            @Override
            public void onError(@NonNull Status status) {

            }
        });
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
                if(response.isSuccessful()) {
                    final ArrayList<String> resp = parseResponse(URLDecoder.decode(response.body().string(), "utf-8"));
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ArrayAdapter<String> resultAdapter = new ArrayAdapter<String>(
                                    context,
                                    android.R.layout.simple_list_item_1,
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
                    + "?city=" + URLEncoder.encode(getCityFromPlace(depAddress),"utf-8")
                    + "?addres1=" + URLEncoder.encode(getAddressFromPlace(depAddress),"utf-8")
                    +  "?address2=" + URLEncoder.encode(getAddressFromPlace(destAddress),"utf-8");
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
        if(listViewResult.getCount() != 0) {
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
}