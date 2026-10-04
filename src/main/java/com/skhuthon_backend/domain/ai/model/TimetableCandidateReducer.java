package com.skhuthon_backend.domain.ai.model;

import com.skhuthon_backend.domain.course.entity.CourseCategory;
import com.skhuthon_backend.domain.course.service.TimetableCombination;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * AI 랭킹 서버에 보낼 후보를 고른다. AI는 받은 후보 중 상위 3개만 돌려주므로,
 * 후보끼리 과목이 많이 겹치면 추천 3개가 같은 과목 몇 개를 돌려 쓴 시간표가 된다.
 * 그래서 좋은 시간표부터 보되, 이미 고른 후보와 과목이 절반 이상 겹치는 조합은 건너뛴다.
 * 다만 1위보다 점수가 크게 낮은 조합은 과목이 달라도 뽑지 않고, 부족한 자리는 겹치더라도 점수 높은 조합으로 채운다.
 */
@Component
@RequiredArgsConstructor
public class TimetableCandidateReducer {

    private static final int MAX_AI_CANDIDATES = 25;
    // 화면에 보여주는 추천 개수 — 겹침 조건으로 이만큼 못 채우면 겹치더라도 점수 순으로 채운다
    private static final int MIN_RECOMMENDATIONS = 3;
    // 두 후보가 공유하는 과목 수가 작은 쪽 과목 수의 이 비율 이상이면 너무 비슷하다고 본다 (2과목이면 1과목만 겹쳐도 제외)
    private static final double MAX_SHARED_COURSE_RATIO = 0.5;
    // 1위보다 이만큼 넘게 점수가 낮은 조합은 과목이 다르다는 이유만으로 뽑지 않는다(다양성보다 품질 우선)
    private static final int MAX_SCORE_GAP_FROM_TOP = 20;

    private final FallbackTimetableRanker scorer;

    public List<TimetableCombination> reduce(
            List<TimetableCombination> combinations
    ) {
        List<TimetableCombination> byScore = pickDistinctCourseSets(combinations).stream()
                .sorted(Comparator.comparingInt(scorer::scoreOf).reversed())
                .toList();

        int minScore = byScore.isEmpty() ? 0 : scorer.scoreOf(byScore.get(0)) - MAX_SCORE_GAP_FROM_TOP;

        List<TimetableCombination> selected = new ArrayList<>();
        List<Set<String>> selectedCourseSets = new ArrayList<>();
        for (TimetableCombination candidate : byScore) {
            if (selected.size() >= MAX_AI_CANDIDATES || scorer.scoreOf(candidate) < minScore) {
                break;
            }
            Set<String> courseSet = selectedCourseCodes(candidate);
            if (selectedCourseSets.stream().noneMatch(picked -> tooSimilar(picked, courseSet))) {
                selected.add(candidate);
                selectedCourseSets.add(courseSet);
            }
        }

        for (TimetableCombination candidate : byScore) {
            if (selected.size() >= MIN_RECOMMENDATIONS) {
                break;
            }
            if (!selected.contains(candidate)) {
                selected.add(candidate);
            }
        }

        if (selected.size() < MIN_RECOMMENDATIONS) {
            return combinations.subList(0, Math.min(combinations.size(), MAX_AI_CANDIDATES));
        }

        return selected;
    }

    // 고정 과목(교양필수·사회봉사)은 분반마다 따로 조합을 만들어, 같은 전공·교양 구성이 고정 과목 분반만 바꿔
    // 여러 번 생긴다. 직접 고르는 과목 구성마다 점수가 가장 높은 하나만 남긴다.
    private List<TimetableCombination> pickDistinctCourseSets(
            List<TimetableCombination> combinations
    ) {
        Map<Set<String>, TimetableCombination> bestByCourseSet = new LinkedHashMap<>();

        for (TimetableCombination combination : combinations) {
            bestByCourseSet.merge(
                    selectedCourseCodes(combination),
                    combination,
                    (current, candidate) -> scorer.scoreOf(candidate) > scorer.scoreOf(current) ? candidate : current
            );
        }

        return new ArrayList<>(bestByCourseSet.values());
    }

    private boolean tooSimilar(Set<String> a, Set<String> b) {
        int smaller = Math.min(a.size(), b.size());
        if (smaller == 0) {
            return a.equals(b);
        }

        Set<String> shared = new HashSet<>(a);
        shared.retainAll(b);

        return shared.size() >= smaller * MAX_SHARED_COURSE_RATIO;
    }

    // 분반(시간)이 아닌 과목 단위로 비교해야 "같은 강의에 시간만 다른" 조합도 하나로 묶인다
    private Set<String> selectedCourseCodes(TimetableCombination combination) {
        return combination.offerings().stream()
                .filter(offering -> offering.getCategory() != CourseCategory.GENERAL_REQUIRED)
                .map(offering -> offering.getCourse().getCourseCode())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
