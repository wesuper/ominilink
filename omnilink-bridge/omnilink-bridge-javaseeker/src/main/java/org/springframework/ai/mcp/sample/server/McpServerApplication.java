package org.springframework.ai.mcp.sample.server;

// import org.springframework.ai.tool.ToolCallbackProvider; // Old API
// import org.springframework.ai.tool.method.MethodToolCallbackProvider; // Old API
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// import org.springframework.context.annotation.Bean; // Bean for tools might be handled differently or auto-configured
//import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration;
//import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
//import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
//import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration;
//import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
//import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration;


//@SpringBootApplication(exclude = {
//    OpenAiAudioSpeechAutoConfiguration.class,
//    OpenAiChatAutoConfiguration.class,
//    OpenAiEmbeddingAutoConfiguration.class,
//    OpenAiAudioTranscriptionAutoConfiguration.class,
//    OpenAiImageAutoConfiguration.class,
//    OpenAiModerationAutoConfiguration.class
//})
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    // Spring AI 1.0.0 typically auto-registers @Service beans that implement java.util.function.Function as tools,
    // or tools are registered with ChatClient.Builder if a ChatClient is used.
    // Since this module is defining a tool for others to call, simply being a @Service might be enough
    // for frameworks that consume it if they use component scanning and look for @Description.
    // Commenting out this explicit ToolProvider bean as its API is from older milestones.
    // @Bean
    // public ToolCallbackProvider weatherTools(OpenMeteoService openMeteoService) {
    // return MethodToolCallbackProvider.builder().toolObjects(openMeteoService).build(); // Old API
    // }

}
