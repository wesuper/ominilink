package org.wesuper.liteai.regsvr.nacos3.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

/**
 * @author yHong
 * @version 1.0
 * @since 2025/4/21 20:00
 */
@Service
public class WeatherService {
    @Tool(description = "Get weather information by city name") // Updated annotation
    public String getWeather(String cityName) {
        return "Sunny in " + cityName;
    }

}
