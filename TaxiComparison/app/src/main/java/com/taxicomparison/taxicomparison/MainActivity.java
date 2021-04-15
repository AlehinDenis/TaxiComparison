package com.taxicomparison.taxicomparison;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onBtnGetResult(View view) {
        EditText city = findViewById(R.id.etdTxtCity);
        EditText address1 = findViewById(R.id.etdTxtAddress1);
        EditText address2 = findViewById(R.id.etdTxtAddress2);
        TextView txtResult = findViewById(R.id.txtResult);

        OkHttpClient client = new OkHttpClient.Builder()
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        String url = "";
        try {
            url = "https://20210415t110208-dot-taxiapi-310121.oa.r.appspot.com/"
                    + "?city=" + URLEncoder.encode(city.getText().toString(),"utf-8")
                    + "?addres1=" + URLEncoder.encode(address1.getText().toString(),"utf-8")
                    +  "?address2=" + URLEncoder.encode(address2.getText().toString(),"utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    String resp = URLDecoder.decode(response.body().string(), "utf-8");
                    final String res = resp = resp.replaceAll(";", "\n");
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txtResult.setText(res);
                        }
                    });
                }
            }
        });

    }
}