package org.wesuper.liteai.flywise.weather;

// import org.springframework.ai.tool.ToolCallback; // Old API
// import org.springframework.ai.tool.ToolCallbackProvider; // Old API
// import org.springframework.ai.tool.function.FunctionToolCallback; // Old API
// import org.springframework.ai.tool.method.MethodToolCallbackProvider; // Old API
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

// import org.springframework.context.annotation.Bean; // Tool registration might change
//import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioSpeechAutoConfiguration;
//import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
//import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
//import org.springframework.ai.model.openai.autoconfigure.OpenAiAudioTranscriptionAutoConfiguration;
//import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
//import org.springframework.ai.model.openai.autoconfigure.OpenAiModerationAutoConfiguration;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

// @SpringBootApplication(exclude = {
//     OpenAiAudioSpeechAutoConfiguration.class,
//     OpenAiChatAutoConfiguration.class,
//     OpenAiEmbeddingAutoConfiguration.class,
//     OpenAiAudioTranscriptionAutoConfiguration.class,
//     OpenAiImageAutoConfiguration.class,
//     OpenAiModerationAutoConfiguration.class
// })
@SpringBootApplication
public class McpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServerApplication.class, args);
    }

    @Bean
	public ToolCallbackProvider toolCallbackProvider(OpenMeteoService openMeteoService) {
		return MethodToolCallbackProvider.builder().toolObjects(openMeteoService).build();
	}

}
