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
        double dailyBudget = budget.isSuccess() ?
                budget.getTotalBudget() / days : request.getBudget() / days;

        LocalDate startDate = LocalDate.now().plusDays(1);

        for (int i = 0; i < days; i++) {
            DayPlan.DayPlanBuilder dayPlanBuilder = DayPlan.builder()
                    .day(i + 1)
                    .date(startDate.plusDays(i).format(DATE_FORMATTER))
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

    /**
     * 构建当日活动
     */
    private List<DayPlan.Activity> buildDailyActivities(int dayIndex,
                                                         int totalDays,
                                                         List<PoiResult> pois,
                                                         List<MealResult> meals,
                                                         TravelPlanRequest request) {
        List<DayPlan.Activity> activities = new ArrayList<>();
        int poiIndex = dayIndex;

        // 上午：景点1
        if (poiIndex < pois.size()) {
            PoiResult poi = pois.get(poiIndex);
            activities.add(DayPlan.Activity.builder()
                    .time("09:00")
                    .type("sightseeing")
                    .name(poi.getName())
                    .location(poi.getAddress())
                    .duration(120)
                    .cost(50)
                    .notes("建议提前购票")
                    .build());
        }

        // 午餐
        if (dayIndex < meals.size()) {
            MealResult meal = meals.get(dayIndex);
            activities.add(DayPlan.Activity.builder()
                    .time("12:00")
                    .type("meal")
                    .name(meal.getName())
                    .location(meal.getAddress())
                    .duration(90)
                    .cost(meal.getAvgPrice() != null ? meal.getAvgPrice() : 100)
                    .notes("人均消费约 " + (meal.getAvgPrice() != null ? meal.getAvgPrice() : 100) + " 元")
                    .build());
        }

        // 下午：景点2
        poiIndex = (dayIndex + totalDays) % Math.max(pois.size(), 1);
        if (poiIndex < pois.size()) {
            PoiResult poi = pois.get(poiIndex);
            activities.add(DayPlan.Activity.builder()
                    .time("14:00")
                    .type("sightseeing")
                    .name(poi.getName())
                    .location(poi.getAddress())
                    .duration(180)
                    .cost(50)
                    .notes("适合拍照打卡")
                    .build());
        }

        // 晚餐
        int dinnerIndex = (dayIndex + 1) % Math.max(meals.size(), 1);
        if (dinnerIndex < meals.size()) {
            MealResult meal = meals.get(dinnerIndex);
            activities.add(DayPlan.Activity.builder()
                    .time("18:00")
                    .type("meal")
                    .name(meal.getName())
                    .location(meal.getAddress())
                    .duration(90)
                    .cost(meal.getAvgPrice() != null ? meal.getAvgPrice() : 100)
                    .notes("特色" + (meal.getCuisine() != null ? meal.getCuisine() : "美食"))
                    .build());
        }

        // 晚上休息
        activities.add(DayPlan.Activity.builder()
                .time("20:00")
                .type("rest")
                .name("返回酒店休息")
                .location(request.getDestination())
                .duration(0)
                .cost(0)
                .notes("好好休息，明天继续探索")
                .build());

        return activities;
    }
}
