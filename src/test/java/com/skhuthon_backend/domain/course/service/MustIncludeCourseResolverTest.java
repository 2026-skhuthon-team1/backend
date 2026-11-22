package com.skhuthon_backend.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.entity.Course;
import com.skhuthon_backend.domain.course.entity.CourseCategory;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.domain.course.entity.DayOfWeek;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import com.skhuthon_backend.domain.course.exception.MustIncludeCourseException;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MustIncludeCourseResolverTest {

    private final TimeConflictChecker timeConflictChecker = new TimeConflictChecker();
    private final TimetableCombinationGenerator generator =
            new TimetableCombinationGenerator(timeConflictChecker, new TimetableFeatureExtractor());
    private final MustIncludeCourseResolver resolver =
            new MustIncludeCourseResolver(new TimetableConstraintFilter(), timeConflictChecker, generator);

    @Test
    void 같은_과목의_분반은_하나의_묶음이_된다() {
        CourseOffering section1 = offering(1L, "SW00001", "자료구조", "01", 3, CourseCategory.MAJOR_REQUIRED);
        CourseOffering section2 = offering(2L, "SW00001", "자료구조", "02", 3, CourseCategory.MAJOR_REQUIRED);
        CourseOffering other = offering(3L, "SW00002", "운영체제", "01", 3, CourseCategory.MAJOR_ELECTIVE);
        Map<Long, List<OfferingTime>> times = Map.of(
                1L, List.of(time(DayOfWeek.MON, 10)),
                2L, List.of(time(DayOfWeek.TUE, 10)),
                3L, List.of(time(DayOfWeek.WED, 10))
        );

        List<List<CourseOffering>> groups =
                resolver.resolve(List.of(section1, section2, other), times, request(6, 0, false, List.of()), List.of());

        assertThat(groups).containsExactly(List.of(section1, section2), List.of(other));
    }

    @Test
    void 분반이_모두_1교시면_이유를_알려준다() {
        CourseOffering firstPeriod = offering(1L, "SW00001", "자료구조", "01", 3, CourseCategory.MAJOR_REQUIRED);
        Map<Long, List<OfferingTime>> times = Map.of(1L, List.of(time(DayOfWeek.MON, 9)));

        assertThatThrownBy(() -> resolver.resolve(List.of(firstPeriod), times, request(3, 0, true, List.of()), List.of()))
                .isInstanceOf(MustIncludeCourseException.class)
                .hasMessageContaining("자료구조")
                .hasMessageContaining("1교시");
    }

    @Test
    void 일부_분반만_1교시면_나머지_분반을_쓴다() {
        CourseOffering firstPeriod = offering(1L, "SW00001", "자료구조", "01", 3, CourseCategory.MAJOR_REQUIRED);
        CourseOffering later = offering(2L, "SW00001", "자료구조", "02", 3, CourseCategory.MAJOR_REQUIRED);
        Map<Long, List<OfferingTime>> times = Map.of(
                1L, List.of(time(DayOfWeek.MON, 9)),
                2L, List.of(time(DayOfWeek.MON, 13))
        );

        List<List<CourseOffering>> groups =
                resolver.resolve(List.of(firstPeriod, later), times, request(3, 0, true, List.of()), List.of());

        assertThat(groups).containsExactly(List.of(later));
    }

    @Test
    void 공강_희망_요일이_아닌_분반이_있으면_그_분반만_쓴다() {
        CourseOffering onFriday = offering(1L, "SW00001", "자료구조", "01", 3, CourseCategory.MAJOR_REQUIRED);
        CourseOffering onMonday = offering(2L, "SW00001", "자료구조", "02", 3, CourseCategory.MAJOR_REQUIRED);
        Map<Long, List<OfferingTime>> times = Map.of(
                1L, List.of(time(DayOfWeek.FRI, 10)),
                2L, List.of(time(DayOfWeek.MON, 10))
        );

        List<List<CourseOffering>> groups =
                resolver.resolve(List.of(onFriday, onMonday), times, request(3, 0, false, List.of("금")), List.of());

        assertThat(groups).containsExactly(List.of(onMonday));
    }

    @Test
    void 어떤_분반을_골라도_겹치는_과목이_있으면_이유를_알려준다() {
        CourseOffering first = offering(1L, "SW00001", "자료구조", "01", 3, CourseCategory.MAJOR_REQUIRED);
        CourseOffering second = offering(2L, "SW00002", "운영체제", "01", 3, CourseCategory.MAJOR_ELECTIVE);
        Map<Long, List<OfferingTime>> times = Map.of(
                1L, List.of(time(DayOfWeek.MON, 10)),
                2L, List.of(time(DayOfWeek.MON, 10))
        );

        assertThatThrownBy(() -> resolver.resolve(List.of(first, second), times, request(6, 0, false, List.of()), List.of()))
                .isInstanceOf(MustIncludeCourseException.class)
                .hasMessageContaining("자료구조(01분반)")
                .hasMessageContaining("운영체제(01분반)");
    }

    @Test
    void 안_겹치는_분반이_하나라도_있으면_통과한다() {
        CourseOffering first = offering(1L, "SW00001", "자료구조", "01", 3, CourseCategory.MAJOR_REQUIRED);
        CourseOffering secondA = offering(2L, "SW00002", "운영체제", "01", 3, CourseCategory.MAJOR_ELECTIVE);
        CourseOffering secondB = offering(3L, "SW00002", "운영체제", "02", 3, CourseCategory.MAJOR_ELECTIVE);
        Map<Long, List<OfferingTime>> times = Map.of(
                1L, List.of(time(DayOfWeek.MON, 10)),
                2L, List.of(time(DayOfWeek.MON, 10)),
                3L, List.of(time(DayOfWeek.TUE, 10))
        );

        List<List<CourseOffering>> groups =
                resolver.resolve(List.of(first, secondA, secondB), times, request(6, 0, false, List.of()), List.of());

        assertThat(groups).hasSize(2);
    }

    @Test
    void 고른_교양필수와_반드시_겹쳐도_이유를_알려준다() {
        CourseOffering generalRequired = offering(1L, "AC00007", "인권과평화", "01", 2, CourseCategory.GENERAL_REQUIRED);
        CourseOffering mustInclude = offering(2L, "SW00001", "자료구조", "01", 3, CourseCategory.MAJOR_REQUIRED);
        Map<Long, List<OfferingTime>> times = Map.of(
                1L, List.of(time(DayOfWeek.FRI, 10)),
                2L, List.of(time(DayOfWeek.FRI, 10))
        );

        assertThatThrownBy(() -> resolver.resolve(
                List.of(mustInclude), times, request(3, 0, false, List.of()), List.of(generalRequired)))
                .isInstanceOf(MustIncludeCourseException.class)
                .hasMessageContaining("인권과평화");
    }

    @Test
    void 담은_과목만으로_전공_목표_학점을_넘으면_이유를_알려준다() {
        CourseOffering first = offering(1L, "SW00001", "자료구조", "01", 3, CourseCategory.MAJOR_REQUIRED);
        CourseOffering second = offering(2L, "SW00002", "운영체제", "01", 3, CourseCategory.MAJOR_ELECTIVE);
        Map<Long, List<OfferingTime>> times = Map.of(
                1L, List.of(time(DayOfWeek.MON, 10)),
                2L, List.of(time(DayOfWeek.TUE, 10))
        );

        assertThatThrownBy(() -> resolver.resolve(List.of(first, second), times, request(3, 0, false, List.of()), List.of()))
                .isInstanceOf(MustIncludeCourseException.class)
                .hasMessageContaining("전공 6학점");
    }

    @Test
    void 담은_교양만으로_교양_목표_학점을_넘으면_이유를_알려준다() {
        CourseOffering general = offering(1L, "GE00001", "교양A", "01", 3, CourseCategory.GENERAL);
        Map<Long, List<OfferingTime>> times = Map.of(1L, List.of(time(DayOfWeek.MON, 10)));

        assertThatThrownBy(() -> resolver.resolve(List.of(general), times, request(0, 2, false, List.of()), List.of()))
                .isInstanceOf(MustIncludeCourseException.class)
                .hasMessageContaining("교양 3학점");
    }

    @Test
    void 꼭_넣을_과목으로_담은_분반_중_하나가_모든_시간표에_들어간다() {
        CourseOffering sectionA = offering(1L, "SW00001", "자료구조", "01", 3, CourseCategory.MAJOR_REQUIRED);
        CourseOffering sectionB = offering(2L, "SW00001", "자료구조", "02", 3, CourseCategory.MAJOR_REQUIRED);
        CourseOffering electiveA = offering(3L, "SW00002", "운영체제", "01", 3, CourseCategory.MAJOR_ELECTIVE);
        CourseOffering electiveB = offering(4L, "SW00003", "네트워크", "01", 3, CourseCategory.MAJOR_ELECTIVE);
        Map<Long, List<OfferingTime>> times = Map.of(
                1L, List.of(time(DayOfWeek.MON, 10)),
                2L, List.of(time(DayOfWeek.TUE, 10)),
                3L, List.of(time(DayOfWeek.WED, 10)),
                4L, List.of(time(DayOfWeek.THU, 10))
        );
        TimetableCombinationRequestDto request = request(6, 0, false, List.of());

        List<List<CourseOffering>> groups = resolver.resolve(List.of(sectionA, sectionB), times, request, List.of());
        List<TimetableCombination> combinations = groups.get(0).stream()
                .flatMap(section -> generator.generate(List.of(electiveA, electiveB), times, request, List.of(section)).stream())
                .toList();

        assertThat(combinations).isNotEmpty();
        assertThat(combinations).allSatisfy(combination -> {
            List<String> codes = combination.offerings().stream()
                    .map(offering -> offering.getCourse().getCourseCode())
                    .collect(Collectors.toList());
            assertThat(codes).containsOnlyOnce("SW00001");
        });
    }

    private TimetableCombinationRequestDto request(
            int targetMajorCredits,
            int targetGeneralCredits,
            boolean excludeFirstPeriod,
            List<String> freeDays
    ) {
        return new TimetableCombinationRequestDto(
                List.of("소프트웨어공학전공"),
                2,
                targetMajorCredits,
                targetGeneralCredits,
                freeDays,
                excludeFirstPeriod,
                Collections.emptyList(),
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private CourseOffering offering(
            Long id,
            String code,
            String name,
            String sectionNo,
            int credits,
            CourseCategory category
    ) {
        Course course = Course.builder().courseCode(code).courseName(name).credits(credits).build();
        return CourseOffering.builder().id(id).course(course).category(category).sectionNo(sectionNo).build();
    }

    private OfferingTime time(DayOfWeek day, int startHour) {
        return OfferingTime.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(startHour, 0))
                .endTime(LocalTime.of(startHour + 1, 50))
                .build();
    }
}
