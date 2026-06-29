package com.skhuthon_backend.domain.course.controller;

import com.skhuthon_backend.domain.course.dto.CourseCandidateRequestDto;
import com.skhuthon_backend.domain.course.dto.CourseOfferingCandidateResponseDto;
import com.skhuthon_backend.domain.course.service.TimetableEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "과목", description = "시간표 후보 과목 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/courses")
public class CourseController {

    private final TimetableEngineService timetableEngineService;

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
