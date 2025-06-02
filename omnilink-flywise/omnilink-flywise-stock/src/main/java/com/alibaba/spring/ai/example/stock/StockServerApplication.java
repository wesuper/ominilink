package com.alibaba.spring.ai.example.stock;

import com.alibaba.spring.ai.example.stock.service.StockService;
// import org.springframework.ai.tool.ToolCallbackProvider; // Old API
// import org.springframework.ai.tool.method.MethodToolCallbackProvider; // Old API
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.context.annotation.Bean; // Tool registration might change
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration;

@SpringBootApplication(exclude = {
    OpenAiAudioSpeechAutoConfiguration.class,
    OpenAiChatAutoConfiguration.class,
    OpenAiEmbeddingAutoConfiguration.class,
    OpenAiAudioTranscriptionAutoConfiguration.class,
    OpenAiImageAutoConfiguration.class,
    OpenAiModerationAutoConfiguration.class
})
public class StockServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(StockServerApplication.class, args);
    }

    // In Spring AI 1.0.0, Functions are typically auto-discovered if they are @Bean
    // and their class is a java.util.function.Function, or via @Service component scan
    // and then registered with ChatClient.Builder or ChatModel.
    // Commenting out this old way of providing tools.
    // @Bean
    // public ToolCallbackProvider stockTools(StockService stockService) {
    // return MethodToolCallbackProvider.builder().toolObjects(stockService).build();
    // }
}
