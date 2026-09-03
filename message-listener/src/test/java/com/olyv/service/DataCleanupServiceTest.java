package com.olyv.service;

import com.olyv.repository.WeatherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataCleanupServiceTest {

    @Mock
    private WeatherRepository weatherRepository;

    private DataCleanupService dataCleanupService;

    @BeforeEach
    void setUp() {
        dataCleanupService = new DataCleanupService(weatherRepository);
    }

    @Test
    @DisplayName("Should calculate cutoff date and invoke repository cleanup")
    void removeOldData_InvokesRepositoryDeleteOlderThan() {
        LocalDateTime beforeExecution = LocalDateTime.now().minusDays(3).minusMinutes(1);

        dataCleanupService.removeOldData();

        ArgumentCaptor<LocalDateTime> cutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(weatherRepository).deleteOlderThan(cutoffCaptor.capture());

        LocalDateTime actualCutoff = cutoffCaptor.getValue();
        assertThat(actualCutoff).isAfterOrEqualTo(beforeExecution);
    }
}

