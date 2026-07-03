package com.skhuthon_backend.domain.course.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record RecommendedCourseDto(
        String courseName,
        String room,
        String category,
        String professor,
        Integer credits,
        List<RecommendedCourseTimeDto> times
) {
}
