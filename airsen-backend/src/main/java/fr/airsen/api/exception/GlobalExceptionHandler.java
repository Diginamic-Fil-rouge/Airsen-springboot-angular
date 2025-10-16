package fr.airsen.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.beans.TypeMismatchException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global exception handler for the Airsen API.
 * 
 * This component provides centralized exception handling across all controllers
 * in the application. It ensures consistent error response formats and proper
 * HTTP status codes for different types of exceptions, particularly focusing
 * on validation errors and format issues that should return 400 Bad Request
 * instead of 401 Unauthorized.
 * 
 * Key Responsibilities:
 * - Handle Bean Validation errors (@Valid annotation failures)
 * - Process malformed JSON and request body parsing errors
 * - Provide consistent error response format across the API
 * - Log exceptions appropriately for monitoring and debugging
 * - Return proper HTTP status codes for better client error handling
 * 
 * Error Response Format:
 * - success: false for all error responses
 * - message: Human-readable error description
 * - code: Machine-readable error code for client handling
 * - timestamp: ISO formatted timestamp
 * - details: Additional context (for validation errors)
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles Bean Validation errors from @Valid annotations.
     * 
     * This method is triggered when request DTOs fail validation constraints
     * (e.g., @NotBlank, @Email, @Size annotations). It processes all field errors
     * and returns a 400 Bad Request response with detailed validation messages.
     * 
     * @param ex MethodArgumentNotValidException containing validation errors
     * @param request WebRequest for additional context
     * @return ResponseEntity with 400 status and validation error details
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {
        
        logger.debug("Validation error for request {}: {}", 
                    request.getDescription(false), ex.getMessage());

        // Extract field validation errors
        List<Map<String, String>> fieldErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            Map<String, String> fieldError = new HashMap<>();
            fieldError.put("field", error.getField());
            fieldError.put("message", error.getDefaultMessage());
            fieldError.put("rejectedValue", error.getRejectedValue() != null ? 
                          error.getRejectedValue().toString() : "null");
            fieldErrors.add(fieldError);
        }

        // Create error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "Invalid request data - validation failed");
        errorResponse.put("code", "VALIDATION_ERROR");
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("details", fieldErrors);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles parameter type mismatch errors from @PathVariable and @RequestParam.
     * 
     * This method is triggered when URL path variables or request parameters cannot
     * be converted to the expected type (e.g., "abc" cannot be converted to Long).
     * It returns a 400 Bad Request response with a clear parameter error message.
     * 
     * Common scenarios:
     * - /api/v1/notifications/abc/read (abc cannot be converted to Long)
     * - /api/v1/categories/invalid-id (invalid-id cannot be converted to Long)
     * 
     * @param ex MethodArgumentTypeMismatchException containing parameter conversion error
     * @param request WebRequest for additional context
     * @return ResponseEntity with 400 status and parameter error message
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, WebRequest request) {
        
        logger.debug("Parameter type mismatch for request {}: {}", 
                    request.getDescription(false), ex.getMessage());

        // Extract parameter details
        String parameterName = ex.getName();
        Object parameterValue = ex.getValue();
        String requiredType = ex.getRequiredType() != null ? 
                             ex.getRequiredType().getSimpleName() : "unknown";

        // Create descriptive error message
        String errorMessage = String.format(
            "Invalid value '%s' for parameter '%s'. Expected type: %s",
            parameterValue != null ? parameterValue.toString() : "null",
            parameterName,
            requiredType
        );

        // Create error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", errorMessage);
        errorResponse.put("code", "INVALID_PARAMETER_TYPE");
        errorResponse.put("timestamp", LocalDateTime.now());
        
        // Add additional details for debugging
        Map<String, String> details = new HashMap<>();
        details.put("parameter", parameterName);
        details.put("value", parameterValue != null ? parameterValue.toString() : "null");
        details.put("expectedType", requiredType);
        errorResponse.put("details", details);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles general type mismatch errors as fallback for parameter conversion issues.
     * 
     * This method catches TypeMismatchException instances that aren't caught by the more
     * specific MethodArgumentTypeMismatchException handler. It provides a fallback for
     * various parameter conversion errors.
     * 
     * @param ex TypeMismatchException containing parameter conversion error
     * @param request WebRequest for additional context
     * @return ResponseEntity with 400 status and parameter error message
     */
    @ExceptionHandler(TypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleGeneralTypeMismatchException(
            TypeMismatchException ex, WebRequest request) {
        
        logger.debug("General type mismatch for request {}: {}", 
                    request.getDescription(false), ex.getMessage());

        // Extract error information
        String errorMessage = "Invalid parameter format";
        Object value = ex.getValue();
        Class<?> requiredType = ex.getRequiredType();
        
        if (value != null && requiredType != null) {
            errorMessage = String.format(
                "Cannot convert value '%s' to required type %s",
                value.toString(),
                requiredType.getSimpleName()
            );
        }

        // Create error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", errorMessage);
        errorResponse.put("code", "PARAMETER_TYPE_MISMATCH");
        errorResponse.put("timestamp", LocalDateTime.now());
        
        // Add additional details
        Map<String, String> details = new HashMap<>();
        if (value != null) {
            details.put("providedValue", value.toString());
        }
        if (requiredType != null) {
            details.put("expectedType", requiredType.getSimpleName());
        }
        errorResponse.put("details", details);

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles malformed JSON and request body parsing errors.
     * 
     * This method is triggered when the request body cannot be parsed as valid JSON
     * or when JSON structure doesn't match expected DTO format. It returns a 400
     * Bad Request response with a clear format error message.
     * 
     * @param ex HttpMessageNotReadableException containing parsing error
     * @param request WebRequest for additional context
     * @return ResponseEntity with 400 status and format error message
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, WebRequest request) {
        
        logger.debug("JSON parsing error for request {}: {}", 
                    request.getDescription(false), ex.getMessage());

        // Determine specific error message based on exception details
        String errorMessage = "Invalid JSON format";
        String errorCode = "INVALID_JSON";
        
        if (ex.getMessage() != null) {
            String message = ex.getMessage().toLowerCase();
            if (message.contains("unexpected end")) {
                errorMessage = "Incomplete JSON - unexpected end of input";
                errorCode = "INCOMPLETE_JSON";
            } else if (message.contains("unexpected character")) {
                errorMessage = "Wrong format-Invalid JSON syntax - unexpected character";
                errorCode = "INVALID_JSON_SYNTAX";
            } else if (message.contains("cannot deserialize")) {
                errorMessage = "JSON structure doesn't match expected format";
                errorCode = "JSON_STRUCTURE_MISMATCH";
            }
        }

        // Create error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", errorMessage);
        errorResponse.put("code", errorCode);
        errorResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles illegal argument exceptions from service layer.
     * 
     * This method catches IllegalArgumentException instances thrown by service
     * methods when business rule validation fails. It returns a 400 Bad Request
     * response with the specific error message.
     * 
     * @param ex IllegalArgumentException containing business rule error
     * @param request WebRequest for additional context
     * @return ResponseEntity with 400 status and business rule error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {
        
        logger.debug("Business rule validation error for request {}: {}", 
                    request.getDescription(false), ex.getMessage());

        // Create error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", ex.getMessage() != null ? ex.getMessage() : "Invalid request parameters");
        errorResponse.put("code", "INVALID_PARAMETERS");
        errorResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * Handles runtime exceptions from service layer.
     * 
     * This method catches RuntimeException instances that represent application
     * errors (not system errors). It determines appropriate HTTP status codes
     * based on exception message content.
     * 
     * @param ex RuntimeException containing application error
     * @param request WebRequest for additional context
     * @return ResponseEntity with appropriate status and error message
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        
        logger.warn("Runtime error for request {}: {}", 
                   request.getDescription(false), ex.getMessage());

        // Determine HTTP status based on exception message
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorCode = "RUNTIME_ERROR";
        String errorMessage = ex.getMessage() != null ? ex.getMessage() : "An error occurred processing your request";

        if (ex.getMessage() != null) {
            String message = ex.getMessage().toLowerCase();
            if (message.contains("not found") || message.contains("does not exist")) {
                status = HttpStatus.NOT_FOUND;
                errorCode = "RESOURCE_NOT_FOUND";
            } else if (message.contains("already exists") || message.contains("duplicate")) {
                status = HttpStatus.CONFLICT;
                errorCode = "RESOURCE_CONFLICT";
            } else if (message.contains("forbidden") || message.contains("not allowed")) {
                status = HttpStatus.FORBIDDEN;
                errorCode = "OPERATION_FORBIDDEN";
            }
        }

        // Create error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", errorMessage);
        errorResponse.put("code", errorCode);
        errorResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(status).body(errorResponse);
    }

    /**
     * Handles generic exceptions as fallback.
     * 
     * This method catches any unexpected exceptions that aren't handled by
     * more specific handlers. It returns a 500 Internal Server Error response
     * while logging the full exception for debugging.
     * 
     * @param ex Exception containing unexpected error
     * @param request WebRequest for additional context
     * @return ResponseEntity with 500 status and generic error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        
        logger.error("Unexpected error for request {}: {}", 
                    request.getDescription(false), ex.getMessage(), ex);

        // Create generic error response
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "An unexpected error occurred");
        errorResponse.put("code", "INTERNAL_ERROR");
        errorResponse.put("timestamp", LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    /**
     * Creates a standardized error response map.
     * 
     * @param message error message
     * @param code error code
     * @param status HTTP status
     * @return standardized error response map
     */
    private Map<String, Object> createErrorResponse(String message, String code, HttpStatus status) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        errorResponse.put("code", code);
        errorResponse.put("status", status.value());
        errorResponse.put("timestamp", LocalDateTime.now());
        return errorResponse;
    }
}