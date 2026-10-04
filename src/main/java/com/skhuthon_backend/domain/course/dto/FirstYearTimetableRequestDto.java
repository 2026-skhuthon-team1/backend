package com.skhuthon_backend.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record FirstYearTimetableRequestDto(
        @ArraySchema(
                arraySchema = @Schema(description = "학생의 소속 학부 목록. 자유전공학부는 전공탐색 후보로 쓸 학부를 함께 보낼 수 있다(없으면 전체 학부)"),
                schema = @Schema(description = "학부명", example = "소프트웨어융합학부")
        )
        @NotEmpty(message = "학부 목록은 필수입니다.")
        @Size(max = 7, message = "학부는 자유전공학부를 포함해 최대 7개까지 입력할 수 있습니다.")
        List<@NotBlank(message = "학부명은 비어 있을 수 없습니다.") String> studentMajors,

        @Schema(description = "전공 목표 학점", example = "0")
        @NotNull(message = "전공 목표 학점은 필수입니다.")
        @Min(value = 0, message = "전공 목표 학점은 0 이상이어야 합니다.")
        Integer targetMajorCredits,

        @Schema(description = "교양 목표 학점", example = "10")
        @NotNull(message = "교양 목표 학점은 필수입니다.")
        @Min(value = 0, message = "교양 목표 학점은 0 이상이어야 합니다.")
        Integer targetGeneralCredits,

        @ArraySchema(
                arraySchema = @Schema(description = "희망 공강 요일 목록"),
                schema = @Schema(description = "공강 희망 요일", example = "금")
        )
        List<@Pattern(regexp = "^[월화수목금토일]$", message = "공강 요일은 월, 화, 수, 목, 금, 토, 일 중 하나여야 합니다.") String> freeDays,

        @Schema(description = "1교시(09:00 시작) 강의 제외 여부", example = "false")
        @NotNull(message = "1교시 제외 여부는 필수입니다.")
        Boolean excludeFirstPeriod,

        @ArraySchema(
                arraySchema = @Schema(description = "선택한 교양필수 강좌 목록"),
                schema = @Schema(description = "교양필수 과목별 교수/시간 선택")
        )
        List<@Valid GeneralRequiredCourseSelectionDto> fixedCourses
) {
}
