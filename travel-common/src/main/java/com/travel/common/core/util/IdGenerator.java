package com.travel.common.core.util;

import cn.hutool.core.util.IdUtil;

/**
 * ID生成工具
 */
public class IdGenerator {

    /**
     * 生成简单UUID (不含横杠)
     */
    public static String simpleUUID() {
        return IdUtil.simpleUUID();
    }

    /**
     * 生成会话ID
     */
    public static String generateSessionId() {
        return "session-" + IdUtil.fastSimpleUUID();
    }

    /**
     * 生成消息ID
     */
    public static String generateMessageId() {
        return "msg-" + IdUtil.fastSimpleUUID();
    }
}
