package com.taha.accessiblejourneyplanner.ui.data.api;

import java.util.List;

/**
 * Minimal DTO for GET StopPoint/{id}. Only fields we display (accessibility, name).
 * TfL may return accessibilitySummary (string) or additionalProperties with key "Accessibility"/"StepFreeAccess".
 */
public class TflStopPointDto {
    public String id;
    public String commonName;
    /** Human-readable accessibility summary if present. */
    public String accessibilitySummary;
    /** Key-value pairs; look for "Accessibility", "StepFreeAccess" if accessibilitySummary missing. */
    public List<AdditionalProperty> additionalProperties;

    public static class AdditionalProperty {
        public String key;
        public String value;
    }
}
