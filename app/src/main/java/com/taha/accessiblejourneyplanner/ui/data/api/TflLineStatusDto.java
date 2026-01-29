package com.taha.accessiblejourneyplanner.ui.data.api;

import java.util.List;

/**
 * Minimal DTO for TfL Line Status (disruptions) – only fields we display.
 * GET Line/{id}/Status returns one line with nested lineStatuses.
 */
public class TflLineStatusDto {
    public String id;
    public String name;
    public List<LineStatusEntry> lineStatuses;

    /**
     * Single status entry (e.g. Good Service, Minor Delays). reason may be null when no disruption.
     */
    public static class LineStatusEntry {
        public int statusSeverity;
        public String statusSeverityDescription;
        public String reason;
    }
}
