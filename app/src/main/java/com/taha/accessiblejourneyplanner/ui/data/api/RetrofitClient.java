package com.taha.accessiblejourneyplanner.ui.data.api;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static final String BASE_URL = "https://api.tfl.gov.uk/";
    private static final int CONNECT_TIMEOUT_SEC = 30;
    private static final int READ_TIMEOUT_SEC = 30;
    private static Retrofit retrofit;

    private RetrofitClient() {}

    public static TflApiService getService() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT_SEC, TimeUnit.SECONDS)
                    .build();
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(TflApiService.class);
    }
}
