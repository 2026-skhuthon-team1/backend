package com.skhuthon_backend.domain.ai.service;

import com.skhuthon_backend.domain.ai.dto.AiRankingResponseDto;
import com.skhuthon_backend.domain.ai.dto.AiTimetableRequestDto;
import com.skhuthon_backend.domain.ai.model.AiTimetableMapper;
import com.skhuthon_backend.domain.course.service.TimetableCombination;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FastApiService {

    private final AiTimetableMapper aiTimetableMapper;

    public List<AiRankingResponseDto> rank(
            List<TimetableCombination> combinations
    ) {

        // AI 요청 DTO 생성
        AiTimetableRequestDto request =
                aiTimetableMapper.toRequest(
                        combinations
                );

        // TODO FastAPI 호출

        return List.of();
    }
}