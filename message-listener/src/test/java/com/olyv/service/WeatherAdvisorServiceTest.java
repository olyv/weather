package com.olyv.service;

import com.olyv.model.WeatherEntity;
import com.olyv.repository.WeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.dao.DataRetrievalFailureException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeatherAdvisorServiceTest {

    @Mock
    private WeatherRepository weatherRepository;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient.Builder chatClientBuilder;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private WeatherAdvisorService weatherAdvisorService;

    @BeforeEach
    void setUp() {
        lenient().when(chatClientBuilder.defaultAdvisors((Advisor[]) any())).thenReturn(chatClientBuilder);
        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);
        weatherAdvisorService = new WeatherAdvisorService(chatClientBuilder, weatherRepository);
    }

    @Test
    @DisplayName("Should query telemetry and return response from LLM using conversationId")
    void analyzeForUserQuery_SuccessWithTelemetry() {
        WeatherEntity entity = new WeatherEntity(1L, LocalDateTime.now(), 20, 50, 1012);
        when(weatherRepository.getMetricsForLastHours(4)).thenReturn(List.of(entity));
        when(chatClient.prompt().system(anyString()).user(anyString()).advisors(any(Consumer.class)).call().content())
                .thenReturn("Weather looks great!");

        String response = weatherAdvisorService.analyzeForUserQuery("chat123", "Can I go running?");

        assertThat(response).isEqualTo("Weather looks great!");
    }

    @Test
    @DisplayName("Should handle database exception gracefully and provide fallback context to LLM")
    void analyzeForUserQuery_DatabaseFailure_UsesFallbackTelemetry() {
        when(weatherRepository.getMetricsForLastHours(anyInt()))
                .thenThrow(new DataRetrievalFailureException("DB connection error"));
        when(chatClient.prompt().system(anyString()).user(anyString()).advisors(any(Consumer.class)).call().content())
                .thenReturn("General advice: bring a jacket.");

        String response = weatherAdvisorService.analyzeForUserQuery("chat123", "Should I wear a coat?");

        assertThat(response).isEqualTo("General advice: bring a jacket.");
    }

    @Test
    @DisplayName("Should throw RuntimeException when LLM call fails")
    void analyzeForUserQuery_LLMFailure_ThrowsRuntimeException() {
        when(weatherRepository.getMetricsForLastHours(4)).thenReturn(List.of());
        when(chatClient.prompt().system(anyString()).user(anyString()).advisors(any(Consumer.class)).call()).thenThrow(new RuntimeException("LLM error"));

        assertThatThrownBy(() -> weatherAdvisorService.analyzeForUserQuery("chat123", "Any news?"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to generate AI weather analysis");
    }
}
