package com.alibaba.cloud.ai.example.service;

// import org.springframework.ai.tool.annotation.Tool; // Old API
import org.springframework.context.annotation.Description; // New API for tool description
import org.springframework.stereotype.Service;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/4/21 20:00
 */
@Service
public class WeatherService {
    @Description("Get weather information by city name") // Updated annotation
    public String getWeather(String cityName) {
        return "Sunny in " + cityName;
    }

}
