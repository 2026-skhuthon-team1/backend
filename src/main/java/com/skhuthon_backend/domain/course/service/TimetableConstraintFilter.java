package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.CourseOffering;
import com.skhuthon_backend.domain.course.OfferingTime;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import java.time.LocalTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

//- 1교시 제외
//- 공강 요일 제외

@Component
public class TimetableConstraintFilter {

    private static final LocalTime FIRST_PERIOD_START_TIME = LocalTime.of(9, 0);

    public List<CourseOffering> apply(
            List<CourseOffering> candidates,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto request
    ) {
        Set<String> requestedFreeDays = toSet(request.freeDays());

        return candidates.stream()
                .filter(courseOffering -> isAllowedByFirstPeriodOption(
                        courseOffering,
                        timesByOfferingId,
                        request.excludeFirstPeriod()
                ))
                .filter(courseOffering -> isAllowedByFreeDays(courseOffering, timesByOfferingId, requestedFreeDays))
                .collect(Collectors.toList());
    }

    private boolean isAllowedByFirstPeriodOption(
            CourseOffering courseOffering,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            Boolean excludeFirstPeriod
    ) {
        if (!Boolean.TRUE.equals(excludeFirstPeriod)) {
            return true;
        }

        return timesByOfferingId.getOrDefault(courseOffering.getId(), Collections.emptyList()).stream()
                .noneMatch(offeringTime -> FIRST_PERIOD_START_TIME.equals(offeringTime.getStartTime()));
    }

    private boolean isAllowedByFreeDays(
            CourseOffering courseOffering,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            Set<String> requestedFreeDays
    ) {
        if (requestedFreeDays.isEmpty()) {
            return true;
        }

        return timesByOfferingId.getOrDefault(courseOffering.getId(), Collections.emptyList()).stream()
                .noneMatch(offeringTime -> requestedFreeDays.contains(offeringTime.getDayOfWeek()));
    }

    private Set<String> toSet(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }

        return values.stream().collect(Collectors.toSet());
    }
}
