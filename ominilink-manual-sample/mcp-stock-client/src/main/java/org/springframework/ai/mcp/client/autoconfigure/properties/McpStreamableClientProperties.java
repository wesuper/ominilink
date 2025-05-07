package org.springframework.ai.mcp.client.autoconfigure.properties;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(McpStreamableClientProperties.CONFIG_PREFIX)
public class McpStreamableClientProperties {

	public static final String CONFIG_PREFIX = "spring.ai.mcp.client.streamable";

	/**
	 * Parameters for configuring an SSE connection to an MCP server.
	 *
	 * @param url the URL endpoint for SSE communication with the MCP server
	 */
	public record StreamableParameters(String url) {
	}

	/**
	 * Map of named SSE connection configurations.
	 * <p>
	 * The key represents the connection name, and the value contains the SSE parameters
	 * for that connection.
	 */
	private final Map<String, StreamableParameters> connections = new HashMap<>();

	/**
	 * Returns the map of configured SSE connections.
	 * @return map of connection names to their SSE parameters
	 */
	public Map<String, StreamableParameters> getConnections() {
		return this.connections;
	}

}
