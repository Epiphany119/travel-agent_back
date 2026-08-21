package com.travel.agent.controller;

import com.travel.agent.service.PoiImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 地点/餐厅图片接口
 *
 * <p>GET /api/poi/image?name=西湖&city=杭州
 * 返回该地点的高德官方图片 URL 列表，供行程卡展示（景点与餐厅通用）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/poi")
@RequiredArgsConstructor
public class PoiImageController {

    private final PoiImageService poiImageService;

    @GetMapping("/image")
    public Map<String, Object> image(@RequestParam String name,
                                     @RequestParam(required = false) String city) {
        List<String> urls = poiImageService.fetchImages(name, city);
        return Map.of(
                "name", name == null ? "" : name,
                "city", city == null ? "" : city,
                "imageUrls", urls);
    }
}