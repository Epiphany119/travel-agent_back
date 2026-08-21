package com.travel.a2a.service.subagent;

import com.travel.a2a.model.AgentResult;
import com.travel.a2a.model.PoiResult;
import com.travel.a2a.model.TravelPlanRequest;
import com.travel.mcp.client.McpSession;
import com.travel.mcp.protocol.dto.McpToolResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * POI子Agent
 */
@Slf4j
@Service
public class PoiSubAgent {

    private final McpSession poiSession;

    public PoiSubAgent(@Lazy @Qualifier("poiSession") McpSession poiSession) {
        this.poiSession = poiSession;
    }

    /**
     * 搜索景点
     *
     * @param request 行程请求
     * @return Agent执行结果
     */
    public AgentResult search(TravelPlanRequest request) {
        long startTime = System.currentTimeMillis();
        String destination = request.getDestination();
        List<String> interests = request.getInterests();

        log.info("PoiSubAgent: 搜索POI, destination={}, interests={}", destination, interests);

        try {
            // 构建搜索关键词
            String keywords = buildKeywords(interests);
            Map<String, Object> params = new HashMap<>();
            params.put("keywords", keywords);
            params.put("city", destination);
            params.put("offset", 20);

            McpToolResult result = poiSession.callTool("poi.search", params);

            long elapsedMs = System.currentTimeMillis() - startTime;

            if (result.success()) {
                List<PoiResult> pois = parsePoiResults(result.result());
                log.info("PoiSubAgent: 搜索POI成功, count={}, elapsedMs={}", pois.size(), elapsedMs);
                return AgentResult.success("poi", pois, elapsedMs);
            } else {
                log.warn("PoiSubAgent: 搜索POI失败, error={}", result.error());
                return AgentResult.failure("poi", result.error(), elapsedMs);
            }
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("PoiSubAgent: 搜索POI异常", e);
            return AgentResult.failure("poi", e.getMessage(), elapsedMs);
        }
    }

    /**
     * 搜索指定类型的POI
     *
     * @param city     城市
     * @param keywords 关键词
     * @param type     类型（景点/餐饮/购物等）
     * @return Agent执行结果
     */
    public AgentResult searchByType(String city, String keywords, String type) {
        long startTime = System.currentTimeMillis();
        log.info("PoiSubAgent: 按类型搜索POI, city={}, keywords={}, type={}", city, keywords, type);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("keywords", keywords);
            params.put("city", city);
            params.put("type", type);
            params.put("offset", 10);

            McpToolResult result = poiSession.callTool("poi.search", params);

            long elapsedMs = System.currentTimeMillis() - startTime;

            if (result.success()) {
                List<PoiResult> pois = parsePoiResults(result.result());
                return AgentResult.success("poi", pois, elapsedMs);
            } else {
                return AgentResult.failure("poi", result.error(), elapsedMs);
            }
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("PoiSubAgent: 按类型搜索POI异常", e);
            return AgentResult.failure("poi", e.getMessage(), elapsedMs);
        }
    }

    /**
     * 获取两点间距离
     *
     * @param origin      起点坐标
     * @param destination 终点坐标
     * @return Agent执行结果
     */
    public AgentResult getDistance(String origin, String destination) {
        long startTime = System.currentTimeMillis();
        log.info("PoiSubAgent: 获取距离, origin={}, destination={}", origin, destination);

        try {
            Map<String, Object> params = new HashMap<>();
            params.put("origin", origin);
            params.put("destination", destination);

            McpToolResult result = poiSession.callTool("poi.distance", params);

            long elapsedMs = System.currentTimeMillis() - startTime;

            if (result.success()) {
                return AgentResult.success("poi", result.result(), elapsedMs);
            } else {
                return AgentResult.failure("poi", result.error(), elapsedMs);
            }
        } catch (Exception e) {
            long elapsedMs = System.currentTimeMillis() - startTime;
            log.error("PoiSubAgent: 获取距离异常", e);
            return AgentResult.failure("poi", e.getMessage(), elapsedMs);
        }
    }

    /**
     * 构建搜索关键词
     */
    private String buildKeywords(List<String> interests) {
        if (interests == null || interests.isEmpty()) {
            return "景点";
        }
        // 支持逗号/空格混合分隔的字符串列表，如 ["人文,自然,美食"]
        List<String> flat = new ArrayList<>();
        for (String item : interests) {
            if (item != null && item.contains(",")) {
                for (String part : item.split(",")) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) flat.add(trimmed);
                }
            } else if (item != null && !item.trim().isEmpty()) {
                flat.add(item.trim());
            }
        }
        return flat.isEmpty() ? "景点" : String.join(" ", flat);
    }

    @SuppressWarnings("unchecked")
    private List<PoiResult> parsePoiResults(Object raw) {
        List<PoiResult> pois = new ArrayList<>();

        Object poisObj = extractField(raw, "pois");
        if (poisObj == null) {
            poisObj = extractListRecursive(raw);
        }
        if (poisObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    pois.add(PoiResult.builder()
                            .name(getStr(map, "name"))
                            .address(getStr(map, "address"))
                            .type(getStr(map, "type"))
                            .distance(getInt(map, "distance"))
                            .tel(getStr(map, "tel"))
                            .longitude(getDbl(map, "longitude"))
                            .latitude(getDbl(map, "latitude"))
                            .build());
                }
            }
        }
        return pois;
    }

    @SuppressWarnings("unchecked")
    private Object extractField(Object obj, String field) {
        if (!(obj instanceof Map<?, ?> m)) return null;
        Object val = m.get(field);
        if (val != null) return val;
        Object inner = m.get("result");
        if (inner instanceof Map<?, ?> m2) {
            val = m2.get(field);
            if (val instanceof List<?>) return val;
            Object inner2 = m2.get("result");
            if (inner2 instanceof Map<?, ?> m3) {
                val = m3.get(field);
                if (val instanceof List<?>) return val;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<?> extractListRecursive(Object obj) {
        if (obj instanceof List<?> l && !l.isEmpty()) return l;
        if (obj instanceof Map<?, ?> m) {
            for (Object v : m.values()) {
                List<?> found = extractListRecursive(v);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static String getStr(Map<?, ?> m, String k) {
        Object v = m.get(k);
        return v != null ? v.toString() : null;
    }

    private static Integer getInt(Map<?, ?> m, String k) {
        Object v = m.get(k);
        return v instanceof Number n ? n.intValue() : null;
    }

    private static Double getDbl(Map<?, ?> m, String k) {
        Object v = m.get(k);
        return v instanceof Number n ? n.doubleValue() : null;
    }
}
