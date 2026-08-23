package com.travel.auth.service;

import com.travel.auth.infrastructure.AuthSessionRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Token 服务 - 签发、验证、吊销会话 Token
 */
@Service
public class TokenService {

    private final AuthSessionRepository sessions;
    private final SecureRandom random = new SecureRandom();

    public TokenService(AuthSessionRepository sessions) {
        this.sessions = sessions;
    }

    /** 签发新 Token，有效期 30 天 */
    public String issue(String userId) {
        String token = UUID.randomUUID() + "." + UUID.randomUUID() + "." + random.nextLong();
        sessions.create(hash(token), userId, 30);
        return token;
    }

    /** 吊销 Token */
    public void revoke(String token) {
        if (token != null) {
            sessions.revoke(hash(token));
        }
    }

    /** 验证 Token，返回用户 ID；无效返回 null */
    public String verify(String token) {
        if (token == null) {
            return null;
        }
        return sessions.findUserId(hash(token)).orElse(null);
    }

    /** SHA-256 哈希 */
    public static String hash(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
