package com.skhuthon_backend.domain.ai.model;

import com.skhuthon_backend.domain.ai.dto.AiRankingResponseDto;
import com.skhuthon_backend.domain.course.dto.TimetableFeature;
import com.skhuthon_backend.domain.course.entity.DayOfWeek;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import com.skhuthon_backend.domain.course.service.TimetableCombination;
import com.skhuthon_backend.domain.course.service.TimetableFeatureExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * AI 랭킹 서버가 실패하거나 빈 결과를 줄 때 쓰는 대체 랭킹.
 * AI 응답과 같은 모양(timetableId는 1부터, score 0~100, rank, tags)으로 만들어 매퍼가 구분 없이 처리하게 한다.
 */
@Component
@RequiredArgsConstructor
public class FallbackTimetableRanker {

    private static final List<DayOfWeek> WEEKDAYS =
            List.of(DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED, DayOfWeek.THU, DayOfWeek.FRI);

    private static final int MAX_SCORE = 100;
    private static final int FREE_ATTENDANCE_DAYS = 2;
    private static final int ATTENDANCE_DAY_PENALTY = 8;
    private static final int FIRST_PERIOD_PENALTY = 10;
    private static final int LONG_BREAK_PENALTY = 10;
    private static final int NO_LUNCH_DAY_PENALTY = 5;

    private final TimetableFeatureExtractor featureExtractor;

    public List<AiRankingResponseDto> rank(List<TimetableCombination> combinations) {
        List<AiRankingResponseDto> scored = IntStream.range(0, combinations.size())
                .mapToObj(index -> score(index + 1L, combinations.get(index)))
                .sorted(Comparator.comparing(AiRankingResponseDto::score).reversed()
                        .thenComparing(AiRankingResponseDto::timetableId))
                .toList();

        return IntStream.range(0, scored.size())
                .mapToObj(index -> AiRankingResponseDto.builder()
                        .timetableId(scored.get(index).timetableId())
                        .score(scored.get(index).score())
                        .rank(index + 1)
                        .tags(scored.get(index).tags())
                        .build())
                .toList();
    }

    // 등교일이 적을수록, 1교시·우주공강·점심 없는 날이 적을수록 높은 점수를 준다
    private AiRankingResponseDto score(Long timetableId, TimetableCombination combination) {
        TimetableFeature feature = featureExtractor.extract(combination);
        int noLunchDays = feature.attendanceDays() - feature.lunchBreakCount();

        int penalty = Math.max(0, feature.attendanceDays() - FREE_ATTENDANCE_DAYS) * ATTENDANCE_DAY_PENALTY
                + feature.firstPeriodCount() * FIRST_PERIOD_PENALTY
                + feature.longBreakCount() * LONG_BREAK_PENALTY
                + noLunchDays * NO_LUNCH_DAY_PENALTY;

        return AiRankingResponseDto.builder()
                .timetableId(timetableId)
                .score(Math.max(0, MAX_SCORE - penalty))
                .tags(toTags(combination, feature, noLunchDays))
                .build();
    }

    // AI 서버 태그와 같은 표기(#월공강, #1교시없음, #점심시간보장, #우주공강없음)를 쓴다
    private List<String> toTags(TimetableCombination combination, TimetableFeature feature, int noLunchDays) {
        Set<DayOfWeek> attendanceDays = combination.times().stream()
                .map(OfferingTime::getDayOfWeek)
                .collect(Collectors.toSet());

        List<String> tags = new ArrayList<>();
        WEEKDAYS.stream()
                .filter(day -> !attendanceDays.contains(day))
                .forEach(day -> tags.add("#" + day.getLabel() + "공강"));
        if (feature.firstPeriodCount() == 0) {
            tags.add("#1교시없음");
        }
        if (noLunchDays == 0) {
            tags.add("#점심시간보장");
        }
        if (feature.longBreakCount() == 0) {
            tags.add("#우주공강없음");
        }

        return tags;
    }
}
