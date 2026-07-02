package com.skhuthon_backend.domain.ai.model;

import com.skhuthon_backend.domain.course.dto.TimetableFeature;
import com.skhuthon_backend.domain.course.service.TimetableCombination;
import com.skhuthon_backend.domain.course.service.TimetableFeatureExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TimetableCandidateReducer {

    private static final int MAX_AI_CANDIDATES = 25;

    private final TimetableFeatureExtractor featureExtractor;

    public List<TimetableCombination> reduce(
            List<TimetableCombination> combinations
    ) {

        if (combinations.size() <= MAX_AI_CANDIDATES) {
            return combinations;
        }

        List<TimetableCombination> selected = new ArrayList<>();

        selected.add(combinations.get(0));

        List<TimetableCombination> remaining =
                new ArrayList<>(combinations);

        remaining.remove(0);

        while (!remaining.isEmpty()
                && selected.size() < MAX_AI_CANDIDATES) {

            TimetableCombination best = null;
            double bestDistance = -1;

            for (TimetableCombination candidate : remaining) {

                double minDistance = Double.MAX_VALUE;

                for (TimetableCombination picked : selected) {

                    double distance =
                            calculateDistance(candidate, picked);

                    minDistance =
                            Math.min(minDistance, distance);
                }

                if (minDistance > bestDistance) {
                    bestDistance = minDistance;
                    best = candidate;
                }
            }

            selected.add(best);
            remaining.remove(best);
        }

        return selected;
    }

    private double calculateDistance(
            TimetableCombination a,
            TimetableCombination b
    ) {

        TimetableFeature fa =
                featureExtractor.extract(a);

        TimetableFeature fb =
                featureExtractor.extract(b);

        return Math.abs(
                fa.firstPeriodCount()
                        - fb.firstPeriodCount())
                + Math.abs(
                fa.longBreakCount()
                        - fb.longBreakCount())
                + Math.abs(
                fa.longestBreakMinutes()
                        - fb.longestBreakMinutes())
                + Math.abs(
                fa.lunchBreakCount()
                        - fb.lunchBreakCount())
                + Math.abs(
                fa.earlyFinishDayCount()
                        - fb.earlyFinishDayCount())
                + Math.abs(
                fa.attendanceDays()
                        - fb.attendanceDays())
                + Math.abs(
                fa.earliestStartTime().toSecondOfDay()
                        - fb.earliestStartTime().toSecondOfDay())
                / 3600.0
                + Math.abs(
                fa.latestEndTime().toSecondOfDay()
                        - fb.latestEndTime().toSecondOfDay())
                / 3600.0;
    }
}
