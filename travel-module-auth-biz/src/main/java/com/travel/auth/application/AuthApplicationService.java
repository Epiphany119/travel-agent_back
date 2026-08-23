package com.travel.auth.application;

import com.travel.auth.api.dto.*;
import com.travel.auth.domain.PasswordPolicy;
import com.travel.auth.infrastructure.AuthAccountRepository;
import com.travel.auth.service.*;
import com.travel.common.core.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 认证应用服务 - 编排注册、登录、邮箱验证等业务流程
 */
@Slf4j
@Service
public class AuthApplicationService {

    private final AuthAccountRepository accounts;
    private final TokenService tokens;
    private final EmailVerificationService codes;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
    private final SecureRandom random = new SecureRandom();

    public AuthApplicationService(AuthAccountRepository accounts,
                                  TokenService tokens,
                                  EmailVerificationService codes) {
        this.accounts = accounts;
        this.tokens = tokens;
        this.codes = codes;
    }

    /** 用户注册 */
    @Transactional
    public AuthTokenResponse register(RegisterRequest request) {
        // 1. 校验用户名格式
        if (request.username() == null
                || !request.username().matches("^[A-Za-z0-9_]{3,32}$")) {
            throw new BusinessException(400, "用户名需为3-32位字母、数字或下划线");
        }

        // 2. 校验两次密码一致
        if (!request.password().equals(request.confirmPassword())) {
            throw new BusinessException(400, "两次输入的密码不一致");
        }

        // 3. 校验密码强度
        PasswordPolicy.validate(request.password());

        // 4. 检查唯一性
        if (accounts.existsByUsername(request.username())) {
            throw new BusinessException(409, "用户名已存在");
        }
        if (request.email() != null && !request.email().isBlank()
                && accounts.existsByEmail(request.email())) {
            throw new BusinessException(409, "邮箱已绑定其他账号");
        }

        // 5. 邮箱验证（如果提供了邮箱）
        if (request.email() != null && !request.email().isBlank()) {
            if (request.emailCode() == null
                    || !codes.verify(request.email(), request.emailCode())) {
                throw new BusinessException(400, "请先完成邮箱验证");
            }
        }

        // 6. 生成用户 ID 并入库
        String userId = String.format("%08d", random.nextInt(100_000_000));
        log.info("创建新用户: userId={}, username={}, email={}", userId, request.username(), request.email());
        accounts.insert(userId, request.username(),
                encoder.encode(request.password()), request.email());

        return issueToken(userId, request.username());
    }

    /** 密码登录 */
    public AuthTokenResponse login(LoginRequest request) {
        Map<String, Object> account = accounts.findByUsername(request.username())
                .orElseThrow(() -> new BusinessException(401, "用户名或密码错误"));

        // 账号锁定检查
        if (isLocked(account)) {
            throw new BusinessException(423, "账号已被锁定，请稍后再试");
        }

        // 密码验证
        if (!encoder.matches(request.password(), (String) account.get("password_hash"))) {
            accounts.markFailure(((Number) account.get("id")).longValue());
            throw new BusinessException(401, "用户名或密码错误");
        }

        accounts.markLogin(((Number) account.get("id")).longValue());
        return issueToken((String) account.get("user_id"), (String) account.get("username"));
    }

    /**
     * 邮箱验证码登录
     * 如果邮箱未注册，则自动创建新账号（用户名即邮箱）
     */
    @Transactional
    public AuthTokenResponse emailLogin(EmailLoginRequest request) {
        // 1. 验证验证码
        if (!codes.verify(request.email(), request.code())) {
            throw new BusinessException(401, "验证码错误或已过期");
        }

        // 2. 查找邮箱是否已注册
        Map<String, Object> account = accounts.findByEmail(request.email()).orElse(null);

        if (account != null) {
            // 已注册用户：直接登录
            accounts.markLogin(((Number) account.get("id")).longValue());
            return issueToken((String) account.get("user_id"),
                    (String) account.get("username"));
        }

        // 未注册：自动创建新账号
        String userId = String.format("%08d", random.nextInt(100_000_000));
        String tempPassword = String.format("Roamly@%d", random.nextInt(1_000_000));
        log.info("邮箱登录自动创建用户: userId={}, email={}", userId, request.email());
        accounts.insert(userId, request.email(),
                encoder.encode(tempPassword), request.email());

        return issueToken(userId, request.email());
    }

    /** 绑定邮箱（已登录用户） */
    @Transactional
    public void bindEmail(String userId, BindEmailRequest request) {
        log.info("绑定邮箱开始: userId={}, email={}", userId, request.email());

        // 1. 先检查邮箱是否已被其他账号绑定（在验证验证码之前检查，避免验证码被消耗后再失败）
        if (accounts.existsByEmail(request.email())) {
            log.warn("绑定邮箱失败: 邮箱已被占用, email={}", request.email());
            throw new BusinessException(409, "该邮箱已被其他账号绑定");
        }

        // 2. 验证验证码（这会消耗验证码）
        if (!codes.verify(request.email(), request.code())) {
            log.warn("绑定邮箱失败: 验证码错误或已过期, email={}", request.email());
            throw new BusinessException(400, "验证码错误或已过期");
        }

        // 3. 更新用户邮箱
        try {
            int updated = accounts.updateEmail(userId, request.email());
            if (updated == 0) {
                log.error("绑定邮箱失败: 未找到用户, userId={}", userId);
                throw new BusinessException(404, "用户不存在或已被删除");
            }
            log.info("绑定邮箱成功: userId={}, email={}", userId, request.email());
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("绑定邮箱异常: userId={}, email={}, error={}", userId, request.email(), e.getMessage(), e);
            throw new BusinessException(500, "绑定邮箱失败，请稍后重试");
        }
    }

    /** 解绑邮箱（已登录用户） */
    @Transactional
    public void unbindEmail(String userId) {
        log.info("解绑邮箱开始: userId={}", userId);
        int updated = accounts.unbindEmail(userId);
        if (updated == 0) {
            log.warn("解绑邮箱失败: 未找到用户, userId={}", userId);
            throw new BusinessException(404, "用户不存在或已被删除");
        }
        log.info("解绑邮箱成功: userId={}", userId);
    }

    /** 发送邮箱验证码 */
    public void sendCode(String email) {
        codes.send(email);
    }

    /** 登出 */
    public void logout(String token) {
        tokens.revoke(token);
    }

    /** 验证 Token */
    public String verify(String token) {
        return tokens.verify(token);
    }

    /** 签发 Token */
    private AuthTokenResponse issueToken(String userId, String username) {
        return new AuthTokenResponse(tokens.issue(userId), userId, username, 2_592_000);
    }

    /** 检查账号是否被锁定 */
    private boolean isLocked(Map<String, Object> account) {
        Object lockedUntil = account.get("locked_until");
        if (lockedUntil instanceof Timestamp) {
            return ((Timestamp) lockedUntil).toLocalDateTime().isAfter(LocalDateTime.now());
        }
        return false;
    }
}
