package com.skhuthon_backend.domain.course.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.entity.Course;
import com.skhuthon_backend.domain.course.entity.CourseCategory;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.domain.course.entity.DayOfWeek;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TimetableCombinationGeneratorTest {

    private final TimetableCombinationGenerator generator =
            new TimetableCombinationGenerator(new TimeConflictChecker(), new TimetableFeatureExtractor());

    private final CourseOffering socialService =
            offering(1L, "AC00003", "사회봉사Ⅰ", 2, CourseCategory.GENERAL_REQUIRED);
    private final CourseOffering threeCreditGeneral =
            offering(2L, "GE00001", "교양A", 3, CourseCategory.GENERAL);
    private final CourseOffering twoCreditGeneral =
            offering(3L, "GE00002", "교양B", 2, CourseCategory.GENERAL);

    private final Map<Long, List<OfferingTime>> timesByOfferingId = Map.of(
            1L, List.of(time(DayOfWeek.TUE, 10)),
            2L, List.of(time(DayOfWeek.WED, 10)),
            3L, List.of(time(DayOfWeek.THU, 10))
    );

    @Test
    void 교양_학점이_0이어도_사회봉사는_고정으로_포함된다() {
        List<TimetableCombination> combinations = generator.generate(
                List.of(threeCreditGeneral, twoCreditGeneral),
                timesByOfferingId,
                request(0),
                List.of(socialService)
        );

        assertThat(combinations).hasSize(1);
        assertThat(combinations.get(0).offerings()).containsExactly(socialService);
    }

    @Test
    void 교양_학점은_사회봉사를_제외한_교양_과목으로만_채운다() {
        List<TimetableCombination> combinations = generator.generate(
                List.of(threeCreditGeneral, twoCreditGeneral),
                timesByOfferingId,
                request(3),
                List.of(socialService)
        );

        assertThat(combinations).hasSize(1);
        assertThat(combinations.get(0).offerings()).containsExactlyInAnyOrder(socialService, threeCreditGeneral);
    }

    private TimetableCombinationRequestDto request(int targetGeneralCredits) {
        return new TimetableCombinationRequestDto(
                List.of("소프트웨어공학전공"),
                2,
                0,
                targetGeneralCredits,
                Collections.emptyList(),
                false,
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    private CourseOffering offering(Long id, String code, String name, int credits, CourseCategory category) {
        Course course = Course.builder().courseCode(code).courseName(name).credits(credits).build();
        return CourseOffering.builder().id(id).course(course).category(category).sectionNo("01").build();
    }

    private OfferingTime time(DayOfWeek day, int startHour) {
        return OfferingTime.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(startHour, 0))
                .endTime(LocalTime.of(startHour + 1, 50))
                .build();
    }
}
