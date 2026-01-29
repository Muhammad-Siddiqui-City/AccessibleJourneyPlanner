package com.taha.accessiblejourneyplanner.util;

import com.taha.accessiblejourneyplanner.ui.data.api.TflApiService;
import com.taha.accessiblejourneyplanner.ui.data.api.TflDisruptionDto;
import com.taha.accessiblejourneyplanner.ui.data.api.TflStopPointDto;
import com.taha.accessiblejourneyplanner.ui.data.db.AppDatabase;
import com.taha.accessiblejourneyplanner.ui.data.db.StopPointDisruptionDao;
import com.taha.accessiblejourneyplanner.ui.data.db.StopPointDisruptionEntity;
import com.taha.accessiblejourneyplanner.ui.data.db.StopPointInfoDao;
import com.taha.accessiblejourneyplanner.ui.data.db.StopPointInfoEntity;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import retrofit2.Response;

/**
 * Fetches StopPoint step-free and disruption (lift) with Room TTL cache.
 * 24h TTL for stop info, 10 min for disruptions per spec. Not perfect; data may be incomplete.
 */
public class StopPointCacheHelper {

    private static final long TTL_STOP_INFO_MS = 24 * 60 * 60 * 1000L;
    private static final long TTL_DISRUPTION_MS = 10 * 60 * 1000L;

    private final StopPointInfoDao infoDao;
    private final StopPointDisruptionDao disruptionDao;
    private final TflApiService api;
    private final String appId;
    private final String appKey;

    public interface Callback {
        void onLoaded(Map<String, String> stepFreeByStopId, Map<String, Boolean> liftByStopId);
    }

    public StopPointCacheHelper(AppDatabase db, TflApiService api, String appId, String appKey) {
        this.infoDao = db.stopPointInfoDao();
        this.disruptionDao = db.stopPointDisruptionDao();
        this.api = api;
        this.appId = appId;
        this.appKey = appKey;
    }

    /**
     * Load step-free and lift disruption for each stop. Uses cache if fresh; else fetches and caches.
     * Callback runs on the executor thread; caller should runOnUiThread if updating UI.
     */
    public void loadStopAccessibility(Set<String> stopIds, Executor executor, Callback callback) {
        executor.execute(() -> {
            Map<String, String> stepFree = new HashMap<>();
            Map<String, Boolean> lift = new HashMap<>();
            long now = System.currentTimeMillis();

            for (String stopId : stopIds) {
                if (stopId == null || stopId.trim().isEmpty()) continue;

                StopPointInfoEntity infoCached = infoDao.getByStopPointId(stopId);
                if (infoCached != null && (now - infoCached.fetchedAt) < TTL_STOP_INFO_MS) {
                    stepFree.put(stopId, infoCached.stepFreeText);
                } else {
                    try {
                        Response<List<TflStopPointDto>> r = api.getStopPoint(stopId, appId, appKey).execute();
                        if (r.isSuccessful() && r.body() != null && !r.body().isEmpty()) {
                            String parsed = parseStepFreeFromDto(r.body().get(0));
                            stepFree.put(stopId, parsed);
                            infoDao.insert(new StopPointInfoEntity(stopId, parsed, now));
                        } else {
                            stepFree.put(stopId, "Unknown");
                        }
                    } catch (Exception e) {
                        if (infoCached != null) {
                            stepFree.put(stopId, infoCached.stepFreeText);
                        } else {
                            stepFree.put(stopId, "Unknown");
                        }
                    }
                }

                StopPointDisruptionEntity dispCached = disruptionDao.getByStopPointId(stopId);
                if (dispCached != null && (now - dispCached.fetchedAt) < TTL_DISRUPTION_MS) {
                    lift.put(stopId, dispCached.hasLiftDisruption);
                } else {
                    try {
                        Response<List<TflDisruptionDto>> r = api.getStopPointDisruptions(stopId, appId, appKey).execute();
                        if (r.isSuccessful() && r.body() != null) {
                            boolean hasLift = hasLiftDisruptionFromList(r.body());
                            lift.put(stopId, hasLift);
                            disruptionDao.insert(new StopPointDisruptionEntity(stopId, hasLift, now));
                        } else {
                            lift.put(stopId, dispCached != null && dispCached.hasLiftDisruption);
                        }
                    } catch (Exception e) {
                        if (dispCached != null) {
                            lift.put(stopId, dispCached.hasLiftDisruption);
                        } else {
                            lift.put(stopId, false);
                        }
                    }
                }
            }
            callback.onLoaded(stepFree, lift);
        });
    }

