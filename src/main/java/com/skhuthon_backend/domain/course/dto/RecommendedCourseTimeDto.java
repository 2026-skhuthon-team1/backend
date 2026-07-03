package com.skhuthon_backend.domain.course.dto;

import com.skhuthon_backend.domain.course.entity.OfferingTime;
import lombok.Builder;

import java.time.LocalTime;

@Builder
public record RecommendedCourseTimeDto(
        String dayOfWeek,
        LocalTime startTime,
        LocalTime endTime
) {

    public static RecommendedCourseTimeDto from(OfferingTime offeringTime) {
        return RecommendedCourseTimeDto.builder()
                .dayOfWeek(offeringTime.getDayOfWeek().getLabel())
                .startTime(offeringTime.getStartTime())
                .endTime(offeringTime.getEndTime())
                .build();
    }
}
