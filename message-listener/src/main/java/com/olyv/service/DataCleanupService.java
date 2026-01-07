package com.olyv.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DataCleanupService {

    @Value("${cutoff.days:3}")
    private int days;

    private static final Logger log = LoggerFactory.getLogger(DataCleanupService.class);

    private final JdbcTemplate jdbcTemplate;
    private static final String DELETE_SQL = "DELETE FROM weather_records WHERE timestamp < ?";

    public DataCleanupService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Scheduled(cron = "0 0 0,12 * * *")
    @Transactional
    public void removeOldData() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(days);
        log.info("🧹 Starting JdbcTemplate cleanup. Removing data older than: {}", cutoff);
        try {
            int deletedRows = jdbcTemplate.update(DELETE_SQL, cutoff);
            log.info("✅ Cleanup complete. Removed {} records using JdbcTemplate.", deletedRows);
        } catch (Exception e) {
            log.error("❌ SQL Cleanup failed: {}", e.getMessage());
        }
    }
}
