package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.ai.dto.AiRankingResponseDto;
import com.skhuthon_backend.domain.ai.dto.AiTimetableRequestDto;
import com.skhuthon_backend.domain.ai.model.AiTimetableMapper;
import com.skhuthon_backend.domain.ai.model.TimetableCandidateReducer;
import com.skhuthon_backend.domain.ai.service.FastApiService;
import com.skhuthon_backend.domain.course.dto.CourseCandidateRequestDto;
import com.skhuthon_backend.domain.course.dto.CourseOfferingCandidateResponseDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationResponseDto;
import com.skhuthon_backend.domain.course.dto.TimetableGenerateRequestDto;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.parser.TranscriptParserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TimetableEngineService {

    private final CourseCandidateProvider courseCandidateProvider;
    private final TimetableConstraintFilter timetableConstraintFilter;
    private final TimetableCombinationGenerator timetableCombinationGenerator;
    private final TimetableCombinationMapper timetableCombinationMapper;
    private final TranscriptParserService transcriptParserService;
    private final FastApiService fastApiService;
    private final AiTimetableMapper aiTimetableMapper;
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

        List<AiRankingResponseDto> rankings = fastApiService.rank(reducedCombinations);

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
    public List<TimetableCombinationResponseDto> generateTimetable(
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
        return generateCombinations(combinationRequest);
    }

    @Transactional(readOnly = true)
    public AiTimetableRequestDto generateAiRequest(
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

        List<CourseOffering> filteredOfferings =
                timetableConstraintFilter.apply(
                        context.offerings(),
                        context.timesByOfferingId(),
                        combinationRequest
                );

        List<TimetableCombination> combinations =
                timetableCombinationGenerator.generate(
                        filteredOfferings,
                        context.timesByOfferingId(),
                        combinationRequest
                );
        List<TimetableCombination> reducedCombinations =
                timetableCandidateReducer.reduce(combinations);
        return aiTimetableMapper.toRequest(
                reducedCombinations
        );
    }
}
