#include "IrLearner.h"

#include <IRutils.h>

IrLearner::IrLearner(uint8_t recvPin)
    : recvPin_(recvPin), receiver_(recvPin) {}

void IrLearner::begin() {
  receiver_.enableIRIn();
  ready_ = true;
  Serial.printf("[IR][LEARN] Ready (receiver pin=%u)\n", recvPin_);
}

void IrLearner::loop() {
  if (!ready_ || !learning_) {
    return;
  }

  if (receiver_.decode(&results_)) {
    if (results_.repeat) {
      emitResult(false, "repeat_signal");
    } else {
      String protocol = typeToString(results_.decode_type);
      if (protocol.isEmpty()) {
        protocol = "UNKNOWN";
      }
      String code = uint64ToString(results_.value, 16);
      code.toUpperCase();
      emitResult(true, nullptr, protocol, code, results_.bits);
    }
    receiver_.resume();
    reset();
    return;
  }

  if (millis() - startTime_ > kLearningTimeoutMs) {
    emitResult(false, "timeout");
    reset();
  }
}

bool IrLearner::startLearning(const String &device, const String &key,
                              String &errorOut) {
  if (!ready_) {
    errorOut = "receiver_not_ready";
    return false;
  }
  if (key.length() == 0) {
    errorOut = "missing_key";
    return false;
  }
  if (learning_) {
    errorOut = "busy";
    return false;
  }

  device_ = device;
  if (device_.length() == 0) {
    device_ = "GENERIC";
  }
  device_.toUpperCase();

  key_ = key;
  key_.toUpperCase();

  learning_ = true;
  startTime_ = millis();
  receiver_.resume();
  Serial.printf("[IR][LEARN] Waiting for %s/%s\n", device_.c_str(),
                key_.c_str());
  return true;
}

void IrLearner::emitResult(bool success, const char *error,
                           const String &protocol, const String &code,
                           uint16_t bits) {
  if (callback_ == nullptr) {
    return;
  }

  IrLearningResult result;
  result.success = success;
  result.device = device_;
  result.key = key_;
  if (success) {
    result.protocol = protocol;
    result.code = code;
    result.bits = bits;
  } else if (error != nullptr) {
    result.error = error;
  }

  callback_(result);
}

void IrLearner::reset() {
  learning_ = false;
  startTime_ = 0;
  key_.clear();
}