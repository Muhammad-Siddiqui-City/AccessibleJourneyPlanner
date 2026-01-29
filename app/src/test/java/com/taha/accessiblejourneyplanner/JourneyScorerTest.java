package com.taha.accessiblejourneyplanner;

import com.taha.accessiblejourneyplanner.util.JourneyScorer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for JourneyScorer: ordering and rule handling.
 */
public class JourneyScorerTest {

    @Test
    public void normalizedDuration_sameMinMax_returnsZero() {
        assertEquals(0.0, JourneyScorer.normalizedDuration(10, 10, 10), 1e-9);
    }

    @Test
    public void normalizedDuration_minReturnsZero() {
        assertEquals(0.0, JourneyScorer.normalizedDuration(20, 20, 40), 1e-9);
    }

    @Test
    public void normalizedDuration_maxReturnsLessThanOne() {
        double d = JourneyScorer.normalizedDuration(40, 20, 40);
        assertTrue(d > 0 && d <= 1.0);
    }

    @Test
    public void score_stepFreeFriendly_lowerThanNotStepFree() {
        double stepFree = JourneyScorer.score(0, true, false, false);
        double notStepFree = JourneyScorer.score(0, false, true, false);
        assertTrue(stepFree < notStepFree);
    }

    @Test
    public void score_liftIssues_addsPenalty() {
        double noLift = JourneyScorer.score(0, false, false, false);
        double withLift = JourneyScorer.score(0, false, false, true);
        assertTrue(withLift > noLift);
    }

    @Test
    public void fullScore_stepFreeFriendly_ranksLower() {
        int min = 20, max = 40;
        double stepFree = JourneyScorer.fullScore(25, min, max, true, false, false);
        double notStepFree = JourneyScorer.fullScore(25, min, max, false, true, false);
        assertTrue(stepFree < notStepFree);
    }

    @Test
    public void fullScore_shorterDuration_ranksLower() {
        int min = 20, max = 40;
        double shortDur = JourneyScorer.fullScore(20, min, max, false, false, false);
        double longDur = JourneyScorer.fullScore(40, min, max, false, false, false);
        assertTrue(shortDur < longDur);
    }

    @Test
    public void rankReason_firstRoute_includesRankedHigher() {
        String s = JourneyScorer.rankReason(true, false, 0);
        assertTrue(s.contains("Ranked higher") || s.contains("step-free"));
    }
}
