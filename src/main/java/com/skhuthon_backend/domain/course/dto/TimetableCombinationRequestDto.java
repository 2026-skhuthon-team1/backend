package com.skhuthon_backend.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
public record TimetableCombinationRequestDto(

        @ArraySchema(
                arraySchema = @Schema(description = "학생의 본전공 및 복수전공 목록"),
                schema = @Schema(description = "학생 전공명", example = "소프트웨어융합전공")
        )
        @NotEmpty(message = "전공 목록은 필수입니다.")
        @Size(max = 2, message = "전공은 최대 2개까지 입력할 수 있습니다.")
        List<@NotBlank(message = "전공명은 비어 있을 수 없습니다.") String> studentMajors,

        @Schema(description = "학생의 현재 학년", example = "3")
        @NotNull(message = "학년은 필수입니다.")
        @Min(value = 2, message = "학년은 2 이상이어야 합니다.")
        @Max(value = 4, message = "학년은 4 이하여야 합니다.")
        Integer studentYear,

        @Schema(description = "전공 목표 학점", example = "12")
        @NotNull(message = "전공 목표 학점은 필수입니다.")
        @Min(value = 0, message = "전공 목표 학점은 0 이상이어야 합니다.")
        Integer targetMajorCredits,

        @Schema(description = "교양 목표 학점", example = "6")
        @NotNull(message = "교양 목표 학점은 필수입니다.")
        @Min(value = 0, message = "교양 목표 학점은 0 이상이어야 합니다.")
        Integer targetGeneralCredits,

        @ArraySchema(
                arraySchema = @Schema(description = "희망 공강 요일 목록"),
                schema = @Schema(description = "공강 희망 요일", example = "금")
        )
        List<@Pattern(regexp = "^[월화수목금토일]$", message = "공강 요일은 월, 화, 수, 목, 금, 토, 일 중 하나여야 합니다.") String> freeDays,

        @Schema(description = "1교시(09:00 시작) 강좌 제외 여부", example = "true")
        @NotNull(message = "1교시 제외 여부는 필수입니다.")
        Boolean excludeFirstPeriod,

        @ArraySchema(
                arraySchema = @Schema(description = "이수 완료하여 후보에서 제외할 과목 코드 목록"),
                schema = @Schema(description = "기이수 과목 코드", example = "IS00001")
        )
        List<
                @NotBlank(message = "과목 코드는 비어 있을 수 없습니다.")
                @Pattern(regexp = "^[A-Z]{2}\\d{5}$", message = "과목 코드는 영문 대문자 2자리와 숫자 5자리 형식이어야 합니다.")
                String> completedCourseCodes
) {
}
