package com.skhuthon_backend.domain.ai.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AiCourseDto(

        String courseName,

        String category,

        Integer credits,

        List<String> schedules

) {
}
