#pragma once

#include <IRac.h>
#include <IRremoteESP8266.h>
#include <IRsend.h>

#include "Config.h"
#include "DeviceManager.h"

struct RemoteProfile {
  String brand;
  String type;
  uint16_t index = 0;
};

struct AcState {
  bool power = false;
  String mode = "cool";
  int temp = 24;
  String fan = "auto";
  bool swing = false;
};

class AcController : public DeviceController {
 public:
  AcController(const char *nodeId, uint8_t irPin, uint8_t relayPin);

  const char *deviceType() const override { return "ac"; }
  const char *stateTopic() const override { return stateTopic_.c_str(); }
  void begin() override;
  void serializeState(JsonDocument &doc) const override;
  bool handleCommand(JsonObjectConst cmd, JsonDocument &stateDoc) override;

 private:
  struct IrModelConfig {
    const char *brand;
    const char *type;
    decode_type_t protocol;
    uint16_t model;
  };

  static const IrModelConfig kModels[];
  static const IrModelConfig *findModel(const String &brand, const String &type,
                                        uint16_t index);
  static stdAc::opmode_t parseMode(const String &mode);
  static stdAc::fanspeed_t parseFan(const String &fan);
  static stdAc::swingv_t parseSwing(bool enabled);

  bool applyState(JsonDocument &stateDoc);

  String stateTopic_;
  uint8_t irPin_;
  uint8_t relayPin_;
  IRac irAc_;
  bool irReady_ = false;
  AcState state_;
  RemoteProfile remote_;
};