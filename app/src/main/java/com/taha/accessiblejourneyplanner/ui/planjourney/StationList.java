package com.taha.accessiblejourneyplanner.ui.planjourney;

import java.util.ArrayList;
import java.util.List;

/**
 * Curated list of major London stations with NaPTAN IDs for reliable journey planning.
 * TfL Journey API accepts these as from/to.
 */
public final class StationList {

    private StationList() {}

    private static final List<Station> STATIONS = new ArrayList<>();


    static {
        STATIONS.add(new Station("940GZZLUKSX", "King's Cross St Pancras"));
        STATIONS.add(new Station("940GZZLULVT", "Liverpool Street"));
        STATIONS.add(new Station("940GZZLUWLO", "Waterloo"));
        STATIONS.add(new Station("940GZZLUPAC", "Piccadilly Circus"));
        STATIONS.add(new Station("940GZZLUOXC", "Oxford Circus"));
        STATIONS.add(new Station("940GZZLUVIC", "Victoria"));
        STATIONS.add(new Station("940GZZLULBK", "London Bridge"));
        STATIONS.add(new Station("940GZZLUBNK", "Bank"));
        STATIONS.add(new Station("940GZZLUEUS", "Euston"));
        STATIONS.add(new Station("940GZZLUPAD", "Paddington"));
        STATIONS.add(new Station("940GZZLUGPK", "Green Park"));
        STATIONS.add(new Station("940GZZLUCHX", "Charing Cross"));
        STATIONS.add(new Station("940GZZLUMGT", "Moorgate"));
    }

    public static List<Station> getStations() {
        return new ArrayList<>(STATIONS);
    }

    public static class Station {
        public final String naptanId;
        public final String displayName;

        public Station(String naptanId, String displayName) {
            this.naptanId = naptanId;
            this.displayName = displayName;
        }
    }
}
