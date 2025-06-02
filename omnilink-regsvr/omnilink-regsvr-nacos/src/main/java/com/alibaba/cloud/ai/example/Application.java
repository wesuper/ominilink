package com.alibaba.cloud.ai.example;

import com.alibaba.cloud.ai.example.service.WeatherService;
// import org.springframework.ai.tool.ToolCallbackProvider; // Old API
// import org.springframework.ai.tool.method.MethodToolCallbackProvider; // Old API
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.context.annotation.Bean; // Tool registration might change

/**
 * @author yHong
 * @version 1.0
 * @since 2025/4/21 20:00
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // Commenting out old tool registration
    // @Bean
    // public ToolCallbackProvider weatherTools(WeatherService weatherService) {
    // return MethodToolCallbackProvider.builder().toolObjects(weatherService).build();
    // }

}
