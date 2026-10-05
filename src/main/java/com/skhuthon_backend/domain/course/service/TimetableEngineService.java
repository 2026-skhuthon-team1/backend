package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.ai.dto.AiRankingResponseDto;
import com.skhuthon_backend.domain.ai.exception.AiRankingException;
import com.skhuthon_backend.domain.ai.model.FallbackTimetableRanker;
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
import com.skhuthon_backend.domain.course.exception.SemesterNotOpenException;
import com.skhuthon_backend.parser.ParsedTranscript;
import com.skhuthon_backend.parser.TranscriptParserService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimetableEngineService {

    private static final int FIRST_YEAR = 1;
    private static final String SOCIAL_SERVICE_KEYWORD = "사회봉사";
    private static final Set<String> REQUIRED_GENERAL_COURSE_NAMES = Set.of(
            "인권과평화",
            "말과글",
            "과학기술과에콜로지",
            "디지털리터러시",
            "대학생활세미나",
            "전공진로세미나",
            "자유전공팀세미나"
    );
    private static final Map<String, String> REQUIRED_GENERAL_COURSE_NAME_ALIASES = Map.of(
            "데이터리터러시", "디지털리터러시"
    );

    // 개설강좌 데이터가 몇 학기 것인지. 지금 DB는 2학기(seed_2026_2.sql)라 1학년 1학기 학생은 존재하지 않는다.
    @Value("${timetable.current-semester:2}")
    private int currentSemester;

    private final CourseCandidateProvider courseCandidateProvider;
    private final TimetableConstraintFilter timetableConstraintFilter;
    private final TimetableCombinationGenerator timetableCombinationGenerator;
    private final TimetableCombinationMapper timetableCombinationMapper;
    private final TranscriptParserService transcriptParserService;
    private final FastApiService fastApiService;
    private final TimetableCandidateReducer timetableCandidateReducer;
    private final FallbackTimetableRanker fallbackTimetableRanker;

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
        return generateTimetableCombinations(request, Collections.emptySet(), false);
    }

    public int getCurrentSemester() {
        return currentSemester;
    }

    @Transactional(readOnly = true)
    public List<TimetableCombinationResponseDto> generateFirstYearFirstSemester(
            FirstYearTimetableRequestDto request
    ) {
        if (currentSemester != 1) {
            throw new SemesterNotOpenException(
                    "현재는 %d학기라 1학년 1학기 시간표를 만들 수 없습니다. 1학년 2학기를 선택해 주세요.".formatted(currentSemester)
            );
        }

        TimetableCombinationRequestDto combinationRequest = toFirstYearCombinationRequest(request, Collections.emptyList());

        return generateTimetableCombinations(combinationRequest, Collections.emptySet(), request.includeChapel());
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
                generateReducedCombinations(combinationRequest, parsedTranscript.courseNames(), request.includeChapel());
        List<AiRankingResponseDto> rankings = rankOrFallback(reducedCombinations);

        return timetableCombinationMapper.toRecommendationResponses(
                rankings,
                reducedCombinations
        );
    }

    private List<TimetableCombinationResponseDto> generateTimetableCombinations(
            TimetableCombinationRequestDto request,
            Set<String> completedCourseNames,
            Boolean includeChapel
    ) {
        List<TimetableCombination> reducedCombinations =
                generateReducedCombinations(request, completedCourseNames, includeChapel);
        CandidateContext fixedContext = resolveFixedGeneralRequiredContext(request);
        CandidateContext candidateContext = courseCandidateProvider.findCandidates(request, completedCourseNames);
        CandidateContext chapelContext =
                resolveChapelContext(includeChapel, request.completedCourseCodes(), completedCourseNames);
        Map<Long, List<OfferingTime>> timesByOfferingId = mergeTimesByOfferingId(candidateContext, fixedContext);
        timesByOfferingId.putAll(chapelContext.timesByOfferingId());

        return timetableCombinationMapper.toTimetableResponses(
                reducedCombinations,
                timesByOfferingId,
                request
        );
    }

    private List<TimetableCombination> generateReducedCombinations(
            TimetableCombinationRequestDto request,
            Set<String> completedCourseNames,
            Boolean includeChapel
    ) {
        CandidateContext fixedContext = resolveFixedGeneralRequiredContext(request);
        CandidateContext candidateContext = courseCandidateProvider.findCandidates(request, completedCourseNames);
        CandidateContext chapelContext =
                resolveChapelContext(includeChapel, request.completedCourseCodes(), completedCourseNames);
        Map<Long, List<OfferingTime>> timesByOfferingId = mergeTimesByOfferingId(candidateContext, fixedContext);
        timesByOfferingId.putAll(chapelContext.timesByOfferingId());

        List<CourseOffering> filteredOfferings = timetableConstraintFilter.apply(
                excludeOptionalFixedOfferings(candidateContext.offerings()),
                timesByOfferingId,
                request
        );
        List<TimetableCombination> combinations = generateWithOptionalFixedOfferings(
                filteredOfferings,
                timesByOfferingId,
                request,
                fixedContext.offerings(),
                List.of(selectEligibleChapelOfferings(chapelContext, timesByOfferingId, request))
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
                        request.getFixedCourses()
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

        CandidateContext socialServiceContext =
                resolveSocialServiceContext(request, completedCourseCodes, completedCourseNames);
        CandidateContext chapelContext =
                resolveChapelContext(request.getIncludeChapel(), completedCourseCodes.stream().toList(), completedCourseNames);
        Map<Long, List<OfferingTime>> timesByOfferingId = mergeTimesByOfferingId(context, fixedContext);
        timesByOfferingId.putAll(socialServiceContext.timesByOfferingId());
        timesByOfferingId.putAll(chapelContext.timesByOfferingId());

        List<CourseOffering> filteredOfferings =
                timetableConstraintFilter.apply(
                        excludeOptionalFixedOfferings(context.offerings()),
                        timesByOfferingId,
                        combinationRequest
                );
        if (filteredOfferings.isEmpty()) {
            log.warn("제약조건 필터링 후 남은 강의가 없음: 후보 강의 수={}건", context.offerings().size());
        }

        List<CourseOffering> eligibleSocialServiceOfferings =
                timetableConstraintFilter.apply(socialServiceContext.offerings(), timesByOfferingId, combinationRequest);
        if (Boolean.TRUE.equals(request.getIncludeSocialService()) && eligibleSocialServiceOfferings.isEmpty()) {
            log.warn(
                    "사회봉사 포함이 요청되었지만 제약조건을 만족하는 사회봉사 강의가 없어 제외됨: studentYear={}",
                    request.getStudentYear()
            );
        }

        List<CourseOffering> eligibleChapelOfferings =
                selectEligibleChapelOfferings(chapelContext, timesByOfferingId, combinationRequest);
        if (Boolean.TRUE.equals(request.getIncludeChapel())) {
            if (chapelContext.offerings().isEmpty()) {
                log.warn("채플 포함이 요청되었지만 남은 채플 분반이 없어 제외됨 (모두 이수했거나 개설 없음)");
            } else if (eligibleChapelOfferings.isEmpty()) {
                log.warn("채플 포함이 요청되었지만 남은 채플 분반이 모두 1교시라 제외됨: 분반 수={}", chapelContext.offerings().size());
            }
        }

        List<TimetableCombination> combinations =
                generateWithOptionalFixedOfferings(
                        filteredOfferings,
                        timesByOfferingId,
                        combinationRequest,
                        fixedContext.offerings(),
                        List.of(eligibleSocialServiceOfferings, eligibleChapelOfferings)
                );
        if (combinations.isEmpty()) {
            log.warn("생성된 시간표 조합이 없음: 필터링된 강의 수={}건", filteredOfferings.size());
        }

        List<TimetableCombination> reducedCombinations =
                timetableCandidateReducer.reduce(combinations);

        List<AiRankingResponseDto> rankings = rankOrFallback(reducedCombinations);

        return timetableCombinationMapper.toRecommendationResponses(
                rankings,
                reducedCombinations
        );
    }

    // AI 랭킹 서버가 느리거나 실패해도 시간표는 보여줘야 하므로, 실패·빈 응답이면 백엔드 기준으로 정렬해 대신 쓴다
    private List<AiRankingResponseDto> rankOrFallback(List<TimetableCombination> combinations) {
        try {
            List<AiRankingResponseDto> rankings = fastApiService.rank(combinations);
            if (!combinations.isEmpty() && (rankings == null || rankings.isEmpty())) {
                log.warn("AI 랭킹이 비어 있어 대체 랭킹을 사용함: 조합 수={}", combinations.size());
                return fallbackTimetableRanker.rank(combinations);
            }

            return rankings;
        } catch (AiRankingException e) {
            log.warn("AI 랭킹 실패로 대체 랭킹을 사용함: 조합 수={}, 원인={}", combinations.size(), e.getMessage());
            return fallbackTimetableRanker.rank(combinations);
        }
    }

    private CandidateContext resolveFixedGeneralRequiredContext(TimetableCombinationRequestDto request) {
        validateGeneralRequiredSelections(request);

        return courseCandidateProvider.findSelectedGeneralRequiredOfferings(request.fixedCourses());
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
                request.fixedCourses()
        );
    }

    private void validateGeneralRequiredSelections(TimetableCombinationRequestDto request) {
        List<GeneralRequiredCourseSelectionDto> selections =
                request.fixedCourses() == null
                        ? Collections.emptyList()
                        : request.fixedCourses();

        if (request.studentYear() == null || request.studentYear() != FIRST_YEAR) {
            return;
        }

        Set<String> selectedCourseNames = selections.stream()
                .map(GeneralRequiredCourseSelectionDto::courseName)
                .map(this::canonicalGeneralRequiredCourseName)
                .collect(Collectors.toSet());

        if (!REQUIRED_GENERAL_COURSE_NAMES.containsAll(selectedCourseNames)) {
            throw new IllegalArgumentException(
                    "교양필수 선택 과목은 인권과평화, 말과글, 데이터리터러시, 대학생활세미나, 전공진로세미나, 자유전공팀세미나, 과학기술과에콜로지 중 하나여야 합니다."
            );
        }
    }

    private String canonicalGeneralRequiredCourseName(String courseName) {
        String normalizedCourseName = courseName == null
                ? ""
                : courseName.replaceAll("\\s+", "");

        return REQUIRED_GENERAL_COURSE_NAME_ALIASES.getOrDefault(normalizedCourseName, normalizedCourseName);
    }

    // 사회봉사·채플을 다른 교양과목과 동등하게 "선택 가능한" 후보로 두면, 포함을 골라도 빠지거나
    // 포함하지 않음을 골라도 들어갈 수 있다. 그래서 자유 선택 후보에서는 항상 제외하고, 포함 요청 시 fixedOfferings로 강제 포함시킨다.
    private List<CourseOffering> excludeOptionalFixedOfferings(List<CourseOffering> offerings) {
        return offerings.stream()
                .filter(courseOffering -> !courseOffering.getCourse().getCourseName().contains(SOCIAL_SERVICE_KEYWORD))
                .filter(courseOffering -> !courseCandidateProvider.isChapel(courseOffering))
                .collect(Collectors.toList());
    }

    private CandidateContext resolveChapelContext(
            Boolean includeChapel,
            List<String> completedCourseCodes,
            Set<String> completedCourseNames
    ) {
        if (!Boolean.TRUE.equals(includeChapel)) {
            return new CandidateContext(Collections.emptyList(), Collections.emptyMap());
        }

        return courseCandidateProvider.findChapelOfferings(completedCourseCodes, completedCourseNames);
    }

    // 채플은 분반이 많아, 1교시 제외 조건을 만족하는 분반 중 공강 희망 요일에 없는 분반을 우선 쓴다.
    // 그런 분반이 없을 때만 공강 희망 요일의 분반도 쓴다(고정 과목은 공강 요일 검사를 받지 않으므로 여기서 거른다).
    private List<CourseOffering> selectEligibleChapelOfferings(
            CandidateContext chapelContext,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto request
    ) {
        List<CourseOffering> eligible =
                timetableConstraintFilter.apply(chapelContext.offerings(), timesByOfferingId, request);
        List<String> freeDays = request.freeDays() == null ? Collections.emptyList() : request.freeDays();
        List<CourseOffering> notOnFreeDays = eligible.stream()
                .filter(offering -> timesByOfferingId.getOrDefault(offering.getId(), Collections.emptyList()).stream()
                        .noneMatch(time -> freeDays.contains(time.getDayOfWeek().getLabel())))
                .collect(Collectors.toList());

        return notOnFreeDays.isEmpty() ? eligible : notOnFreeDays;
    }

    // 사회봉사는 1학년 수강이 불가능하고, 그 외 학년은 프론트에서 받은 포함 여부가 true일 때만 강제 포함 대상이 된다.
    // 분반(시간)이 여러 개일 수 있으므로, 호출하는 쪽에서 1교시 제외 등 제약조건을 만족하는 분반만 남긴다.
    private CandidateContext resolveSocialServiceContext(
            TimetableGenerateRequestDto request,
            Set<String> completedCourseCodes,
            Set<String> completedCourseNames
    ) {
        boolean isFirstYear = request.getStudentYear() != null && FIRST_YEAR == request.getStudentYear();
        boolean includeSocialService = Boolean.TRUE.equals(request.getIncludeSocialService());
        if (isFirstYear || !includeSocialService) {
            return new CandidateContext(Collections.emptyList(), Collections.emptyMap());
        }

        return courseCandidateProvider.findSocialServiceOfferings(
                request.getStudentYear(),
                completedCourseCodes.stream().toList(),
                completedCourseNames
        );
    }

    // 사회봉사·채플처럼 "분반 중 하나를 반드시 넣는" 과목은 그룹마다 분반 하나씩 고른 모든 경우를 고정 과목으로 넣어
    // 조합을 생성한 뒤 합친다. 이렇게 해야 어떤 분반을 쓰든 시간표에 그 과목이 반드시 포함된다.
    // 분반이 비어 있는 그룹(포함 안 함, 또는 조건에 맞는 분반 없음)은 건너뛴다.
    private List<TimetableCombination> generateWithOptionalFixedOfferings(
            List<CourseOffering> filteredOfferings,
            Map<Long, List<OfferingTime>> timesByOfferingId,
            TimetableCombinationRequestDto combinationRequest,
            List<CourseOffering> fixedOfferings,
            List<List<CourseOffering>> optionalFixedGroups
    ) {
        List<List<CourseOffering>> fixedOfferingSets = List.of(fixedOfferings);
        for (List<CourseOffering> group : optionalFixedGroups) {
            if (group.isEmpty()) {
                continue;
            }
            List<List<CourseOffering>> expanded = new ArrayList<>();
            for (List<CourseOffering> fixedSet : fixedOfferingSets) {
                for (CourseOffering offering : group) {
                    List<CourseOffering> withOffering = new ArrayList<>(fixedSet);
                    withOffering.add(offering);
                    expanded.add(withOffering);
                }
            }
            fixedOfferingSets = expanded;
        }

        List<TimetableCombination> combinations = new ArrayList<>();
        for (List<CourseOffering> fixedSet : fixedOfferingSets) {
            combinations.addAll(timetableCombinationGenerator.generate(
                    filteredOfferings,
                    timesByOfferingId,
                    combinationRequest,
                    fixedSet
            ));
        }

        return combinations;
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
