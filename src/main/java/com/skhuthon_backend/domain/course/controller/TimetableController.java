package com.skhuthon_backend.domain.course.controller;

import com.skhuthon_backend.domain.course.dto.FirstYearTimetableRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationResponseDto;
import com.skhuthon_backend.domain.course.dto.TimetableGenerateRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableRecommendationResponseDto;
import com.skhuthon_backend.domain.course.service.TimetableEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "시간표", description = "시간표 자동 조합 생성 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/timetables")
public class TimetableController {

    private final TimetableEngineService timetableEngineService;

    @Operation(
            summary = "1학년 1학기 시간표 생성",
            description = "엑셀 없이 선택한 교양필수 강좌와 조건을 반영해 1학년 1학기 시간표를 생성합니다."
    )
    @PostMapping("/first-year/first-semester")
    public ResponseEntity<List<TimetableCombinationResponseDto>> generateFirstYearFirstSemester(
            @Valid @RequestBody FirstYearTimetableRequestDto request
    ) {
        return ResponseEntity.ok(timetableEngineService.generateFirstYearFirstSemester(request));
    }

    @Operation(
            summary = "1학년 2학기 시간표 생성",
            description = "선택한 교양필수 강좌, 조건, 성적 엑셀 파일을 반영해 1학년 2학기 시간표를 생성합니다."
    )
    @PostMapping(
            value = "/first-year/second-semester",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<List<TimetableRecommendationResponseDto>> generateFirstYearSecondSemester(
            @RequestPart("request") @Valid FirstYearTimetableRequestDto request,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(timetableEngineService.generateFirstYearSecondSemester(request, file));
    }

    @Operation(
            summary = "시간표 자동 조합 생성",
            description = "학생 전공, 학년, 전공/교양 목표 학점, 공강 요일, 1교시 제외 조건을 바탕으로 시간표 조합을 생성합니다."
    )
    @PostMapping("/combinations")
    public ResponseEntity<List<TimetableCombinationResponseDto>> generateCombinations(
            @Valid @RequestBody TimetableCombinationRequestDto request
    ) {
        return ResponseEntity.ok(timetableEngineService.generateCombinations(request));
    }

    @Operation(
            summary = "엑셀 파일 및 조건을 사용한 시간표 추천",
            description = "조건과 성적 엑셀 파일을 사용해 시간표를 생성하고 AI 랭킹 결과를 반환합니다."
    )
    @PostMapping(
            value = "/generate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<List<TimetableRecommendationResponseDto>> generate(
            @RequestPart("request") @Valid TimetableGenerateRequestDto request,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(timetableEngineService.generateRecommendations(request, file));
    }
}
