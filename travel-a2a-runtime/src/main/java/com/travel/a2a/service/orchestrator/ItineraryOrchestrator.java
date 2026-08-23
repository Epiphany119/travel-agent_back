package com.travel.a2a.service.orchestrator;

import com.travel.a2a.model.*;
import com.travel.a2a.service.subagent.BudgetSubAgent;
import com.travel.a2a.service.subagent.MealSubAgent;
import com.travel.a2a.service.subagent.PoiSubAgent;
import com.travel.a2a.service.subagent.WeatherSubAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * 行程编排器
 * 
 * <p>负责并行调用 4 个子 Agent（weather/poi/meal/budget），
 * 收集结果并编排每日行程。支持 30s 超时降级。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ItineraryOrchestrator {

    private static final int TIMEOUT_SECONDS = 30;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final WeatherSubAgent weatherSubAgent;
    private final PoiSubAgent poiSubAgent;
    private final MealSubAgent mealSubAgent;
    private final BudgetSubAgent budgetSubAgent;

    /**
     * 执行行程编排
     *
     * @param request 行程请求
     * @return 行程结果
     */
    public TravelPlanResult orchestrate(TravelPlanRequest request) {
        log.info("ItineraryOrchestrator: 开始编排行程, destination={}, days={}",
                request.getDestination(), request.getDays());

        ExecutorService executor = Executors.newFixedThreadPool(4);

        try {
            // 阶段1：并行调用 4 个子 Agent
            CompletableFuture<AgentResult> weatherFuture = CompletableFuture.supplyAsync(
                    () -> weatherSubAgent.getWeather(request.getDestination()), executor);
            CompletableFuture<AgentResult> poiFuture = CompletableFuture.supplyAsync(
                    () -> poiSubAgent.search(request), executor);
            CompletableFuture<AgentResult> mealFuture = CompletableFuture.supplyAsync(
                    () -> mealSubAgent.search(request), executor);
            CompletableFuture<AgentResult> budgetFuture = CompletableFuture.supplyAsync(
                    () -> budgetSubAgent.estimate(request), executor);

            // 使用 allOf 等待所有结果，带超时降级
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    weatherFuture, poiFuture, mealFuture, budgetFuture);

            try {
                allFutures.orTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS).join();
            } catch (Exception e) {
                log.warn("ItineraryOrchestrator: 子 Agent 并行调用超时或异常: {}", e.getMessage());
            }

            // 收集结果和警告
            List<DataWarning> warnings = new ArrayList<>();
            WeatherResult weather = collectWeather(weatherFuture, warnings);
            List<PoiResult> pois = collectPois(poiFuture, warnings);
            List<MealResult> meals = collectMeals(mealFuture, warnings);
            BudgetEstimate budget = collectBudget(budgetFuture, warnings);

            // 编排每日行程
            List<DayPlan> dayPlans = buildDayPlans(request, weather, pois, meals, budget);

            log.info("ItineraryOrchestrator: 编排完成, dayPlans={}, warnings={}",
                    dayPlans.size(), warnings.size());

            return TravelPlanResult.builder()
                    .success(true)
                    .destination(request.getDestination())
                    .days(request.getDays())
                    .travelers(request.getTravelers())
                    .travelStyle(request.getTravelStyle())
                    .interests(request.getInterests())
                    .strategyNotes(List.of("按天气调整户外与室内比例", "按区域串联地点，减少折返", "根据偏好平衡餐饮与景点预算"))
                    .dayPlans(dayPlans)
                    .dataWarnings(warnings)
                    .weather(weather)
                    .pois(pois)
                    .meals(meals)
                    .budget(budget)
                    .build();

        } finally {
            executor.shutdown();
        }
    }

    /**
     * 收集天气结果
     */
    private WeatherResult collectWeather(CompletableFuture<AgentResult> future,
                                          List<DataWarning> warnings) {
        try {
            if (future.isDone() && !future.isCompletedExceptionally()) {
                AgentResult result = future.get();
                if (result.isSuccess() && result.getData() instanceof WeatherResult) {
                    return (WeatherResult) result.getData();
                } else if (!result.isSuccess()) {
                    warnings.add(DataWarning.builder()
                            .source("weather")
                            .message(result.getError())
                            .elapsedMs(result.getElapsedMs())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("ItineraryOrchestrator: 收集天气结果异常: {}", e.getMessage());
            warnings.add(DataWarning.builder()
                    .source("weather")
                    .message("超时或异常: " + e.getMessage())
                    .elapsedMs(TIMEOUT_SECONDS * 1000L)
                    .build());
        }

        // 返回默认天气结果
        return WeatherResult.builder()
                .success(false)
                .city("未知")
                .build();
    }

    /**
     * 收集 POI 结果
     */
    @SuppressWarnings("unchecked")
    private List<PoiResult> collectPois(CompletableFuture<AgentResult> future,
                                        List<DataWarning> warnings) {
        try {
            if (future.isDone() && !future.isCompletedExceptionally()) {
                AgentResult result = future.get();
                if (result.isSuccess() && result.getData() instanceof List) {
                    return (List<PoiResult>) result.getData();
                } else if (!result.isSuccess()) {
                    warnings.add(DataWarning.builder()
                            .source("poi")
                            .message(result.getError())
                            .elapsedMs(result.getElapsedMs())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("ItineraryOrchestrator: 收集 POI 结果异常: {}", e.getMessage());
            warnings.add(DataWarning.builder()
                    .source("poi")
                    .message("超时或异常: " + e.getMessage())
                    .elapsedMs(TIMEOUT_SECONDS * 1000L)
                    .build());
        }

        return new ArrayList<>();
    }

    /**
     * 收集餐饮结果
     */
    @SuppressWarnings("unchecked")
    private List<MealResult> collectMeals(CompletableFuture<AgentResult> future,
                                          List<DataWarning> warnings) {
        try {
            if (future.isDone() && !future.isCompletedExceptionally()) {
                AgentResult result = future.get();
                if (result.isSuccess() && result.getData() instanceof List) {
                    return (List<MealResult>) result.getData();
                } else if (!result.isSuccess()) {
                    warnings.add(DataWarning.builder()
                            .source("meal")
                            .message(result.getError())
                            .elapsedMs(result.getElapsedMs())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("ItineraryOrchestrator: 收集餐饮结果异常: {}", e.getMessage());
            warnings.add(DataWarning.builder()
                    .source("meal")
                    .message("超时或异常: " + e.getMessage())
                    .elapsedMs(TIMEOUT_SECONDS * 1000L)
                    .build());
        }

        return new ArrayList<>();
    }

    /**
     * 收集预算结果
     */
    private BudgetEstimate collectBudget(CompletableFuture<AgentResult> future,
                                          List<DataWarning> warnings) {
        try {
            if (future.isDone() && !future.isCompletedExceptionally()) {
                AgentResult result = future.get();
                if (result.isSuccess() && result.getData() instanceof BudgetEstimate) {
                    return (BudgetEstimate) result.getData();
                } else if (!result.isSuccess()) {
                    warnings.add(DataWarning.builder()
                            .source("budget")
                            .message(result.getError())
                            .elapsedMs(result.getElapsedMs())
                            .build());
                }
            }
        } catch (Exception e) {
            log.warn("ItineraryOrchestrator: 收集预算结果异常: {}", e.getMessage());
            warnings.add(DataWarning.builder()
                    .source("budget")
                    .message("超时或异常: " + e.getMessage())
                    .elapsedMs(TIMEOUT_SECONDS * 1000L)
                    .build());
        }

        // 返回默认预算结果
        return BudgetEstimate.builder()
                .success(false)
                .totalBudget(0)
                .perPersonBudget(0)
                .breakdown(new ArrayList<>())
                .build();
    }

    /**
     * 构建每日行程
     */
    private List<DayPlan> buildDayPlans(TravelPlanRequest request,
                                         WeatherResult weather,
                                         List<PoiResult> pois,
                                         List<MealResult> meals,
                                         BudgetEstimate budget) {
        List<DayPlan> dayPlans = new ArrayList<>();
        int days = request.getDays();
        double totalBudget = budget.isSuccess() && budget.getTotalBudget() > 0 ? budget.getTotalBudget() : request.getBudget();
        double[] weights = {0.82, 1.08, 1.22, 0.94, 1.16};
        double weightTotal = 0;
        for (int i = 0; i < days; i++) weightTotal += weights[Math.min(i, weights.length - 1)];

        LocalDate startDate = LocalDate.now().plusDays(1);

        for (int i = 0; i < days; i++) {
            double dailyBudget = Math.round(totalBudget * weights[Math.min(i, weights.length - 1)] / weightTotal);
            DayPlan.DayPlanBuilder dayPlanBuilder = DayPlan.builder()
                    .day(i + 1)
                    .date(startDate.plusDays(i).format(DATE_FORMATTER))
                    .theme(themeFor(request, i))
                    .dailyBudget(dailyBudget);

            // 设置天气信息
            if (weather.isSuccess() && weather.getData() != null) {
                dayPlanBuilder.weather("多云")
                        .temperature("20-28℃");
            }

            // 编排当日活动
            List<DayPlan.Activity> activities = buildDailyActivities(
                    i, days, pois, meals, request);
            dayPlanBuilder.activities(activities);

            dayPlans.add(dayPlanBuilder.build());
        }

        return dayPlans;
    }

    private String themeFor(TravelPlanRequest request, int day) {
        String style = request.getTravelStyle();
        if (style == null || style.isBlank()) style = "轻松漫游";
        String[] themes = {"城市初见与核心地标", "在地文化与美食探索", "慢节奏收束与自由漫游"};
        return themes[Math.min(day, themes.length - 1)] + " · " + style;
    }

    /**
     * 构建当日活动
     * <p>优先用去重后的景点/餐厅，每天配 2 个不同景点 + 2 个不同餐厅，
     * 并按天错开索引，避免同一天/相邻天重复同一地点导致图片重复。</p>
     */
    private List<DayPlan.Activity> buildDailyActivities(int dayIndex,
                                                         int totalDays,
                                                         List<PoiResult> pois,
                                                         List<MealResult> meals,
                                                         TravelPlanRequest request) {
        List<DayPlan.Activity> activities = new ArrayList<>();

        List<PoiResult> uniqPois = distinctByName(pois);
        List<MealResult> uniqMeals = distinctByName(meals);

        // 上午：景点1（每天取不同的索引，错开重复）
        PoiResult poi1 = pick(uniqPois, dayIndex * 2);
        if (poi1 != null) {
            activities.add(activity("09:00", "sightseeing", poi1.getName(), poi1.getAddress(),
                    150, 45, "景点 · 建议上午前往，错峰游览"));
        }

        // 午餐
        MealResult lunch = pick(uniqMeals, dayIndex * 2);
        if (lunch != null) {
            activities.add(activity("12:00", "meal", lunch.getName(), lunch.getAddress(),
                    90, 80, "午餐 · 当地特色美食"));
        }

        // 下午：景点2（与上午不同）
        PoiResult poi2 = pick(uniqPois, dayIndex * 2 + 1);
        if (poi2 != null && !sameName(poi1, poi2)) {
            activities.add(activity("14:00", "sightseeing", poi2.getName(), poi2.getAddress(),
                    150, 45, "景点 · 下午光线好，适合游览打卡"));
        }

        // 晚餐
        MealResult dinner = pick(uniqMeals, dayIndex * 2 + 1);
        if (dinner != null && !sameName(lunch, dinner)) {
            activities.add(activity("18:00", "meal", dinner.getName(), dinner.getAddress(),
                    90, 90, "晚餐 · 结束一天的行程"));
        }

        // 晚上休息
        activities.add(activity("20:00", "rest", "返回酒店休息", request.getDestination(),
                0, 0, "好好休息，明天继续探索"));

        return activities;
    }

    /** 按名称去重（名称相同视为同一点，避免重复地点/重复图片） */
    private <T> List<T> distinctByName(List<T> list) {
        if (list == null) return new ArrayList<>();
        List<T> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (T item : list) {
            String n = poiName(item);
            if (n == null || n.isBlank() || seen.add(n)) out.add(item);
        }
        return out;
    }

    /** 取列表第 idx 个元素；不循环复用，避免跨天重复地点。 */
    private <T> T pick(List<T> list, int idx) {
        if (list == null || list.isEmpty()) return null;
        return idx >= 0 && idx < list.size() ? list.get(idx) : null;
    }

    private <T> String poiName(T item) {
        if (item instanceof PoiResult p) return p.getName();
        if (item instanceof MealResult m) return m.getName();
        return null;
    }

    private <T> boolean sameName(T a, T b) {
        if (a == null || b == null) return false;
        String na = poiName(a), nb = poiName(b);
        return na != null && na.equals(nb);
    }

    private DayPlan.Activity activity(String time, String type, String name, String loc,
                                      int dur, int cost, String note) {
        return DayPlan.Activity.builder()
                .time(time)
                .type(type)
                .name(name == null ? "" : name)
                .location(loc == null ? "" : loc)
                .transport(type.equals("rest") ? "返回酒店" : "公共交通 / 步行优先")
                .duration(dur)
                .cost(cost)
                .notes(note)
                .build();
    }
}
