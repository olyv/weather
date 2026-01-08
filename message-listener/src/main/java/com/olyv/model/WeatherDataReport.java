package com.olyv.model;

public record WeatherDataReport(int temperature, int humidity, int pressure) {

    @Override
    public String toString() {
        return String.format(
                "📊 *Weather Data Report*\n\n" +
                        "🌡️ Temperature: %d°C\n" +
                        "💧 Humidity: %d%%\n" +
                        "⏲️ Pressure: %d hPa",
                temperature, humidity, pressure
        );
    }
}
