package com.skhuthon_backend.domain.ai.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AiTimetableRequestDto(

        List<AiTimetableDto> candidates

) {
}
