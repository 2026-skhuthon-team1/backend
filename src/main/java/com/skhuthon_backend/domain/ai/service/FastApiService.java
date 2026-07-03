package com.skhuthon_backend.domain.ai.service;

import com.skhuthon_backend.domain.ai.dto.AiRankingResponseDto;
import com.skhuthon_backend.domain.ai.dto.AiTimetableRequestDto;
import com.skhuthon_backend.domain.ai.exception.AiRankingException;
import com.skhuthon_backend.domain.ai.model.AiTimetableMapper;
import com.skhuthon_backend.domain.course.service.TimetableCombination;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FastApiService {

    private static final String RANK_PATH = "/recommend";

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
            List<AiRankingResponseDto> rankings = restClient.post()
                    .uri(RANK_PATH)
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<AiRankingResponseDto>>() {
                    });
            if (rankings == null || rankings.isEmpty()) {
                log.warn("AI 랭킹 서버가 빈 결과를 반환함: 요청 조합 수={}", combinations.size());
            }

            return rankings;
        } catch (RestClientException e) {
            log.error("AI 랭킹 서버 호출 실패: path={}, 조합 수={}", RANK_PATH, combinations.size(), e);
            throw new AiRankingException("AI 순위 서버 호출에 실패했습니다.", e);
        }
    }
}
