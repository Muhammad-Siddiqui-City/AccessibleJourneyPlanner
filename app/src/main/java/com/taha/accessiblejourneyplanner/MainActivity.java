package com.taha.accessiblejourneyplanner;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);
        textView.setText("Accessible Journey Planner\nJava activity works ✅");
        textView.setTextSize(20f);
        textView.setPadding(40, 40, 40, 40);

        setContentView(textView);
    }
}
