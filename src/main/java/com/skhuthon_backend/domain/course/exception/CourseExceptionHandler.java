package com.skhuthon_backend.domain.course.exception;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// 프론트는 오류 응답의 message를 그대로 화면에 띄운다. 기본 오류 응답은 message를 비워 보내므로
// 사용자에게 보여줄 경고만 여기서 message에 담아 내려준다.
@RestControllerAdvice
public class CourseExceptionHandler {

    @ExceptionHandler(SemesterNotOpenException.class)
    public ResponseEntity<Map<String, String>> handleSemesterNotOpen(SemesterNotOpenException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
