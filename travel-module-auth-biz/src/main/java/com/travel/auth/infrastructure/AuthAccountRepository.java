package com.travel.auth.infrastructure;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

/**
 * 账号仓储 - 基于 JdbcTemplate 的账号数据操作
 */
@Slf4j
@Repository
public class AuthAccountRepository {

    private final JdbcTemplate jdbc;

    public AuthAccountRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<Map<String, Object>> findByUsername(String username) {
        return jdbc.queryForList(
                        "select * from auth_account where username = ?", username)
                .stream().findFirst();
    }

    public Optional<Map<String, Object>> findByEmail(String email) {
        return jdbc.queryForList(
                        "select * from auth_account where email = ? and status = 'active'", email)
                .stream().findFirst();
    }

    public boolean existsByUsername(String username) {
        Integer count = jdbc.queryForObject(
                "select count(*) from auth_account where username = ?",
                Integer.class, username);
        return count != null && count > 0;
    }

    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        Integer count = jdbc.queryForObject(
                "select count(*) from auth_account where email = ?",
                Integer.class, email);
        return count != null && count > 0;
    }

    public void insert(String userId, String username, String passwordHash, String email) {
        jdbc.update(
                "insert into auth_account(user_id, username, password_hash, email) values(?,?,?,?)",
                userId, username, passwordHash, email);
    }

    /** 更新用户邮箱，返回影响的行数 */
    public int updateEmail(String userId, String email) {
        int updated = jdbc.update(
                "update auth_account set email = ? where user_id = ?",
                email, userId);
        log.info("updateEmail: userId={}, email={}, updated={}", userId, email, updated);
        return updated;
    }

    /** 解绑邮箱（置空），返回影响的行数 */
    public int unbindEmail(String userId) {
        int updated = jdbc.update(
                "update auth_account set email = null where user_id = ?",
                userId);
        log.info("unbindEmail: userId={}, updated={}", userId, updated);
        return updated;
    }

    public void markLogin(long id) {
        jdbc.update(
                "update auth_account set failed_attempts = 0, locked_until = null, last_login_at = now() where id = ?",
                id);
    }

    public void markFailure(long id) {
        jdbc.update(
                "update auth_account " +
                        "set failed_attempts = failed_attempts + 1, " +
                        "locked_until = case when failed_attempts + 1 >= 5 " +
                        "then date_add(now(), interval 15 minute) else locked_until end " +
                        "where id = ?",
                id);
    }
}
