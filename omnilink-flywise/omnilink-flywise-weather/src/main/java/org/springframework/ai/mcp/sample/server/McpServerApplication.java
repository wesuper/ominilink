package org.springframework.ai.mcp.sample.server;

// import org.springframework.ai.tool.ToolCallback; // Old API
// import org.springframework.ai.tool.ToolCallbackProvider; // Old API
// import org.springframework.ai.tool.function.FunctionToolCallback; // Old API
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
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    // Commenting out old tool registration
    // @Bean
    // public ToolCallbackProvider weatherTools(OpenMeteoService openMeteoService) {
    // return MethodToolCallbackProvider.builder().toolObjects(openMeteoService).build();
    // }

}
