package com.skhuthon_backend.domain.course.controller;

import com.skhuthon_backend.domain.course.dto.TimetableCombinationRequestDto;
import com.skhuthon_backend.domain.course.dto.TimetableCombinationResponseDto;
import com.skhuthon_backend.domain.course.dto.TimetableGenerateRequestDto;
import com.skhuthon_backend.domain.course.service.TimetableEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "시간표", description = "시간표 자동 조합 생성 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/timetables")
public class TimetableController {

    private final TimetableEngineService timetableEngineService;

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

    @PostMapping(
            value = "/generate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<List<TimetableCombinationResponseDto>> generate(

            @RequestPart("request")
            @Valid TimetableGenerateRequestDto request,

            @RequestPart("file")
            MultipartFile file
    ) {

        return ResponseEntity.ok(
                timetableEngineService.generateTimetable(request, file)
        );
    }
}
