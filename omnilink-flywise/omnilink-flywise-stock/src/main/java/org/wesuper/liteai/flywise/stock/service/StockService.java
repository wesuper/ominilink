package org.wesuper.liteai.flywise.stock.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.context.annotation.Description;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;

/**
 * Stock service for retrieving real-time stock information from Eastmoney API.
 * This service provides functionality to fetch stock data including current price,
 * high/low prices, opening price, trading volume, and amount.
 *
 * @author Brian Xiadong
 */
@Service
public class StockService {
    private static final Logger logger = LoggerFactory.getLogger(StockService.class);
    private static final String BASE_URL = "https://push2.eastmoney.com/api/qt/stock/get";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final RestClient restClient;

    public StockService() {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record StockData(
            @JsonProperty("f43") Double currentPrice,    // Latest price (in cents)
            @JsonProperty("f44") Double highPrice,       // Highest price (in cents)
            @JsonProperty("f45") Double lowPrice,        // Lowest price (in cents)
            @JsonProperty("f46") Double openPrice,       // Opening price (in cents)
            @JsonProperty("f47") Double volume,          // Trading volume (in lots)
            @JsonProperty("f48") Double amount,          // Trading amount (in yuan)
            @JsonProperty("f57") String code,            // Stock code
            @JsonProperty("f58") String name) {          // Stock name
    }

    @JsonSerialize
    public record StockInfo(
            @JsonProperty("code") String code,
            @JsonProperty("name") String name,
            @JsonProperty("currentPrice") Double currentPrice,
            @JsonProperty("highPrice") Double highPrice,
            @JsonProperty("lowPrice") Double lowPrice,
            @JsonProperty("openPrice") Double openPrice,
            @JsonProperty("volume") Double volume,
            @JsonProperty("amount") Double amount
    ) implements Serializable {
    }

    @Tool(description = "Get real-time stock information for the specified stock code") // Updated annotation
    public StockInfo getStockInfo(String stockCode) { // Parameter description can be handled by name or request object
        try {
            // Validate stock code format
            if (!stockCode.matches("^[0-9]{6}$")) {
                throw new IllegalArgumentException("Stock code must be 6 digits");
            }

            logger.info("Fetching stock information for {}", stockCode);
            
            // Eastmoney API parameters
            String secid = stockCode.startsWith("6") ? "1." + stockCode : "0." + stockCode;
            
            String response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("secid", secid)
                            .queryParam("fields", "f43,f44,f45,f46,f47,f48,f57,f58")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            logger.info("Raw response: {}", response);

            JsonNode root = objectMapper.readTree(response);
            JsonNode data = root.path("data");
            
            if (data.isMissingNode()) {
                logger.warn("No stock data found");
                throw new IllegalArgumentException("No information found for stock code " + stockCode);
            }

            StockData stockData = objectMapper.treeToValue(data, StockData.class);
            logger.info("Parsed data: {}", stockData);

            if (stockData == null || stockData.name() == null) {
                throw new IllegalArgumentException("Invalid data format for stock code " + stockCode);
            }

            // Convert data format
            return new StockInfo(
                    stockCode,
                    stockData.name(),
                    stockData.currentPrice() / 100.0, // Convert to yuan
                    stockData.highPrice() / 100.0,    // Convert to yuan
                    stockData.lowPrice() / 100.0,     // Convert to yuan
                    stockData.openPrice() / 100.0,    // Convert to yuan
                    stockData.volume() / 10000.0,     // Convert to 10,000 lots
                    stockData.amount() / 100000000.0  // Convert to 100 million yuan
            );
        } catch (IllegalArgumentException e) {
            logger.error("Parameter error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Failed to get stock {} information: {}", stockCode, e.getMessage(), e);
            throw new RuntimeException("Failed to get stock " + stockCode + " information: " + e.getMessage());
        }
    }
}

