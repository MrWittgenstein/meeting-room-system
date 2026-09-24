package com.uestcfir.logservice;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice
public class LogErrors {
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<?> unavailable(DataAccessException exception) {
        return ResponseEntity.status(503).body(Map.of("code", 503, "message", "Log storage or session service unavailable"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> rejected(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("code", exception.getStatusCode().value(),
                "message", exception.getReason() == null ? "Request rejected" : exception.getReason()));
    }
}
