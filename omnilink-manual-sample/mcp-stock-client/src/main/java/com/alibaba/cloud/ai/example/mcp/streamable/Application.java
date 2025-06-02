package com.alibaba.cloud.ai.example.mcp.streamable;

import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.tool.ToolCallbackProvider; // Old API, tools are registered differently
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.ComponentScan; // No longer needed for MCP
// import org.springframework.stereotype.Component; // Not used directly

// Removed exclude and ComponentScan related to MCP
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    // 直接硬编码中文问题，避免配置文件编码问题
    // @Value("${ai.user.input}")
    // private String userInput;
    private String userInput = "阿里巴巴西溪园区";

    @Bean
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder,
            /* ToolCallbackProvider tools, */ ConfigurableApplicationContext context) { // Tools parameter removed

        return args -> {

            var chatClient = chatClientBuilder
                    // .defaultTools(tools) // Tool registration has changed
                    .build();

            System.out.println("\n>>> QUESTION: " + userInput);
            System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());

            System.out.println("\n>>> QUESTION: " + "黄金价格走势");
            System.out.println("\n>>> ASSISTANT: " + chatClient.prompt("黄金价格走势").call().content());

//            context.close();
        };
    }
}