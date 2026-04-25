package io.browserservice.api.error;

import io.browserservice.api.dto.ErrorResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handle(Throwable ex) {
        ErrorMapper.Mapped mapped = ErrorMapper.map(ex, RequestIdFilter.currentRequestId());
        return ResponseEntity.status(mapped.status()).body(new ErrorResponse(mapped.body()));
    }
}
