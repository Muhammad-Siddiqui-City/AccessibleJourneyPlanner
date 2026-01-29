package com.taha.accessiblejourneyplanner.ui.livearrivals;

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

    private static final String TAG = "LiveArrivals";
    private static final String STOP_POINT_ID = "940GZZLUKSX";
    private static final String LINE_ID_DISRUPTIONS = "victoria";

    private AppDatabase db;
    private ArrivalDao dao;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    private TextView textOfflineBanner;
    private TextView textStepFree;
    private TextView textLiftDisruption;
    private TextView textLineStatus;
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

        textOfflineBanner = findViewById(R.id.text_offline_banner);
        textStepFree = findViewById(R.id.text_step_free);
        textLiftDisruption = findViewById(R.id.text_lift_disruption);
        textLineStatus = findViewById(R.id.text_line_status);
        progressBar = findViewById(R.id.progress_live_arrivals);
        textErrorOrEmpty = findViewById(R.id.text_error_or_empty);
        swipeRefresh = findViewById(R.id.swipe_refresh_live_arrivals);
        recycler = findViewById(R.id.recycler_live_arrivals);
        textLastUpdated = findViewById(R.id.text_last_updated);

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

        api.getArrivals(STOP_POINT_ID, appId, appKey).enqueue(new Callback<List<TflArrivalDto>>() {
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
                    entities.add(new CachedArrivalEntity(STOP_POINT_ID, a.lineName, a.towards,
                            a.platformName, a.timeToStation, fetchedAt));
                }
                dbExecutor.execute(() -> {
                    dao.deleteByStopPoint(STOP_POINT_ID);
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
                // Phase 3: line status (Victoria) – read-only; log and show "Disruptions checked – see Logcat"
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
            List<CachedArrivalEntity> cached = dao.getByStopPoint(STOP_POINT_ID);
            runOnUiThread(() -> {
                if (cached != null && !cached.isEmpty()) {
                    Log.d(TAG, "Offline – using cached data");
                    Log.d(TAG, "Loaded " + cached.size() + " cached arrivals");
                    textOfflineBanner.setVisibility(View.VISIBLE);
                    textOfflineBanner.setText("Offline – showing cached arrivals");
                    int limit = Math.min(cached.size(), 10);
                    List<ArrivalItem> items = new ArrayList<>();
                    long fetchedAt = 0;
                    for (int i = 0; i < limit; i++) {
                        CachedArrivalEntity a = cached.get(i);
                        fetchedAt = a.fetchedAt;
                        String crowding = inferCrowdingFromTimeToStation(a.timeToStation);
                        items.add(new ArrivalItem(a.lineName, a.towards, a.platformName, Math.max(0, a.timeToStation / 60), crowding));
                    }
                    adapter.setItems(items);
                    textLastUpdated.setText("Cached at: " + formatTime(fetchedAt));
                    recycler.setVisibility(View.VISIBLE);
                    textErrorOrEmpty.setVisibility(View.GONE);
                    fetchAccessibilityAndDisruptions();
                } else {
                    textErrorOrEmpty.setText(errorMessage);
                    textErrorOrEmpty.setVisibility(View.VISIBLE);
                    recycler.setVisibility(View.GONE);
                    adapter.setItems(null);
                }
            });
        });
    }

    private void fetchAccessibilityAndDisruptions() {
        textStepFree.setText("Step-free: Unknown");
        textLiftDisruption.setText("Lift disruption: Unknown");

        api.getStopPoint(STOP_POINT_ID, appId, appKey).enqueue(new Callback<List<TflStopPointDto>>() {
            @Override
            public void onResponse(Call<List<TflStopPointDto>> call, Response<List<TflStopPointDto>> response) {
                if (response.isSuccessful() && response.body() != null && !response.body().isEmpty()) {
                    TflStopPointDto dto = response.body().get(0);
                    String stepFree = parseStepFree(dto);
                    textStepFree.setText("Step-free: " + stepFree);
                }
            }

            @Override
            public void onFailure(Call<List<TflStopPointDto>> call, Throwable t) {
                Log.w(TAG, "StopPoint details failed: " + t.getMessage());
            }
        });

        api.getStopPointDisruptions(STOP_POINT_ID, appId, appKey).enqueue(new Callback<List<TflDisruptionDto>>() {
            @Override
            public void onResponse(Call<List<TflDisruptionDto>> call, Response<List<TflDisruptionDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean liftIssues = hasLiftDisruption(response.body());
                    textLiftDisruption.setText("Lift disruption: " + (liftIssues ? "Yes" : "No"));
                }
            }

            @Override
            public void onFailure(Call<List<TflDisruptionDto>> call, Throwable t) {
                Log.w(TAG, "StopPoint disruptions failed: " + t.getMessage());
            }
        });
    }

    /**
     * Phase 3: fetch line status (e.g. Victoria) for disruptions. Read-only; log line name, status, reason;
     * update TextView with "Disruptions checked – see Logcat". Not called when showing cached arrivals.
     */
    private void fetchLineStatusAndUpdateUi() {
        textLineStatus.setText("");
        Log.d(TAG, "Requesting line status for: " + LINE_ID_DISRUPTIONS);
        api.getLineStatus(LINE_ID_DISRUPTIONS, appId, appKey).enqueue(new Callback<List<TflLineStatusDto>>() {
            @Override
            public void onResponse(Call<List<TflLineStatusDto>> call, Response<List<TflLineStatusDto>> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Line status HTTP error: " + response.code());
                    textLineStatus.setText("Disruptions: check Logcat");
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
                            reason = " | " + safe(first.reason);
                        }
                        Log.i(TAG, "Disruption – Line: " + lineName + " | Status: " + status + reason);
                    }
                }
                textLineStatus.setText("Disruptions checked – see Logcat");
            }

            @Override
            public void onFailure(Call<List<TflLineStatusDto>> call, Throwable t) {
                Log.e(TAG, "Line status network failure: " + t.getMessage(), t);
                textLineStatus.setText("Disruptions: check Logcat");
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
        if (mins < 2) return "High crowding";
        if (mins <= 5) return "Medium crowding";
        return "Low crowding";
    }
}
