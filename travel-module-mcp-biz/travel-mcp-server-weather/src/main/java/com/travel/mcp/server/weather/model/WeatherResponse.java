package com.travel.mcp.server.weather.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponse {
    private boolean success;
    private String city;
    private String message;
    private List<DailyWeather> forecast;

    public static WeatherResponse fallback(String message) {
        return WeatherResponse.builder()
                .success(false)
                .message(message)
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyWeather {
        private String date;
        private Integer tempMin;
        private Integer tempMax;
        private String text;
        private Integer humidity;
        private Double precip;
        private Integer uvIndex;
        private String wind;
    }
}
