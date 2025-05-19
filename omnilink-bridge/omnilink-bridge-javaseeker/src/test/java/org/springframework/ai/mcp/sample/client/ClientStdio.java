package org.springframework.ai.mcp.sample.client;

import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.ServerParameters;
import io.modelcontextprotocol.client.transport.StdioClientTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

/**
 * 使用stdio传输，MCP服务器由客户端自动启动
 * 但你需要先构建服务器jar:
 *
 * <pre>
 * ./mvnw clean install -DskipTests
 * </pre>
 */
public class ClientStdio {

    public static void main(String[] args) {

        var stdioParams = ServerParameters.builder("java")
                .args("-jar",
                        "-Dspring.ai.mcp.server.stdio=true",
                        "-Dspring.main.web-application-type=none",
                        "-Dlogging.pattern.console=",
                        "spring-ai-alibaba-mcp-example/starter-example/server/starter-stdio-server/target/mcp-stdio-server-exmaple-0.0.1-SNAPSHOT.jar")
                .build();

        var transport = new StdioClientTransport(stdioParams);
        var client = McpClient.sync(transport).build();

        client.initialize();

        // 列出并展示可用的工具
        ListToolsResult toolsList = client.listTools();
        System.out.println("可用工具 = " + toolsList);

        // 获取北京的天气预报
        CallToolResult weatherForecastResult = client.callTool(new CallToolRequest("getWeatherForecastByLocation",
                Map.of("latitude", "39.9042", "longitude", "116.4074")));
        System.out.println("北京天气预报: " + weatherForecastResult);

        // 获取北京的空气质量信息（模拟数据）
        CallToolResult airQualityResult = client.callTool(new CallToolRequest("getAirQuality",
                Map.of("latitude", "39.9042", "longitude", "116.4074")));
        System.out.println("北京空气质量: " + airQualityResult);

        client.closeGracefully();
    }
}
