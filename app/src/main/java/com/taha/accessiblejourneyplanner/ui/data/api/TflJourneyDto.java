package com.taha.accessiblejourneyplanner.ui.data.api;

import java.util.List;

/**
 * Minimal DTO for GET Journey/JourneyResults/{from}/to/{to}. Only fields we display.
 */
public class TflJourneyDto {

    /** Root response: list of journey options. */
    public static class ItineraryResult {
        public List<Journey> journeys;
    }

    /** Single journey option: duration (minutes), legs. */
    public static class Journey {
        public int duration;
        public List<Leg> legs;
    }

    /** Single leg: mode, duration, instruction, from/to points. */
    public static class Leg {
        public int duration;
        public Instruction instruction;
        public ModeInfo mode;
        public JourneyPoint departurePoint;
        public JourneyPoint arrivalPoint;
    }

    public static class Instruction {
        public String summary;
        public String detailed;
    }

    public static class ModeInfo {
        public String id;
        public String name;
    }

    /** Point in a leg; may have commonName and naptanId from API. */
    public static class JourneyPoint {
        public String commonName;
        public String naptanId;
    }
}
