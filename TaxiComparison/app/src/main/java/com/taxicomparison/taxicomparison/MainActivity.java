package com.taxicomparison.taxicomparison;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    Spinner city;
    private EditText address1;
    private EditText address2;
    private ListView listViewResult;
    private ProgressBar progressBar;
    public static Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = getApplicationContext();
        initializeFields();
    }

    public void onBtnGetResult(View view) {
        hideKeyBoard();

        if(isInputWrong()) {
            Toast.makeText(this, "Заполните поля", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!TextUtils.isEmpty(listViewResult.toString())){
            //listViewResult.setAdapter();
        }
        progressBar.setVisibility(View.VISIBLE);

        makeHttpRequest();
    }
    private void initializeFields() {
        initializeCitiesSpinner();
        address1 = findViewById(R.id.etdTxtAddress1);
        address2 = findViewById(R.id.etdTxtAddress2);
        listViewResult = findViewById(R.id.listViewResult);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initializeCitiesSpinner() {
        city = findViewById(R.id.spinnerCities);
        ArrayList<String> cities = new ArrayList<>();
        cities.add("Нижний Новгород");
        cities.add("Москва");
        cities.add("Санкт-Петербург");
        cities.add("Казань");
        cities.add("Екатеринбург");

        ArrayAdapter<String> citiesAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                cities
        );
        city.setAdapter(citiesAdapter);
    }

    private void hideKeyBoard() {
        InputMethodManager imm = (InputMethodManager) this.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (null != this.getCurrentFocus())
            imm.hideSoftInputFromWindow(this.getCurrentFocus()
                    .getApplicationWindowToken(), 0);
    }

    private boolean isInputWrong() {
        String strAddress1 = address1.getText().toString();
        String strAddress2 = address2.getText().toString();
        if(TextUtils.isEmpty(strAddress1) || TextUtils.isEmpty(strAddress2)) {
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
        try {
            result = "https://20210415t110208-dot-taxiapi-310121.oa.r.appspot.com/"
                    + "?city=" + URLEncoder.encode(city.getSelectedItem().toString(),"utf-8")
                    + "?addres1=" + URLEncoder.encode(address1.getText().toString(),"utf-8")
                    +  "?address2=" + URLEncoder.encode(address2.getText().toString(),"utf-8");
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


}