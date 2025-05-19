package org.springframework.ai.mcp.sample.client;

import java.util.Map;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.ClientMcpTransport;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.ListToolsResult;

/**
 * @author brianxiadong
 */

public class SampleClient {

    private final ClientMcpTransport transport;

    public SampleClient(ClientMcpTransport transport) {
        this.transport = transport;
    }

    public void run() {

        var client = McpClient.sync(this.transport).build();

        client.initialize();

        client.ping();

        // 列出并展示可用的工具
        ListToolsResult toolsList = client.listTools();
        System.out.println("可用工具 = " + toolsList);

        // 获取北京的天气预报
        CallToolResult weatherForecastResult = client.callTool(new CallToolRequest("getWeatherForecastByLocation",
                Map.of("latitude", "39.9042", "longitude", "116.4074")));
        System.out.println("北京天气预报: " + weatherForecastResult);

        // 获取北京的空气质量信息
        CallToolResult airQualityResult = client.callTool(new CallToolRequest("getAirQuality",
                Map.of("latitude", "39.9042", "longitude", "116.4074")));
        System.out.println("北京空气质量: " + airQualityResult);

        client.closeGracefully();
    }
}
