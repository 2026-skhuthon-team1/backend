package com.skhuthon_backend.domain.ai.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class AiRankingException extends RuntimeException {

    public AiRankingException(String message, Throwable cause) {
        super(message, cause);
    }
}
