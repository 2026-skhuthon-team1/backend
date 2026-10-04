package com.skhuthon_backend.domain.ai.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.skhuthon_backend.domain.course.entity.Course;
import com.skhuthon_backend.domain.course.entity.CourseCategory;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.domain.course.entity.DayOfWeek;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import com.skhuthon_backend.domain.course.service.TimetableCombination;
import com.skhuthon_backend.domain.course.service.TimetableFeatureExtractor;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TimetableCandidateReducerTest {

    private final TimetableCandidateReducer reducer =
            new TimetableCandidateReducer(new FallbackTimetableRanker(new TimetableFeatureExtractor()));

    private final CourseOffering socialService = offering("AC00003", CourseCategory.GENERAL_REQUIRED);

    @Test
    void 사회봉사_분반만_다른_조합은_과목_구성마다_하나만_남긴다() {
        TimetableCombination aWithTue = combination(major("MJ00001"), DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED);
        TimetableCombination aWithMon = combination(major("MJ00001"), DayOfWeek.MON, DayOfWeek.MON, DayOfWeek.WED);
        TimetableCombination b = combination(major("MJ00002"), DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED);
        TimetableCombination c = combination(major("MJ00003"), DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED);

        List<TimetableCombination> reduced = reducer.reduce(List.of(aWithTue, aWithMon, b, c));

        // 같은 구성(MJ00001)은 등교일이 더 적은 쪽(월·수)만 남는다
        assertThat(reduced).containsExactly(aWithMon, b, c);
    }

    @Test
    void 과목_구성이_추천_개수보다_적으면_모두_남긴다() {
        TimetableCombination aWithTue = combination(major("MJ00001"), DayOfWeek.MON, DayOfWeek.TUE, DayOfWeek.WED);
        TimetableCombination aWithThu = combination(major("MJ00001"), DayOfWeek.MON, DayOfWeek.THU, DayOfWeek.WED);

        assertThat(reducer.reduce(List.of(aWithTue, aWithThu))).containsExactly(aWithTue, aWithThu);
    }

    @Test
    void 이미_고른_후보와_과목이_절반_이상_겹치는_조합은_건너뛴다() {
        TimetableCombination ab = twoMajors("MJ0000A", "MJ0000B");
        TimetableCombination ac = twoMajors("MJ0000A", "MJ0000C");
        TimetableCombination de = twoMajors("MJ0000D", "MJ0000E");
        TimetableCombination cf = twoMajors("MJ0000C", "MJ0000F");

        assertThat(reducer.reduce(List.of(ab, ac, de, cf))).containsExactly(ab, de, cf);
    }

    @Test
    void 겹치지_않는_후보가_추천_개수보다_적으면_점수_순으로_채운다() {
        TimetableCombination ab = twoMajors("MJ0000A", "MJ0000B");
        TimetableCombination ac = twoMajors("MJ0000A", "MJ0000C");
        TimetableCombination bc = twoMajors("MJ0000B", "MJ0000C");

        assertThat(reducer.reduce(List.of(ab, ac, bc))).containsExactly(ab, ac, bc);
    }

    @Test
    void 일위보다_점수가_크게_낮은_조합은_과목이_달라도_뽑지_않고_점수_높은_조합으로_채운다() {
        TimetableCombination ab = twoMajors("MJ0000A", "MJ0000B");
        TimetableCombination ac = twoMajors("MJ0000A", "MJ0000C");
        TimetableCombination de = twoMajors("MJ0000D", "MJ0000E");
        // 월~금 매일 1교시 — 1위(100점)보다 20점 넘게 낮다
        TimetableCombination lowScoreFg = new TimetableCombination(
                List.of(major("MJ0000F"), major("MJ0000G")),
                List.of(time(DayOfWeek.MON, 9), time(DayOfWeek.TUE, 9), time(DayOfWeek.WED, 9),
                        time(DayOfWeek.THU, 9), time(DayOfWeek.FRI, 9))
        );

        assertThat(reducer.reduce(List.of(ab, ac, de, lowScoreFg))).containsExactly(ab, de, ac);
    }

    private TimetableCombination twoMajors(String code1, String code2) {
        return new TimetableCombination(
                List.of(major(code1), major(code2)),
                List.of(time(DayOfWeek.MON, 13), time(DayOfWeek.WED, 13))
        );
    }

    // 전공 하나(majorDay1, majorDay2)와 사회봉사(socialServiceDay)로 이뤄진 조합
    private TimetableCombination combination(
            CourseOffering majorOffering,
            DayOfWeek majorDay1,
            DayOfWeek socialServiceDay,
            DayOfWeek majorDay2
    ) {
        return new TimetableCombination(
                List.of(socialService, majorOffering),
                List.of(time(socialServiceDay, 10), time(majorDay1, 13), time(majorDay2, 13))
        );
    }

    private CourseOffering major(String code) {
        return offering(code, CourseCategory.MAJOR_ELECTIVE);
    }

    private CourseOffering offering(String code, CourseCategory category) {
        Course course = Course.builder().courseCode(code).courseName(code).credits(3).build();
        return CourseOffering.builder().course(course).category(category).sectionNo("01").build();
    }

    private OfferingTime time(DayOfWeek day, int startHour) {
        return OfferingTime.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(startHour, 0))
                .endTime(LocalTime.of(startHour + 1, 50))
                .build();
    }
}
