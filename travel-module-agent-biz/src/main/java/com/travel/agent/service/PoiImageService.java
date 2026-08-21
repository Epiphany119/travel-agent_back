package com.travel.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * 地点/餐厅图片服务。
 *
 * <p>复用已有高德 key（gaode.api-key），用：
 *   1. place/text 按名称搜出 POI 的 id；
 *   2. place/detail 按 id 取出该地点的官方照片。
 * 全程容错：异常/无数据都返回空列表，绝不抛出。</p>
 */
@Slf4j
@Service
public class PoiImageService {

    private static final String TEXT_SEARCH_URL = "https://restapi.amap.com/v3/place/text";
    private static final String DETAIL_URL = "https://restapi.amap.com/v3/place/detail";
    private static final int MAX_IMAGES = 3;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${gaode.api-key:}")
    private String apiKey;

    public List<String> fetchImages(String name, String city) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("高德 api-key 未配置，无法获取地点图片");
            return List.of();
        }
        if (name == null || name.isBlank()) {
            return List.of();
        }
        try {
            String poiId = searchPoiId(name, city);
            if (poiId == null) {
                log.debug("未找到 POI: name={}, city={}", name, safe(city));
                return List.of();
            }
            return fetchDetailPhotos(poiId);
        } catch (Exception e) {
            log.warn("获取地点图片失败: name={}, err={}", name, e.getMessage());
            return List.of();
        }
    }

    private String searchPoiId(String keywords, String city) {
        String url = UriComponentsBuilder.fromHttpUrl(TEXT_SEARCH_URL)
                .queryParam("key", apiKey)
                .queryParam("keywords", keywords)
                .queryParam("city", safe(city))
                .queryParam("citylimit", true)
                .queryParam("offset", 1)
                .queryParam("page", 1)
                .build().toUriString();

        JsonNode root = restTemplate.getForObject(url, JsonNode.class);
        if (root == null || !"1".equals(root.path("status").asText())) {
            log.debug("高德文本搜索失败: name={}", keywords);
            return null;
        }
        JsonNode pois = root.path("pois");
        if (pois == null || !pois.isArray() || pois.isEmpty()) {
            return null;
        }
        String id = pois.get(0).path("id").asText(null);
        return (id == null || id.isBlank()) ? null : id;
    }

    private List<String> fetchDetailPhotos(String poiId) {
        String url = UriComponentsBuilder.fromHttpUrl(DETAIL_URL)
                .queryParam("key", apiKey)
                .queryParam("id", poiId)
                .build().toUriString();

        JsonNode root = restTemplate.getForObject(url, JsonNode.class);
        if (root == null || !"1".equals(root.path("status").asText())) {
            log.debug("高德详情接口失败: poiId={}", poiId);
            return List.of();
        }
        JsonNode pois = root.path("pois");
        if (pois == null || !pois.isArray() || pois.isEmpty()) {
            return List.of();
        }
        JsonNode photos = pois.get(0).path("photos");
        List<String> urls = new ArrayList<>();
        if (photos != null && photos.isArray()) {
            for (JsonNode p : photos) {
                String u = p.path("url").asText(null);
                if (u != null && !u.isBlank()) {
                    urls.add(u);
                    if (urls.size() >= MAX_IMAGES) break;
                }
            }
        }
        return urls;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}