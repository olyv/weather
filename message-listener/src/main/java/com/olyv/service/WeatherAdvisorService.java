package com.olyv.service;

import com.olyv.model.WeatherEntity;
import com.olyv.repository.WeatherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class WeatherAdvisorService {

    private static final Logger log = LoggerFactory.getLogger(WeatherAdvisorService.class);
    private static final Duration EXPIRATION_TTL = Duration.ofMinutes(30);
    private static final String CHAT_MEMORY_CONVERSATION_ID_KEY = "chat_memory_conversation_id";

    private static final String SYSTEM_PROMPT = """
        You are a helpful local weather assistant analyzing balcony sensor telemetry (BME280).
        Analyze the provided historical readings (pay special attention to barometric pressure trends: 
        falling pressure often indicates rain or worsening weather).
        If no telemetry data is available, inform the user that live sensor metrics are missing and provide a cautious general advice.
        
        Answer the user's question concisely, directly addressing their planned activity.
        """;

    private final ChatClient chatClient;
    private final WeatherRepository repository;
    private final ChatMemory chatMemory;
    private final Map<String, Instant> lastAccessMap = new ConcurrentHashMap<>();

    public WeatherAdvisorService(ChatClient.Builder chatClientBuilder, WeatherRepository repository) {
        this.chatMemory = MessageWindowChatMemory.builder().maxMessages(6).build();
        this.chatClient = chatClientBuilder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
        this.repository = repository;
    }

    public String analyzeForUserQuery(String userQuestion) {
        return analyzeForUserQuery("default", userQuestion);
    }

    public String analyzeForUserQuery(String conversationId, String userQuestion) {
        log.info("Processing AI weather advisor query for conversationId '{}': '{}'", conversationId, userQuestion);
        checkAndEvictExpiredSession(conversationId);

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

        String dynamicSystemPrompt = SYSTEM_PROMPT + "\n\nLocal Telemetry (Last 4 hours):\n" + telemetryContext;

        try {
            log.debug("Submitting prompt to Gemini LLM with conversationId {}...", conversationId);
            String response = chatClient.prompt()
                    .system(dynamicSystemPrompt)
                    .user(userQuestion)
                    .advisors(a -> a.param(CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                    .call()
                    .content();

            log.info("Successfully received response from Gemini LLM for conversationId {}", conversationId);
            return response;

        } catch (Exception e) {
            log.error("❌ Gemini LLM invocation failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate AI weather analysis", e);
        }
    }

    private void checkAndEvictExpiredSession(String conversationId) {
        Instant lastAccess = lastAccessMap.get(conversationId);
        Instant now = Instant.now();
        if (lastAccess != null && Duration.between(lastAccess, now).compareTo(EXPIRATION_TTL) > 0) {
            log.info("⏰ Chat session expired after 30 minutes of inactivity for conversationId: {}. Clearing memory.", conversationId);
            chatMemory.clear(conversationId);
        }
        lastAccessMap.put(conversationId, now);
    }
}