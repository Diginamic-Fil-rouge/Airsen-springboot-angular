package fr.airsen.api.security;

import fr.airsen.api.service.JwtBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter for processing JWT tokens in HTTP requests.
 * 
 * This filter intercepts HTTP requests to extract and validate JWT tokens
 * from the Authorization header. Upon successful token validation, it establishes
 * the security context for the authenticated user, enabling authorization
 * decisions throughout the request lifecycle.
 * 
 * Filter Flow:
 * 1. Extract JWT token from Authorization header (Bearer scheme)
 * 2. Validate token authenticity and expiration
 * 3. Extract user information from token claims
 * 4. Load complete user details from database
 * 5. Create Spring Security authentication object
 * 6. Set authentication in SecurityContextHolder
 * 7. Continue with filter chain
 * 
 * Security Features:
 * - Bearer token scheme validation
 * - Comprehensive token validation
 * - User details refresh from database
 * - Proper security context management
 * - Request-specific authentication details
 * - Exception handling without breaking filter chain
 * 
 * Performance Considerations:
 * - OncePerRequestFilter ensures single execution per request
 * - Efficient token parsing and validation
 * - Database lookup only for valid tokens
 * - Minimal overhead for unauthenticated requests
 */
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);

    /**
     * Bearer token scheme prefix in Authorization header.
     */
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Authorization header name.
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private JwtBlacklistService jwtBlacklistService;

    /**
     * Filters incoming HTTP requests to process JWT authentication.
     * 
     * This method is called once per request to extract and validate JWT tokens.
     * It performs the complete authentication flow from token extraction to
     * security context establishment.
     * 
     * Process:
     * 1. Extract JWT token from Authorization header
     * 2. Validate token structure and authenticity
     * 3. Extract username from token claims
     * 4. Load user details from UserDetailsService
     * 5. Create authentication object with authorities
     * 6. Set authentication in SecurityContextHolder
     * 7. Continue request processing
     * 
     * @param request HTTP request containing potential JWT token
     * @param response HTTP response for potential error handling
     * @param filterChain filter chain for request processing continuation
     * @throws ServletException if filter processing fails
     * @throws IOException if I/O operations fail
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                   HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {

        try {
            // Extract JWT token from request
            String token = extractTokenFromRequest(request);

            // Process token if present and valid
            if (token != null) {
                // Check if token is blacklisted (revoked via logout)
                if (jwtBlacklistService.isTokenBlacklisted(token)) {
                    logger.warn("Attempted use of blacklisted token from IP: {}", getClientIpAddress(request));
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Token has been revoked\",\"message\":\"Please log in again\"}");
                    return;
                }

                // Validate token structure and expiration
                if (jwtTokenProvider.validateToken(token)) {
                    authenticateUser(token, request);
                } else {
                    // Token present but invalid - log for security monitoring
                    logger.debug("Invalid JWT token in request from IP: {}", getClientIpAddress(request));
                }
            }

        } catch (Exception e) {
            // Log security-related errors but don't break the filter chain
            logger.error("JWT authentication filter error: {}", e.getMessage(), e);
            
            // Clear any partial authentication
            SecurityContextHolder.clearContext();
        }

        // Continue with filter chain regardless of authentication outcome
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from HTTP request Authorization header.
     * 
     * Looks for Bearer scheme in Authorization header and extracts the token.
     * Validates header format and token presence.
     * 
     * @param request HTTP request to examine
     * @return JWT token string, or null if not present/invalid
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        // Check for Authorization header presence
        if (!StringUtils.hasText(authorizationHeader)) {
            logger.trace("No Authorization header found in request");
            return null;
        }

        // Validate Bearer scheme
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            logger.debug("Authorization header does not use Bearer scheme: {}", 
                        authorizationHeader.substring(0, Math.min(10, authorizationHeader.length())));
            return null;
        }

        // Extract token after "Bearer " prefix
        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();

        if (!StringUtils.hasText(token)) {
            logger.debug("Empty token found after Bearer prefix");
            return null;
        }

        logger.trace("JWT token extracted from Authorization header");
        return token;
    }

    /**
     * Authenticates user based on validated JWT token.
     * 
     * This method handles the complete authentication process after token validation:
     * 1. Extract username from token
     * 2. Check if user is already authenticated in current request
     * 3. Load fresh user details from database
     * 4. Create authentication object with authorities
     * 5. Set authentication details and context
     * 
     * @param token validated JWT token
     * @param request HTTP request for authentication details
     */
    private void authenticateUser(String token, HttpServletRequest request) {
        try {
            // Extract username (email) from token
            String username = jwtTokenProvider.getUsernameFromToken(token);
            
            if (username == null || username.trim().isEmpty()) {
                logger.warn("JWT token contains no username/email claim");
                return;
            }

            // Check if user is already authenticated in current security context
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                logger.trace("User already authenticated in current security context: {}", username);
                return;
            }

            // Load user details from database
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (userDetails == null) {
                logger.warn("UserDetailsService returned null for username: {}", username);
                return;
            }

            // Additional validation: ensure token user matches loaded user
            if (!username.equals(userDetails.getUsername())) {
                logger.warn("Token username '{}' does not match loaded user '{}'", username, userDetails.getUsername());
                return;
            }

            // Create authentication token with user details and authorities
            UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(
                    userDetails, 
                    null,  // credentials not needed after authentication
                    userDetails.getAuthorities()
                );

            // Set authentication details (IP address, session ID, etc.)
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            // Set authentication in security context
            SecurityContextHolder.getContext().setAuthentication(authentication);

            logger.debug("JWT authentication successful for user: {} with authorities: {}", 
                        username, userDetails.getAuthorities());

        } catch (Exception e) {
            logger.error("Failed to authenticate user from JWT token: {}", e.getMessage(), e);
            SecurityContextHolder.clearContext();
        }
    }

    /**
     * Extracts client IP address from HTTP request.
     * 
     * Handles various proxy headers for accurate IP identification.
     * Used for security logging and monitoring.
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
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
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
     * Determines whether this filter should process the given request.
     * 
     * Currently, processes all requests, but can be customized to skip
     * certain paths or request types for performance optimization.
     * 
     * @param request HTTP request to evaluate
     * @return true if filter should process this request
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Process all requests by default
        // Can be customized to skip static resources, health checks, etc.
        String path = request.getRequestURI();
        
        // Skip filter for certain paths if needed (example)
        // return path.startsWith("/actuator/health") || 
        //        path.startsWith("/swagger-ui/") ||
        //        path.startsWith("/api-docs/");
        
        return false;
    }
}