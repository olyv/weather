package com.olyv.service;

import com.olyv.model.WeatherEntity;
import com.olyv.repository.WeatherRepository;
import org.springframework.stereotype.Service;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WeatherAdvisorService {

    private final ChatClient chatClient;
    private final WeatherRepository repository;

    public WeatherAdvisorService(ChatClient.Builder chatClientBuilder, WeatherRepository repository) {
        this.chatClient = chatClientBuilder.build();
        this.repository = repository;
    }

    public String analyzeForUserQuery(String userQuestion) {
        List<WeatherEntity> recentData = repository.getMetricsForLastHours(4);

        // Format telemetry into a clean string for the LLM context
        String telemetryContext = recentData.stream()
                .map(m -> String.format("[%s] Temp: %d°C, Hum: %d, Press: %d hPa",
                        m.createdAt().toString(), m.temperature(), m.humidity(), m.pressure()))
                .collect(Collectors.joining("\n"));

        String systemPrompt = """
            You are a helpful local weather assistant analyzing balcony sensor telemetry (BME280).
            Analyze the provided historical readings (pay special attention to barometric pressure trends: 
            falling pressure often indicates rain or worsening weather).
            
            Answer the user's question concisely, directly addressing their planned activity.
            """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(u -> u.text("""
                Local Telemetry (Last 4 hours):
                {telemetry}
                
                User Question: {question}
                """)
                        .param("telemetry", telemetryContext)
                        .param("question", userQuestion))
                .call()
                .content();
    }
}