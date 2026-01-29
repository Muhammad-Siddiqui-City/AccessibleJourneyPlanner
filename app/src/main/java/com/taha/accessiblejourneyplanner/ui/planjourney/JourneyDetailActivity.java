package com.taha.accessiblejourneyplanner.ui.planjourney;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.appbar.MaterialToolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.taha.accessiblejourneyplanner.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the legs of a selected journey. Receives legs and total duration via Intent.
 */
public class JourneyDetailActivity extends AppCompatActivity {

    private static final String EXTRA_LEGS = "legs";
    private static final String EXTRA_DURATION = "duration";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journey_detail);

        MaterialToolbar toolbar = findViewById(R.id.toolbar_journey_detail);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        TextView textDuration = findViewById(R.id.text_journey_duration);
        RecyclerView recycler = findViewById(R.id.recycler_legs);

        int duration = getIntent().getIntExtra(EXTRA_DURATION, 0);
        @SuppressWarnings("unchecked")
        ArrayList<LegDisplayItem> legs = (ArrayList<LegDisplayItem>) getIntent().getSerializableExtra(EXTRA_LEGS);

        textDuration.setText("Total: " + duration + " min");
        textDuration.setContentDescription("Total journey duration " + duration + " minutes");

        LegsAdapter adapter = new LegsAdapter();
        recycler.setLayoutManager(new LinearLayoutManager(this));
        recycler.setAdapter(adapter);

        if (legs != null && !legs.isEmpty()) {
            adapter.setItems(legs);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
