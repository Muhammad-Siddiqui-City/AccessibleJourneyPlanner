package com.taha.accessiblejourneyplanner.ui.livearrivals;

/**
 * Display model for one arrival row: line, towards, platform, minutes, crowding label.
 */
public class ArrivalItem {
    public final String lineName;
    public final String towards;
    public final String platformName;
    public final int minutesToArrival;
    public final String crowdingLabel;

    public ArrivalItem(String lineName, String towards, String platformName,
                       int minutesToArrival, String crowdingLabel) {
        this.lineName = lineName != null ? lineName.trim() : "";
        this.towards = towards != null ? towards.trim() : "";
        this.platformName = platformName != null ? platformName.trim() : "";
        this.minutesToArrival = Math.max(0, minutesToArrival);
        this.crowdingLabel = crowdingLabel != null ? crowdingLabel : "";
    }
}
