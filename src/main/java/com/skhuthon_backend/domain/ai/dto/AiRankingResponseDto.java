package com.skhuthon_backend.domain.ai.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AiRankingResponseDto(

        Long timetableId,

        Integer score,

        Integer rank,

        List<String> tags

) {
}
