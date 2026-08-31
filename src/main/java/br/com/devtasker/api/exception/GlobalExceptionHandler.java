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

import br.com.devtasker.api.email.exception.EmailDeliveryException;
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

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiError> handleEmailNotVerified(
            EmailNotVerifiedException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                "EMAIL_NOT_VERIFIED",
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error);
    }

    @ExceptionHandler(EmailVerificationException.class)
    public ResponseEntity<ApiError> handleEmailVerification(
            EmailVerificationException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                exception.getStatus().value(),
                exception.getErrorCode(),
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity
                .status(exception.getStatus())
                .body(error);
    }

    @ExceptionHandler(PasswordRecoveryException.class)
    public ResponseEntity<ApiError> handlePasswordRecovery(
            PasswordRecoveryException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                exception.getStatus().value(),
                exception.getErrorCode(),
                "Não foi possível concluir a recuperação de senha.",
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity
                .status(exception.getStatus())
                .body(error);
    }

    @ExceptionHandler(EmailDeliveryException.class)
    public ResponseEntity<ApiError> handleEmailDelivery(
            EmailDeliveryException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "EMAIL_DELIVERY_FAILED",
                "Não foi possível enviar o e-mail. Tente novamente.",
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(error);
    }

    @ExceptionHandler(ProjectMembershipException.class)
    public ResponseEntity<ApiError> handleProjectMembership(
            ProjectMembershipException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                exception.getStatus().value(),
                exception.getErrorCode(),
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity.status(exception.getStatus()).body(error);
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

    @ExceptionHandler(BoardNameAlreadyInUseException.class)
    public ResponseEntity<ApiError> handleBoardNameAlreadyInUse(
            BoardNameAlreadyInUseException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                HttpStatus.CONFLICT.value(),
                "BOARD_NAME_ALREADY_IN_USE",
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }
    
    @ExceptionHandler(ProjectPermissionDeniedException.class)
    public ResponseEntity<ApiError> handleProjectPermissionDenied(
            ProjectPermissionDeniedException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                HttpStatus.FORBIDDEN.value(),
                "PROJECT_PERMISSION_DENIED",
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error);
    }
    
    @ExceptionHandler(BoardColumnNotFoundException.class)
    public ResponseEntity<ApiError> handleBoardColumnNotFound(
            BoardColumnNotFoundException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "BOARD_COLUMN_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }
    
    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiError> handleTaskNotFound(
            TaskNotFoundException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "TASK_NOT_FOUND",
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }
    
    @ExceptionHandler(InvalidTaskMoveException.class)
    public ResponseEntity<ApiError> handleInvalidTaskMove(
            InvalidTaskMoveException exception,
            HttpServletRequest request
    ) {
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_TASK_MOVE",
                exception.getMessage(),
                request.getRequestURI(),
                OffsetDateTime.now(ZoneOffset.UTC),
                Map.of()
        );

        return ResponseEntity
                .badRequest()
                .body(error);
    }
}
