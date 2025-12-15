#pragma once

#include <Arduino.h>

// ==== Wi-Fi configuration ==================================================
// Đặt thông tin mạng Wi-Fi mà ESP32 sẽ kết nối.
// Wi‑Fi fallback credentials (optional). Leave empty if using BLE provisioning.
constexpr auto WIFI_SSID = "HA SON";
constexpr auto WIFI_PASSWORD = "24091996";

// ==== MQTT configuration ====================================================
// Nếu bỏ trống MQTT_HOST, ESP32 sẽ cố gắng tự động tìm broker bằng broadcast.
// Chỉ cần điền IP khi muốn ép kết nối tới một broker cụ thể.
constexpr auto MQTT_HOST = "";
constexpr uint16_t MQTT_PORT = 1883;

// Nếu broker yêu cầu username/password thì điền tại đây.
constexpr auto MQTT_USERNAME = "";

constexpr auto MQTT_PASSWORD = "";

// ID node phải trùng với cấu hình trong ứng dụng Android.
constexpr auto NODE_ID = "esp-remote";

// ==== MQTT auto discovery ===================================================
// Khi MQTT_HOST để trống, ESP32 sẽ gửi UDP broadcast tới cổng dưới đây và chờ
// phản hồi "MQTT://<ip>:<port>" để suy ra broker.
constexpr uint16_t MQTT_DISCOVERY_PORT = 4210;
constexpr unsigned long MQTT_DISCOVERY_TIMEOUT_MS = 5000UL;
constexpr auto MQTT_DISCOVERY_REQUEST = "DISCOVER_IOT_MQTT";

// ==== Optional hardware configuration ======================================
// Chân LED trạng thái (tuỳ board). Với ESP32 DevKit v1, LED onboard nằm tại GPIO2.
constexpr uint8_t STATUS_LED_PIN = 2;


constexpr uint8_t IR_RECEIVER_PIN = 27;       // Chân nhận tín hiệu IR để học lệnh
constexpr uint8_t IR_LED_PIN = 26;         // LED IR truyền lệnh
