package com.taha.accessiblejourneyplanner.util;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.Locale;

/**
 * Limited TTS: speak station names and route summary. Lifecycle-safe: init in Activity onCreate, shutdown in onDestroy.
 */
public class TtsManager {

    private static final String TAG = "TtsManager";

    private final Context context;
    private TextToSpeech tts;
    private boolean enabled = true;

    public TtsManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Call from Activity onCreate. Initialises TTS engine.
     */
    public void init() {
        if (tts != null) return;
        tts = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.UK);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.w(TAG, "TTS language not supported");
                }
            } else {
                Log.e(TAG, "TTS init failed");
            }
        });
    }

    /**
     * Call from Activity onDestroy. Stops and releases TTS.
     */
    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    /**
     * Speak station name when user selects From/To.
     */
    public void speakStationName(String stationName) {
        if (!enabled || tts == null) return;
        String text = stationName != null && !stationName.trim().isEmpty() ? stationName.trim() : "Unknown station";
        speak(text);
    }

    /**
     * Speak short route summary when a route is selected.
     */
    public void speakRouteSummary(int routeIndex, int durationMinutes, String stepFreeBadge, String liftBadge) {
        if (!enabled || tts == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("Route ").append(routeIndex + 1).append(". ");
        sb.append(durationMinutes).append(" minutes. ");
        sb.append(stepFreeBadge != null ? stepFreeBadge : "Unknown").append(". ");
        sb.append(liftBadge != null ? liftBadge : "Unknown.");
        speak(sb.toString());
    }

    private void speak(String text) {
        if (tts == null || text == null || text.trim().isEmpty()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text.trim(), TextToSpeech.QUEUE_FLUSH, null, "utt");
        } else {
            tts.speak(text.trim(), TextToSpeech.QUEUE_FLUSH, null);
        }
    }
}
