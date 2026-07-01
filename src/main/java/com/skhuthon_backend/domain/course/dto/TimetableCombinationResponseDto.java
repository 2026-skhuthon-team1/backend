package com.skhuthon_backend.domain.course.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record TimetableCombinationResponseDto(
        Long timetableId,
        Integer totalCredits,
        Integer majorCredits,
        Integer generalCredits,
        Integer attendanceDays,
        List<String> freeDays,
        Boolean excludeFirstPeriod,
        List<CourseOfferingCandidateResponseDto> offerings
) {
}
