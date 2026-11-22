package com.skhuthon_backend.domain.course.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// 꼭 넣을 과목 때문에 시간표를 하나도 만들 수 없을 때, 빈 결과 대신 무엇을 고치면 되는지 알려 주기 위한 예외
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class MustIncludeCourseException extends RuntimeException {

    public MustIncludeCourseException(String message) {
        super(message);
    }
}
