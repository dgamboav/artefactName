package com.groupName.artefactName.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import io.jsonwebtoken.io.Decoders;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.io.Serial;
import java.io.Serializable;
import java.util.*;

@Component
public class JwtUtil implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String SECRET_KEY_BASE64 = "ZmlybWUtY29udHJhc2VuYS1qd3Qtc2VnYXJvLWRlLWZhbGxlLWxhdGV4"; // 🔐 reemplazar
    private static final long EXPIRATION_TIME_MS = 60L * 60 * 1000; // 1 hora

    private final SecretKey secretKey;

    public JwtUtil() {
        byte[] decodedKey = Decoders.BASE64.decode(SECRET_KEY_BASE64);
        this.secretKey = Keys.hmacShaKeyFor(decodedKey);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList(); // Java 16+: unmodifiable

        claims.put("authorities", authorities);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_TIME_MS);

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(now)
                .expiration(expiry)
                .claims(claims)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (SecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public List<String> extractAuthorities(String token) {
        Object raw = extractAllClaims(token).get("authorities");
        if (raw instanceof Collection<?> collection) {
            return collection.stream()
                    .map(Object::toString)
                    .toList();
        }
        return List.of();
    }

    private boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}