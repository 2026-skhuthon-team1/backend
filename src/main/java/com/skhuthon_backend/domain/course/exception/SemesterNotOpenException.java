package com.skhuthon_backend.domain.course.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class SemesterNotOpenException extends RuntimeException {

    public SemesterNotOpenException(String message) {
        super(message);
    }
}
