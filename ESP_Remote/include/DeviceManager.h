#pragma once

#include <Arduino.h>
#include <ArduinoJson.h>

class DeviceController {
 public:
  virtual ~DeviceController() = default;
  virtual const char *deviceType() const = 0;
  virtual const char *stateTopic() const = 0;
  virtual void begin() {}
  virtual void serializeState(JsonDocument &doc) const = 0;
  virtual bool handleCommand(JsonObjectConst cmd, JsonDocument &stateDoc) = 0;
};

class DeviceManager {
 public:
  void registerController(DeviceController &controller);
  void begin();
  DeviceController *find(const String &deviceType);
  size_t count() const { return controllerCount_; }
  DeviceController *at(size_t index) {
    if (index >= controllerCount_) return nullptr;
    return controllers_[index];
  }

 private:
  static constexpr size_t kMaxControllers = 6;
  DeviceController *controllers_[kMaxControllers] = {nullptr};
  size_t controllerCount_ = 0;
};
