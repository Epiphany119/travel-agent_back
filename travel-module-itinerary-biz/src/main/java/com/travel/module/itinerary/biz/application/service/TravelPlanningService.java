package com.travel.module.itinerary.biz.application.service;

import com.travel.module.itinerary.biz.api.dto.*;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Deterministic planning fallback. It deliberately produces transparent, editable suggestions
 * while external maps/weather/LLM tools are unavailable, so the UI is never blocked by a model call.
 */
@Service
public class TravelPlanningService {
    private static final List<String> THEMES = List.of("城市初见与地标漫游", "人文街区与在地生活", "自然风光与慢旅行", "美食探索与夜色体验");

    public TravelPlanResponse generate(GenerateItineraryRequest request) {
        String city = request.getDestination().trim();
        int days = request.getDays();
        double perDay = Math.floor(request.getBudget() / days);
        List<DayPlanResponse> plans = new ArrayList<>();
        for (int i = 0; i < days; i++) plans.add(day(city, i + 1, perDay, request.getTravelStyle()));
        double estimate = Math.round(request.getBudget() * 0.86 * 100.0) / 100.0;
        return TravelPlanResponse.builder()
                .planId("TP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .destination(city).days(days).totalBudget(request.getBudget()).estimatedCost(estimate)
                .budgetStatus(estimate <= request.getBudget() ? "预算充足，预留了约 " + Math.round(request.getBudget() - estimate) + " 元弹性" : "建议上调预算或减少付费体验")
                .overview(city + " · " + days + " 天「" + request.getTravelStyle() + "」路线，按区域串联安排，减少折返与无效通勤。")
                .dayPlans(plans)
                .travelTips(List.of("热门场馆、演出与交通票请提前在官方渠道预约。", "同一区域的点位集中安排；每天留出 60–90 分钟机动时间。", "餐厅评分仅作筛选参考，到店前请核对营业时间、排队和过敏原信息。"))
                .packingList(List.of("身份证件与充电宝", "舒适步行鞋", "薄外套 / 雨具", "常用药与防晒用品"))
                .build();
    }

    private DayPlanResponse day(String city, int number, double budget, String style) {
        String theme = THEMES.get((number - 1) % THEMES.size());
        List<AttractionResponse> attractions = List.of(
                AttractionResponse.builder().name(city + "城市地标 / 核心景区").description("建议选择官方开放的代表性景点，提前预约并避开午间高峰。").duration(2.5).ticketPrice(0D).openTime("以官方公告为准").address("核心城区").rating(4.7).visitOrder(1).build(),
                AttractionResponse.builder().name(city + "特色街区与文化空间").description("步行串联周边小店、展馆或公园，按现场情况灵活取舍。").duration(2D).ticketPrice(0D).openTime("以商家公告为准").address("同区域步行范围").rating(4.6).visitOrder(2).build());
        List<MealResponse> meals = List.of(
                MealResponse.builder().mealType("午餐").restaurantName("当地口碑餐馆（到店前确认）").cuisine("本地特色").avgPrice((double) Math.round(budget * .12)).address("景区周边 1 公里内").reason("减少跨区通勤，优先选择明码标价、近期评价稳定的店铺。").rating(4.5).build(),
                MealResponse.builder().mealType("晚餐").restaurantName("夜市 / 社区餐馆（到店前确认）").cuisine("当地风味").avgPrice((double) Math.round(budget * .16)).address("住宿或夜游区域附近").reason("避开网红排队店，预留替代选项。").rating(4.5).build());
        return DayPlanResponse.builder().dayNumber(number).date("第 " + number + " 天").theme(theme + " · " + style)
                .attractions(attractions).meals(meals).transportation("优先步行 + 公共交通；跨区出行建议避开早晚高峰。")
                .notes("门票、营业时间与天气会变化，请在出发前通过官方渠道复核。")
                .dayBudget(budget).build();
    }
}
