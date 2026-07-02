package com.skhuthon_backend.domain.ai.service;

import com.skhuthon_backend.domain.ai.dto.AiRankingResponseDto;
import com.skhuthon_backend.domain.ai.dto.AiTimetableRequestDto;
import com.skhuthon_backend.domain.ai.exception.AiRankingException;
import com.skhuthon_backend.domain.ai.model.AiTimetableMapper;
import com.skhuthon_backend.domain.course.service.TimetableCombination;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FastApiService {

    private static final String RANK_PATH = "/api/rank";

    private final AiTimetableMapper aiTimetableMapper;
    private final RestClient restClient;

    public List<AiRankingResponseDto> rank(
            List<TimetableCombination> combinations
    ) {

        AiTimetableRequestDto request =
                aiTimetableMapper.toRequest(
                        combinations
                );

        try {
            return restClient.post()
                    .uri(RANK_PATH)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AiRankingResponseDto>>() {
                    });
        } catch (RestClientException e) {
            throw new AiRankingException("AI 순위 서버 호출에 실패했습니다.", e);
        }
    }
}
