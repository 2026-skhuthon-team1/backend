package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.ai.dto.AiRankingResponseDto;
import com.skhuthon_backend.domain.ai.model.TimetableCandidateReducer;
import com.skhuthon_backend.domain.ai.service.FastApiService;
import com.skhuthon_backend.domain.course.dto.CourseCandidateRequestDto;
import com.skhuthon_backend.domain.course.dto.CourseOfferingCandidateResponseDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationResponseDto;
import com.skhuthon_backend.domain.course.dto.TimetableGenerateRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableRecommendationResponseDto;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.parser.TranscriptParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableEngineService {

    private final CourseCandidateProvider courseCandidateProvider;
    private final TimetableConstraintFilter timetableConstraintFilter;
    private final TimetableCombinationGenerator timetableCombinationGenerator;
    private final TimetableCombinationMapper timetableCombinationMapper;
    private final TranscriptParserService transcriptParserService;
    private final FastApiService fastApiService;
    private final TimetableCandidateReducer timetableCandidateReducer;

    @Transactional(readOnly = true)
    public List<CourseOfferingCandidateResponseDto> findAllOfferings() {
        CandidateContext candidateContext = courseCandidateProvider.findAllOfferings();

        return timetableCombinationMapper.toCourseOfferingResponses(candidateContext);
    }

    @Transactional(readOnly = true)
    public List<CourseOfferingCandidateResponseDto> findSelectableOfferings(List<String> studentMajors) {
        CandidateContext candidateContext = courseCandidateProvider.findSelectableOfferings(studentMajors);

        return timetableCombinationMapper.toCourseOfferingResponses(candidateContext);
    }

    @Transactional(readOnly = true)
    public List<TimetableCombinationResponseDto> generateCombinations(TimetableCombinationRequestDto request) {
        CandidateContext candidateContext = courseCandidateProvider.findCandidates(request);
        List<CourseOffering> filteredOfferings = timetableConstraintFilter.apply(
                candidateContext.offerings(),
                candidateContext.timesByOfferingId(),
                request
        );
        List<TimetableCombination> combinations = timetableCombinationGenerator.generate(
                filteredOfferings,
                candidateContext.timesByOfferingId(),
                request
        );
        List<TimetableCombination> reducedCombinations =
                timetableCandidateReducer.reduce(combinations);

        return timetableCombinationMapper.toTimetableResponses(
                reducedCombinations,
                candidateContext.timesByOfferingId(),
                request
        );
    }

    @Transactional(readOnly = true)
    public List<CourseOfferingCandidateResponseDto> findCandidateOfferings(CourseCandidateRequestDto request) {
        CandidateContext candidateContext = courseCandidateProvider.findCandidates(request);

        return timetableCombinationMapper.toCourseOfferingResponses(candidateContext);
    }

    @Transactional(readOnly = true)
    public List<TimetableRecommendationResponseDto> generateRecommendations(
            TimetableGenerateRequestDto request,
            MultipartFile transcript
    ) {
        Set<String> completedCourseCodes = transcriptParserService.parse(transcript);

        TimetableCombinationRequestDto combinationRequest =
                new TimetableCombinationRequestDto(
                        request.getStudentMajors(),
                        request.getStudentYear(),
                        request.getTargetMajorCredits(),
                        request.getTargetGeneralCredits(),
                        request.getFreeDays(),
                        request.getExcludeFirstPeriod(),
                        completedCourseCodes.stream().toList()
                );

        CandidateContext context =
                courseCandidateProvider.findCandidates(combinationRequest);
        if (context.offerings().isEmpty()) {
            log.warn("조건에 맞는 후보 강의가 없음: majors={}, completedCourseCodes={}건",
                    request.getStudentMajors(), completedCourseCodes.size());
        }

        List<CourseOffering> filteredOfferings =
                timetableConstraintFilter.apply(
                        context.offerings(),
                        context.timesByOfferingId(),
                        combinationRequest
                );
        if (filteredOfferings.isEmpty()) {
            log.warn("제약조건 필터링 후 남은 강의가 없음: 후보 강의 수={}건", context.offerings().size());
        }

        List<TimetableCombination> combinations =
                timetableCombinationGenerator.generate(
                        filteredOfferings,
                        context.timesByOfferingId(),
                        combinationRequest
                );
        if (combinations.isEmpty()) {
            log.warn("생성된 시간표 조합이 없음: 필터링된 강의 수={}건", filteredOfferings.size());
        }

        List<TimetableCombination> reducedCombinations =
                timetableCandidateReducer.reduce(combinations);

        List<AiRankingResponseDto> rankings = fastApiService.rank(reducedCombinations);

        return timetableCombinationMapper.toRecommendationResponses(
                rankings,
                reducedCombinations
        );
    }
}
