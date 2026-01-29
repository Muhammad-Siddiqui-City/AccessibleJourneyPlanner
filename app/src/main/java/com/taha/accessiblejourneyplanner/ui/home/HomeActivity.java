package com.taha.accessiblejourneyplanner.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.taha.accessiblejourneyplanner.R;
import com.taha.accessiblejourneyplanner.ui.stopsearch.StopSearchActivity;
import com.taha.accessiblejourneyplanner.ui.planjourney.PlanJourneyActivity;

/**
 * Simple Phase 1 home screen.
 * Shows high‑level navigation only; real logic will be added later phases.
 */
public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbar_home);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Accessible Journey Planner");
        }

        Button liveArrivalsButton = findViewById(R.id.button_live_arrivals);
        Button planJourneyButton = findViewById(R.id.button_plan_journey);

        // Minimal, explicit navigation using Intents. No Navigation Component.
        liveArrivalsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, StopSearchActivity.class);
                intent.putExtra(StopSearchActivity.EXTRA_MODE, StopSearchActivity.MODE_ARRIVALS);
                startActivity(intent);
            }
        });

        planJourneyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, PlanJourneyActivity.class);
                startActivity(intent);
            }
        });
    }
}
