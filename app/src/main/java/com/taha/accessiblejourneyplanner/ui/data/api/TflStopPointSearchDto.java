package com.taha.accessiblejourneyplanner.ui.data.api;

import java.util.List;

/**
 * Minimal DTO for GET StopPoint/Search?query=... Only fields we display: id, name, modes.
 * TfL returns: { "query", "total", "matches": [ { "id", "name", "modes": ["tube","bus",...], "icsId", ... } ] }
 */
public class TflStopPointSearchDto {

    public String query;
    public int total;
    public List<MatchedStop> matches;

    public static class MatchedStop {
        public String id;
        public String name;
        public List<String> modes;
        public String icsId;
    }
}