    public static String parseStepFreeFromDto(TflStopPointDto dto) {
        if (dto == null) return "Unknown";
        String summary = dto.accessibilitySummary;
        if (summary != null && !summary.trim().isEmpty()) {
            String s = summary.trim().toLowerCase(Locale.UK);
            if (s.contains("step") && s.contains("free")) return "Yes";
            if (s.contains("no step") || s.contains("not step")) return "No";
            return summary.trim();
        }
        if (dto.additionalProperties != null) {
            for (TflStopPointDto.AdditionalProperty p : dto.additionalProperties) {
                if (p == null || p.key == null) continue;
                String k = p.key.toLowerCase(Locale.UK);
                if (k.contains("accessibility") || k.contains("stepfree") || k.contains("step_free")) {
                    String v = p.value != null ? p.value.trim() : "";
                    if (v.toLowerCase(Locale.UK).contains("yes") || v.toLowerCase(Locale.UK).contains("step free")) return "Yes";
                    if (v.toLowerCase(Locale.UK).contains("no")) return "No";
                    return v.isEmpty() ? "Unknown" : v;
                }
            }
        }
        return "Unknown";
    }

    public static boolean hasLiftDisruptionFromList(List<TflDisruptionDto> list) {
        if (list == null) return false;
        for (TflDisruptionDto d : list) {
            String cat = d.category != null ? d.category.toLowerCase(Locale.UK) : "";
            String desc = d.description != null ? d.description.toLowerCase(Locale.UK) : "";
            if (cat.contains("lift") || desc.contains("lift") || desc.contains("elevator")) return true;
        }
        return false;
    }

    /**
     * Classify journey step-free: all Yes -> Step-free friendly, any No -> Not step-free, else Unknown.
     */
    public static String classifyJourneyStepFree(Map<String, String> stepFreeByStopId, Set<String> stopIdsUsed) {
        if (stopIdsUsed == null || stopIdsUsed.isEmpty()) return "Unknown";
        boolean anyNo = false;
        boolean anyYes = false;
        for (String id : stopIdsUsed) {
            String v = stepFreeByStopId != null ? stepFreeByStopId.get(id) : null;
            if (v == null) continue;
            if ("No".equals(v)) anyNo = true;
            if ("Yes".equals(v)) anyYes = true;
        }
        if (anyNo) return "Not step-free";
        if (anyYes && stepIdsAllResolved(stepFreeByStopId, stopIdsUsed)) return "Step-free friendly";
        return "Unknown";
    }

    private static boolean stepIdsAllResolved(Map<String, String> stepFreeByStopId, Set<String> stopIdsUsed) {
        for (String id : stopIdsUsed) {
            if (!stepFreeByStopId.containsKey(id)) return false;
        }
        return true;
    }

    /**
     * True if any stop in the journey has lift disruption.
     */
    public static boolean journeyHasLiftIssues(Map<String, Boolean> liftByStopId, Set<String> stopIdsUsed) {
        if (stopIdsUsed == null || liftByStopId == null) return false;
        for (String id : stopIdsUsed) {
            Boolean v = liftByStopId.get(id);
            if (Boolean.TRUE.equals(v)) return true;
        }
        return false;
    }

    /**
     * Extract unique stop IDs (NaPTAN) from journey legs.
     */
    public static Set<String> extractStopIdsFromJourney(com.taha.accessiblejourneyplanner.ui.data.api.TflJourneyDto.Journey journey) {
        Set<String> ids = new HashSet<>();
        if (journey == null || journey.legs == null) return ids;
        for (com.taha.accessiblejourneyplanner.ui.data.api.TflJourneyDto.Leg leg : journey.legs) {
            if (leg.departurePoint != null && leg.departurePoint.naptanId != null) {
                ids.add(leg.departurePoint.naptanId.trim());
            }
            if (leg.arrivalPoint != null && leg.arrivalPoint.naptanId != null) {
                ids.add(leg.arrivalPoint.naptanId.trim());
            }
        }
        return ids;
    }
}
