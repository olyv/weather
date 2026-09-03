package com.olyv.service;

import com.olyv.repository.WeatherRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DataCleanupService {

    @Value("${cutoff.days:3}")
    private int days;

    private static final Logger log = LoggerFactory.getLogger(DataCleanupService.class);

    private final WeatherRepository weatherRepository;

    public DataCleanupService(WeatherRepository weatherRepository) {
        this.weatherRepository = weatherRepository;
    }

    @Scheduled(cron = "0 0 0,12 * * *")
    @Transactional
    public void removeOldData() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        log.info("🧹 Starting cleanup. Removing data older than: {}", cutoff);
        weatherRepository.deleteOlderThan(cutoff);
    }
}
