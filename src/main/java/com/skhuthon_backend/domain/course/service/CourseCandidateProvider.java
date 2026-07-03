package com.skhuthon_backend.domain.course.service;

import com.skhuthon_backend.domain.course.dto.CourseCandidateRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.entity.CourseCategory;
import com.skhuthon_backend.domain.course.entity.CourseOffering;
import com.skhuthon_backend.domain.course.entity.OfferingTime;
import com.skhuthon_backend.domain.course.repository.CourseOfferingRepository;
import com.skhuthon_backend.domain.course.repository.OfferingTimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

//- 전공/교양 후보 조회
//- 학년 조건 적용
//- 기이수 과목 제외
//- OfferingTime 조회

@Slf4j
@Component
@RequiredArgsConstructor
public class CourseCandidateProvider {

    private static final List<String> SELECTABLE_MAJOR_COURSE_TYPES = List.of("전공필수", "전공선택", "교양");

    // 소프트웨어융합학부(IT융합자율학부) 4학년 트랙 - 2~3학년은 공통 전공으로 개설되므로
    // 트랙을 선언한 학생도 학부 공통 전공 과목을 자격 대상에 포함해야 함
    private static final Set<String> SOFTWARE_CONVERGENCE_TRACKS = Set.of(
            "소프트웨어공학전공", "정보통신공학전공", "컴퓨터공학전공"
    );
    private static final String SOFTWARE_CONVERGENCE_COMMON_MAJOR = "소프트웨어융합전공";

    // 교육과정 개편으로 과목명이 바뀌면서 구 명칭 과목이 DB에 남아있는 경우,
    // 신 명칭 과목과 중복 노출되지 않도록 구 명칭 과목은 후보에서 항상 제외한다.
    private static final Set<String> SUPERSEDED_COURSE_NAMES = Set.of(
            "프론트엔드개발",
            "웹프로그래밍"
    );

    // 선수필수: 과목명 -> 이수해야 하는 선수과목명 목록(전부 이수해야 함, AND 조건).
    // completedCourseNames(성적표에서 파싱한 기이수 과목명)로만 판단하므로,
    // 성적표 업로드 흐름이 아닌 다른 후보 조회 경로에서는 적용되지 않는다.
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
            Map.entry("Java네트워크프로그래밍", List.of("Java프로그래밍")),
            Map.entry("프론트엔드프레임워크", List.of("프론트엔드프로그래밍")),
            Map.entry("Node.js프로그래밍", List.of("Javascript프로그래밍")),
            Map.entry("빅데이터", List.of("통계자료분석및실습")),
            Map.entry("머신러닝", List.of("Python프로그래밍")),
            Map.entry("코딩테스트지도", List.of("알고리즘"))
    );

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

    // completedCourseNames: 성적표(엑셀)에서 파싱한 기이수 과목명. course_code가 다르더라도
    // 이름이 완전히 같은 후보 과목은 이미 이수한 것으로 간주해 제외한다.
    public CandidateContext findCandidates(TimetableCombinationRequestDto request, Set<String> completedCourseNames) {
        return findCandidates(
                request.studentMajors(),
                request.studentYear(),
                request.completedCourseCodes(),
                completedCourseNames
        );
    }

    public CandidateContext findAllOfferings() {
        List<CourseOffering> courseOfferings = excludeSupersededCourses(courseOfferingRepository.findAll());

        return new CandidateContext(courseOfferings, findTimesByOfferingId(courseOfferings));
    }

    public CandidateContext findSelectableOfferings(List<String> studentMajors) {
        List<CourseOffering> selectableMajorOfferings = findMajorOfferings(studentMajors).stream()
                .filter(courseOffering -> SELECTABLE_MAJOR_COURSE_TYPES.contains(courseOffering.getCategory().getLabel()))
                .collect(Collectors.toList());
        List<CourseOffering> generalOfferings = courseOfferingRepository.findByCategory(CourseCategory.GENERAL);
        List<CourseOffering> selectableOfferings =
                excludeSupersededCourses(mergeWithoutDuplicate(selectableMajorOfferings, generalOfferings));

        return new CandidateContext(selectableOfferings, findTimesByOfferingId(selectableOfferings));
    }

    private CandidateContext findCandidates(
            List<String> studentMajors,
            Integer studentYear,
            List<String> completedCourseCodes,
            Set<String> completedCourseNames
    ) {
        List<CourseOffering> majorOfferings = findMajorOfferings(studentMajors, studentYear);
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

    // 선수필수: 요구되는 선수과목을 모두 이수하지 않았다면 후보에서 제외한다.
    // completedCourseNames가 비어있으면(성적표 업로드 흐름이 아니면) 판단할 근거가 없으므로 걸러내지 않는다.
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
            log.debug("선수과목 미이수로 후보에서 제외: course={}, requiredPrerequisites={}",
                    courseOffering.getCourse().getCourseName(), requiredPrerequisites);
        }

        return allCompleted;
    }

    private List<CourseOffering> excludeSupersededCourses(List<CourseOffering> offerings) {
        List<CourseOffering> filteredOfferings = offerings.stream()
                .filter(courseOffering -> !SUPERSEDED_COURSE_NAMES.contains(courseOffering.getCourse().getCourseName()))
                .collect(Collectors.toList());

        if (filteredOfferings.size() != offerings.size()) {
            log.debug("개편으로 대체된 구 명칭 과목 제외: 제외 전={}건, 제외 후={}건", offerings.size(), filteredOfferings.size());
        }

        return filteredOfferings;
    }

    private List<CourseOffering> findMajorOfferings(List<String> studentMajors) {
        if (studentMajors == null || studentMajors.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> eligibleMajors = resolveEligibleMajors(studentMajors);
        log.debug("전공 후보 조회 대상 sectionGroup 목록: {}", eligibleMajors);

        List<CourseOffering> majorOfferings = courseOfferingRepository.findByCategoryInAndSectionGroupIn(
                List.of(CourseCategory.MAJOR_ELECTIVE, CourseCategory.MAJOR_REQUIRED), eligibleMajors);
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
            log.debug("학년({}) 필터링으로 전공 강의 수 변화: {}건 -> {}건", studentYear, majorOfferings.size(), filteredByYear.size());
        }

        return filteredByYear;
    }

    // 트랙(소프트웨어공학전공/정보통신공학전공/컴퓨터공학전공) 학생에게 자격을 부여한 소프트웨어융합전공 과목은
    // 원래 2~3학년용으로 offered_year가 설정돼 있어, 학년 필터를 그대로 적용하면 트랙 학생에게 열어준 의미가
    // 사라진다. 이 경우에는 학년 제한을 적용하지 않는다.
    private boolean isEligibleRegardlessOfYear(CourseOffering courseOffering, boolean isSoftwareConvergenceTrackStudent) {
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

    // offered_year는 '2.3.4', '23,4', '2,3,4,' 등 오타가 섞여 있으므로 1~4 숫자만 추출해 매칭한다.
    // '전체'/빈값이거나 숫자를 하나도 추출하지 못하면 과도한 차단을 막기 위해 전 학년에게 노출한다.
    private boolean isOfferedForStudentYear(String offeredYear, Integer studentYear) {
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
