package com.skhuthon_backend.domain.course.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record TimetableRecommendationResponseDto(
        Long timetableId,
        Integer score,
        Integer rank,
        List<String> tags,
        List<RecommendedCourseDto> courses
) {
}
