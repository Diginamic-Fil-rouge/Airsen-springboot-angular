package fr.airsen.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for managing JWT token blacklist using Redis.
 *
 * When a user logs out, their access token is added to the blacklist
 * with an expiration time matching the token's natural expiration.
 * This prevents the use of valid but revoked tokens.
 */
@Service
public class JwtBlacklistService {

    private static final Logger log = LoggerFactory.getLogger(JwtBlacklistService.class);
    private static final String BLACKLIST_KEY_PREFIX = "blacklist:jwt:";

    private final RedisTemplate<String, Object> redisTemplate;

    public JwtBlacklistService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Add token to blacklist with expiration matching JWT expiration.
     *
     * @param token the JWT token to blacklist
     * @param expirationTimeMs time in milliseconds until the token would naturally expire
     */
    public void blacklistToken(String token, long expirationTimeMs) {
        String key = BLACKLIST_KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, "revoked", expirationTimeMs, TimeUnit.MILLISECONDS);

        log.info("Token blacklisted: {}... (expires in {}ms)",
                 token.substring(0, Math.min(20, token.length())), expirationTimeMs);
    }

    /**
     * Check if token is blacklisted.
     *
     * @param token the JWT token to check
     * @return true if token is blacklisted, false otherwise
     */
    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_KEY_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        boolean isBlacklisted = Boolean.TRUE.equals(exists);

        if (isBlacklisted) {
            log.debug("Token is blacklisted: {}...", token.substring(0, Math.min(20, token.length())));
        }

        return isBlacklisted;
    }

    /**
     * Remove token from blacklist (admin only).
     *
     * @param token the JWT token to remove from blacklist
     */
    public void removeFromBlacklist(String token) {
        String key = BLACKLIST_KEY_PREFIX + token;
        redisTemplate.delete(key);

        log.info("Token removed from blacklist: {}...", token.substring(0, Math.min(20, token.length())));
    }

    /**
     * Get the number of blacklisted tokens.
     *
     * @return count of blacklisted tokens
     */
    public long getBlacklistCount() {
        var keys = redisTemplate.keys(BLACKLIST_KEY_PREFIX + "*");
        return keys != null ? keys.size() : 0;
    }
}
