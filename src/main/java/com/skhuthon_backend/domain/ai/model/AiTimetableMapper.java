package com.skhuthon_backend.domain.ai.model;

import com.skhuthon_backend.domain.ai.dto.AiCourseDto;
import com.skhuthon_backend.domain.ai.dto.AiTimetableDto;
import com.skhuthon_backend.domain.ai.dto.AiTimetableFeatureDto;
import com.skhuthon_backend.domain.ai.dto.AiTimetableRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableFeature;
import com.skhuthon_backend.domain.course.service.TimetableCombination;
import com.skhuthon_backend.domain.course.service.TimetableFeatureExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class AiTimetableMapper {

    private final TimetableFeatureExtractor featureExtractor;

    public AiTimetableRequestDto toRequest(
            List<TimetableCombination> combinations
    ) {

        List<AiTimetableDto> candidates =
                IntStream.range(0, combinations.size())
                        .mapToObj(i -> toCandidate(
                                i + 1L,
                                combinations.get(i)
                        ))
                        .toList();

        return AiTimetableRequestDto.builder()
                .candidates(candidates)
                .build();
    }

    private AiTimetableDto toCandidate(
            Long timetableId,
            TimetableCombination combination
    ) {

        TimetableFeature feature =
                featureExtractor.extract(combination);

        return AiTimetableDto.builder()
                .timetableId(timetableId)
                .feature(toFeature(feature))
                .courses(toCourses(combination))
                .build();
    }

    private AiTimetableFeatureDto toFeature(
            TimetableFeature feature
    ) {

        return AiTimetableFeatureDto.builder()
                .attendanceDays(feature.attendanceDays())
                .earliestStartTime(feature.earliestStartTime())
                .latestEndTime(feature.latestEndTime())
                .firstPeriodCount(feature.firstPeriodCount())
                .longestBreakMinutes(feature.longestBreakMinutes())
                .longBreakCount(feature.longBreakCount())
                .lunchBreakCount(feature.lunchBreakCount())
                .earlyFinishDayCount(feature.earlyFinishDayCount())
                .build();
    }

    private List<AiCourseDto> toCourses(
            TimetableCombination combination
    ) {

        return combination.offerings().stream()
                .map(courseOffering -> {

                    List<String> schedules =
                            combination.times().stream()
                                    .filter(time ->
                                            time.getCourseOffering()
                                                    .getId()
                                                    .equals(courseOffering.getId())
                                    )
                                    .map(time ->
                                            time.getDayOfWeek().getLabel()
                                                    + " "
                                                    + time.getStartTime()
                                                    + "~"
                                                    + time.getEndTime()
                                    )
                                    .toList();

                    return AiCourseDto.builder()
                            .courseName(courseOffering.getCourse().getCourseName())
                            .category(courseOffering.getCategory().getLabel())
                            .credits(courseOffering.getCourse().getCredits())
                            .schedules(schedules)
                            .build();
                })
                .toList();
    }
}
