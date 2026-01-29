package com.taha.accessiblejourneyplanner.ui.stopsearch;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.taha.accessiblejourneyplanner.BuildConfig;
import com.taha.accessiblejourneyplanner.R;
import com.taha.accessiblejourneyplanner.ui.data.api.RetrofitClient;
import com.taha.accessiblejourneyplanner.ui.data.api.TflApiService;
import com.taha.accessiblejourneyplanner.ui.data.api.TflStopPointSearchDto;
import com.taha.accessiblejourneyplanner.ui.livearrivals.LiveArrivalsActivity;
import com.taha.accessiblejourneyplanner.ui.planjourney.PlanJourneyActivity;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Callback;
import retrofit2.Response;

/**
 * Search for a station/stop by name. Used for Live Arrivals (pick stop) and Plan Journey (pick From/To).
 * Mode: ARRIVALS -> on select start LiveArrivalsActivity with stopId + name.
 * JOURNEY_FROM / JOURNEY_TO -> return result to PlanJourneyActivity.
 */
public class StopSearchActivity extends AppCompatActivity {

    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_STOP_ID = "stop_id";
    public static final String EXTRA_STOP_NAME = "stop_name";
    public static final String MODE_ARRIVALS = "arrivals";
    public static final String MODE_JOURNEY_FROM = "journey_from";
    public static final String MODE_JOURNEY_TO = "journey_to";

    private String mode;
    private TflApiService api;
    private String appId, appKey;

    private EditText editSearch;
    private com.google.android.material.button.MaterialButton buttonSearch;
    private TextView textMessage;
    private RecyclerView recycler;
    private ProgressBar progressBar;
    private StopSearchAdapter adapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stop_search);

        mode = getIntent().getStringExtra(EXTRA_MODE);
        if (mode == null) mode = MODE_ARRIVALS;

        appId = BuildConfig.TFL_APP_ID;
        appKey = BuildConfig.TFL_APP_KEY;
        if (appId == null || appId.trim().isEmpty() || appKey == null || appKey.trim().isEmpty()) {
            Toast.makeText(this, "Missing TfL API keys.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        api = RetrofitClient.getService();

        MaterialToolbar toolbar = findViewById(R.id.toolbar_stop_search);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        editSearch = findViewById(R.id.edit_stop_search);
        buttonSearch = findViewById(R.id.button_stop_search);
        textMessage = findViewById(R.id.text_stop_search_message);
        recycler = findViewById(R.id.recycler_stop_search);
        progressBar = findViewById(R.id.progress_stop_search);

        adapter = new StopSearchAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        buttonSearch.setOnClickListener(v -> doSearch());
        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch();
                return true;
            }
            return false;
        });

        adapter.setOnItemClickListener((id, name, modesStr) -> {
            if (MODE_ARRIVALS.equals(mode)) {
                Intent i = new Intent(this, LiveArrivalsActivity.class);
                i.putExtra(LiveArrivalsActivity.EXTRA_STOP_ID, id);
                i.putExtra(LiveArrivalsActivity.EXTRA_STOP_NAME, name);
                startActivity(i);
                finish();
            } else {
                Intent result = new Intent();
                result.putExtra(EXTRA_STOP_ID, id);
                result.putExtra(EXTRA_STOP_NAME, name);
                setResult(RESULT_OK, result);
                finish();
            }
        });
    }

    private void doSearch() {
        String q = editSearch.getText() != null ? editSearch.getText().toString().trim() : "";
        if (q.isEmpty()) {
            textMessage.setVisibility(View.VISIBLE);
            textMessage.setText("Type a station or stop name (e.g. Victoria, King's Cross)");
            adapter.setItems(null);
            return;
        }
        textMessage.setVisibility(View.GONE);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        buttonSearch.setEnabled(false);

        api.searchStopPoints(q, appId, appKey).enqueue(new Callback<TflStopPointSearchDto>() {
            @Override
            public void onResponse(retrofit2.Call<TflStopPointSearchDto> call, Response<TflStopPointSearchDto> response) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                buttonSearch.setEnabled(true);
                if (!response.isSuccessful()) {
                    textMessage.setVisibility(View.VISIBLE);
                    textMessage.setText("Search failed. Try again.");
                    adapter.setItems(null);
                    return;
                }
                TflStopPointSearchDto body = response.body();
                if (body == null || body.matches == null || body.matches.isEmpty()) {
                    textMessage.setVisibility(View.VISIBLE);
                    textMessage.setText("No results for \"" + q + "\"");
                    adapter.setItems(null);
                    return;
                }
                List<StopSearchItem> items = new ArrayList<>();
                for (TflStopPointSearchDto.MatchedStop m : body.matches) {
                    String id = m.id != null ? m.id : "";
                    String name = m.name != null ? m.name : "";
                    String modesStr = formatModes(m.modes);
                    items.add(new StopSearchItem(id, name, modesStr));
                }
                adapter.setItems(items);
                textMessage.setVisibility(View.GONE);
            }

            @Override
            public void onFailure(retrofit2.Call<TflStopPointSearchDto> call, Throwable t) {
                if (progressBar != null) progressBar.setVisibility(View.GONE);
                buttonSearch.setEnabled(true);
                textMessage.setVisibility(View.VISIBLE);
                textMessage.setText("Network error. Check connection and try again.");
                adapter.setItems(null);
            }
        });
    }

    private static String formatModes(List<String> modes) {
        if (modes == null || modes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < modes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(modes.get(i));
        }
        return sb.toString();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
