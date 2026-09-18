package com.olyv.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import org.springframework.beans.factory.annotation.Value;

import java.net.InetSocketAddress;
import java.net.Socket;

@Component
public class MqttHealthIndicator implements HealthIndicator {

    @Value("${MQTT_HOST}")
    private String host;

    @Value("${MQTT_PORT:1883}")
    private int port;

    @Override
    public Health health() {
        try (Socket socket = new Socket()) {
            // Ping the broker to ensure it is reachable on the network
            socket.connect(new InetSocketAddress(host, port), 2000);
            return Health.up()
                    .withDetail("broker", host + ":" + port)
                    .withDetail("status", "Reachable")
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("reason", "Cannot reach MQTT broker: " + e.getMessage())
                    .build();
        }
    }
}