package com.taha.accessiblejourneyplanner.ui.data.api;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TflApiService {

    // Example: GET https://api.tfl.gov.uk/StopPoint/940GZZLUKSX/Arrivals?app_id=...&app_key=...
    @GET("StopPoint/{id}/Arrivals")
    Call<List<TflArrivalDto>> getArrivals(
            @Path("id") String stopPointId,
            @Query("app_id") String appId,
            @Query("app_key") String appKey
    );
}
