package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.entity.OfferingTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TimeConflictChecker {

    public boolean hasConflict(List<OfferingTime> selectedTimes, List<OfferingTime> candidateTimes) {
        return selectedTimes.stream()
                .anyMatch(selectedTime -> candidateTimes.stream()
                        .anyMatch(candidateTime -> isConflict(selectedTime, candidateTime)));
    }

    private boolean isConflict(OfferingTime first, OfferingTime second) {
        if (!first.getDayOfWeek().equals(second.getDayOfWeek())) {
            return false;
        }

        return first.getStartTime().isBefore(second.getEndTime())
                && second.getStartTime().isBefore(first.getEndTime());
    }
}
