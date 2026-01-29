package com.taha.accessiblejourneyplanner.ui.livearrivals;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.taha.accessiblejourneyplanner.BuildConfig;
import com.taha.accessiblejourneyplanner.ui.data.api.RetrofitClient;
import com.taha.accessiblejourneyplanner.ui.data.api.TflApiService;
import com.taha.accessiblejourneyplanner.ui.data.api.TflArrivalDto;
import com.taha.accessiblejourneyplanner.ui.data.api.TflLineStatusDto;
import com.taha.accessiblejourneyplanner.ui.data.db.AppDatabase;
import com.taha.accessiblejourneyplanner.ui.data.db.ArrivalDao;
import com.taha.accessiblejourneyplanner.ui.data.db.CachedArrivalEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveArrivalsActivity extends AppCompatActivity {

    private static final String TAG = "LiveArrivals";

    // Change this to any valid StopPoint (NaPTAN) ID you want.
    // Example below is King's Cross St Pancras (commonly used in examples).
    private static final String STOP_POINT_ID = "940GZZLUKSX";

    // Hardcoded line for Phase 3 disruptions (read-only; we only fetch and display status).
    private static final String LINE_ID_DISRUPTIONS = "victoria";

    private AppDatabase db;
    private ArrivalDao dao;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Simple placeholder UI so you can see the screen is open.
        TextView tv = new TextView(this);
        tv.setPadding(32, 32, 32, 32);
        tv.setText("Live Arrivals (Phase 2)\nCalling TfL API…\nCheck Logcat: " + TAG);
        setContentView(tv);

        db = AppDatabase.getInstance(this);
        dao = db.arrivalDao();

        String appId = BuildConfig.TFL_APP_ID;
        String appKey = BuildConfig.TFL_APP_KEY;

        if (appId == null || appId.trim().isEmpty() || appKey == null || appKey.trim().isEmpty()) {
            Log.e(TAG, "Missing TfL API keys. Check secrets.properties + BuildConfigFields.");
            tv.setText("ERROR: Missing TfL API keys.\nSet TFL_APP_ID and TFL_APP_KEY in secrets.properties.");
            return;
        }

        TflApiService api = RetrofitClient.getService();

        Log.d(TAG, "Requesting arrivals for StopPoint: " + STOP_POINT_ID);
        api.getArrivals(STOP_POINT_ID, appId, appKey).enqueue(new Callback<List<TflArrivalDto>>() {
            @Override
            public void onResponse(Call<List<TflArrivalDto>> call, Response<List<TflArrivalDto>> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "HTTP error: " + response.code() + " " + response.message());
                    tryCacheThenShowError(tv, "HTTP error: " + response.code() + "\nCheck Logcat: " + TAG);
                    return;
                }

                List<TflArrivalDto> arrivals = response.body();
                if (arrivals == null) arrivals = Collections.emptyList();

                // Sort by soonest arrival (timeToStation is seconds)
                Collections.sort(arrivals, Comparator.comparingInt(a -> a.timeToStation));

                Log.d(TAG, "Arrivals returned: " + arrivals.size());
                int limit = Math.min(arrivals.size(), 10);

                StringBuilder sb = new StringBuilder();
                sb.append("Live Arrivals (Phase 2)\n");
                sb.append("StopPoint: ").append(STOP_POINT_ID).append("\n\n");
                sb.append("Logged top ").append(limit).append(" results to Logcat.\n");

                for (int i = 0; i < limit; i++) {
                    TflArrivalDto a = arrivals.get(i);

                    String line = safe(a.lineName);
                    String towards = safe(a.towards);
                    String platform = safe(a.platformName);
                    int mins = Math.max(0, a.timeToStation / 60);
                    // Crowding is inferred from timeToStation; TfL does not provide a direct crowding API here.
                    String crowding = inferCrowdingFromTimeToStation(a.timeToStation);

                    Log.i(TAG, (i + 1) + ") " + line +
                            " → " + towards +
                            " | " + mins + " min" +
                            (platform.isEmpty() ? "" : " | " + platform) +
                            " | " + crowding);
                }

                tv.setText(sb.toString());

                // Write-through cache: save arrivals so we can show them when offline.
                long fetchedAt = System.currentTimeMillis();
                final List<CachedArrivalEntity> entities = new ArrayList<>();
                for (TflArrivalDto a : arrivals) {
                    entities.add(new CachedArrivalEntity(
                            STOP_POINT_ID,
                            a.lineName,
                            a.towards,
                            a.platformName,
                            a.timeToStation,
                            fetchedAt
                    ));
                }
                dbExecutor.execute(() -> {
                    dao.deleteByStopPoint(STOP_POINT_ID);
                    dao.insertAll(entities);
                    Log.d(TAG, "Saved " + entities.size() + " arrivals to cache");
                });

                // Disruptions are read-only; we only fetch and display status (no actions taken).
                fetchDisruptionsAndAppendToUi(api, appId, appKey, tv);
            }

            @Override
            public void onFailure(Call<List<TflArrivalDto>> call, Throwable t) {
                Log.e(TAG, "Network/API failure: " + t.getMessage(), t);
                tryCacheThenShowError(tv, "Network/API failure.\nCheck Logcat: " + TAG + "\n\n" + t.getMessage());
            }
        });
    }

    /**
     * On failure, try to load cached arrivals from Room (off main thread), then update UI on main thread.
     * If cache is empty, show the given error message. Do not call disruptions when showing cache.
     */
    private void tryCacheThenShowError(final TextView tv, final String errorMessage) {
        dbExecutor.execute(() -> {
            final List<CachedArrivalEntity> cached = dao.getByStopPoint(STOP_POINT_ID);
            runOnUiThread(() -> showCachedOrError(tv, cached, errorMessage));
        });
    }

    /**
     * If cache has data: log "Offline – using cached data", "Loaded X cached arrivals", log top 10 with [CACHED] prefix,
     * set TextView to "Offline – showing cached arrivals". Otherwise set TextView to errorMessage.
     */
    private void showCachedOrError(TextView tv, List<CachedArrivalEntity> cached, String errorMessage) {
        if (cached == null || cached.isEmpty()) {
            tv.setText(errorMessage);
            return;
        }
        Log.d(TAG, "Offline – using cached data");
        Log.d(TAG, "Loaded " + cached.size() + " cached arrivals");
        int limit = Math.min(cached.size(), 10);
        for (int i = 0; i < limit; i++) {
            CachedArrivalEntity a = cached.get(i);
            String line = safe(a.lineName);
            String towards = safe(a.towards);
            String platform = safe(a.platformName);
            int mins = Math.max(0, a.timeToStation / 60);
            String crowding = inferCrowdingFromTimeToStation(a.timeToStation);
            Log.i(TAG, "[CACHED] " + (i + 1) + ") " + line +
                    " → " + towards +
                    " | " + mins + " min" +
                    (platform.isEmpty() ? "" : " | " + platform) +
                    " | " + crowding);
        }
        tv.setText("Offline – showing cached arrivals\nStopPoint: " + STOP_POINT_ID);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Inferred crowding from time-to-arrival (placeholder). TfL does not expose a direct crowding
     * API in this context, so we use timeToStation as a simple proxy for viva explanation.
     */
    private static String inferCrowdingFromTimeToStation(int timeToStationSeconds) {
        int mins = Math.max(0, timeToStationSeconds / 60);
        if (mins < 2) return "High crowding";
        if (mins <= 5) return "Medium crowding";
        return "Low crowding";
    }

    /**
     * Fetches line status (disruptions) for a hardcoded line and appends result message to the
     * given TextView. Logic kept simple and explicit for clarity and viva explanation.
     */
    private void fetchDisruptionsAndAppendToUi(TflApiService api, String appId, String appKey, final TextView tv) {
        Log.d(TAG, "Requesting line status for: " + LINE_ID_DISRUPTIONS);
        api.getLineStatus(LINE_ID_DISRUPTIONS, appId, appKey).enqueue(new Callback<List<TflLineStatusDto>>() {
            @Override
            public void onResponse(Call<List<TflLineStatusDto>> call, Response<List<TflLineStatusDto>> response) {
                if (!response.isSuccessful()) {
                    Log.e(TAG, "Disruptions HTTP error: " + response.code() + " " + response.message());
                    tv.setText(tv.getText() + "\nDisruptions: check Logcat");
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
                    }
                    Log.i(TAG, "Disruption – Line: " + lineName + " | Status: " + status + reason);
                }
                tv.setText(tv.getText() + "\nDisruptions checked – see Logcat");
            }

            @Override
            public void onFailure(Call<List<TflLineStatusDto>> call, Throwable t) {
                Log.e(TAG, "Disruptions network failure: " + t.getMessage(), t);
                tv.setText(tv.getText() + "\nDisruptions: check Logcat");
            }
        });
    }
}
