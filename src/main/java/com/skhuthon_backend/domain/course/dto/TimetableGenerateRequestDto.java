package com.skhuthon_backend.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TimetableGenerateRequestDto {

    @ArraySchema(
            arraySchema = @Schema(description = "학생의 본전공 및 복수전공 목록"),
            schema = @Schema(description = "전공명", example = "소프트웨어융합전공")
    )
    @NotEmpty(message = "전공 목록은 필수입니다.")
    @Size(max = 2, message = "전공은 최대 2개까지 입력할 수 있습니다.")
    private List<@NotBlank(message = "전공명은 비어 있을 수 없습니다.") String> studentMajors;

    @Schema(description = "학생의 현재 학년", example = "1")
    @NotNull(message = "학년은 필수입니다.")
    @Min(value = 1, message = "학년은 1 이상이어야 합니다.")
    @Max(value = 4, message = "학년은 4 이하여야 합니다.")
    private Integer studentYear;

    @Schema(description = "전공 목표 학점", example = "12")
    @NotNull(message = "전공 목표 학점은 필수입니다.")
    @Min(value = 0, message = "전공 목표 학점은 0 이상이어야 합니다.")
    private Integer targetMajorCredits;

    @Schema(description = "교양선택 목표 학점 (교양필수·채플·사회봉사 제외)", example = "6")
    @NotNull(message = "교양 목표 학점은 필수입니다.")
    @Min(value = 0, message = "교양 목표 학점은 0 이상이어야 합니다.")
    private Integer targetGeneralCredits;

    @ArraySchema(
            arraySchema = @Schema(description = "희망 공강 요일 목록"),
            schema = @Schema(description = "공강 희망 요일", example = "금")
    )
    private List<@Pattern(regexp = "^[월화수목금토일]$", message = "공강 요일은 월, 화, 수, 목, 금, 토, 일 중 하나여야 합니다.") String> freeDays;

    @Schema(description = "1교시(09:00 시작) 강의 제외 여부", example = "true")
    @NotNull(message = "1교시 제외 여부는 필수입니다.")
    private Boolean excludeFirstPeriod;

    @Schema(description = "사회봉사 과목 포함 여부 (1학년은 이 값과 무관하게 항상 제외됩니다)", example = "true")
    @NotNull(message = "사회봉사 포함 여부는 필수입니다.")
    private Boolean includeSocialService;

    @Schema(description = "채플(비아메디아채플) 포함 여부. 포함하면 이수하지 않은 채플 분반 하나를 넣고, 값이 없으면 포함하지 않습니다.", example = "true")
    private Boolean includeChapel;

    @ArraySchema(
            arraySchema = @Schema(description = "1학년 교양필수 선택 강좌 목록"),
            schema = @Schema(description = "교양필수 과목별 교수/시간 선택")
    )
    private List<@Valid GeneralRequiredCourseSelectionDto> fixedCourses;
}
