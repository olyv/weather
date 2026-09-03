package com.olyv.model;

public record WeatherMqttEvent(
        int temperature,
        int humidity,
        int pressure) {

    @Override
    public String toString() {
        return String.format(
                """
                        📊 **Weather Mqtt Event**

                        🌡️ Temperature: %d°C
                        💧 Humidity: %d%%
                        ⏲️ Pressure: %d hPa""",
                temperature, humidity, pressure
        );
    }
}
