package com.travel.auth.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 会话仓储 - 基于 JdbcTemplate 的 Token 会话管理
 */
@Repository
public class AuthSessionRepository {

    private final JdbcTemplate jdbc;

    public AuthSessionRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 创建新会话，days 为有效期天数 */
    public void create(String tokenHash, String userId, long days) {
        jdbc.update(
                "insert into auth_session(token_hash, user_id, expires_at) " +
                        "values(?, ?, date_add(now(), interval ? day))",
                tokenHash, userId, days);
    }

    /** 吊销会话 */
    public void revoke(String tokenHash) {
        jdbc.update(
                "update auth_session set revoked_at = now() where token_hash = ?",
                tokenHash);
    }

    /** 根据 Token Hash 查找有效会话对应的用户 ID */
    public Optional<String> findUserId(String tokenHash) {
        return jdbc.queryForList(
                        "select user_id from auth_session " +
                                "where token_hash = ? and revoked_at is null and expires_at > now()",
                        tokenHash)
                .stream()
                .map(row -> (String) row.get("user_id"))
                .findFirst();
    }
}
