package br.com.devtasker.api.exception;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.authentication.BadCredentialsException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyInUse(
            EmailAlreadyInUseException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                HttpStatus.CONFLICT.value(),
                "EMAIL_ALREADY_IN_USE",
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fields = new LinkedHashMap<>();

        exception.getBindingResult()
                .getFieldErrors()
                .forEach(fieldError ->
                        fields.putIfAbsent(
                                fieldError.getField(),
                                fieldError.getDefaultMessage()
                        )
                );

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Um ou mais campos enviados são inválidos.",
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                fields
        );

        return ResponseEntity
                .badRequest()
                .body(error);
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(
            BadCredentialsException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_CREDENTIALS",
                "E-mail ou senha inválidos.",
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }
    
    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ApiError> handleProjectNotFound(
            ProjectNotFoundException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "PROJECT_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }
    
    @ExceptionHandler(BoardNotFoundException.class)
    public ResponseEntity<ApiError> handleBoardNotFound(
            BoardNotFoundException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "BOARD_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }
}