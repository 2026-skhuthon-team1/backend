package com.skhuthon_backend.domain.course.controller;

import com.skhuthon_backend.domain.ai.dto.AiTimetableRequestDto;
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

    @Operation(
            summary = "엑셀 파일 및 조건을 사용한 시간표 조합 생성",
            description =
                    """
                    사용자의 시간표를 생성합니다.
                    
                    body로 사용자의 전공명(최대 2개), 현재 학년(2~4 정수),
                    목표전공학점, 목표교양학점, 필수공강요일, 1교시 제외 여부를 json으로 받고,
                    종정시에서 내려받을 수 있는 수강과목 엑셀 파일 또한 필요로 합니다.
                    
                    반환값으로, 각 시간표의 점수, 순위, 태그, 강의를 넘깁니다.
                    
                    엑셀 파일에 기본적이로 존재하는 '과목코드' 컬럼이 존재하지 않으면 에러가 발생합니다.
                    또한 엑셀 파일을 업로드하지 않거나 다른 파일을 업로드하여도 에러가 발생합니다.
                    """
    )
    @PostMapping(
            value = "/generate",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<AiTimetableRequestDto> generate(

            @RequestPart("request")
            @Valid TimetableGenerateRequestDto request,

            @RequestPart("file")
            MultipartFile file
    ) {

        return ResponseEntity.ok(
                timetableEngineService.generateAiRequest(request, file)
        );
    }
}
