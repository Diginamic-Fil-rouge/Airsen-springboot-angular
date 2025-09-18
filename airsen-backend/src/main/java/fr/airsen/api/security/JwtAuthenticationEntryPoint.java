package fr.airsen.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom authentication entry point for JWT-based authentication.
 * 
 * This component handles authentication failures in the Airsen API by providing
 * consistent error responses when users attempt to access protected resources
 * without valid authentication credentials. It replaces the default Spring Security
 * authentication error handling with JSON responses suitable for API consumption.
 * 
 * Key Responsibilities:
 * - Handle authentication failures for protected endpoints
 * - Provide consistent JSON error response format
 * - Log authentication attempts for security monitoring
 * - Return appropriate HTTP status codes for different error scenarios
 * - Maintain API consistency with existing error response structure
 * 
 * Error Response Format:
 * - HTTP 401 Unauthorized for authentication failures
 * - JSON response with error details and timestamp
 * - Consistent with application's error response structure
 * - No sensitive information leakage in error messages
 * 
 * Security Considerations:
 * - Generic error messages to prevent information disclosure
 * - Request details logging for security monitoring
 * - Proper HTTP status codes for client error handling
 * - Integration with application's security logging framework
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationEntryPoint.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Handles authentication entry point when authentication fails.
     * 
     * This method is called whenever an exception is thrown due to an
     * unauthenticated user trying to access a resource that requires authentication.
     * It generates a consistent JSON error response and logs the authentication
     * attempt for security monitoring.
     * 
     * @param request HTTP request that resulted in authentication failure
     * @param response HTTP response to send back to client
     * @param authException exception that triggered authentication failure
     * @throws IOException if response writing fails
     * @throws ServletException if servlet processing fails
     */
    @Override
    public void commence(HttpServletRequest request, 
                        HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {

        // Log authentication failure for security monitoring
        String clientIp = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        logger.warn("Authentication failure: {} {} from IP: {} - {}", 
                   method, requestUri, clientIp, authException.getMessage());

        // Log additional request details for security analysis
        if (logger.isDebugEnabled()) {
            logger.debug("Authentication failure details - User-Agent: {}, Referer: {}", 
                        userAgent, request.getHeader("Referer"));
        }

        // Set response status and content type
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        // Create error response body
        Map<String, Object> errorResponse = createErrorResponse(request, authException);

        // Write JSON response
        try {
            String jsonResponse = objectMapper.writeValueAsString(errorResponse);
            response.getWriter().write(jsonResponse);
        } catch (Exception e) {
            logger.error("Failed to write authentication error response: {}", e.getMessage(), e);
            // Fallback to simple text response
            response.getWriter().write("{\"error\":\"Authentication required\",\"status\":401}");
        }
    }

    /**
     * Creates a standardized error response map for authentication failures.
     * 
     * The response format is consistent with the application's error handling
     * structure and provides enough information for client-side error handling
     * without exposing sensitive system details.
     * 
     * @param request HTTP request that failed authentication
     * @param authException authentication exception that occurred
     * @return Map containing error response data
     */
    private Map<String, Object> createErrorResponse(HttpServletRequest request, 
                                                   AuthenticationException authException) {
        Map<String, Object> errorResponse = new HashMap<>();

        // Basic error information
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("message", determineErrorMessage(authException));
        errorResponse.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        errorResponse.put("path", request.getRequestURI());

        // Additional context for API consumers
        errorResponse.put("code", "AUTH_REQUIRED");
        
        // Suggest authentication method
        if (isMissingAuthorizationHeader(request)) {
            errorResponse.put("hint", "Include 'Authorization: Bearer <token>' header");
        } else {
            errorResponse.put("hint", "Check token validity and expiration");
        }

        return errorResponse;
    }

    /**
     * Determines appropriate error message based on authentication exception type.
     * 
     * This method analyzes the authentication exception to provide more specific
     * error messages while avoiding information leakage that could aid attackers.
     * 
     * @param authException authentication exception to analyze
     * @return user-friendly error message
     */
    private String determineErrorMessage(AuthenticationException authException) {
        if (authException == null) {
            return "Authentication required to access this resource";
        }

        String exceptionMessage = authException.getMessage();
        
        // Provide specific messages for common authentication scenarios
        if (exceptionMessage != null) {
            if (exceptionMessage.contains("expired")) {
                return "Authentication token has expired";
            }
            if (exceptionMessage.contains("invalid")) {
                return "Invalid authentication credentials";
            }
            if (exceptionMessage.contains("malformed")) {
                return "Malformed authentication token";
            }
        }

        // Default generic message for security
        return "Authentication required to access this resource";
    }

    /**
     * Checks if the request is missing the Authorization header.
     * 
     * @param request HTTP request to check
     * @return true if Authorization header is missing or empty
     */
    private boolean isMissingAuthorizationHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return authHeader == null || authHeader.trim().isEmpty();
    }

    /**
     * Extracts client IP address from HTTP request.
     * 
     * This method checks various proxy headers to determine the real client IP
     * address, which is important for security logging and monitoring.
     * 
     * @param request HTTP request to examine
     * @return client IP address
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // Check various proxy headers for real client IP
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle comma-separated list (X-Forwarded-For can contain multiple IPs)
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        // Fallback to direct remote address
        return request.getRemoteAddr();
    }

    /**
     * Determines if the authentication failure is due to a JWT-specific issue.
     * 
     * @param authException authentication exception to analyze
     * @return true if this appears to be a JWT-related authentication failure
     */
    private boolean isJwtAuthenticationFailure(AuthenticationException authException) {
        if (authException == null || authException.getMessage() == null) {
            return false;
        }

        String message = authException.getMessage().toLowerCase();
        return message.contains("jwt") || 
               message.contains("token") || 
               message.contains("bearer");
    }

    /**
     * Adds security headers to the response for enhanced protection.
     * 
     * @param response HTTP response to modify
     */
    private void addSecurityHeaders(HttpServletResponse response) {
        // Prevent caching of error responses
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
        
        // Content security headers
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "1; mode=block");
    }
}