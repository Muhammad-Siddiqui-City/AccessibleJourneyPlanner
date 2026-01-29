package com.taha.accessiblejourneyplanner.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Simple, transparent scoring for journey options. Lower score = better.
 * Inputs: duration (min), step-free (Yes/No/Unknown), lift issues (yes/no).
 * No crowding for journeys unless TfL provides explicit value; kept off by default.
 */
public final class JourneyScorer {

    private JourneyScorer() {}

    /** Step-free friendly: strong preference (lower score). */
    private static final double BONUS_STEP_FREE = -0.25;
    /** Not step-free: penalty. */
    private static final double PENALTY_NOT_STEP_FREE = 0.25;
    /** Lift issues present: strong penalty. */
    private static final double PENALTY_LIFT_ISSUES = 0.35;

    /**
     * Score for one journey. Lower is better.
     * Formula: normalizedDuration + stepFreeAdjustment + liftPenalty.
     */
    public static double score(
            int durationMinutes,
            boolean stepFreeFriendly,
            boolean notStepFree,
            boolean liftIssuesPresent
    ) {
        double s = 0.0;
        if (stepFreeFriendly) s += BONUS_STEP_FREE;
        if (notStepFree) s += PENALTY_NOT_STEP_FREE;
        if (liftIssuesPresent) s += PENALTY_LIFT_ISSUES;
        return s;
    }

    /**
     * Normalize duration to [0,1] given min and max. Used to rank by duration first.
     * (duration - min) / (max - min + 1) to avoid div by zero.
     */
    public static double normalizedDuration(int durationMinutes, int minDuration, int maxDuration) {
        int range = maxDuration - minDuration + 1;
        if (range <= 0) return 0.0;
        return (double) (durationMinutes - minDuration) / range;
    }

    /**
     * Full score for ranking: normalizedDuration + stepFreeAdjustment + liftPenalty.
     * Sort ascending.
     */
    public static double fullScore(
            int durationMinutes,
            int minDuration,
            int maxDuration,
            boolean stepFreeFriendly,
            boolean notStepFree,
            boolean liftIssuesPresent
    ) {
        double base = normalizedDuration(durationMinutes, minDuration, maxDuration);
        return base + score(0, stepFreeFriendly, notStepFree, liftIssuesPresent);
    }

    /**
     * Build a short "why this route ranked" explanation.
     */
    public static String rankReason(boolean stepFreeFriendly, boolean liftIssuesPresent, int rankIndex) {
        StringBuilder sb = new StringBuilder();
        if (rankIndex == 0) {
            sb.append("Ranked higher: ");
        } else {
            sb.append("Rank #").append(rankIndex + 1).append(": ");
        }
        if (stepFreeFriendly && !liftIssuesPresent) {
            sb.append("step-free access, no lift issues.");
        } else if (stepFreeFriendly) {
            sb.append("step-free access; lift issues present.");
        } else if (!liftIssuesPresent) {
            sb.append("no lift issues; step-free unknown.");
        } else {
            sb.append("lift issues present.");
        }
        return sb.toString();
    }

    /**
     * Sort journey items by score ascending (lower score first). Modifies the list.
     */
    public static <T> void sortByScore(
            List<T> items,
            int minDuration,
            int maxDuration,
            ScoreExtractor<T> extractor
    ) {
        if (items == null || items.size() <= 1) return;
        Collections.sort(items, new Comparator<T>() {
            @Override
            public int compare(T a, T b) {
                double scoreA = extractor.score(a, minDuration, maxDuration);
                double scoreB = extractor.score(b, minDuration, maxDuration);
                return Double.compare(scoreA, scoreB);
            }
        });
    }

    public interface ScoreExtractor<T> {
        double score(T item, int minDuration, int maxDuration);
    }
}
