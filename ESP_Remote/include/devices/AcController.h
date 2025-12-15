#pragma once

#include <IRac.h>
#include <IRremoteESP8266.h>
#include <IRsend.h>
#include <vector>

#if defined(__has_include)
#if __has_include(<IRremoteESP8266Version.h>)
#include <IRremoteESP8266Version.h>
#endif
#endif

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

#if defined(IRREMOTEESP8266_VERSION_MAJOR)
#if (IRREMOTEESP8266_VERSION_MAJOR > 2) ||                         \
    (IRREMOTEESP8266_VERSION_MAJOR == 2 &&                         \
     (IRREMOTEESP8266_VERSION_MINOR > 8 ||                         \
      (IRREMOTEESP8266_VERSION_MINOR == 8 &&                       \
       IRREMOTEESP8266_VERSION_PATCH >= 7)))
#define AC_CONTROLLER_HAS_REMOTE_MODEL_ENUM 1
#endif
#endif

#ifndef AC_CONTROLLER_HAS_REMOTE_MODEL_ENUM
#define AC_CONTROLLER_HAS_REMOTE_MODEL_ENUM 0
#endif

class AcController : public DeviceController {
 public:
  AcController(const char *nodeId, uint8_t irPin);

  const char *deviceType() const override { return "ac"; }
  const char *stateTopic() const override { return stateTopic_.c_str(); }
  void begin() override;
  void serializeState(JsonDocument &doc) const override;
  bool handleCommand(JsonObjectConst cmd, JsonDocument &stateDoc) override;
  bool learnKey(const String &key, decode_type_t protocol, uint64_t value,
                uint16_t nbits, const std::vector<uint8_t> &raw = {});

 private:
  struct IrModelConfig {
    const char *brand;
    const char *type;
    decode_type_t protocol;
#if AC_CONTROLLER_HAS_REMOTE_MODEL_ENUM
    stdAc::ac_remote_model_t model;
#else
    uint16_t model;
#endif
    uint16_t index;
  };

  static const IrModelConfig kModels[];
  static const IrModelConfig *findModel(const String &brand, const String &type,
                                        uint16_t index);
  static stdAc::opmode_t parseMode(const String &mode);
  static stdAc::fanspeed_t parseFan(const String &fan);
  static stdAc::swingv_t parseSwing(bool enabled);

  bool applyState(JsonDocument &stateDoc);

  struct LearnedCommand {
    String key;
    decode_type_t protocol;
    uint64_t value;
    uint16_t nbits;
    std::vector<uint8_t> raw;
  };

  void saveLearnedCommand(const String &key, decode_type_t protocol,
                          uint64_t value, uint16_t nbits,
                          const std::vector<uint8_t> &raw = {});
  bool sendLearnedKey(const String &key);

  String stateTopic_;
  uint8_t irPin_;
  IRac irAc_;
  IRsend irSend_;
  bool irReady_ = false;
  AcState state_;
  RemoteProfile remote_;
  std::vector<LearnedCommand> learnedCommands_;
};
