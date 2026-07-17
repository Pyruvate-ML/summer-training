package com.mindbridge.backend.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Date;

public class JwtUtil {
    private static final String SECRET_KEY = "BZHANG_PING_SHI_FEN_80";
    private static final long EXPIRE_TIME = 24 * 60 * 60 * 1000;
    public static String generateToken(Long userId, String loginRole) {
        return JWT.create()
                .withClaim("userId", userId)
                .withClaim("role", loginRole)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRE_TIME))
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }
    public static DecodedJWT verifyToken(String token) {
        return JWT.require(Algorithm.HMAC256(SECRET_KEY)).build().verify(token);
    }
}