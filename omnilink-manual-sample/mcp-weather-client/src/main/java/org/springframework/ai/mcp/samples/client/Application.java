package org.springframework.ai.mcp.samples.client;

import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.tool.ToolCallbackProvider; // Old API
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

// Removed exclude related to MCP
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // 直接硬编码中文问题，避免配置文件编码问题
    // @Value("${ai.user.input}")
    // private String userInput;
    private String userInput = "北京的天气如何？";

    @Bean
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder,
            /* ToolCallbackProvider tools, */ ConfigurableApplicationContext context) { // Tools parameter removed

        return args -> {

            var chatClient = chatClientBuilder
                    // .defaultTools(tools) // Tool registration has changed
                    .build();

            System.out.println("\n>>> QUESTION: " + userInput);
            System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());

            context.close();
        };
    }
}