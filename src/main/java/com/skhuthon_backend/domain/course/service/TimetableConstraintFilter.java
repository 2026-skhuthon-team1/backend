package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

//- 1교시 제외
// 공강 요일은 하드 필터 대상이 아님(소프트 조건, AI 순위 단계에서 판단)

@Component
public class TimetableConstraintFilter {

    private static final LocalTime FIRST_PERIOD_START_TIME = LocalTime.of(9, 0);

    public List<CourseOffering> apply(
            List<CourseOffering> candidates,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto request
    ) {
        return candidates.stream()
                .filter(courseOffering -> isAllowedByFirstPeriodOption(
                        courseOffering,
                        timesByOfferingId,
                        request.excludeFirstPeriod()
                ))
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
}
