#include "DeviceManager.h"

void DeviceManager::registerController(DeviceController &controller) {
  if (controllerCount_ >= kMaxControllers) {
    Serial.println(F("[DEVICE] Too many controllers registered"));
    return;
  }
  controllers_[controllerCount_++] = &controller;
}

void DeviceManager::begin() {
  for (size_t i = 0; i < controllerCount_; ++i) {
    if (controllers_[i] != nullptr) {
      controllers_[i]->begin();
    }
  }
}

DeviceController *DeviceManager::find(const String &deviceType) {
  for (size_t i = 0; i < controllerCount_; ++i) {
    if (controllers_[i] == nullptr) continue;
    if (deviceType.equalsIgnoreCase(controllers_[i]->deviceType())) {
      return controllers_[i];
    }
  }
  return nullptr;
}