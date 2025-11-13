package fr.airsen.api.security;

import fr.airsen.api.AbstractTestContainersTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

/**
 * Test utility to generate valid JWT tokens for integration tests
 */
@SpringBootTest
public class JwtTokenGeneratorTest extends AbstractTestContainersTest {

    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci1haXJzZW4tdGVzdGluZy1wdXJwb3Nlcy1vbmx5LW1pbmltdW0tMzItY2hhcmFjdGVycw==";
    private static final long JWT_EXPIRATION = 86400000; // 24 hours

    @Test
    public void generateTestToken() {
        String email = "sarah@airsen.fr";
        String role = "ADMIN";
        
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION);
        
        SecretKey key = Keys.hmacShaKeyFor(Base64.getDecoder().decode(TEST_SECRET));
        
        String token = Jwts.builder()
                .setSubject(email)
                .claim("email", email)
                .claim("role", role)
                .claim("type", "access")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
        
        System.out.println("=================================================");
        System.out.println("Generated JWT Token for Integration Tests:");
        System.out.println("=================================================");
        System.out.println(token);
        System.out.println("=================================================");
        System.out.println("Token details:");
        System.out.println("- Email: " + email);
        System.out.println("- Role: " + role);
        System.out.println("- Issued: " + now);
        System.out.println("- Expires: " + expiryDate);
        System.out.println("=================================================");
    }
}