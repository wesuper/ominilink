package org.wesuper.liteai.regsvr.nacos3client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.Scanner;

/**
 * @author yingzi
 * @date 2025/5/13:13:47
 */
@SpringBootApplication
public class NacosMcpSseClientApplication {


    public static void main(String[] args) {
        SpringApplication.run(NacosMcpSseClientApplication.class, args);
    }

    @Bean
    public CommandLineRunner predefinedQuestions(ChatClient.Builder chatClientBuilder, @Qualifier("loadbalancedMcpAsyncToolCallbacks") ToolCallbackProvider tools,
                                                 ConfigurableApplicationContext context) {

        return args -> {
            var chatClient = chatClientBuilder
                    .defaultToolCallbacks(tools.getToolCallbacks())
                    .build();

            System.out.print("\n>>> QUESTION: ");
            String userInput = "北京天气如何？";
//            System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());
//            Scanner scanner = new Scanner(System.in);
//            while (true) {
//                System.out.print("\n>>> QUESTION: ");
//                String userInput = "北京时间现在几点钟";
//                String userInput = scanner.nextLine();
//                if (userInput.equalsIgnoreCase("exit")) {
//                    break;
//                }
//                if (userInput.isEmpty()) {
//                    userInput = "北京时间现在几点钟";
//                }
//                System.out.println("\n>>> ASSISTANT: " + chatClient.prompt(userInput).call().content());
//            }
//            scanner.close();
            context.close();
        };
    }
}