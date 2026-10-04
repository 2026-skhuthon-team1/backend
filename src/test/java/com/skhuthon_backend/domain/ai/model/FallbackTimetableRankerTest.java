package com.skhuthon_backend.domain.ai.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.skhuthon_backend.domain.ai.dto.AiRankingResponseDto;
import com.skhuthon_backend.domain.course.entity.DayOfWeek;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import com.skhuthon_backend.domain.course.service.TimetableCombination;
import com.skhuthon_backend.domain.course.service.TimetableFeatureExtractor;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class FallbackTimetableRankerTest {

    private final FallbackTimetableRanker ranker = new FallbackTimetableRanker(new TimetableFeatureExtractor());

    @Test
    void 등교일이_적고_1교시가_없는_시간표를_먼저_추천한다() {
        TimetableCombination fourDaysWithFirstPeriod = combination(
                time(DayOfWeek.MON, 9), time(DayOfWeek.TUE, 13), time(DayOfWeek.WED, 13), time(DayOfWeek.THU, 13)
        );
        TimetableCombination twoDaysNoFirstPeriod = combination(
                time(DayOfWeek.MON, 13), time(DayOfWeek.WED, 13)
        );

        List<AiRankingResponseDto> rankings = ranker.rank(List.of(fourDaysWithFirstPeriod, twoDaysNoFirstPeriod));

        assertThat(rankings).extracting(AiRankingResponseDto::timetableId).containsExactly(2L, 1L);
        assertThat(rankings).extracting(AiRankingResponseDto::rank).containsExactly(1, 2);
        assertThat(rankings.get(0).score()).isEqualTo(100);
        assertThat(rankings.get(0).tags())
                .containsExactly("#화공강", "#목공강", "#금공강", "#1교시없음", "#점심시간보장", "#우주공강없음");
    }

    private TimetableCombination combination(OfferingTime... times) {
        return new TimetableCombination(List.of(), List.of(times));
    }

    private OfferingTime time(DayOfWeek day, int startHour) {
        return OfferingTime.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(startHour, 0))
                .endTime(LocalTime.of(startHour + 1, 50))
                .build();
    }
}
