package com.taxicomparison.taxicomparison;

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

import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.net.PlacesClient;

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
    private SearchView searchAddressFrom;
    private SearchView searchAddressTo;
    private ListView listViewResFrom;
    private ListView listViewResTo;
    private ListView listViewResult;
    private ProgressBar progressBar;
    private  static Context context;
    final Address[] addressFrom = new Address[1];
    final Address[] addressTo = new Address[1];

    ArrayAdapter<String> adapterSearchAddressResFrom;
    ArrayAdapter<String> adapterSearchAddressResTo;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        initializeFields();
        createListeners();
        Places.initialize(context, "AIzaSyDq6GUrEFzocsgbecJWTOpecf7tJcycNCo");
        PlacesClient placesClient = Places.createClient(this);
    }

    public void onBtnGetResult(View view) {
        hideKeyBoard();
        if(isInputWrong()) {
            return;
        }
        clearResultListView();
        progressBar.setVisibility(View.VISIBLE);

        makeHttpRequest();
    }
    private void initializeFields() {
        searchAddressFrom = findViewById(R.id.searchViewAddressFrom);
        searchAddressTo = findViewById(R.id.searchViewAddressTo);
        listViewResult = findViewById(R.id.listViewResult);
        progressBar = findViewById(R.id.progressBar);
        listViewResFrom = findViewById(R.id.listViewResFrom);
        listViewResTo = findViewById(R.id.listViewResTo);
    }

    private void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (null != this.getCurrentFocus())
            imm.hideSoftInputFromWindow(this.getCurrentFocus()
                    .getApplicationWindowToken(), 0);
    }

    private boolean isInputWrong() {
        if(searchAddressFrom.getQuery().toString().length() == 0
                || searchAddressTo.getQuery().toString().length() == 0 ) {
            Toast.makeText(this, "Заполните поля", Toast.LENGTH_SHORT).show();
            return true;
        }
        if(!addressFrom[0].getLocality().equals(addressTo[0].getLocality())) {
            Toast.makeText(this, "Выбранны адреса в разных городах", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
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
        String address1 = addressFrom[0].getThoroughfare() + " " + addressFrom[0].getSubThoroughfare();
        String address2 = addressTo[0].getThoroughfare() + " " + addressFrom[0].getSubThoroughfare();
        try {
            result = "https://20210415t110208-dot-taxiapi-310121.oa.r.appspot.com/"
                    + "?city=" + URLEncoder.encode(addressFrom[0].getLocality(),"utf-8")
                    + "?addres1=" + URLEncoder.encode(address1,"utf-8")
                    +  "?address2=" + URLEncoder.encode(address2,"utf-8");
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

    private void createSearchListeners() {
        Geocoder geocoder = new Geocoder(context, getRusLocal());
        final Thread[] searchThread = {null};

        searchAddressFrom.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchAddressFrom.setQuery(addressFrom[0].getAddressLine(0), false);
                listViewResFrom.setAdapter(null);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String search) {
                if(searchThread[0] != null) {
                    searchThread[0].interrupt();
                    searchThread[0] = null;
                }
                searchThread[0] = new Thread(new Runnable() {
                    public void run() {
                        try {
                            List<Address> addresses = geocoder.getFromLocationName(search, 1);
                            if(addresses.size() == 0) {
                                return;
                            }
                            addressFrom[0] = addresses.get(0);
                            ArrayList<String> strAddresses = new ArrayList<>();
                            strAddresses.add(addressFrom[0].getAddressLine(0));
                            adapterSearchAddressResFrom = new ArrayAdapter<String>(
                                    context,
                                    android.R.layout.simple_list_item_1,
                                    strAddresses
                            );
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listViewResFrom.setAdapter(adapterSearchAddressResFrom);
                                }
                            });

                        } catch (IOException e) {
                            Toast.makeText(context, "Ничего не найдено", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                searchThread[0].start();
                return false;
            }
        });

        searchAddressTo.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchAddressTo.setQuery(addressTo[0].getAddressLine(0), false);
                listViewResTo.setAdapter(null);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String search) {
                if(searchThread[0] != null) {
                    searchThread[0].interrupt();
                    searchThread[0] = null;
                }
                searchThread[0] = new Thread(new Runnable() {
                    public void run() {
                        try {
                            List<Address> addresses = geocoder.getFromLocationName(search, 1);
                            if(addresses.size() == 0) {
                                return;
                            }
                            addressTo[0] = addresses.get(0);
                            ArrayList<String> strAddresses = new ArrayList<>();
                            strAddresses.add(addressTo[0].getAddressLine(0));
                            adapterSearchAddressResTo = new ArrayAdapter<String>(
                                    context,
                                    android.R.layout.simple_list_item_1,
                                    strAddresses
                            );
                            MainActivity.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    listViewResTo.setAdapter(adapterSearchAddressResTo);
                                }
                            });

                        } catch (IOException e) {
                            Toast.makeText(context, "Ничего не найдено", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                searchThread[0].start();
                return false;
            }
        });
    }

    private Locale getRusLocal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new Locale.Builder().setLanguage("RU").setScript("Latn").setRegion("RS").build();
        } else {
            return new Locale("RU");
        }
    }

    private void createListViewListeners() {
        listViewResFrom.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                searchAddressFrom.setQuery(addressFrom[0].getAddressLine(0), false);
                listViewResFrom.setAdapter(null);
            }
        });

        listViewResTo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                searchAddressTo.setQuery(addressTo[0].getAddressLine(0), false);
                listViewResTo.setAdapter(null);
            }
        });
    }

    private void createListeners() {
        createSearchListeners();
        createListViewListeners();
    }

    public Context getContext() { return context; }
}