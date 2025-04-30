/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.mcp.samples.filesystem;

import java.nio.file.Paths;
import java.time.Duration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

	@Bean
	public CommandLineRunner predefinedQuestions(
			ChatClient.Builder chatClientBuilder,
			ToolCallbackProvider tools,
			ConfigurableApplicationContext context) {
		return args -> {
			// 构建ChatClient并注入MCP工具
			var chatClient = chatClientBuilder
					.defaultTools(tools)
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
