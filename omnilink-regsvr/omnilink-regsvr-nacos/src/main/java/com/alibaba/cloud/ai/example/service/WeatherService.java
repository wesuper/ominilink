package com.alibaba.cloud.ai.example.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/4/21 20:00
 */
@Service
public class WeatherService {
    @Tool(description = "Get weather information by city name")
    public String getWeather(String cityName) {
        return "Sunny in " + cityName;
    }

}
