package com.skhuthon_backend.domain.course.dto;

import lombok.Builder;

@Builder
public record RecommendedCourseDto(
        String courseName,
        String room,
        String category,
        String professor
) {
}
