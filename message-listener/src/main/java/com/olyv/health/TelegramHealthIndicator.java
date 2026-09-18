package com.olyv.health;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
public class TelegramHealthIndicator implements HealthIndicator {

    private final RestClient restClient = RestClient.create();

    @Value("${telegram.bot.token}")
    private String botToken;

    @Override
    public Health health() {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/getMe";
            Map<?, ?> response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(Map.class);

            if (response != null && Boolean.TRUE.equals(response.get("ok"))) {
                return Health.up()
                        .withDetail("telegramApi", "Reachable")
                        .build();
            }
            return Health.down()
                    .withDetail("reason", "Telegram API returned non-OK response")
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("reason", "Unable to reach Telegram API: " + e.getMessage())
                    .build();
        }
    }
}
