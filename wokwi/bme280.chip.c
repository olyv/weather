// SPDX-License-Identifier: MIT
#include <stdio.h>
#include <stdlib.h>
#include "wokwi-api.h"

#define BME280_I2C_ADDR 0x76

typedef struct {
  uint32_t temperature_attr;
  uint32_t humidity_attr;
  uint32_t pressure_attr;
  uint8_t  reg_addr;
} chip_state_t;

static bool on_i2c_connect(void *user_ctx, uint32_t address, bool read) {
  return (address == BME280_I2C_ADDR);
}

static bool on_i2c_write(void *user_ctx, uint8_t data) {
  chip_state_t *chip = (chip_state_t*)user_ctx;
  chip->reg_addr = data;
  return true;
}

static uint8_t on_i2c_read(void *user_ctx) {
  chip_state_t *chip = (chip_state_t*)user_ctx;

  // Read slider values as integers
  int32_t t_raw = (int32_t)attr_read_float(chip->temperature_attr);
  uint32_t p_raw = (uint32_t)attr_read_float(chip->pressure_attr);
  uint16_t h_raw = (uint16_t)attr_read_float(chip->humidity_attr);

  switch (chip->reg_addr++) {
    case 0xD0: return 0x60; // Chip ID
    
    // Pressure registers
    case 0xF7: return (p_raw >> 12) & 0xFF;
    case 0xF8: return (p_raw >> 4) & 0xFF;
    case 0xF9: return (p_raw << 4) & 0xF0;

    // Temperature registers
    case 0xFA: return (t_raw >> 12) & 0xFF;
    case 0xFB: return (t_raw >> 4) & 0xFF;
    case 0xFC: return (t_raw << 4) & 0xF0;

    // Humidity registers
    case 0xFD: return (h_raw >> 8) & 0xFF;
    case 0xFE: return h_raw & 0xFF;

    default: return 0x00;
  }
}

void chip_init() {
  chip_state_t *chip = calloc(1, sizeof(chip_state_t));
  chip->temperature_attr = attr_init_float("temperature", 25.0f);
  chip->humidity_attr    = attr_init_float("humidity", 50.0f);
  chip->pressure_attr    = attr_init_float("pressure", 1013.0f);

  const i2c_config_t i2c_config = {
    .address = BME280_I2C_ADDR,
    .scl = pin_init("SCL", INPUT),
    .sda = pin_init("SDA", INPUT),
    .connect = on_i2c_connect,
    .write = on_i2c_write,
    .read = on_i2c_read,
    .user_data = chip,
  };
  i2c_init(&i2c_config);
}