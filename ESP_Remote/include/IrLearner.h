#pragma once

#include <Arduino.h>
#include <IRrecv.h>
#include <IRremoteESP8266.h>
#include <vector>

struct IrLearningResult {
  bool success = false;
  String device;
  String key;
  String protocol;
  String code;
  uint16_t bits = 0;
  String error;
  std::vector<uint8_t> raw;  // dữ liệu đầy đủ để gửi lại gói >64 bit
};

class IrLearner {
 public:
  using ResultCallback = void (*)(const IrLearningResult &result);

  explicit IrLearner(uint8_t recvPin);

  void begin();
  void loop();

  bool startLearning(const String &device, const String &key, String &errorOut);
  bool isLearning() const { return learning_; }

  void setResultCallback(ResultCallback cb) { callback_ = cb; }

 private:
  void emitResult(bool success, const char *error = nullptr,
                  const String &protocol = String(),
                  const String &code = String(), uint16_t bits = 0,
                  const uint8_t *raw = nullptr, uint16_t nbytes = 0);
  void reset();

  static constexpr unsigned long kLearningTimeoutMs = 15000UL;
  static constexpr uint16_t kCaptureBuffer = 2000;  // lớn hơn để giữ trọn gói AC
  static constexpr uint8_t kTimeoutMs = 50;         // giữ mặc định 50 ms

  uint8_t recvPin_;
  IRrecv receiver_;
  decode_results results_{};
  bool ready_ = false;
  bool learning_ = false;
  unsigned long startTime_ = 0;
  String device_;
  String key_;
  ResultCallback callback_ = nullptr;
};
