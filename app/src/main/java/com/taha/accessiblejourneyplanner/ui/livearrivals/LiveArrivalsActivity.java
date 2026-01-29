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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Simple placeholder UI so you can see the screen is open.
        TextView tv = new TextView(this);
        tv.setPadding(32, 32, 32, 32);
        tv.setText("Live Arrivals (Phase 2)\nCalling TfL API…\nCheck Logcat: " + TAG);
        setContentView(tv);

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
                    tv.setText("HTTP error: " + response.code() + "\nCheck Logcat: " + TAG);
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

                // Disruptions are read-only; we only fetch and display status (no actions taken).
                fetchDisruptionsAndAppendToUi(api, appId, appKey, tv);
            }

            @Override
            public void onFailure(Call<List<TflArrivalDto>> call, Throwable t) {
                Log.e(TAG, "Network/API failure: " + t.getMessage(), t);
                tv.setText("Network/API failure.\nCheck Logcat: " + TAG + "\n\n" + t.getMessage());
            }
        });
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
