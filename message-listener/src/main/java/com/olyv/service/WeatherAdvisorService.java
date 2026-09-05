package com.olyv.service;

import com.olyv.model.WeatherEntity;
import com.olyv.repository.WeatherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WeatherAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(WeatherAdvisorService.class);
    private static final String SYSTEM_PROMPT = """
        You are a helpful local weather assistant analyzing balcony sensor telemetry (BME280).
        Analyze the provided historical readings (pay special attention to barometric pressure trends: 
        falling pressure often indicates rain or worsening weather).
        If no telemetry data is available, inform the user that live sensor metrics are missing and provide a cautious general advice.
        
        Answer the user's question concisely, directly addressing their planned activity.
        """;

    private final ChatClient chatClient;
    private final WeatherRepository repository;

    public WeatherAdvisorService(ChatClient.Builder chatClientBuilder, WeatherRepository repository) {
        this.chatClient = chatClientBuilder.build();
        this.repository = repository;
    }

    public String analyzeForUserQuery(String userQuestion) {
        log.info("Processing AI weather advisor query: '{}'", userQuestion);

        List<WeatherEntity> recentData;
        try {
            recentData = repository.getMetricsForLastHours(4);
            log.debug("Successfully retrieved {} weather telemetry records for the last 4 hours", recentData.size());
        } catch (DataAccessException e) {
            log.error("❌ Database failure while fetching weather telemetry: {}", e.getMessage(), e);
            recentData = Collections.emptyList();
        }

        String telemetryContext;
        if (recentData.isEmpty()) {
            log.warn("⚠️ No weather telemetry data available for the last 4 hours");
            telemetryContext = "No recent telemetry data available from the sensor DB.";
        } else {
            telemetryContext = recentData.stream()
                    .map(m -> String.format("[%s] Temp: %d°C, Hum: %d%%, Press: %d hPa",
                            m.createdAt(), m.temperature(), m.humidity(), m.pressure()))
                    .collect(Collectors.joining("\n"));
        }

        try {
            log.debug("Submitting telemetry context and user query to Gemini LLM...");
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(u -> u.text("""
                        Local Telemetry (Last 4 hours):
                        {telemetry}
                        
                        User Question: {question}
                        """)
                            .param("telemetry", telemetryContext)
                            .param("question", userQuestion))
                    .call()
                    .content();

            log.info("Successfully received response from Gemini LLM");
            return response;

        } catch (Exception e) {
            log.error("❌ Gemini LLM invocation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate AI weather analysis", e);
        }
    }
}