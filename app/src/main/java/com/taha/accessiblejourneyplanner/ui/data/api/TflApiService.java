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

    // Line disruptions/status – read-only; we only fetch and display.
    // GET https://api.tfl.gov.uk/Line/{id}/Status returns an array of line objects (one when single id).
    @GET("Line/{id}/Status")
    Call<List<TflLineStatusDto>> getLineStatus(
            @Path("id") String lineId,
            @Query("app_id") String appId,
            @Query("app_key") String appKey
    );

    // StopPoint details for accessibility (step-free, etc.). API returns array even for single id.
    @GET("StopPoint/{id}")
    Call<List<TflStopPointDto>> getStopPoint(
            @Path("id") String stopPointId,
            @Query("app_id") String appId,
            @Query("app_key") String appKey
    );

    // StopPoint disruptions (e.g. lift out of service).
    @GET("StopPoint/{id}/Disruption")
    Call<List<TflDisruptionDto>> getStopPointDisruptions(
            @Path("id") String stopPointId,
            @Query("app_id") String appId,
            @Query("app_key") String appKey
    );

    // StopPoint search: user types station/stop name. Do not filter modes so buses etc. are included.
    @GET("StopPoint/Search")
    Call<TflStopPointSearchDto> searchStopPoints(
            @Query("query") String query,
            @Query("app_id") String appId,
            @Query("app_key") String appKey
    );

    // Journey planner: from/to can be NaPTAN stop IDs or lat,lon. Buses included by default; we do not filter modes.
    @GET("Journey/JourneyResults/{from}/to/{to}")
    Call<TflJourneyDto.ItineraryResult> getJourneyResults(
            @Path("from") String fromId,
            @Path("to") String toId,
            @Query("app_id") String appId,
            @Query("app_key") String appKey
    );
}
