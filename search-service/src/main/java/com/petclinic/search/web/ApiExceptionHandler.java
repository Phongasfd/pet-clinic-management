package com.petclinic.search.web;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.petclinic.search.search.InvalidSearchRequestException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UnauthenticatedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Map<String, Object> unauthenticated(UnauthenticatedException e, HttpServletRequest request) {
        return body(HttpStatus.UNAUTHORIZED, e.getMessage(), request);
    }

    @ExceptionHandler({InvalidSearchRequestException.class, MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> badRequest(Exception e, HttpServletRequest request) {
        String message = e instanceof MethodArgumentTypeMismatchException m
                ? "Invalid value for '" + m.getName() + "'"
                : e.getMessage();
        return body(HttpStatus.BAD_REQUEST, message, request);
    }

    /** The read model is down; pet-service (the source of truth) is not affected. */
    @ExceptionHandler({IOException.class, ElasticsearchException.class})
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, Object> searchUnavailable(Exception e, HttpServletRequest request) {
        log.error("Search failed: {}", e.getMessage());
        return body(HttpStatus.SERVICE_UNAVAILABLE, "Search is temporarily unavailable", request);
    }

    private static Map<String, Object> body(HttpStatus status, String message, HttpServletRequest request) {
        return Map.of(
                "timestamp", Instant.now().toString(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message,
                "path", request.getRequestURI());
    }
}
