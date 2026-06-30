package com.skhuthon_backend.domain.course.controller;

import com.skhuthon_backend.domain.course.dto.CourseCandidateRequestDto;
import com.skhuthon_backend.domain.course.dto.CourseOfferingCandidateResponseDto;
import com.skhuthon_backend.domain.course.service.TimetableEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "과목", description = "시간표 후보 과목 조회 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/courses")
public class CourseController {

    private final TimetableEngineService timetableEngineService;

    @Operation(
            summary = "전체 개설 강좌 조회",
            description = "DB에 저장된 전체 개설 강좌를 강의 시간 정보와 함께 조회합니다."
    )
    @GetMapping("/offerings")
    public ResponseEntity<List<CourseOfferingCandidateResponseDto>> findAllOfferings() {
        return ResponseEntity.ok(timetableEngineService.findAllOfferings());
    }

    @Operation(
            summary = "전공 선택 기반 수강 가능 과목 조회",
            description = "선택한 전공에 해당하는 전필/전선 과목과 전체 교양 과목을 강의 시간 정보와 함께 조회합니다."
    )
    @GetMapping("/selectable-offerings")
    public ResponseEntity<List<CourseOfferingCandidateResponseDto>> findSelectableOfferings(
            @RequestParam
            @NotEmpty(message = "전공 목록은 필수입니다.")
            @Size(max = 2, message = "전공은 최대 2개까지 입력할 수 있습니다.")
            List<@NotBlank(message = "전공명은 비어 있을 수 없습니다.") String> studentMajors
    ) {
        return ResponseEntity.ok(timetableEngineService.findSelectableOfferings(studentMajors));
    }

    @Operation(
            summary = "후보 과목 조회",
            description = "학생의 전공, 학년, 기이수 과목 정보를 바탕으로 수강 가능한 개설 강좌 후보를 조회합니다."
    )
    @PostMapping("/candidates")
    public ResponseEntity<List<CourseOfferingCandidateResponseDto>> findCandidateOfferings(
            @Valid @RequestBody CourseCandidateRequestDto request
    ) {
        return ResponseEntity.ok(timetableEngineService.findCandidateOfferings(request));
    }
}
