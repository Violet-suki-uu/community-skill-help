package com.rita.community.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class JwtUtil {

    // 先写死，后面可以挪到 application.yml
    private static final String SECRET = "replace-this-with-a-long-secret-key-32bytes+";
    private static final long EXPIRE_MS = 7L * 24 * 60 * 60 * 1000; // 7天

    public static String generateToken(Long userId, String role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRE_MS);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 从 token 里取 userId
    public static Long getUserId(String token) {
        return Long.valueOf(parse(token).getSubject());
    }
}
