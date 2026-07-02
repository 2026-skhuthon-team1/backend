package com.skhuthon_backend.domain.ai.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AiTimetableDto(

        Long timetableId,

        AiTimetableFeatureDto feature,

        List<AiCourseDto> courses

) {
}
