package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.ai.dto.AiRankingResponseDto;
import com.skhuthon_backend.domain.ai.model.TimetableCandidateReducer;
import com.skhuthon_backend.domain.ai.service.FastApiService;
import com.skhuthon_backend.domain.course.dto.CourseCandidateRequestDto;
import com.skhuthon_backend.domain.course.dto.CourseOfferingCandidateResponseDto;
import com.skhuthon_backend.domain.course.dto.FirstYearTimetableRequestDto;
import com.skhuthon_backend.domain.course.dto.GeneralRequiredCourseSelectionDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationResponseDto;
import com.skhuthon_backend.domain.course.dto.TimetableGenerateRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableRecommendationResponseDto;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import com.skhuthon_backend.parser.ParsedTranscript;
import com.skhuthon_backend.parser.TranscriptParserService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableEngineService {

    private static final int FIRST_YEAR = 1;
    private static final Set<String> REQUIRED_GENERAL_COURSE_NAMES = Set.of(
            "인권과평화",
            "말과글",
            "과학기술과에콜로지",
            "디지털리터러시",
            "대학생활세미나"
    );
    private static final Map<String, String> REQUIRED_GENERAL_COURSE_NAME_ALIASES = Map.of(
            "데이터리터러시", "디지털리터러시"
    );

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
    public List<CourseOfferingCandidateResponseDto> findGeneralRequiredOfferings() {
        CandidateContext candidateContext = courseCandidateProvider.findGeneralRequiredOfferings();

        return timetableCombinationMapper.toCourseOfferingResponses(candidateContext);
    }

    @Transactional(readOnly = true)
    public List<TimetableCombinationResponseDto> generateCombinations(TimetableCombinationRequestDto request) {
        return generateTimetableCombinations(request, Collections.emptySet());
    }

    @Transactional(readOnly = true)
    public List<TimetableCombinationResponseDto> generateFirstYearFirstSemester(
            FirstYearTimetableRequestDto request
    ) {
        TimetableCombinationRequestDto combinationRequest = toFirstYearCombinationRequest(request, Collections.emptyList());

        return generateTimetableCombinations(combinationRequest, Collections.emptySet());
    }

    @Transactional(readOnly = true)
    public List<TimetableRecommendationResponseDto> generateFirstYearSecondSemester(
            FirstYearTimetableRequestDto request,
            MultipartFile transcript
    ) {
        ParsedTranscript parsedTranscript = transcriptParserService.parse(transcript);
        TimetableCombinationRequestDto combinationRequest =
                toFirstYearCombinationRequest(request, parsedTranscript.courseCodes().stream().toList());

        List<TimetableCombination> reducedCombinations =
                generateReducedCombinations(combinationRequest, parsedTranscript.courseNames());
        List<AiRankingResponseDto> rankings = fastApiService.rank(reducedCombinations);

        return timetableCombinationMapper.toRecommendationResponses(
                rankings,
                reducedCombinations
        );
    }

    private List<TimetableCombinationResponseDto> generateTimetableCombinations(
            TimetableCombinationRequestDto request,
            Set<String> completedCourseNames
    ) {
        List<TimetableCombination> reducedCombinations =
                generateReducedCombinations(request, completedCourseNames);
        CandidateContext fixedContext = resolveFixedGeneralRequiredContext(request);
        CandidateContext candidateContext = courseCandidateProvider.findCandidates(request, completedCourseNames);
        Map<Long, List<OfferingTime>> timesByOfferingId = mergeTimesByOfferingId(candidateContext, fixedContext);

        return timetableCombinationMapper.toTimetableResponses(
                reducedCombinations,
                timesByOfferingId,
                request
        );
    }

    private List<TimetableCombination> generateReducedCombinations(
            TimetableCombinationRequestDto request,
            Set<String> completedCourseNames
    ) {
        CandidateContext fixedContext = resolveFixedGeneralRequiredContext(request);
        CandidateContext candidateContext = courseCandidateProvider.findCandidates(request, completedCourseNames);
        Map<Long, List<OfferingTime>> timesByOfferingId = mergeTimesByOfferingId(candidateContext, fixedContext);

        if (!areAllowedByConstraints(fixedContext, timesByOfferingId, request)) {
            return Collections.emptyList();
        }

        List<CourseOffering> filteredOfferings = timetableConstraintFilter.apply(
                candidateContext.offerings(),
                timesByOfferingId,
                request
        );
        List<TimetableCombination> combinations = timetableCombinationGenerator.generate(
                filteredOfferings,
                timesByOfferingId,
                request,
                fixedContext.offerings()
        );
        List<TimetableCombination> reducedCombinations =
                timetableCandidateReducer.reduce(combinations);

        return reducedCombinations;
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
        ParsedTranscript parsedTranscript = transcriptParserService.parse(transcript);
        Set<String> completedCourseCodes = parsedTranscript.courseCodes();
        Set<String> completedCourseNames = parsedTranscript.courseNames();

        TimetableCombinationRequestDto combinationRequest =
                new TimetableCombinationRequestDto(
                        request.getStudentMajors(),
                        request.getStudentYear(),
                        request.getTargetMajorCredits(),
                        request.getTargetGeneralCredits(),
                        request.getFreeDays(),
                        request.getExcludeFirstPeriod(),
                        completedCourseCodes.stream().toList(),
                        request.getGeneralRequiredCourses()
                );

        CandidateContext fixedContext = resolveFixedGeneralRequiredContext(combinationRequest);
        CandidateContext context =
                courseCandidateProvider.findCandidates(combinationRequest, completedCourseNames);
        if (context.offerings().isEmpty()) {
            log.warn(
                    "조건에 맞는 후보 강의가 없음: majors={}, completedCourseCodes={}건",
                    request.getStudentMajors(),
                    completedCourseCodes.size()
            );
        }

        Map<Long, List<OfferingTime>> timesByOfferingId = mergeTimesByOfferingId(context, fixedContext);
        if (!areAllowedByConstraints(fixedContext, timesByOfferingId, combinationRequest)) {
            return Collections.emptyList();
        }

        List<CourseOffering> filteredOfferings =
                timetableConstraintFilter.apply(
                        context.offerings(),
                        timesByOfferingId,
                        combinationRequest
                );
        if (filteredOfferings.isEmpty()) {
            log.warn("제약조건 필터링 후 남은 강의가 없음: 후보 강의 수={}건", context.offerings().size());
        }

        List<TimetableCombination> combinations =
                timetableCombinationGenerator.generate(
                        filteredOfferings,
                        timesByOfferingId,
                        combinationRequest,
                        fixedContext.offerings()
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

    private CandidateContext resolveFixedGeneralRequiredContext(TimetableCombinationRequestDto request) {
        validateGeneralRequiredSelections(request);

        return courseCandidateProvider.findSelectedGeneralRequiredOfferings(request.generalRequiredCourses());
    }

    private TimetableCombinationRequestDto toFirstYearCombinationRequest(
            FirstYearTimetableRequestDto request,
            List<String> completedCourseCodes
    ) {
        return new TimetableCombinationRequestDto(
                request.studentMajors(),
                FIRST_YEAR,
                request.targetMajorCredits(),
                request.targetGeneralCredits(),
                request.freeDays(),
                request.excludeFirstPeriod(),
                completedCourseCodes,
                request.generalRequiredCourses()
        );
    }

    private void validateGeneralRequiredSelections(TimetableCombinationRequestDto request) {
        List<GeneralRequiredCourseSelectionDto> selections =
                request.generalRequiredCourses() == null
                        ? Collections.emptyList()
                        : request.generalRequiredCourses();

        if (request.studentYear() == null || request.studentYear() != FIRST_YEAR) {
            return;
        }

        Set<String> selectedCourseNames = selections.stream()
                .map(GeneralRequiredCourseSelectionDto::courseName)
                .map(this::canonicalGeneralRequiredCourseName)
                .collect(Collectors.toSet());

        if (!REQUIRED_GENERAL_COURSE_NAMES.containsAll(selectedCourseNames)) {
            throw new IllegalArgumentException(
                    "교양필수 선택 과목은 인권과평화, 말과글, 데이터리터러시, 대학생활세미나, 과학기술과에콜로지 중 하나여야 합니다."
            );
        }
    }

    private String canonicalGeneralRequiredCourseName(String courseName) {
        String normalizedCourseName = courseName == null
                ? ""
                : courseName.replaceAll("\\s+", "");

        return REQUIRED_GENERAL_COURSE_NAME_ALIASES.getOrDefault(normalizedCourseName, normalizedCourseName);
    }

    private boolean areAllowedByConstraints(
            CandidateContext fixedContext,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto request
    ) {
        if (fixedContext.offerings().isEmpty()) {
            return true;
        }

        List<CourseOffering> allowedFixedOfferings = timetableConstraintFilter.apply(
                fixedContext.offerings(),
                timesByOfferingId,
                request
        );

        return allowedFixedOfferings.size() == fixedContext.offerings().size();
    }

    private Map<Long, List<OfferingTime>> mergeTimesByOfferingId(
            CandidateContext candidateContext,
            CandidateContext fixedContext
    ) {
        Map<Long, List<OfferingTime>> merged = new LinkedHashMap<>();
        merged.putAll(candidateContext.timesByOfferingId());
        merged.putAll(fixedContext.timesByOfferingId());

        return merged;
    }
}
