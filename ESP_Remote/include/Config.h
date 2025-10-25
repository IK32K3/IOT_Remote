#pragma once

#include <Arduino.h>

// ==== Wi-Fi configuration ==================================================
// Đặt thông tin mạng Wi-Fi mà ESP32 sẽ kết nối.
constexpr auto WIFI_SSID = "YOUR_WIFI_SSID";
constexpr auto WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

// ==== MQTT configuration ====================================================
// Host của Mosquitto broker. Khi chạy cùng máy với Android emulator, hãy
// dùng địa chỉ IP của máy tính (ví dụ 192.168.1.10). "localhost" chỉ đúng khi
// broker nằm trực tiếp trên ESP32.
constexpr auto MQTT_HOST = "192.168.1.10";  // Thay bằng IP của máy chạy mosquitto
constexpr uint16_t MQTT_PORT = 1883;

#warning "Update WIFI_SSID, WIFI_PASSWORD, and MQTT_HOST before building"

// Nếu broker yêu cầu username/password thì điền tại đây.
constexpr auto MQTT_USERNAME = "";
constexpr auto MQTT_PASSWORD = "";

// ID node phải trùng với cấu hình trong ứng dụng Android.
constexpr auto NODE_ID = "esp-bedroom";

// ==== Optional hardware configuration ======================================
// Chân LED trạng thái (tuỳ board). Với ESP32 DevKit v1, LED onboard nằm tại GPIO2.
constexpr uint8_t STATUS_LED_PIN = 2;


constexpr uint8_t AC_POWER_RELAY_PIN = 26;   // relay điều khiển nguồn điều hoà
constexpr uint8_t AC_IR_LED_PIN = 4;         // LED IR truyền lệnh