package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.dto.CourseCandidateRequestDto;
import com.skhuthon_backend.domain.course.dto.GeneralRequiredCourseSelectionDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.entity.CourseCategory;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import com.skhuthon_backend.domain.course.repository.CourseOfferingRepository;
import com.skhuthon_backend.domain.course.repository.OfferingTimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseCandidateProvider {

    private static final Set<CourseCategory> SELECTABLE_MAJOR_COURSE_TYPES = Set.of(
            CourseCategory.MAJOR_REQUIRED,
            CourseCategory.MAJOR_ELECTIVE
    );

    private static final Set<String> SOFTWARE_CONVERGENCE_TRACKS = Set.of(
            "소프트웨어공학전공",
            "정보통신공학전공",
            "컴퓨터공학전공"
    );
    private static final String SOFTWARE_CONVERGENCE_COMMON_MAJOR = "소프트웨어융합전공";

    private static final Set<String> SUPERSEDED_COURSE_NAMES = Set.of(
            "프론트엔드개발",
            "웹프로그래밍"
    );

    private static final Map<String, List<String>> PREREQUISITE_COURSE_NAMES = Map.ofEntries(
            Map.entry("데이터분석입문", List.of("Python프로그래밍")),
            Map.entry("C++프로그래밍", List.of("C프로그래밍")),
            Map.entry("JSP프로그래밍", List.of("웹개발입문", "Java프로그래밍")),
            Map.entry("백엔드프로그래밍", List.of("웹개발입문", "Java프로그래밍", "데이터베이스")),
            Map.entry("웹디자인", List.of("웹개발입문")),
            Map.entry("자료구조", List.of("Java프로그래밍")),
            Map.entry("고급Java프로그래밍", List.of("Java프로그래밍")),
            Map.entry("백엔드프레임워크", List.of("백엔드프로그래밍")),
            Map.entry("알고리즘", List.of("자료구조")),
            Map.entry("프론트엔드프로그래밍", List.of("웹개발입문", "Javascript프로그래밍")),
            Map.entry("하이브리드앱프로그래밍", List.of("웹개발입문")),
            Map.entry("네트워크프로그래밍", List.of("Java프로그래밍")),
            Map.entry("프론트엔드프레임워크", List.of("프론트엔드프로그래밍")),
            Map.entry("Node.js프로그래밍", List.of("Javascript프로그래밍")),
            Map.entry("머신러닝", List.of("Python프로그래밍")),
            Map.entry("코딩테스트지도", List.of("알고리즘"))
    );

    private static final Map<String, String> GENERAL_REQUIRED_COURSE_NAME_ALIASES = Map.of(
            "데이터리터러시", "디지털리터러시"
    );

    private static final Map<String, String> MAJOR_EXPLORATION_GROUP_ALIASES = Map.ofEntries(
            Map.entry("인문융합콘텐츠학부", "인문융합콘텐츠학부(인문융합자율학부)"),
            Map.entry("인문융합자율학부", "인문융합콘텐츠학부(인문융합자율학부)"),
            Map.entry("영어학전공", "인문융합콘텐츠학부(인문융합자율학부)"),
            Map.entry("일어일본학전공", "인문융합콘텐츠학부(인문융합자율학부)"),
            Map.entry("중어중국학전공", "인문융합콘텐츠학부(인문융합자율학부)"),
            Map.entry("종교와신학(기독교문화)전공", "인문융합콘텐츠학부(인문융합자율학부)"),
            Map.entry("사회융합학부", "사회융합학부(사회융합자율학부)"),
            Map.entry("사회융합자율학부", "사회융합학부(사회융합자율학부)"),
            Map.entry("사회복지학전공", "사회융합학부(사회융합자율학부)"),
            Map.entry("사회학전공", "사회융합학부(사회융합자율학부)"),
            Map.entry("경제학전공", "사회융합학부(사회융합자율학부)"),
            Map.entry("정치외교학전공", "사회융합학부(사회융합자율학부)"),
            Map.entry("경영학부", "경영학부"),
            Map.entry("경영학전공(경영학부)", "경영학부"),
            Map.entry("경영학전공", "경영학부"),
            Map.entry("미디어콘텐츠융합학부", "미디어콘텐츠융합학부(미디어콘텐츠융합자율학부)"),
            Map.entry("미디어콘텐츠융합자율학부", "미디어콘텐츠융합학부(미디어콘텐츠융합자율학부)"),
            Map.entry("신문방송학전공", "미디어콘텐츠융합학부(미디어콘텐츠융합자율학부)"),
            Map.entry("디지털콘텐츠전공", "미디어콘텐츠융합학부(미디어콘텐츠융합자율학부)"),
            Map.entry("영상콘텐츠전공", "미디어콘텐츠융합학부(미디어콘텐츠융합자율학부)"),
            Map.entry("소프트웨어융합학부", "소프트웨어융합학부(IT융합자율학부)"),
            Map.entry("IT융합자율학부", "소프트웨어융합학부(IT융합자율학부)"),
            Map.entry("소프트웨어융합전공", "소프트웨어융합학부(IT융합자율학부)"),
            Map.entry("컴퓨터공학전공", "소프트웨어융합학부(IT융합자율학부)"),
            Map.entry("소프트웨어공학전공", "소프트웨어융합학부(IT융합자율학부)"),
            Map.entry("정보통신공학전공", "소프트웨어융합학부(IT융합자율학부)"),
            Map.entry("인공지능전공", "소프트웨어융합학부(IT융합자율학부)"),
            Map.entry("미래융합학부", "미래융합학부"),
            Map.entry("미래인공지능전공", "미래융합학부"),
            Map.entry("빅데이터응용전공", "미래융합학부")
    );
    private static final Set<String> FREE_MAJOR_GROUP_NAMES = Set.of(
            "자유전공학부",
            "자유전공"
    );

    private static final String SOCIAL_SERVICE_KEYWORD = "사회봉사";
    private static final String CHAPEL_KEYWORD = "채플";

    private final CourseOfferingRepository courseOfferingRepository;
    private final OfferingTimeRepository offeringTimeRepository;

    public CandidateContext findCandidates(CourseCandidateRequestDto request) {
        return findCandidates(
                request.studentMajors(),
                request.studentYear(),
                request.completedCourseCodes(),
                Collections.emptySet()
        );
    }

    public CandidateContext findCandidates(TimetableCombinationRequestDto request) {
        return findCandidates(request, Collections.emptySet());
    }

    public CandidateContext findCandidates(TimetableCombinationRequestDto request, Set<String> completedCourseNames) {
        return findCandidates(
                request.studentMajors(),
                request.studentYear(),
                request.completedCourseCodes(),
                completedCourseNames
        );
    }

    public CandidateContext findSelectedGeneralRequiredOfferings(
            List<GeneralRequiredCourseSelectionDto> selections
    ) {
        if (selections == null || selections.isEmpty()) {
            return new CandidateContext(Collections.emptyList(), Collections.emptyMap());
        }

        List<CourseOffering> generalRequiredOfferings =
                courseOfferingRepository.findByCategory(CourseCategory.GENERAL_REQUIRED);
        Map<Long, List<OfferingTime>> timesByOfferingId = findTimesByOfferingId(generalRequiredOfferings);

        List<CourseOffering> selectedOfferings = selections.stream()
                .map(selection -> findMatchingGeneralRequiredOffering(
                        selection,
                        generalRequiredOfferings,
                        timesByOfferingId
                ))
                .distinct()
                .collect(Collectors.toList());

        Map<Long, List<OfferingTime>> selectedTimesByOfferingId = selectedOfferings.stream()
                .collect(Collectors.toMap(
                        CourseOffering::getId,
                        offering -> timesByOfferingId.getOrDefault(offering.getId(), Collections.emptyList())
                ));

        return new CandidateContext(selectedOfferings, selectedTimesByOfferingId);
    }

    public CandidateContext findAllOfferings() {
        List<CourseOffering> courseOfferings = excludeSupersededCourses(courseOfferingRepository.findAll());

        return new CandidateContext(courseOfferings, findTimesByOfferingId(courseOfferings));
    }

    public CandidateContext findSelectableOfferings(List<String> studentMajors) {
        List<CourseOffering> selectableMajorOfferings = findMajorOfferings(studentMajors).stream()
                .filter(courseOffering -> SELECTABLE_MAJOR_COURSE_TYPES.contains(courseOffering.getCategory()))
                .collect(Collectors.toList());
        List<CourseOffering> generalOfferings = courseOfferingRepository.findByCategory(CourseCategory.GENERAL);
        List<CourseOffering> selectableOfferings =
                excludeSupersededCourses(mergeWithoutDuplicate(selectableMajorOfferings, generalOfferings));

        return new CandidateContext(selectableOfferings, findTimesByOfferingId(selectableOfferings));
    }

    // 사회봉사는 교양필수라 findCandidates(전공 + 일반 교양)에 들어오지 않는다. 포함 요청 시 여기서 따로 찾는다.
    public CandidateContext findSocialServiceOfferings(
            Integer studentYear,
            List<String> completedCourseCodes,
            Set<String> completedCourseNames
    ) {
        Set<String> completedCodeSet = toSet(completedCourseCodes);
        List<CourseOffering> socialServiceOfferings =
                courseOfferingRepository.findByCategory(CourseCategory.GENERAL_REQUIRED).stream()
                        .filter(courseOffering -> courseOffering.getCourse().getCourseName().contains(SOCIAL_SERVICE_KEYWORD))
                        .filter(courseOffering -> isOfferedForStudentYear(courseOffering.getOfferedYear(), studentYear))
                        .filter(courseOffering -> !completedCodeSet.contains(courseOffering.getCourse().getCourseCode()))
                        .filter(courseOffering -> !completedCourseNames.contains(courseOffering.getCourse().getCourseName()))
                        .collect(Collectors.toList());

        return new CandidateContext(socialServiceOfferings, findTimesByOfferingId(socialServiceOfferings));
    }

    // 채플(비아메디아채플)은 학기 데이터에 따라 교양필수 또는 교양으로 들어와 일반 후보로는 다룰 수 없다.
    // 포함 요청 시 여기서 따로 찾고, 이미 이수한 채플(그리스도교와세계/그리스도교와인간)은 뺀다.
    public CandidateContext findChapelOfferings(
            List<String> completedCourseCodes,
            Set<String> completedCourseNames
    ) {
        Set<String> completedCodeSet = toSet(completedCourseCodes);
        List<CourseOffering> chapelOfferings = courseOfferingRepository.findAll().stream()
                .filter(this::isChapel)
                .filter(courseOffering -> !completedCodeSet.contains(courseOffering.getCourse().getCourseCode()))
                .filter(courseOffering -> !completedCourseNames.contains(courseOffering.getCourse().getCourseName()))
                .collect(Collectors.toList());

        return new CandidateContext(chapelOfferings, findTimesByOfferingId(chapelOfferings));
    }

    public boolean isChapel(CourseOffering courseOffering) {
        return courseOffering.getCourse().getCourseName().contains(CHAPEL_KEYWORD);
    }

    public CandidateContext findGeneralRequiredOfferings() {
        List<CourseOffering> generalRequiredOfferings =
                courseOfferingRepository.findByCategory(CourseCategory.GENERAL_REQUIRED);

        return new CandidateContext(generalRequiredOfferings, findTimesByOfferingId(generalRequiredOfferings));
    }

    private CandidateContext findCandidates(
            List<String> studentMajors,
            Integer studentYear,
            List<String> completedCourseCodes,
            Set<String> completedCourseNames
    ) {
        validateFreeMajorYear(studentMajors, studentYear);

        List<CourseOffering> majorOfferings = mergeWithoutDuplicate(
                findMajorOfferings(studentMajors, studentYear),
                findMajorExplorationOfferings(studentMajors, studentYear)
        );
        List<CourseOffering> generalOfferings = findGeneralOfferings(studentYear);
        List<CourseOffering> candidateOfferings = mergeWithoutDuplicate(majorOfferings, generalOfferings);
        Set<String> completedCodeSet = toSet(completedCourseCodes);

        List<CourseOffering> filteredOfferings = excludeSupersededCourses(candidateOfferings.stream()
                .filter(courseOffering -> !completedCodeSet.contains(courseOffering.getCourse().getCourseCode()))
                .filter(courseOffering -> !completedCourseNames.contains(courseOffering.getCourse().getCourseName()))
                .filter(courseOffering -> hasCompletedPrerequisites(courseOffering, completedCourseNames))
                .collect(Collectors.toList()));

        return new CandidateContext(filteredOfferings, findTimesByOfferingId(filteredOfferings));
    }

    private boolean hasCompletedPrerequisites(CourseOffering courseOffering, Set<String> completedCourseNames) {
        if (completedCourseNames.isEmpty()) {
            return true;
        }

        List<String> requiredPrerequisites = PREREQUISITE_COURSE_NAMES.get(courseOffering.getCourse().getCourseName());
        if (requiredPrerequisites == null) {
            return true;
        }

        boolean allCompleted = completedCourseNames.containsAll(requiredPrerequisites);
        if (!allCompleted) {
            log.debug(
                    "선수과목 미이수로 후보에서 제외: course={}, requiredPrerequisites={}",
                    courseOffering.getCourse().getCourseName(),
                    requiredPrerequisites
            );
        }

        return allCompleted;
    }

    private List<CourseOffering> excludeSupersededCourses(List<CourseOffering> offerings) {
        return offerings.stream()
                .filter(courseOffering -> !SUPERSEDED_COURSE_NAMES.contains(courseOffering.getCourse().getCourseName()))
                .collect(Collectors.toList());
    }

    private List<CourseOffering> findMajorOfferings(List<String> studentMajors) {
        if (studentMajors == null || studentMajors.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> eligibleMajors = resolveEligibleMajors(studentMajors);
        List<CourseOffering> majorOfferings = courseOfferingRepository.findByCategoryInAndSectionGroupIn(
                List.of(CourseCategory.MAJOR_ELECTIVE, CourseCategory.MAJOR_REQUIRED),
                eligibleMajors
        );
        Map<String, Long> countBySectionGroup = majorOfferings.stream()
                .collect(Collectors.groupingBy(CourseOffering::getSectionGroup, Collectors.counting()));
        log.debug("sectionGroup별 조회된 전공 강의 수: {}", countBySectionGroup);

        return majorOfferings;
    }

    private List<CourseOffering> findMajorOfferings(List<String> studentMajors, Integer studentYear) {
        boolean isSoftwareConvergenceTrackStudent =
                studentMajors != null && studentMajors.stream().anyMatch(SOFTWARE_CONVERGENCE_TRACKS::contains);
        List<CourseOffering> majorOfferings = findMajorOfferings(studentMajors);
        List<CourseOffering> filteredByYear = majorOfferings.stream()
                .filter(courseOffering -> isEligibleRegardlessOfYear(courseOffering, isSoftwareConvergenceTrackStudent)
                        || isOfferedForStudentYear(courseOffering.getOfferedYear(), studentYear))
                .collect(Collectors.toList());

        if (filteredByYear.size() != majorOfferings.size()) {
            log.debug(
                    "학년({}) 필터링으로 전공 강의 수 변경: {}건 -> {}건",
                    studentYear,
                    majorOfferings.size(),
                    filteredByYear.size()
            );
        }

        return filteredByYear;
    }

    private List<CourseOffering> findMajorExplorationOfferings(List<String> studentMajors, Integer studentYear) {
        if (studentYear == null || studentYear != 1) {
            return Collections.emptyList();
        }

        Set<String> eligibleSectionGroups = resolveEligibleMajorExplorationGroups(studentMajors);
        boolean freeMajorStudent = isFreeMajorStudent(studentMajors);
        if (!freeMajorStudent && eligibleSectionGroups.isEmpty()) {
            return Collections.emptyList();
        }

        // 자유전공 학생은 함께 보낸 학부의 전공탐색만 후보로 쓰고, 학부를 고르지 않았다면 전체 학부를 후보로 둔다.
        boolean allowAllGroups = freeMajorStudent && eligibleSectionGroups.isEmpty();

        return courseOfferingRepository.findByCategory(CourseCategory.MAJOR_EXPLORATION).stream()
                .filter(courseOffering -> allowAllGroups
                        || eligibleSectionGroups.contains(courseOffering.getSectionGroup()))
                .filter(courseOffering -> isOfferedForStudentYear(courseOffering.getOfferedYear(), studentYear))
                .collect(Collectors.toList());
    }

    // 자유전공은 1학년에만 존재한다. 2학년부터는 전공을 정해야 하므로 자유전공으로 요청하면 거절한다.
    private void validateFreeMajorYear(List<String> studentMajors, Integer studentYear) {
        if (isFreeMajorStudent(studentMajors) && (studentYear == null || studentYear != 1)) {
            throw new IllegalArgumentException("자유전공은 1학년만 선택할 수 있습니다: studentYear=%s".formatted(studentYear));
        }
    }

    private boolean isFreeMajorStudent(List<String> studentMajors) {
        if (studentMajors == null || studentMajors.isEmpty()) {
            return false;
        }

        return studentMajors.stream()
                .map(this::normalizeMajorName)
                .anyMatch(FREE_MAJOR_GROUP_NAMES::contains);
    }

    private Set<String> resolveEligibleMajorExplorationGroups(List<String> studentMajors) {
        if (studentMajors == null || studentMajors.isEmpty()) {
            return Collections.emptySet();
        }

        return studentMajors.stream()
                .map(this::normalizeMajorName)
                .filter(normalizedMajor -> !FREE_MAJOR_GROUP_NAMES.contains(normalizedMajor))
                .map(normalizedMajor -> MAJOR_EXPLORATION_GROUP_ALIASES.getOrDefault(normalizedMajor, normalizedMajor))
                .filter(group -> group != null && !group.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isEligibleRegardlessOfYear(
            CourseOffering courseOffering,
            boolean isSoftwareConvergenceTrackStudent
    ) {
        return isSoftwareConvergenceTrackStudent
                && SOFTWARE_CONVERGENCE_COMMON_MAJOR.equals(courseOffering.getSectionGroup());
    }

    private List<String> resolveEligibleMajors(List<String> studentMajors) {
        Set<String> eligibleMajors = new LinkedHashSet<>(studentMajors);

        if (studentMajors.stream().anyMatch(SOFTWARE_CONVERGENCE_TRACKS::contains)) {
            eligibleMajors.add(SOFTWARE_CONVERGENCE_COMMON_MAJOR);
        }

        return new ArrayList<>(eligibleMajors);
    }

    private List<CourseOffering> findGeneralOfferings(Integer studentYear) {
        return courseOfferingRepository.findByCategory(CourseCategory.GENERAL).stream()
                .filter(courseOffering -> isAvailableForStudentYear(courseOffering, studentYear))
                .collect(Collectors.toList());
    }

    private boolean isAvailableForStudentYear(CourseOffering courseOffering, Integer studentYear) {
        if (courseOffering.getYearRestricted() == null || !courseOffering.getYearRestricted()) {
            return true;
        }

        return isOfferedForStudentYear(courseOffering.getOfferedYear(), studentYear);
    }

    private boolean isOfferedForStudentYear(String offeredYear, Integer studentYear) {
        if (studentYear == null) {
            return true;
        }
        if (offeredYear == null || offeredYear.isBlank() || offeredYear.contains("전체")) {
            return true;
        }

        Set<Integer> offeredYears = parseOfferedYears(offeredYear);
        if (offeredYears.isEmpty()) {
            return true;
        }

        return offeredYears.contains(studentYear);
    }

    private Set<Integer> parseOfferedYears(String offeredYear) {
        Set<Integer> years = new HashSet<>();

        for (char character : offeredYear.toCharArray()) {
            if (character >= '1' && character <= '4') {
                years.add(character - '0');
            }
        }

        return years;
    }

    private List<CourseOffering> mergeWithoutDuplicate(
            List<CourseOffering> firstOfferings,
            List<CourseOffering> secondOfferings
    ) {
        Map<Long, CourseOffering> offeringsById = new LinkedHashMap<>();

        firstOfferings.forEach(courseOffering -> offeringsById.put(courseOffering.getId(), courseOffering));
        secondOfferings.forEach(courseOffering -> offeringsById.put(courseOffering.getId(), courseOffering));

        return new ArrayList<>(offeringsById.values());
    }

    // courseName + professor만으로 대부분은 유일하게 찾아지지만, 같은 교수가 같은 과목을 여러 분반으로
    // 개설한 경우(예: 화요일반/목요일반이 서로 다른 분반인 경우) 여러 건이 남을 수 있다.
    // 이때만 day/start/end로 추가로 좁힌다.
    private CourseOffering findMatchingGeneralRequiredOffering(
            GeneralRequiredCourseSelectionDto selection,
            List<CourseOffering> offerings,
            Map<Long, List<OfferingTime>> timesByOfferingId
    ) {
        List<CourseOffering> matchedByCourseName = offerings.stream()
                .filter(offering -> matchesCourseName(offering, selection.courseName()))
                .collect(Collectors.toList());
        List<CourseOffering> matchedByCourseAndProfessor = matchedByCourseName.stream()
                .filter(offering -> matchesProfessor(offering, selection.professor()))
                .collect(Collectors.toList());

        if (matchedByCourseAndProfessor.isEmpty()) {
            log.warn(
                    "교양필수 매칭 실패: 요청 courseName='{}'(정규화='{}'), professor='{}'(정규화='{}'), "
                            + "이름 일치 후보 수={}, 전체 교양필수 강의 수={}",
                    selection.courseName(), normalizeCourseName(selection.courseName()),
                    selection.professor(), normalizeProfessor(selection.professor()),
                    matchedByCourseName.size(), offerings.size()
            );
            matchedByCourseName.forEach(offering -> log.warn(
                    "  이름은 일치하는 DB 후보: courseCode={}, courseName='{}', professor='{}'(정규화='{}')",
                    offering.getCourse().getCourseCode(), offering.getCourse().getCourseName(),
                    offering.getProfessor(), normalizeProfessor(offering.getProfessor())
            ));
            throw new IllegalArgumentException(
                    "일치하는 교양필수 강좌를 찾을 수 없습니다: courseName=%s, professor=%s"
                            .formatted(selection.courseName(), selection.professor())
            );
        }

        if (matchedByCourseAndProfessor.size() == 1) {
            return matchedByCourseAndProfessor.get(0);
        }

        CourseOffering matched = matchedByCourseAndProfessor.stream()
                .filter(offering -> hasMatchingTime(
                        timesByOfferingId.getOrDefault(offering.getId(), Collections.emptyList()),
                        selection
                ))
                .findFirst()
                .orElse(null);

        if (matched == null) {
            log.warn(
                    "교양필수 매칭 실패(분반 여러 개, 시간 불일치): 요청 day={}, start={}, end={}, 후보 분반 수={}",
                    selection.day(), selection.start(), selection.end(), matchedByCourseAndProfessor.size()
            );
            matchedByCourseAndProfessor.forEach(offering -> log.warn(
                    "  분반 후보: offeringId={}, sectionNo={}, times={}",
                    offering.getId(), offering.getSectionNo(),
                    timesByOfferingId.getOrDefault(offering.getId(), Collections.emptyList()).stream()
                            .map(time -> "%s %s-%s".formatted(time.getDayOfWeek().getLabel(), time.getStartTime(), time.getEndTime()))
                            .collect(Collectors.toList())
            ));
            throw new IllegalArgumentException(
                    "일치하는 교양필수 강좌를 찾을 수 없습니다: courseName=%s, professor=%s, day=%s, start=%s, end=%s"
                            .formatted(selection.courseName(), selection.professor(), selection.day(), selection.start(), selection.end())
            );
        }

        return matched;
    }

    // day/start/end가 비어 있으면 시간 조건 없이 통과시킨다. day가 있으면 그 요일의 시간이 있어야 하고,
    // start/end가 주어졌다면 그 값과도 일치해야 한다.
    private boolean hasMatchingTime(List<OfferingTime> times, GeneralRequiredCourseSelectionDto selection) {
        String requestedDay = selection.day();
        if (requestedDay == null || requestedDay.isBlank()) {
            return true;
        }

        return times.stream().anyMatch(time ->
                time.getDayOfWeek().getLabel().equals(requestedDay)
                        && matchesRequestedTime(time.getStartTime(), selection.start())
                        && matchesRequestedTime(time.getEndTime(), selection.end())
        );
    }

    private boolean matchesRequestedTime(LocalTime actual, LocalTime requested) {
        return requested == null || requested.equals(actual);
    }

    private boolean matchesCourseName(CourseOffering offering, String requestedCourseName) {
        String normalizedRequestedName = normalizeCourseName(requestedCourseName);
        String canonicalRequestedName = GENERAL_REQUIRED_COURSE_NAME_ALIASES.getOrDefault(
                normalizedRequestedName,
                normalizedRequestedName
        );

        return normalizeCourseName(offering.getCourse().getCourseName()).equals(canonicalRequestedName);
    }

    // 공백만 제거해서는 부족하다 - 프론트/DB가 서로 다른 한글 유니코드 정규화 형태(NFC/NFD)를 쓰면
    // 육안으로 같아 보이는 문자열도 equals()에서 다르게 취급된다. NFC로 통일해서 비교한다.
    private String normalizeCourseName(String courseName) {
        if (courseName == null) {
            return "";
        }

        return Normalizer.normalize(courseName, Normalizer.Form.NFC).replaceAll("\\s+", "");
    }

    private String normalizeMajorName(String majorName) {
        if (majorName == null) {
            return "";
        }

        return Normalizer.normalize(majorName, Normalizer.Form.NFC).replaceAll("\\s+", "");
    }

    private boolean matchesProfessor(CourseOffering offering, String requestedProfessor) {
        return normalizeProfessor(offering.getProfessor()).equals(normalizeProfessor(requestedProfessor));
    }

    private String normalizeProfessor(String professor) {
        if (professor == null) {
            return "";
        }

        return Normalizer.normalize(professor, Normalizer.Form.NFC).replaceAll("\\s+", "");
    }

    private Set<String> toSet(Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptySet();
        }

        return values.stream().collect(Collectors.toSet());
    }

    private Map<Long, List<OfferingTime>> findTimesByOfferingId(List<CourseOffering> courseOfferings) {
        if (courseOfferings.isEmpty()) {
            return Collections.emptyMap();
        }

        return offeringTimeRepository.findByCourseOfferingIn(courseOfferings).stream()
                .collect(Collectors.groupingBy(offeringTime -> offeringTime.getCourseOffering().getId()));
    }
}
