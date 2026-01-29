package com.taha.accessiblejourneyplanner.ui.planjourney;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.taha.accessiblejourneyplanner.BuildConfig;
import com.taha.accessiblejourneyplanner.R;
import com.taha.accessiblejourneyplanner.ui.data.api.RetrofitClient;
import com.taha.accessiblejourneyplanner.ui.data.api.TflApiService;
import com.taha.accessiblejourneyplanner.ui.data.api.TflJourneyDto;
import com.taha.accessiblejourneyplanner.ui.data.db.AppDatabase;
import com.taha.accessiblejourneyplanner.util.JourneyScorer;
import com.taha.accessiblejourneyplanner.util.StopPointCacheHelper;
import com.taha.accessiblejourneyplanner.util.TtsManager;
import com.taha.accessiblejourneyplanner.ui.stopsearch.StopSearchActivity;

import androidx.appcompat.widget.SwitchCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlanJourneyActivity extends AppCompatActivity {

    private static final String TAG = "PlanJourney";
    private static final int REQ_FROM = 1001;
    private static final int REQ_TO = 1002;
    private static final String EXTRA_LEGS = "legs";
    private static final String EXTRA_DURATION = "duration";

    private TextView textFromStation;
    private TextView textToStation;
    private View buttonSearch;
    private ProgressBar progressBar;
    private TextView textErrorOrEmpty;
    private TextView labelResults;
    private RecyclerView recycler;
    private JourneyResultsAdapter adapter;

    private String fromId;
    private String fromName;
    private String toId;
    private String toName;

    private TflApiService api;
    private String appId;
    private String appKey;
    private AppDatabase db;
    private StopPointCacheHelper cacheHelper;
    private final Executor dbExecutor = Executors.newSingleThreadExecutor();
    private TtsManager ttsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_plan_journey);

        appId = BuildConfig.TFL_APP_ID;
        appKey = BuildConfig.TFL_APP_KEY;
        if (appId == null || appId.trim().isEmpty() || appKey == null || appKey.trim().isEmpty()) {
            Toast.makeText(this, "Missing TfL API keys.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        api = RetrofitClient.getService();
        db = AppDatabase.getInstance(this);
        cacheHelper = new StopPointCacheHelper(db, api, appId, appKey);
        ttsManager = new TtsManager(this);
        ttsManager.init();

        SwitchCompat switchTts = findViewById(R.id.switch_tts);
        switchTts.setChecked(ttsManager.isEnabled());
        switchTts.setOnCheckedChangeListener((buttonView, isChecked) -> ttsManager.setEnabled(isChecked));

        textFromStation = findViewById(R.id.text_from_station);
        textToStation = findViewById(R.id.text_to_station);
        buttonSearch = findViewById(R.id.button_search_routes);
        progressBar = findViewById(R.id.progress_plan_journey);
        textErrorOrEmpty = findViewById(R.id.text_journey_error_or_empty);
        labelResults = findViewById(R.id.label_journey_results);
        recycler = findViewById(R.id.recycler_journey_results);

        adapter = new JourneyResultsAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        buttonSearch.setOnClickListener(v -> searchRoutes());

        adapter.setOnJourneyClickListener((item, position) -> {
            if (ttsManager != null) ttsManager.speakRouteSummary(position, item.durationMinutes, item.stepFreeBadge, item.liftBadge);
            openJourneyDetail(item);
        });

        ActivityResultLauncher<Intent> fromLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    fromId = result.getData().getStringExtra(StopSearchActivity.EXTRA_STOP_ID);
                    fromName = result.getData().getStringExtra(StopSearchActivity.EXTRA_STOP_NAME);
                    if (fromName != null) textFromStation.setText(fromName);
                    if (ttsManager != null && fromName != null) ttsManager.speakStationName(fromName);
                });
        ActivityResultLauncher<Intent> toLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) return;
                    toId = result.getData().getStringExtra(StopSearchActivity.EXTRA_STOP_ID);
                    toName = result.getData().getStringExtra(StopSearchActivity.EXTRA_STOP_NAME);
                    if (toName != null) textToStation.setText(toName);
                    if (ttsManager != null && toName != null) ttsManager.speakStationName(toName);
                });

        findViewById(R.id.row_from).setOnClickListener(v -> {
            Intent i = new Intent(this, StopSearchActivity.class);
            i.putExtra(StopSearchActivity.EXTRA_MODE, StopSearchActivity.MODE_JOURNEY_FROM);
            fromLauncher.launch(i);
        });
        findViewById(R.id.row_to).setOnClickListener(v -> {
            Intent i = new Intent(this, StopSearchActivity.class);
            i.putExtra(StopSearchActivity.EXTRA_MODE, StopSearchActivity.MODE_JOURNEY_TO);
            toLauncher.launch(i);
        });
    }


    private void searchRoutes() {
        if (fromId == null || toId == null) {
            Toast.makeText(this, "Please select From and To stations.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fromId.equals(toId)) {
            Toast.makeText(this, "From and To must be different.", Toast.LENGTH_SHORT).show();
            return;
        }

        textErrorOrEmpty.setVisibility(View.GONE);
        labelResults.setVisibility(View.GONE);
        recycler.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        buttonSearch.setEnabled(false);

        api.getJourneyResults(fromId, toId, appId, appKey).enqueue(new Callback<TflJourneyDto.ItineraryResult>() {
            @Override
            public void onResponse(Call<TflJourneyDto.ItineraryResult> call, Response<TflJourneyDto.ItineraryResult> response) {
                progressBar.setVisibility(View.GONE);
                buttonSearch.setEnabled(true);

                if (!response.isSuccessful()) {
                    Log.e(TAG, "Journey HTTP error: " + response.code());
                    textErrorOrEmpty.setText("Could not load routes. Try again.");
                    textErrorOrEmpty.setVisibility(View.VISIBLE);
                    return;
                }

                TflJourneyDto.ItineraryResult result = response.body();
                if (result == null || result.journeys == null || result.journeys.isEmpty()) {
                    textErrorOrEmpty.setText("No routes found.");
                    textErrorOrEmpty.setVisibility(View.VISIBLE);
                    return;
                }

                Set<String> allStopIds = new HashSet<>();
                for (TflJourneyDto.Journey j : result.journeys) {
                    allStopIds.addAll(StopPointCacheHelper.extractStopIdsFromJourney(j));
                }

                cacheHelper.loadStopAccessibility(allStopIds, dbExecutor, (stepFreeByStop, liftByStop) -> runOnUiThread(() -> {
                    int minDur = Integer.MAX_VALUE;
                    int maxDur = 0;
                    for (TflJourneyDto.Journey j : result.journeys) {
                        minDur = Math.min(minDur, j.duration);
                        maxDur = Math.max(maxDur, j.duration);
                    }
                    if (minDur == Integer.MAX_VALUE) minDur = 0;

                    List<ScoredJourney> scored = new ArrayList<>();
                    for (TflJourneyDto.Journey j : result.journeys) {
                        Set<String> stopIds = StopPointCacheHelper.extractStopIdsFromJourney(j);
                        String stepFreeBadge = StopPointCacheHelper.classifyJourneyStepFree(stepFreeByStop, stopIds);
                        boolean liftIssues = StopPointCacheHelper.journeyHasLiftIssues(liftByStop, stopIds);
                        boolean stepFreeFriendly = "Step-free friendly".equals(stepFreeBadge);
                        boolean notStepFree = "Not step-free".equals(stepFreeBadge);
                        String liftBadge = liftIssues ? "Lift issues present" : "No lift issues";
                        double score = JourneyScorer.fullScore(j.duration, minDur, maxDur, stepFreeFriendly, notStepFree, liftIssues);
                        scored.add(new ScoredJourney(j, stepFreeBadge, liftBadge, stepFreeFriendly, notStepFree, liftIssues, score));
                    }
                    scored.sort((a, b) -> Double.compare(a.score, b.score));

                    List<JourneyResultItem> items = new ArrayList<>();
                    for (int i = 0; i < scored.size(); i++) {
                        ScoredJourney s = scored.get(i);
                        String rankReason = JourneyScorer.rankReason(s.stepFreeFriendly, s.liftIssues, i);
                        boolean busIncluded = hasBusLeg(s.j);
                        items.add(new JourneyResultItem(
                                s.j.duration,
                                buildModesSummary(s.j),
                                s.stepFreeBadge,
                                s.liftBadge,
                                busIncluded,
                                rankReason,
                                s.j
                        ));
                    }
                    adapter.setItems(items);
                    labelResults.setVisibility(View.VISIBLE);
                    recycler.setVisibility(View.VISIBLE);
                    textErrorOrEmpty.setVisibility(View.GONE);
                }));
            }

            @Override
            public void onFailure(Call<TflJourneyDto.ItineraryResult> call, Throwable t) {
                Log.e(TAG, "Journey failure: " + t.getMessage(), t);
                progressBar.setVisibility(View.GONE);
                buttonSearch.setEnabled(true);
                textErrorOrEmpty.setText("Network error. Check connection and try again.");
                textErrorOrEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private static boolean hasBusLeg(TflJourneyDto.Journey j) {
        if (j == null || j.legs == null) return false;
        for (TflJourneyDto.Leg leg : j.legs) {
            if (leg.mode != null && "bus".equalsIgnoreCase(leg.mode.name)) return true;
        }
        return false;
    }

    private static String buildModesSummary(TflJourneyDto.Journey j) {
        if (j.legs == null || j.legs.isEmpty()) return "—";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < j.legs.size(); i++) {
            TflJourneyDto.Leg leg = j.legs.get(i);
            String name = leg.mode != null ? leg.mode.name : "Unknown";
            if (i > 0) sb.append(" + ");
            sb.append(name);
        }
        return sb.toString();
    }

    private void openJourneyDetail(JourneyResultItem item) {
        if (item.journey == null || item.journey.legs == null) return;
        ArrayList<LegDisplayItem> legs = new ArrayList<>();
        for (TflJourneyDto.Leg leg : item.journey.legs) {
            String modeName = leg.mode != null ? leg.mode.name : "";
            int dur = leg.duration;
            String summary = leg.instruction != null ? leg.instruction.summary : "";
            String from = leg.departurePoint != null ? leg.departurePoint.commonName : "";
            String to = leg.arrivalPoint != null ? leg.arrivalPoint.commonName : "";
            legs.add(new LegDisplayItem(modeName, dur, summary, from, to));
        }
        Intent intent = new Intent(this, JourneyDetailActivity.class);
        intent.putExtra(EXTRA_LEGS, legs);
        intent.putExtra(EXTRA_DURATION, item.durationMinutes);
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) ttsManager.shutdown();
    }

    private static class ScoredJourney {
        final TflJourneyDto.Journey j;
        final String stepFreeBadge;
        final String liftBadge;
        final boolean stepFreeFriendly;
        final boolean notStepFree;
        final boolean liftIssues;
        final double score;

        ScoredJourney(TflJourneyDto.Journey j, String stepFreeBadge, String liftBadge,
                      boolean stepFreeFriendly, boolean notStepFree, boolean liftIssues, double score) {
            this.j = j;
            this.stepFreeBadge = stepFreeBadge;
            this.liftBadge = liftBadge;
            this.stepFreeFriendly = stepFreeFriendly;
            this.notStepFree = notStepFree;
            this.liftIssues = liftIssues;
            this.score = score;
        }
    }
}
