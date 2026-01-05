## Intro
Before deploying to physical hardware, the project was validated using a simulator. Wokwi was selected for its intuitive interface, web-based accessibility, and comprehensive free tier. Since the BME280 sensor is not natively supported in the Wokwi component library, a custom chip implementation was developed to facilitate simulation.

## Hardware list
- [ESP32](https://www.espressif.com/en/products/socs/esp32) microcontroller
- [BME280](https://www.bosch-sensortec.com/products/environmental-sensors/humidity-sensors-bme280/) temperature sensor

## Software
[MicroPython](https://micropython.org/)