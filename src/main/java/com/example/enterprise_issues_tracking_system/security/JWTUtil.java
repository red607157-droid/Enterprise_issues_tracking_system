package com.example.enterprise_issues_tracking_system.security;

import com.example.enterprise_issues_tracking_system.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JWTUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long expiration;

    public SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();

    }

    public Long extractUserId(String token) {
        return Long.parseLong(extractClaims(token).getSubject());
    }

    public Date extractExpiration(String token) {
        return extractClaims(token).getExpiration();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            return extractClaims(token).getExpiration().after(new Date());
        } catch (JwtException e) {
            return false;
        }
    }
}
