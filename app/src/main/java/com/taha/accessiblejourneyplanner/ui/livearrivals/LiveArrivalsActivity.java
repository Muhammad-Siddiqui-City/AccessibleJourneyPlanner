package com.taha.accessiblejourneyplanner.ui.livearrivals;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.taha.accessiblejourneyplanner.BuildConfig;
import com.taha.accessiblejourneyplanner.R;
import com.taha.accessiblejourneyplanner.ui.data.api.RetrofitClient;
import com.taha.accessiblejourneyplanner.ui.data.api.TflApiService;
import com.taha.accessiblejourneyplanner.ui.data.api.TflArrivalDto;
import com.taha.accessiblejourneyplanner.ui.data.api.TflDisruptionDto;
import com.taha.accessiblejourneyplanner.ui.data.api.TflLineStatusDto;
import com.taha.accessiblejourneyplanner.ui.data.api.TflStopPointDto;
import com.taha.accessiblejourneyplanner.ui.data.db.AppDatabase;
import com.taha.accessiblejourneyplanner.ui.data.db.ArrivalDao;
import com.taha.accessiblejourneyplanner.ui.data.db.CachedArrivalEntity;
import com.taha.accessiblejourneyplanner.ui.stopsearch.StopSearchActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveArrivalsActivity extends AppCompatActivity {

    public static final String EXTRA_STOP_ID = "stop_id";
    public static final String EXTRA_STOP_NAME = "stop_name";
    private static final String TAG = "LiveArrivals";
    /** Fallback line for disruption status when we do not infer from arrivals. */
    private static final String LINE_ID_DISRUPTIONS = "victoria";

    private String stopPointId;
    private String stationName;
    private AppDatabase db;
    private ArrivalDao dao;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    private TextView textStationName;
    private TextView textOfflineBanner;
    private TextView textLineStatus;
    private TextView textStepFree;
    private TextView textLiftDisruption;
    private ProgressBar progressBar;
    private TextView textErrorOrEmpty;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView recycler;
    private TextView textLastUpdated;
    private ArrivalsAdapter adapter;

    private TflApiService api;
    private String appId;
    private String appKey;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_arrivals);

        db = AppDatabase.getInstance(this);
        dao = db.arrivalDao();

        appId = BuildConfig.TFL_APP_ID;
        appKey = BuildConfig.TFL_APP_KEY;

        if (appId == null || appId.trim().isEmpty() || appKey == null || appKey.trim().isEmpty()) {
            Toast.makeText(this, "Missing TfL API keys. Set TFL_APP_ID and TFL_APP_KEY in secrets.properties.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        api = RetrofitClient.getService();

        stopPointId = getIntent().getStringExtra(EXTRA_STOP_ID);
        stationName = getIntent().getStringExtra(EXTRA_STOP_NAME);
        if (stationName == null) stationName = "";
        if (stopPointId == null || stopPointId.trim().isEmpty()) {
            Intent i = new Intent(this, StopSearchActivity.class);
            i.putExtra(StopSearchActivity.EXTRA_MODE, StopSearchActivity.MODE_ARRIVALS);
            startActivity(i);
            finish();
            return;
        }

        textStationName = findViewById(R.id.text_station_name);
        textOfflineBanner = findViewById(R.id.text_offline_banner);
        textLineStatus = findViewById(R.id.text_line_status);
        textStepFree = findViewById(R.id.text_step_free);
        textLiftDisruption = findViewById(R.id.text_lift_disruption);
        progressBar = findViewById(R.id.progress_live_arrivals);
        textErrorOrEmpty = findViewById(R.id.text_error_or_empty);
        swipeRefresh = findViewById(R.id.swipe_refresh_live_arrivals);
        recycler = findViewById(R.id.recycler_live_arrivals);
        textLastUpdated = findViewById(R.id.text_last_updated);

        textStationName.setText(stationName.isEmpty() ? "Station" : stationName);
        textStepFree.setText("Step-free: Not available");
        textLiftDisruption.setText("Lift disruption: Not available");
        textLineStatus.setText("Line status: Loading…");

        adapter = new ArrivalsAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadArrivals);

        loadArrivals();
    }

    private void loadArrivals() {
        textErrorOrEmpty.setVisibility(View.GONE);
        textOfflineBanner.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        recycler.setVisibility(View.VISIBLE);

        api.getArrivals(stopPointId, appId, appKey).enqueue(new Callback<List<TflArrivalDto>>() {
            @Override
            public void onResponse(Call<List<TflArrivalDto>> call, Response<List<TflArrivalDto>> response) {
                swipeRefresh.setRefreshing(false);
                progressBar.setVisibility(View.GONE);

                if (!response.isSuccessful()) {
                    Log.e(TAG, "HTTP error: " + response.code());
                    tryCacheThenShowError("HTTP error: " + response.code());
                    return;
                }

                List<TflArrivalDto> arrivals = response.body();
                if (arrivals == null) arrivals = Collections.emptyList();
                Collections.sort(arrivals, Comparator.comparingInt(a -> a.timeToStation));

                long fetchedAt = System.currentTimeMillis();
                List<CachedArrivalEntity> entities = new ArrayList<>();
                for (TflArrivalDto a : arrivals) {
                    entities.add(new CachedArrivalEntity(stopPointId, a.lineName, a.towards,
                            a.platformName, a.timeToStation, fetchedAt));
                }
                dbExecutor.execute(() -> {
                    dao.deleteByStopPoint(stopPointId);
                    dao.insertAll(entities);
                    Log.d(TAG, "Saved " + entities.size() + " arrivals to cache");
                });

                int limit = Math.min(arrivals.size(), 10);
                List<ArrivalItem> items = new ArrayList<>();
                for (int i = 0; i < limit; i++) {
                    TflArrivalDto a = arrivals.get(i);
                    int mins = Math.max(0, a.timeToStation / 60);
                    String crowding = inferCrowdingFromTimeToStation(a.timeToStation);
                    items.add(new ArrivalItem(safe(a.lineName), safe(a.towards), safe(a.platformName), mins, crowding));
                }
                adapter.setItems(items);
                textLastUpdated.setText("Last updated: " + formatTime(fetchedAt));
                recycler.setVisibility(View.VISIBLE);

                fetchAccessibilityAndDisruptions();
                fetchLineStatusAndUpdateUi();
            }

            @Override
            public void onFailure(Call<List<TflArrivalDto>> call, Throwable t) {
                Log.e(TAG, "Network failure: " + t.getMessage(), t);
                swipeRefresh.setRefreshing(false);
                progressBar.setVisibility(View.GONE);
                tryCacheThenShowError("Network failure. Pull to refresh.");
            }
        });
    }

    private void tryCacheThenShowError(String errorMessage) {
        dbExecutor.execute(() -> {
            List<CachedArrivalEntity> cached = dao.getByStopPoint(stopPointId);
            runOnUiThread(() -> {
                if (cached != null && !cached.isEmpty()) {
                    textOfflineBanner.setVisibility(View.VISIBLE);
                    long fetchedAt = cached.get(0).fetchedAt;
//                    long fetchedAt = cached.get(0).fetchedAt;
                    textOfflineBanner.setText("Offline — showing cached arrivals (last updated " + formatTime(fetchedAt) + ")");
                    int limit = Math.min(cached.size(), 10);
                    List<ArrivalItem> items = new ArrayList<>();
                    for (int i = 0; i < limit; i++) {
                        CachedArrivalEntity a = cached.get(i);
                        String crowding = inferCrowdingFromTimeToStation(a.timeToStation);
                        items.add(new ArrivalItem(a.lineName, a.towards, a.platformName, Math.max(0, a.timeToStation / 60), crowding));
                    }
                    adapter.setItems(items);
                    textLastUpdated.setText("Cached at: " + formatTime(fetchedAt));
                    recycler.setVisibility(View.VISIBLE);
                    textErrorOrEmpty.setVisibility(View.GONE);
                    fetchAccessibilityAndDisruptions();
                    fetchLineStatusAndUpdateUi();
                } else {
                    textErrorOrEmpty.setText("Could not load arrivals. " + errorMessage + " Pull to refresh or select another station.");
                    textErrorOrEmpty.setVisibility(View.VISIBLE);
                    recycler.setVisibility(View.GONE);
                    adapter.setItems(null);
                }
            });
        });
    }

    private void fetchAccessibilityAndDisruptions() {
        api.getStopPoint(stopPointId, appId, appKey).enqueue(new Callback<List<TflStopPointDto>>() {
            @Override
            public void onResponse(Call<List<TflStopPointDto>> call, Response<List<TflStopPointDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    TflStopPointDto dto = response.body().get(0);
                    String stepFree = parseStepFree(dto);
                    textStepFree.setText("Step-free: " + ("Unknown".equals(stepFree) ? "Not available" : stepFree));
                } else {
                    textStepFree.setText("Step-free: Not available");
                }
            }

            @Override
            public void onFailure(Call<List<TflStopPointDto>> call, Throwable t) {
                textStepFree.setText("Step-free: Not available");
            }
        });

        api.getStopPointDisruptions(stopPointId, appId, appKey).enqueue(new Callback<List<TflDisruptionDto>>() {
            @Override
            public void onResponse(Call<List<TflDisruptionDto>> call, Response<List<TflDisruptionDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean liftIssues = hasLiftDisruption(response.body());
                    textLiftDisruption.setText("Lift disruption: " + (liftIssues ? "Yes" : "No"));
                } else {
                    textLiftDisruption.setText("Lift disruption: Not available");
                }
            }

            @Override
            public void onFailure(Call<List<TflDisruptionDto>> call, Throwable t) {
                textLiftDisruption.setText("Lift disruption: Not available");
            }
        });
    }

    /** Fetch line status and show on screen (e.g. Victoria: Good Service). No Logcat-only messaging. */
    private void fetchLineStatusAndUpdateUi() {
        api.getLineStatus(LINE_ID_DISRUPTIONS, appId, appKey).enqueue(new Callback<List<TflLineStatusDto>>() {
            @Override
            public void onResponse(Call<List<TflLineStatusDto>> call, Response<List<TflLineStatusDto>> response) {
                if (!response.isSuccessful()) {
                    textLineStatus.setText("Line status: Not available");
                    return;
                }
                List<TflLineStatusDto> list = response.body();
                if (list != null && !list.isEmpty()) {
                    TflLineStatusDto dto = list.get(0);
                    String lineName = safe(dto.name);
                    String status = "—";
                    String reason = "";
                    if (dto.lineStatuses != null && !dto.lineStatuses.isEmpty()) {
                        TflLineStatusDto.LineStatusEntry first = dto.lineStatuses.get(0);
                        status = safe(first.statusSeverityDescription);
                        if (first.reason != null && !first.reason.trim().isEmpty()) {
                            reason = " — " + safe(first.reason);
                        }
                    }
                    textLineStatus.setText(lineName + ": " + status + reason);
                } else {
                    textLineStatus.setText("Line status: Not available");
                }
            }

            @Override
            public void onFailure(Call<List<TflLineStatusDto>> call, Throwable t) {
                textLineStatus.setText("Line status: Not available");
            }
        });
    }

    private static String parseStepFree(TflStopPointDto dto) {
        if (dto == null) return "Unknown";
        String summary = dto.accessibilitySummary;
        if (summary != null && !summary.trim().isEmpty()) {
            String s = summary.trim().toLowerCase(Locale.UK);
            if (s.contains("step") && s.contains("free")) return "Yes";
            if (s.contains("no step") || s.contains("not step")) return "No";
            return summary.trim();
        }
        if (dto.additionalProperties != null) {
            for (TflStopPointDto.AdditionalProperty p : dto.additionalProperties) {
                if (p == null || p.key == null) continue;
                String k = p.key.toLowerCase(Locale.UK);
                if (k.contains("accessibility") || k.contains("stepfree") || k.contains("step_free")) {
                    String v = p.value != null ? p.value.trim() : "";
                    if (v.toLowerCase(Locale.UK).contains("yes") || v.toLowerCase(Locale.UK).contains("step free")) return "Yes";
                    if (v.toLowerCase(Locale.UK).contains("no")) return "No";
                    return v.isEmpty() ? "Unknown" : v;
                }
            }
        }
        return "Unknown";
    }

    private static boolean hasLiftDisruption(List<TflDisruptionDto> list) {
        if (list == null) return false;
        for (TflDisruptionDto d : list) {
            String cat = d.category != null ? d.category.toLowerCase(Locale.UK) : "";
            String desc = d.description != null ? d.description.toLowerCase(Locale.UK) : "";
            if (cat.contains("lift") || desc.contains("lift") || desc.contains("elevator")) return true;
        }
        return false;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static String formatTime(long epochMillis) {
        return new java.text.SimpleDateFormat("HH:mm", Locale.UK).format(new java.util.Date(epochMillis));
    }

    private static String inferCrowdingFromTimeToStation(int timeToStationSeconds) {
        int mins = Math.max(0, timeToStationSeconds / 60);
        if (mins < 2) return "High";
        if (mins <= 5) return "Medium";
        return "Low";
    }
}
