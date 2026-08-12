package com.travel.common.core.exception;

import com.travel.common.core.result.ApiResult;
import com.travel.common.exception.RateLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理
 *
 * <p>PR#1 调整：新增 {@link RateLimitException} 的 {@code @ExceptionHandler}，
 * 统一返回 HTTP 429 + 业务码 429，消息体使用异常的 {@link RateLimitException#getResetHint()}
 * 帮助客户端理解恢复时间。原 {@code RateLimitException} 包路径已迁移到
 * {@code com.travel.common.exception}。</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<Void>> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResult.error(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(RateLimitException.class)
    public ResponseEntity<ApiResult<Void>> handleRateLimitException(RateLimitException ex) {
        log.warn("限流异常: service={}, count={}/{}, reset={}",
                ex.getServiceName(), ex.getCurrentCount(), ex.getLimit(), ex.getResetHint());
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(ApiResult.error(429, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<Void>> handleException(Exception ex) {
        log.error("系统异常", ex);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResult.error(500, "系统异常: " + ex.getMessage()));
    }
}