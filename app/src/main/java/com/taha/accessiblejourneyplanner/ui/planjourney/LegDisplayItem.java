package com.taha.accessiblejourneyplanner.ui.planjourney;

import java.io.Serializable;

/**
 * Display model for one leg to pass to JourneyDetailActivity.
 */
public class LegDisplayItem implements Serializable {
    public final String modeName;
    public final int durationMinutes;
    public final String instructionSummary;
    public final String fromName;
    public final String toName;

    public LegDisplayItem(String modeName, int durationMinutes, String instructionSummary,
                          String fromName, String toName) {
        this.modeName = modeName != null ? modeName : "";
        this.durationMinutes = Math.max(0, durationMinutes);
        this.instructionSummary = instructionSummary != null ? instructionSummary : "";
        this.fromName = fromName != null ? fromName : "";
        this.toName = toName != null ? toName : "";
    }
}
