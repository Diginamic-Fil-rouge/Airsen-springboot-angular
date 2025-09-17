package fr.airsen.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Token Filter for intercepting HTTP requests and validating JWT tokens.
 * 
 * This filter runs once per request and extracts JWT tokens from the
 * Authorization header. If a valid token is found, it establishes the
 * security context for the request, enabling Spring Security to authorize
 * access to protected endpoints.
 * 
 * Filter Chain Position:
 * - Runs before Spring Security's main authentication filters
 * - Processes all incoming HTTP requests
 * - Sets up authentication context for subsequent filters
 * 
 * Security Features:
 * - Bearer token extraction from Authorization header
 * - Token validation using JwtTokenProvider
 * - Security context establishment with user details
 * - Comprehensive error handling and logging
 * - Graceful handling of missing or invalid tokens
 */
@Component
public class JwtTokenFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenFilter.class);

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    /**
     * Filters incoming HTTP requests to extract and validate JWT tokens.
     * 
     * This method:
     * 1. Extracts the JWT token from the Authorization header
     * 2. Validates the token using JwtTokenProvider
     * 3. Creates a Spring Security authentication object
     * 4. Sets the security context for the current request
     * 5. Continues the filter chain
     * 
     * @param request HTTP request
     * @param response HTTP response
     * @param filterChain Spring Security filter chain
     * @throws ServletException if servlet processing fails
     * @throws IOException if I/O processing fails
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                  HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        try {
            String jwt = getJwtFromRequest(request);
            
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                String username = jwtTokenProvider.getUsernameFromToken(jwt);
                
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = createUserDetails(jwt, username);
                    
                    if (userDetails != null) {
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                userDetails, 
                                null, 
                                userDetails.getAuthorities()
                            );
                        
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        
                        logger.debug("Successfully authenticated user: {}", username);
                    }
                }
            } else if (StringUtils.hasText(jwt)) {
                // Token exists but is invalid
                logger.debug("Invalid JWT token in request");
            }
            
        } catch (Exception ex) {
            logger.error("Cannot set user authentication in security context", ex);
            // Don't throw exception, let the request continue without authentication
            // Spring Security will handle unauthorized access appropriately
        }
        
        filterChain.doFilter(request, response);
    }

    /**
     * Extracts JWT token from the Authorization header.
     * 
     * Expected header format: "Authorization: Bearer <jwt-token>"
     * 
     * @param request HTTP request
     * @return JWT token string without "Bearer " prefix, or null if not found
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        
        return null;
    }

    /**
     * Creates UserDetails object from JWT token information.
     * 
     * This method extracts user information from the JWT token and creates
     * a Spring Security UserDetails object that can be used for authorization.
     * 
     * @param jwt JWT token string
     * @param username Username extracted from token
     * @return UserDetails object with user information and authorities
     */
    private UserDetails createUserDetails(String jwt, String username) {
        try {
            // Extract role from token
            fr.airsen.api.entity.enums.UserRole userRole = jwtTokenProvider.getRoleFromToken(jwt);
            
            if (userRole == null) {
                logger.warn("No valid role found in JWT token for user: {}", username);
                return null;
            }
            
            // Create authority with ROLE_ prefix (Spring Security convention)
            SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + userRole.name());
            
            // Create UserDetails with minimal information needed for authorization
            return User.builder()
                    .username(username)
                    .password("") // Password not needed for JWT authentication
                    .authorities(Collections.singletonList(authority))
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(false)
                    .build();
                    
        } catch (Exception ex) {
            logger.error("Error creating user details from JWT token", ex);
            return null;
        }
    }

    /**
     * Determines if this filter should not be applied to the given request.
     * 
     * This implementation applies the filter to all requests. Specific
     * endpoints can be excluded from authentication in the Spring Security
     * configuration.
     * 
     * @param request HTTP request
     * @return false to apply filter to all requests
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Apply filter to all requests
        // Endpoint-specific access control is handled in SecurityConfig
        return false;
    }
}