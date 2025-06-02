package org.springframework.ai.mcp.samples.github; // Corrected package

// import java.nio.file.Paths; // Unused
// import java.time.Duration; // Unused

import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.tool.ToolCallbackProvider; // Old API
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
// import org.springframework.util.StringUtils; // Unused

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public CommandLineRunner predefinedQuestions(
			ChatClient.Builder chatClientBuilder,
			/* ToolCallbackProvider tools, */ // Tools parameter removed
			ConfigurableApplicationContext context) {
		return args -> {
			// 构建ChatClient并注入MCP工具
			var chatClient = chatClientBuilder
					// .defaultTools(tools) // Tool registration has changed
					.build();

			// 定义用户输入
			String userInput = "帮我创建一个私有仓库，命名为test-mcp";
			// 打印问题
			System.out.println("\n>>> QUESTION: " + userInput);
			// 调用LLM并打印响应
			System.out.println("\n>>> ASSISTANT: " +
					chatClient.prompt(userInput).call().content());

			// 关闭应用上下文
			context.close();
		};
	}

}
