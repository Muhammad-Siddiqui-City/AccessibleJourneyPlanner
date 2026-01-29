package com.taha.accessiblejourneyplanner.ui.planjourney;

import com.taha.accessiblejourneyplanner.ui.data.api.TflJourneyDto;

import java.util.List;

/**
 * Display model for one journey option: duration, modes summary, step-free/lift/bus chips, rank reason.
 */
public class JourneyResultItem {
    public final int durationMinutes;
    public final String modesSummary;
    public final String stepFreeBadge;
    public final String liftBadge;
    public final boolean busIncluded;
    public final String rankReason;
    /** Original journey for detail screen. */
    public final TflJourneyDto.Journey journey;

    public JourneyResultItem(int durationMinutes, String modesSummary,
                             String stepFreeBadge, String liftBadge, boolean busIncluded, String rankReason,
                             TflJourneyDto.Journey journey) {
        this.durationMinutes = durationMinutes;
        this.modesSummary = modesSummary != null ? modesSummary : "";
        this.stepFreeBadge = stepFreeBadge != null ? stepFreeBadge : "Unknown";
        this.liftBadge = liftBadge != null ? liftBadge : "Unknown";
        this.busIncluded = busIncluded;
        this.rankReason = rankReason != null ? rankReason : "";
        this.journey = journey;
    }
}
