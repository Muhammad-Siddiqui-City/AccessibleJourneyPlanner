package com.taha.accessiblejourneyplanner.ui.data.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class RetrofitClient {

    private static final String BASE_URL = "https://api.tfl.gov.uk/";
    private static Retrofit retrofit;

    private RetrofitClient() {}

    public static TflApiService getService() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(TflApiService.class);
    }
}
